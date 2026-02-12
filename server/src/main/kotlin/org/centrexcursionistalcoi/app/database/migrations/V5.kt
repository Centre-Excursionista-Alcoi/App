package org.centrexcursionistalcoi.app.database.migrations

import java.util.Base64
import org.centrexcursionistalcoi.app.security.AES
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcConnectionImpl

/**
 * Migration V5:
 * - Re-encrypts sensitive data in `user_references` table to use Random IVs.
 * - Previous implementation used a static IV (IV Reuse vulnerability).
 * - Affected columns: `password`, `femecvUsername`, `femecvPassword`.
 */
object V5 : DatabaseMigration {
    override val from: Int = 4
    override val to: Int = 5

    context(tr: JdbcTransaction)
    override fun migrate() {
        // We need to fetch the raw data.
        val conn = (tr.connection as JdbcConnectionImpl).connection
        val statement = conn.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT sub, password, \"femecvUsername\", \"femecvPassword\" FROM user_references"
        )

        val rowsToUpdate = mutableListOf<UserRefData>()

        while (resultSet.next()) {
            val sub = resultSet.getString("sub")
            
            // "password" is type bytea, but AES.encryptor stores Base64 string bytes in it.
            val passwordBytes = resultSet.getBytes("password")
            
            // "femecvUsername" and "femecvPassword" are varchar, storing Base64 string.
            val femecvUsername = resultSet.getString("femecvUsername")
            val femecvPassword = resultSet.getString("femecvPassword")

            rowsToUpdate.add(UserRefData(sub, passwordBytes, femecvUsername, femecvPassword))
        }
        resultSet.close()
        statement.close()

        val updateStatement = conn.prepareStatement(
            "UPDATE user_references SET password = ?, \"femecvUsername\" = ?, \"femecvPassword\" = ? WHERE sub = ?"
        )

        for (row in rowsToUpdate) {
            val newPassword = reEncryptBytes(row.password)
            val newFemecvUsername = reEncryptString(row.femecvUsername)
            val newFemecvPassword = reEncryptString(row.femecvPassword)

            updateStatement.setBytes(1, newPassword)
            updateStatement.setString(2, newFemecvUsername)
            updateStatement.setString(3, newFemecvPassword)
            updateStatement.setString(4, row.sub)
            updateStatement.addBatch()
        }

        updateStatement.executeBatch()
        updateStatement.close()
    }

    private data class UserRefData(
        val sub: String,
        val password: ByteArray?,
        val femecvUsername: String?,
        val femecvPassword: String?
    )

    private fun reEncryptBytes(storedBytes: ByteArray?): ByteArray? {
        if (storedBytes == null) return null
        try {
            // Stored bytes are the bytes of the Base64 string
            val base64String = String(storedBytes)
            val legacyEncrypted = Base64.getDecoder().decode(base64String)
            
            // Decrypt with legacy logic
            @Suppress("DEPRECATION")
            val plaintext = AES.decryptLegacy(legacyEncrypted)
            
            // Encrypt with new logic (Random IV)
            val newEncrypted = AES.encrypt(plaintext)
            
            // Encode back to Base64
            val newBase64String = Base64.getEncoder().encodeToString(newEncrypted)
            
            return newBase64String.toByteArray()
        } catch (e: Exception) {
            // Log error or rethrow? 
            // If data is already in new format or corrupted, this might fail.
            // For now, rethrow to abort migration.
            throw RuntimeException("Failed to re-encrypt password", e)
        }
    }

    private fun reEncryptString(storedString: String?): String? {
        if (storedString == null) return null
        try {
            val legacyEncrypted = Base64.getDecoder().decode(storedString)
            
             // Decrypt with legacy logic
             @Suppress("DEPRECATION")
            val plaintext = AES.decryptLegacy(legacyEncrypted)
            
            // Encrypt with new logic
            val newEncrypted = AES.encrypt(plaintext)
             
            // Encode back to Base64
            return Base64.getEncoder().encodeToString(newEncrypted)
        } catch (e: Exception) {
             throw RuntimeException("Failed to re-encrypt value", e)
        }
    }
}
