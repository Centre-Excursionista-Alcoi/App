package org.centrexcursionistalcoi.app.pdf

import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.Date
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.apache.pdfbox.Loader
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.text.PDFTextStripper
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.utils.toUuid

class TestPdfSigning {
    private lateinit var keysDir: File

    @BeforeTest
    fun setUp() {
        keysDir = Files.createTempDirectory("cea-keys").toFile()
    }

    @AfterTest
    fun tearDown() {
        PdfSigner.reset()
        keysDir.deleteRecursively()
    }

    private fun pdf(): ByteArray {
        val now = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
        val memory = Memory(
            id = "9b57a238-6a3a-4a1a-9f4a-6f4b1e6f5a11".toUuid(),
            place = null,
            members = emptyList(),
            externalUsers = null,
            text = "Una **memòria**.",
            sport = null,
            department = null,
            attachments = emptyList(),
            submittedBy = FakeUser.SUB,
            from = now,
            to = now,
            pdf = null,
            lending = null,
        )
        return ByteArrayOutputStream().use { output ->
            PdfGeneratorService.generateMemoryPdf(
                memory = memory.referenced(listOf(FakeUser.data()), emptyList(), emptyList()),
                itemsUsed = emptyList(),
                submittedBy = "Admin User",
                photoProvider = { error("No photos") },
                outputStream = output,
            )
            output.toByteArray()
        }
    }

    /** The PDF's signature, and whether its contents verify against the certificate it carries. */
    private class Verification(val signer: X509CertificateHolder, val valid: Boolean, val coversWholeFile: Boolean)

    private fun verify(pdf: ByteArray): Verification {
        val signature = Loader.loadPDF(pdf).use { it.signatureDictionaries.single() }
        val byteRange = signature.byteRange
        // The signature is padded with zeros to fill the space reserved for it: read just the signature
        val contents = ContentInfo.getInstance(ASN1InputStream(signature.getContents(pdf)).readObject())
        val cms = CMSSignedData(CMSProcessableByteArray(signature.getSignedContent(pdf)), contents)
        val signerInfo: SignerInformation = cms.signerInfos.signers.single()
        val signer = cms.certificates.getMatches(null).single { signerInfo.sid.match(it) }
        val valid = runCatching { signerInfo.verify(JcaSimpleSignerInfoVerifierBuilder().build(signer)) }.getOrDefault(false)
        // Everything but the signature itself, which is between both ranges
        val coversWholeFile = byteRange[0] == 0 && byteRange[2] + byteRange[3] == pdf.size
        return Verification(signer, valid, coversWholeFile)
    }

    @Test
    fun test_withoutInit_unsigned() {
        Loader.loadPDF(pdf()).use { assertTrue(it.signatureDictionaries.isEmpty()) }
    }

    @Test
    fun test_init_generatesSelfSignedCertificate_andReusesIt() {
        PdfSigner.init(keysDir, CharArray(0))
        val certificate = assertNotNull(PdfSigner.certificate)
        assertTrue(File(keysDir, PdfSigner.KEYSTORE_FILE).exists())
        assertContains(certificate.subjectX500Principal.name, "CN=Centre Excursionista d'Alcoi")
        assertEquals(certificate.subjectX500Principal, certificate.issuerX500Principal, "Not self-signed")

        PdfSigner.init(keysDir, CharArray(0))
        assertEquals(certificate.serialNumber, PdfSigner.certificate!!.serialNumber, "A new certificate was generated")
    }

    @Test
    fun test_signedPdf_verifies_andIsCertified() {
        PdfSigner.init(keysDir, CharArray(0))
        val pdf = pdf()

        val verification = verify(pdf)
        assertTrue(verification.valid, "Signature doesn't verify")
        assertTrue(verification.coversWholeFile, "Signature doesn't cover the whole file")
        assertEquals(PdfSigner.certificate!!.serialNumber, verification.signer.serialNumber)

        Loader.loadPDF(pdf).use { document ->
            // A certification signature, allowing no changes
            val docMdp = document.documentCatalog.cosObject.getCOSDictionary(COSName.PERMS)?.getCOSDictionary(COSName.DOCMDP)
            assertNotNull(docMdp, "Not a certification signature")
            val reference = (docMdp.getDictionaryObject(COSName.getPDFName("Reference")) as COSArray).getObject(0) as COSDictionary
            assertEquals(1, reference.getCOSDictionary(COSName.getPDFName("TransformParams"))!!.getInt(COSName.P))
            // Still the same document
            assertContains(PDFTextStripper().getText(document), "memòria")
        }
    }

    @Test
    fun test_changedPdf_failsVerification() {
        PdfSigner.init(keysDir, CharArray(0))
        val pdf = pdf()
        val byteRange = Loader.loadPDF(pdf).use { it.signatureDictionaries.single().byteRange }

        // Any byte the signature covers
        val changed = pdf.copyOf().also { it[byteRange[1] / 2] = (it[byteRange[1] / 2] + 1).toByte() }

        assertFalse(verify(changed).valid, "A changed PDF still verifies")
    }

    @Test
    fun test_existingKeystore_isUsed() {
        // Like one from a certificate authority: its own password, key type and entry name
        val password = "secret".toCharArray()
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val name = X500Name("CN=Electronic seal, O=Centre Excursionista d'Alcoi, C=ES")
        val now = Date()
        val certificate = JcaX509CertificateConverter().getCertificate(
            JcaX509v3CertificateBuilder(name, BigInteger.TEN, now, Date(now.time + 86_400_000), name, keyPair.public)
                .build(JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private))
        )
        KeyStore.getInstance("PKCS12").apply {
            load(null, password)
            setKeyEntry("seal", keyPair.private, password, arrayOf(certificate))
            File(keysDir, PdfSigner.KEYSTORE_FILE).outputStream().use { store(it, password) }
        }

        PdfSigner.init(keysDir, password)

        assertEquals(certificate, PdfSigner.certificate)
        val verification = verify(pdf())
        assertTrue(verification.valid, "Signature doesn't verify")
        assertEquals(BigInteger.TEN, verification.signer.serialNumber)
    }
}
