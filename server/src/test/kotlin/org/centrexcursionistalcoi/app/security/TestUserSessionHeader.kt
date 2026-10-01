package org.centrexcursionistalcoi.app.security

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSession

class TestUserSessionHeader {
    @Test
    fun test_getUserSession_setsLoggedInHeader() = testApplication {
        routing {
            get("/session") {
                call.getUserSession()
                call.respondText("ok")
            }
        }

        assertEquals("false", client.get("/session").headers["CEA-LoggedIn"])
    }

    @Test
    fun test_getUserSession_afterTheResponseStarted_doesNotFail() {
        // On Netty, as in production: the test engine accepts headers even after the response started
        var session: UserSession? = UserSession("sub", "Name", "email@example.com", emptyList())
        var failure: Throwable? = null
        val server = embeddedServer(Netty, port = 0) {
            routing {
                get("/session") {
                    // Like an SSE stream, whose headers are sent before its handler runs
                    call.respondText("ok")
                    try {
                        session = call.getUserSession()
                    } catch (e: Throwable) {
                        failure = e
                    }
                }
            }
        }.start(wait = false)
        try {
            val port = runBlocking { server.engine.resolvedConnectors().first().port }
            val status = HttpClient(Java).use { client -> runBlocking { client.get("http://localhost:$port/session").status } }

            assertEquals(HttpStatusCode.OK, status)
            assertNull(failure, "getUserSession() failed after the response started: $failure")
            assertNull(session)
        } finally {
            server.stop(0, 0)
        }
    }
}
