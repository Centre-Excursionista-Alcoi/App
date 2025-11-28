package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.ktor.client.request.*
import org.centrexcursionistalcoi.app.data.ServerInfo
import org.centrexcursionistalcoi.app.response.bodyAsJson

object Server {
    private val log = logging()

    private val httpClient = getHttpClient()

    var info: ServerInfo? = null
        private set

    suspend fun loadInfo() {
        try {
            val serverInfo = httpClient.get("/info").bodyAsJson(ServerInfo.serializer())
            info = serverInfo
            log.i { "Fetched server info: $serverInfo" }
        } catch (e: Exception) {
            log.e(e) { "Error fetching server info." }
            info = null
        }
    }
}
