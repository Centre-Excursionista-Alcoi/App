package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.platform.PlatformLoadLogic
import org.centrexcursionistalcoi.app.storage.SettingsCookiesStorage

abstract class ServerTestEnvironment {
    fun runApplicationTest(
        engine: HttpClientEngineBase? = null,
        block: suspend () -> Unit
    ) = runTest {
        Napier.base(DebugAntilog())
        PlatformLoadLogic.load()

        val engine = engine ?: MockEngine { request ->
            respond(
                content = ByteReadChannel("Engine not mocked for ${request.url.fullPath}"),
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
            )
        }

        _httpClient = HttpClient(engine) {
            defaultRequest {
                url(BuildKonfig.SERVER_URL)
            }
            install(HttpCookies) {
                storage = SettingsCookiesStorage.Default
            }
            configureLogging()
        }

        block()
    }
}
