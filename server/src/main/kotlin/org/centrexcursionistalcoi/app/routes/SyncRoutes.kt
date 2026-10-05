package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.routes.sync.SyncSections
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.utils.toInstant
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("SyncRoutes")

/**
 * `GET /sync` answers in a single request what the app otherwise asks of each of the routes that list what it keeps
 * a copy of. It saves the app a round trip per entity, and the session being checked once instead of in each.
 *
 * The response is an object with the `serverTime`, and a section for each of the [SyncSections], named by their
 * key. The app sends, as a query parameter named by the section's key, the time it last synced it (in
 * milliseconds): if the section hasn't changed since, it is `{"modified":false}`; otherwise it is
 * `{"modified":true,"lastUpdate":...,"items":...}`, with what its own route would answer. Without that parameter, it
 * is always sent.
 *
 * All the sections are read in one transaction, so they are consistent with each other.
 *
 * The response is compressed (gzip or deflate, if the client accepts it), the only route that is: it's the one that
 * answers a lot of data at once.
 */
fun Route.syncRoutes() {
    get<Api.Sync> {
        val session = getUserSessionOrFail() ?: return@get
        val sections = SyncSections.all()

        val since = sections.associate { it.key to call.request.queryParameters[it.key]?.toLongOrNull()?.toInstant() }
        for (section in sections) section.prepare(session)
        val lastUpdates = sections.associate { it.key to it.lastUpdate(session) }

        val body = Database {
            buildString {
                append("{\"serverTime\":").append(now().toEpochMilliseconds())
                for (section in sections) {
                    val lastUpdate = lastUpdates[section.key]
                    val sinceSection = since[section.key]
                    append(",\"").append(section.key).append("\":")
                    if (sinceSection != null && lastUpdate != null && lastUpdate <= sinceSection) {
                        append("{\"modified\":false}")
                    } else {
                        append("{\"modified\":true")
                        if (lastUpdate != null) append(",\"lastUpdate\":").append(lastUpdate.toEpochMilliseconds())
                        append(",\"items\":")
                        try {
                            append(section.snapshot(session))
                        } catch (e: Exception) {
                            logger.error("Could not sync the section ${section.key}", e)
                            throw e
                        }
                        append('}')
                    }
                }
                append('}')
            }
        }
        call.respondText(body, ContentType.Application.Json)
    }.install(Compression) {
        // Only this route: it's the one answering a lot of data at once
        gzip()
        deflate()
    }
}
