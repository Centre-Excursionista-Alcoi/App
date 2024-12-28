package org.centrexcursionistalcoi.app.endpoints.status

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.userAgent
import io.ktor.server.routing.RoutingContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.endpoints.model.Endpoint
import org.centrexcursionistalcoi.app.server.response.VersionResponse
import org.centrexcursionistalcoi.app.serverJson

object VersionEndpoint : Endpoint("/version") {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/Centre-Excursionista-Alcoi/App/releases/latest"

    private val httpClient = HttpClient {
        defaultRequest {
            userAgent(
                StringBuilder("CEA_App-Server/")
                    .append("<VERSION>")
                    .append(" (")
                    .append(System.getProperty("os.name"))
                    .append(")")
                    .toString()
            )
        }
    }

    private fun findAssetByName(
        assets: List<JsonObject>,
        condition: (String) -> Boolean
    ): JsonObject? {
        return assets.find { it["name"]?.jsonPrimitive?.toString()?.let(condition) == true }
    }

    private const val CACHE_DURATION_MINUTES = 4 * 60

    private var cachedVersionResponse: VersionResponse? = null
    private var cachedVersionInstant: Instant = Instant.now()

    override suspend fun RoutingContext.body() {
        val now = Instant.now()
        val cachedVersionAgeMinutes = cachedVersionInstant.until(now, ChronoUnit.MINUTES)
        if (cachedVersionResponse == null || cachedVersionAgeMinutes > CACHE_DURATION_MINUTES) {
            val response = httpClient.get(LATEST_RELEASE_URL)
            val json = serverJson.parseToJsonElement(response.bodyAsText()).jsonObject
            val tag = json["tag_name"]?.jsonPrimitive?.toString()
            val assets = json["assets"]?.jsonArray
                ?.let { arr -> (0 until arr.size).map { arr[it].jsonObject } }
                ?: emptyList()
            val apk = findAssetByName(assets) { it.contains("apk") }
            val exe = findAssetByName(assets) { it.contains("exe") }
            val dmg = findAssetByName(assets) { it.contains("dmg") }
            val deb = findAssetByName(assets) { it.contains("deb") }
            val urls = VersionResponse.DownloadUrls(
                apk = apk?.get("url")?.jsonPrimitive?.toString(),
                exe = exe?.get("url")?.jsonPrimitive?.toString(),
                dmg = dmg?.get("url")?.jsonPrimitive?.toString(),
                deb = deb?.get("url")?.jsonPrimitive?.toString()
            )

            cachedVersionResponse = VersionResponse(tag, urls)
        }


        respondSuccess(cachedVersionResponse!!, VersionResponse.serializer())
    }
}
