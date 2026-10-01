package org.centrexcursionistalcoi.app.verification

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.database.table.DocumentVerifications
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

enum class DocumentType { MEMORY }

/**
 * A document the server generated, as recorded by [DocumentVerification.record].
 * @property code The verification code printed on it, formatted ([DocumentVerification.format]).
 */
data class VerifiedDocument(
    val code: String,
    val sha256: String,
    val type: DocumentType,
    val subject: Uuid?,
    val generatedAt: Instant,
)

/**
 * Lets anyone check that a document the server generated is authentic, and unchanged: each one gets a random code,
 * printed on it with a link to the `/verify` page, which is recorded with the SHA-256 of the document's file.
 *
 * The code can't be the file's own hash: it's part of the file.
 */
object DocumentVerification {
    /** Crockford's Base32: no I, L, O or U, so codes are hard to misread or mistype. */
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    /** 60 random bits: there's no guessing one. */
    private const val LENGTH = 12

    private val random = SecureRandom()

    /** A new, random code, unformatted. */
    fun newCode(): String = buildString { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /** [code] in groups of four, as printed: `ABCD-EFGH-JKMN`. */
    fun format(code: String): String = code.chunked(4).joinToString("-")

    /**
     * The code someone typed, as stored: without separators or case, and with the characters Crockford's Base32
     * doesn't use replaced with the ones they're likely mistaken for. `null` if it can't be a code.
     */
    fun normalize(input: String): String? {
        val code = input.uppercase()
            .filterNot { it == '-' || it.isWhitespace() }
            .map { char ->
                when (char) {
                    'O' -> '0'
                    'I', 'L' -> '1'
                    else -> char
                }
            }
            .joinToString("")
        return code.takeIf { it.length == LENGTH && it.all(ALPHABET::contains) }
    }

    /** The page that verifies the document with [code]. */
    fun url(code: String): String = "${AppLinks.baseUrl}/verify?code=${format(code)}"

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    /** Records that the document generated with [code] is [bytes]. */
    context(_: JdbcTransaction)
    fun record(code: String, bytes: ByteArray, type: DocumentType, subject: Uuid?) {
        DocumentVerifications.insert {
            it[id] = code
            it[sha256] = sha256(bytes)
            it[this.type] = type
            it[this.subject] = subject
        }
    }

    context(_: JdbcTransaction)
    fun find(code: String): VerifiedDocument? =
        DocumentVerifications.selectAll().where { DocumentVerifications.id eq code }.singleOrNull()?.toDocument()

    /** The documents whose file is the one with [sha256]: usually one, never none if it was generated here. */
    context(_: JdbcTransaction)
    fun findBySha256(sha256: String): List<VerifiedDocument> =
        DocumentVerifications.selectAll().where { DocumentVerifications.sha256 eq sha256 }.map { it.toDocument() }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toDocument() = VerifiedDocument(
        code = format(this[DocumentVerifications.id].value),
        sha256 = this[DocumentVerifications.sha256],
        type = this[DocumentVerifications.type],
        subject = this[DocumentVerifications.subject],
        generatedAt = this[DocumentVerifications.generatedAt],
    )
}
