package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.verification.DocumentType
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * The documents the server has generated, by the verification code printed on them (see `DocumentVerification`).
 *
 * On purpose, nothing references the document's file or its subject: a copy someone kept must still be verifiable
 * after the document has been generated again, or its subject deleted.
 */
object DocumentVerifications : IdTable<String>("document_verifications") {
    override val id = varchar("code", 12).entityId()
    override val primaryKey = PrimaryKey(id)

    /** SHA-256 of the document's file, as sent to whoever downloads it, in hexadecimal. */
    val sha256 = varchar("sha256", 64)
    val type = enumerationByName<DocumentType>("type", 32)
    /** What the document is about, e.g. the memory's id. */
    val subject = uuid("subject").nullable()
    val generatedAt = timestamp("generated_at").defaultExpression(DatabaseNowExpression)

    init {
        index(false, sha256)
    }
}
