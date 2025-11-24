package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.centrexcursionistalcoi.app.ConfigProvider
import org.centrexcursionistalcoi.app.json

private object WellKnownConfigProvider : ConfigProvider() {
    val packageNames = getenv("WELL_KNOWN_ASSETLINKS_PACKAGE_NAMES")?.split(",") ?: listOf()
    val sha256CertFingerprints = getenv("WELL_KNOWN_ASSETLINKS_SHA256_CERT_FINGERPRINTS")?.split(",") ?: listOf()
}

fun Route.wellKnownRoutes() {
    get("/assetlinks.json") {
        val array = buildJsonArray {
            for (packageName in WellKnownConfigProvider.packageNames) {
                val obj = buildJsonObject {
                    put("relation", buildJsonArray {
                        add(JsonPrimitive("delegate_permission/common.handle_all_urls"))
                    })
                    put("target", buildJsonObject {
                        put("namespace", JsonPrimitive("android_app"))
                        put("package_name", JsonPrimitive(packageName))
                        put("sha256_cert_fingerprints", buildJsonArray {
                            for (fingerprint in WellKnownConfigProvider.sha256CertFingerprints) {
                                add(JsonPrimitive(fingerprint))
                            }
                        })
                    })
                }
                add(obj)
            }
        }
        call.respondText(json.encodeToString(array), ContentType.Application.Json)
    }
}
