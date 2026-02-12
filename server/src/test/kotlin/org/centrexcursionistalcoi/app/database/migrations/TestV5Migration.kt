package org.centrexcursionistalcoi.app.database.migrations

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.security.AES

class TestV5Migration : PostgresTestBase() {
    @Test
    fun test() {
        AES.initForTests()

        // Create V4 schema (simulated)
        // Using quoted identifiers as found in UserReferences/V5
        Database.exec(
            """
                CREATE TABLE user_references (
                    sub text NOT NULL,
                    nif text NOT NULL,
                    "memberNumber" bigint NOT NULL,
                    "fullName" text NOT NULL,
                    email text NOT NULL,
                    groups text[] DEFAULT ARRAY[]::text[] NOT NULL,
                    "isDisabled" boolean DEFAULT false NOT NULL,
                    password bytea NOT NULL,
                    "femecvUsername" character varying(512),
                    "femecvPassword" character varying(512),
                    "femecvLastSync" timestamp without time zone,
                    "lastUpdate" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
                );
            """.trimIndent(),
        ).assertTrue()

        // Encrypt data with legacy logic
        val secret = "Sup3rS3cr3t!"
        @Suppress("DEPRECATION")
        val legacyEncrypted = AES.encryptLegacy(secret.toByteArray())
        val legacyBase64 = Base64.getEncoder().encodeToString(legacyEncrypted)

        // Insert V4 data
        Database.exec(
            """INSERT INTO user_references (sub, nif, "memberNumber", "fullName", email, password, "femecvUsername", "femecvPassword") VALUES (?, ?, ?, ?, ?, ?, ?, ?);""",
            arrayOf(
                "1", 
                "12345678Z", 
                1L, 
                "Test User", 
                "test@example.com", 
                legacyBase64.toByteArray(), 
                legacyBase64, 
                legacyBase64
            )
        ).assertTrue()
        
        // Migrate
        Database { V5.migrate() }
        
        // Verify migration
        Database.execQuery("SELECT password, \"femecvUsername\" FROM user_references WHERE sub = '1'").let { rs ->
            assertTrue(rs.next(), "Should have found user row")
            val storedPasswordBytes = rs.getBytes("password")
            val storedUsername = rs.getString("femecvUsername")
            
            // Password verification
            val passwordBase64 = String(storedPasswordBytes)
            val passwordBytes = Base64.getDecoder().decode(passwordBase64)
            val passwordPlaintext = AES.decrypt(passwordBytes) // New decrypt logic
            assertEquals(secret, String(passwordPlaintext))
            
            // Username verification
            val usernameBytes = Base64.getDecoder().decode(storedUsername)
            val usernamePlaintext = AES.decrypt(usernameBytes) // New decrypt logic
            assertEquals(secret, String(usernamePlaintext))
        }
    }
}
