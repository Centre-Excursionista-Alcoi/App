package org.centrexcursionistalcoi.app.database.migrations

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.json
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcConnectionImpl
import java.util.UUID

/**
 * Migration V6:
 * - Memories used to be stored as a single JSON blob (`memory` column) plus a `memoryPdf` reference directly on the
 *   `lendings` table.
 * - Memories are now their own entity (`memories` table), with their members and attached files stored in proper
 *   many-to-many tables (`memories_members`, `memories_files`) instead of being embedded in the JSON blob.
 * - Memories also become optionally linked to a lending (rather than being embedded in it), so that memories can
 *   exist without a lending.
 * - This migration copies every non-null `memory` blob (and its associated `memoryPdf`) into the new tables, then
 *   drops the old columns.
 */
object V6 : DatabaseMigration {
    override val from: Int = 5
    override val to: Int = 6

    context(tr: JdbcTransaction)
    override fun migrate() {
        val conn = (tr.connection as JdbcConnectionImpl).connection

        data class OldMemory(
            val lendingId: UUID,
            val lendingUserSub: String,
            val json: String,
            val pdfId: UUID?,
        )

        val oldMemories = mutableListOf<OldMemory>()
        conn.createStatement().use { statement ->
            statement.executeQuery("""SELECT id, "userSub", memory, "memoryPdf" FROM lendings WHERE memory IS NOT NULL""").use { rs ->
                while (rs.next()) {
                    oldMemories += OldMemory(
                        lendingId = UUID.fromString(rs.getString("id")),
                        lendingUserSub = rs.getString("userSub"),
                        json = rs.getString("memory"),
                        pdfId = rs.getString("memoryPdf")?.let(UUID::fromString),
                    )
                }
            }
        }

        val insertMemory = conn.prepareStatement(
            """
                INSERT INTO memories (id, place, "externalPeople", "text", sport, department, "submittedBy", lending, pdf)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        )
        val insertMember = conn.prepareStatement("INSERT INTO memories_members (memory, member) VALUES (?, ?)")
        val insertFile = conn.prepareStatement("INSERT INTO memories_files (memory, file) VALUES (?, ?)")

        for (oldMemory in oldMemories) {
            val memoryJson = json.parseToJsonElement(oldMemory.json).jsonObject
            val memoryId = UUID.randomUUID()

            insertMemory.setObject(1, memoryId)
            insertMemory.setString(2, memoryJson["place"]?.jsonPrimitive?.contentOrNull())
            insertMemory.setString(3, memoryJson["externalUsers"]?.jsonPrimitive?.contentOrNull())
            insertMemory.setString(4, memoryJson["text"]?.jsonPrimitive?.contentOrNull() ?: "")
            insertMemory.setString(5, memoryJson["sport"]?.jsonPrimitive?.contentOrNull())
            insertMemory.setObject(6, memoryJson["department"]?.jsonPrimitive?.contentOrNull()?.let(UUID::fromString))
            // The old memory was always submitted by the lending's owner
            insertMemory.setString(7, oldMemory.lendingUserSub)
            insertMemory.setObject(8, oldMemory.lendingId)
            insertMemory.setObject(9, oldMemory.pdfId)
            insertMemory.executeUpdate()

            memoryJson["members"]?.jsonArray?.forEach { member ->
                insertMember.setObject(1, memoryId)
                insertMember.setLong(2, member.jsonPrimitive.content.toLong())
                insertMember.executeUpdate()
            }

            memoryJson["files"]?.jsonArray?.forEach { file ->
                insertFile.setObject(1, memoryId)
                insertFile.setObject(2, UUID.fromString(file.jsonPrimitive.content))
                insertFile.executeUpdate()
            }
        }

        insertMemory.close()
        insertMember.close()
        insertFile.close()

        tr.exec(
            """
                ALTER TABLE lendings DROP COLUMN memory;
                ALTER TABLE lendings DROP COLUMN "memoryPdf";
            """.trimIndent()
        )
    }

    /** Returns the content of this [JsonPrimitive], or `null` if it's JSON `null`. */
    private fun JsonPrimitive.contentOrNull(): String? = if (this == JsonNull) null else content
}
