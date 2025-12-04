package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.ktor.client.request.*
import org.centrexcursionistalcoi.app.data.ServerInfo
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.response.bodyAsJson
import org.centrexcursionistalcoi.app.storage.SETTINGS_SERVER_INFO
import org.centrexcursionistalcoi.app.storage.settings

object Server {
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
            val serverInfo = httpClient.get("/info").bodyAsJson(ServerInfo.serializer())
            info = serverInfo
            settings.putString(SETTINGS_SERVER_INFO, json.encodeToString(ServerInfo.serializer(), serverInfo))
            log.i { "Fetched server info: $serverInfo" }
        } catch (e: Exception) {
            log.e(e) { "Error fetching server info." }
            info = settings.getStringOrNull(SETTINGS_SERVER_INFO)?.let { json.decodeFromString(ServerInfo.serializer(), it) }
        }
    }
}
