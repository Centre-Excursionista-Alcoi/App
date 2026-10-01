package org.centrexcursionistalcoi.app.pdf

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Calendar
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaInstant
import org.apache.pdfbox.Loader
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.jetbrains.annotations.VisibleForTesting
import org.slf4j.LoggerFactory

/**
 * Signs the PDFs the server generates, so anyone can check they come from it and haven't been changed since. It's a
 * certification signature: viewers mark the document as certified, and any change to it invalidates the signature.
 *
 * The key and its certificate are in a PKCS#12 keystore, [KEYSTORE_FILE] in the keys directory, with its password in
 * `PDF_SIGNING_PASSWORD` (none if unset). If there's none yet, a self-signed certificate is generated: changes are
 * still detected, but viewers can't tell who signed unless they trust the certificate. To sign with a certificate
 * they do trust (e.g. an electronic seal issued to the club), replace the keystore, and set its password.
 *
 * Until [init] is called (e.g. in tests), PDFs are saved unsigned.
 */
object PdfSigner {
    const val KEYSTORE_FILE = "pdf-signing.p12"

    private const val SIGNER_NAME = "Centre Excursionista d'Alcoi"

    private val logger = LoggerFactory.getLogger(PdfSigner::class.java)

    private class Credentials(val privateKey: PrivateKey, val chain: List<X509Certificate>)

    private var credentials: Credentials? = null

    /** The certificate PDFs are signed with, once [init] has been called. */
    val certificate: X509Certificate? get() = credentials?.chain?.first()

    /**
     * Loads the signing key and certificate from [keysDir], generating a self-signed one if there's none.
     */
    fun init(keysDir: File, password: CharArray = System.getenv("PDF_SIGNING_PASSWORD")?.toCharArray() ?: CharArray(0)) {
        val file = File(keysDir, KEYSTORE_FILE)
        if (!file.exists()) {
            logger.info("PDF signing certificate not found, generating a self-signed one...")
            keysDir.mkdirs()
            generateKeyStore(password).writeOwnerOnly(file, password)
        }
        val keyStore = KeyStore.getInstance("PKCS12").apply { file.inputStream().use { load(it, password) } }
        // Keystores from certificate authorities name their key in their own way
        val alias = keyStore.aliases().toList().first { keyStore.isKeyEntry(it) }
        credentials = Credentials(
            privateKey = keyStore.getKey(alias, password) as PrivateKey,
            chain = keyStore.getCertificateChain(alias).map { it as X509Certificate },
        )
        logger.info("PDFs will be signed as ${certificate!!.subjectX500Principal.name}")
    }

    @VisibleForTesting
    fun reset() {
        credentials = null
    }

    /**
     * Saves [document] to [output], signed if [init] has been called.
     */
    fun save(document: PDDocument, output: OutputStream) {
        val credentials = credentials ?: return document.save(output)

        // The signature is added to the end of the document as saved, covering everything before it
        val unsigned = ByteArrayOutputStream().also { document.save(it) }.toByteArray()
        Loader.loadPDF(unsigned).use { saved ->
            val signature = PDSignature().apply {
                setFilter(PDSignature.FILTER_ADOBE_PPKLITE)
                setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED)
                name = SIGNER_NAME
                location = "Alcoi"
                reason = "Document generat pel Centre Excursionista d'Alcoi"
                signDate = Calendar.getInstance()
            }
            certify(saved, signature)
            saved.addSignature(signature) { content -> sign(content, credentials) }
            saved.saveIncremental(output)
        }
    }

    /**
     * Makes [signature] certify [document], allowing no changes after it (DocMDP, with permissions `1`).
     */
    private fun certify(document: PDDocument, signature: PDSignature) {
        // As in PDFBox's examples (SigUtils.setMDPPermission). The incremental save only writes what's marked updated.
        val transformParameters = COSDictionary().apply {
            setItem(COSName.TYPE, COSName.getPDFName("TransformParams"))
            setInt(COSName.P, 1)
            setName(COSName.V, "1.2")
            isNeedToBeUpdated = true
        }
        val reference = COSDictionary().apply {
            setItem(COSName.TYPE, COSName.getPDFName("SigRef"))
            setItem(COSName.getPDFName("TransformMethod"), COSName.DOCMDP)
            // Readers before PDF 2.0 only know MD5 and SHA1 here. It's not the signature's digest (SHA-256).
            setItem(COSName.getPDFName("DigestMethod"), COSName.getPDFName("SHA1"))
            setItem(COSName.getPDFName("TransformParams"), transformParameters)
            isNeedToBeUpdated = true
        }
        signature.cosObject.setItem(COSName.getPDFName("Reference"), COSArray().apply {
            add(reference)
            isNeedToBeUpdated = true
        })

        val catalog = document.documentCatalog.cosObject
        catalog.setItem(COSName.PERMS, COSDictionary().apply {
            setItem(COSName.DOCMDP, signature)
            isNeedToBeUpdated = true
        })
        catalog.isNeedToBeUpdated = true
    }

    /** The detached CMS signature of [content], with the certificate chain, as PDF signatures are. */
    private fun sign(content: InputStream, credentials: Credentials): ByteArray {
        val algorithm = if (credentials.privateKey.algorithm == "EC") "SHA256withECDSA" else "SHA256withRSA"
        val signer = JcaContentSignerBuilder(algorithm).build(credentials.privateKey)
        val generator = CMSSignedDataGenerator().apply {
            addSignerInfoGenerator(
                JcaSignerInfoGeneratorBuilder(JcaDigestCalculatorProviderBuilder().build())
                    .build(signer, credentials.chain.first())
            )
            addCertificates(JcaCertStore(credentials.chain))
        }
        return generator.generate(CMSProcessableByteArray(content.readAllBytes()), false).encoded
    }

    /** A keystore with a new key, and a self-signed certificate for it, valid for 10 years. */
    private fun generateKeyStore(password: CharArray): KeyStore {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(3072) }.generateKeyPair()
        val name = X500NameBuilder(BCStyle.INSTANCE)
            .addRDN(BCStyle.CN, SIGNER_NAME)
            .addRDN(BCStyle.O, SIGNER_NAME)
            .addRDN(BCStyle.L, "Alcoi")
            .addRDN(BCStyle.C, "ES")
            .build()
        val now = Clock.System.now()
        val holder = JcaX509v3CertificateBuilder(
            name,
            BigInteger(128, SecureRandom()),
            java.util.Date.from(now.toJavaInstant()),
            java.util.Date.from((now + (10 * 365).days).toJavaInstant()),
            name,
            keyPair.public,
        )
            .addExtension(Extension.basicConstraints, true, BasicConstraints(false))
            .addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature or KeyUsage.nonRepudiation))
            .build(JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private))
        val certificate = JcaX509CertificateConverter().getCertificate(holder)
        return KeyStore.getInstance("PKCS12").apply {
            load(null, password)
            setKeyEntry("pdf-signing", keyPair.private, password, arrayOf(certificate))
        }
    }

    private fun KeyStore.writeOwnerOnly(file: File, password: CharArray) {
        file.outputStream().use { store(it, password) }
        file.setReadable(false, false)
        file.setWritable(false, false)
        file.setReadable(true, true)
        file.setWritable(true, true)
    }
}
