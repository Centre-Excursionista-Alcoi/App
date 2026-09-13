package org.centrexcursionistalcoi.app

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.RecoverPasswordRequests
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

class TestPasswordRecovery : ApplicationTestBase() {
    private fun recoveryTest(password: String, expired: Boolean = false) = runApplicationTest(
        databaseInitBlock = {
            val user = FakeUser.provideEntity()
            RecoverPasswordRequests.insert {
                it[id] = "recovery-request"
                it[this.user] = user.id
                it[timestamp] = if (expired) Instant.EPOCH else Instant.now()
            }
        },
    ) {
        val page = client.get("/reset_password?request_id=recovery-request").bodyAsText()
        assertTrue(page.contains("name=\"request_id\" value=\"recovery-request\""))
        val response = client.submitForm("/reset_password", Parameters.build {
            append("request_id", "recovery-request")
            append("webui", "true")
            append("password", password)
        })
        assertEquals(HttpStatusCode.OK, response.status)
        assertNull(response.headers["Location"], "Web validation must not redirect into an App Link")
        val html = response.bodyAsText()
        assertTrue(html.contains("</html>"))
        if (expired || password == "weak") {
            assertTrue(html.contains(if (expired) "The password reset request has expired." else "The provided password is not safe enough."), html)
            assertTrue(html.contains("name=\"request_id\" value=\"recovery-request\""))
            Database {
                assertEquals(1, RecoverPasswordRequests.selectAll().count().toInt())
                assertTrue(assertNotNull(UserReferenceEntity.findById(FakeUser.SUB)).password.isEmpty())
            }
        } else {
            assertTrue(html.contains("Your password has been changed successfully."), html)
            assertFalse(html.contains("<form"))
            Database {
                val user = assertNotNull(UserReferenceEntity.findById(FakeUser.SUB))
                assertTrue(Passwords.verify(password.toCharArray(), user.password))
                assertEquals(0, RecoverPasswordRequests.selectAll().count().toInt())
            }
            val reused = client.submitForm("/reset_password", Parameters.build {
                append("request_id", "recovery-request")
                append("webui", "true")
                append("password", "OtherPassword123")
            })
            assertTrue(reused.bodyAsText().contains("The given request id is not valid."))
            Database {
                assertTrue(Passwords.verify(password.toCharArray(), assertNotNull(UserReferenceEntity.findById(FakeUser.SUB)).password))
            }
        }
    }

    @Test fun `missing request shows a web error`() = runApplicationTest {
        val response = client.submitForm("/reset_password", Parameters.build {
            append("webui", "true")
            append("password", "NewPassword123")
        })
        assertNull(response.headers["Location"])
        assertTrue(response.bodyAsText().contains("Missing arguments."))
    }

    @Test fun `API validation still returns a structured error`() = runApplicationTest {
        val response = client.submitForm("/reset_password", Parameters.build {
            append("request_id", "recovery-request")
            append("password", "weak")
        })
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("\"code\":28"))
    }

    @Test fun `successful web reset confirms password change`() = recoveryTest("NewPassword123")
    @Test fun `weak password shows error and preserves request`() = recoveryTest("weak")
    @Test fun `expired request shows error`() = recoveryTest("NewPassword123", expired = true)
}
