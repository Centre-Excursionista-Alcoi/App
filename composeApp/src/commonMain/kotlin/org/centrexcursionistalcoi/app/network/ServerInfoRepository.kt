package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.ktor.client.plugins.resources.get
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import org.centrexcursionistalcoi.app.data.ServerInfo
import org.centrexcursionistalcoi.app.response.bodyAsJson
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.centrexcursionistalcoi.app.storage.SETTINGS_SERVER_INFO
import org.koin.core.annotation.Singleton

@Singleton
class ServerInfoRepository(
    private val settings: SettingsStore
) {
    private val log = logging()

    private val httpClient = getHttpClient()

    var info: ServerInfo? = null
        private set

    /**
     * Loads the server info from the `/info` endpoint and stores it into [info].
     *
     * If the request fails, it tries to load the info from local settings.
     */
    suspend fun loadInfo() {
        try {
            val httpResponse = httpClient.get(Api.Info())
            if (!httpResponse.status.isSuccess()) {
                log.w { "Error fetching server info: Server responded with error." }
                info = settings.get(SETTINGS_SERVER_INFO, ServerInfo.serializer())
                return
            }
            val serverInfo = httpResponse.bodyAsJson(ServerInfo.serializer())
            info = serverInfo
            settings.set(SETTINGS_SERVER_INFO, ServerInfo.serializer(), serverInfo)
            log.i { "Fetched server info: $serverInfo" }
        } catch (e: Exception) {
            log.e(e) { "Error fetching server info." }
            info = settings.get(SETTINGS_SERVER_INFO, ServerInfo.serializer())
        }
    }
}
