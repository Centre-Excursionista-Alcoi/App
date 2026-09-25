package org.centrexcursionistalcoi.app.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.centrexcursionistalcoi.app.ConfigProvider
import org.centrexcursionistalcoi.app.json

internal object WellKnownConfigProvider : ConfigProvider() {
    const val PACKAGE_NAMES_VARIABLE = "WELL_KNOWN_ASSETLINKS_PACKAGE_NAMES"
    val packageNames: List<String> get() = getList(PACKAGE_NAMES_VARIABLE)

    /**
     * Also the Android origins accepted for WebAuthn (see `webAuthnAndroidOrigins`), where a single malformed
     * entry makes every registration and redemption fail -- hence the trimming.
     */
    const val SHA256_CERT_FINGERPRINTS_VARIABLE = "WELL_KNOWN_ASSETLINKS_SHA256_CERT_FINGERPRINTS"
    val sha256CertFingerprints: List<String> get() = getList(SHA256_CERT_FINGERPRINTS_VARIABLE)

    /**
     * The iOS apps allowed to open this server's links, from `WELL_KNOWN_APPLE_APP_SITE_ASSOCIATION_APP_IDS`:
     * comma-separated App IDs, each the Apple Team ID and the bundle identifier joined by a dot (for this app,
     * `UQSVPP37UL.org.centrexcursionistalcoi.app`). The iOS counterpart of [packageNames].
     */
    const val APPLE_APP_IDS_VARIABLE = "WELL_KNOWN_APPLE_APP_SITE_ASSOCIATION_APP_IDS"
    val appleAppIds: List<String>
        get() = getList(APPLE_APP_IDS_VARIABLE)

    /** A comma-separated list, tolerating whitespace around entries and empty ones (e.g. a trailing comma). */
    private fun getList(name: String): List<String> =
        getenv(name)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: listOf()
}

/**
 * Paths on this server that are web pages with no screen in the app, so they must open in a browser even though
 * the app claims the whole host. Android does the same, see `MainActivity.WEB_ONLY_PATHS`.
 */
private val WEB_ONLY_PATHS = listOf("/reset_password")

/**
 * The `apple-app-site-association` file: which iOS apps may open this server's links (universal links), and share
 * its saved passwords. The iOS counterpart of `assetlinks.json`, which claims the same: every link, and the login
 * credentials.
 *
 * With no [appIds] configured it lists no app, and iOS opens every link in the browser.
 */
internal fun appleAppSiteAssociation(appIds: List<String>): JsonObject = buildJsonObject {
    put("applinks", buildJsonObject {
        put("details", buildJsonArray {
            if (appIds.isNotEmpty()) {
                add(buildJsonObject {
                    put("appIDs", buildJsonArray { appIds.forEach { add(JsonPrimitive(it)) } })
                    put("components", buildJsonArray {
                        // The first match wins, so what stays in the browser has to come before "everything"
                        for (path in WEB_ONLY_PATHS) {
                            add(buildJsonObject {
                                put("/", JsonPrimitive(path))
                                put("exclude", JsonPrimitive(true))
                            })
                        }
                        add(buildJsonObject { put("/", JsonPrimitive("*")) })
                    })
                })
            }
        })
    })
    put("webcredentials", buildJsonObject {
        put("apps", buildJsonArray { appIds.forEach { add(JsonPrimitive(it)) } })
    })
}

fun Route.wellKnownRoutes() {
    get("/assetlinks.json") {
        val array = buildJsonArray {
            for (packageName in WellKnownConfigProvider.packageNames) {
                val obj = buildJsonObject {
                    put("relation", buildJsonArray {
                        add(JsonPrimitive("delegate_permission/common.handle_all_urls"))
                        add(JsonPrimitive("delegate_permission/common.get_login_creds"))
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

    // Served without a file extension and as JSON, with no redirect and no login, as iOS requires
    get("/apple-app-site-association") {
        call.respondText(
            json.encodeToString(JsonObject.serializer(), appleAppSiteAssociation(WellKnownConfigProvider.appleAppIds)),
            ContentType.Application.Json,
        )
    }
}
