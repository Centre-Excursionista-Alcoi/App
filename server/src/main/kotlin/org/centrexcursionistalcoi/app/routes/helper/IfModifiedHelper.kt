package org.centrexcursionistalcoi.app.routes.helper

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import java.sql.Timestamp
import java.time.Instant
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.ifModifiedSince
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.storage.RedisStoreMap
import org.centrexcursionistalcoi.app.utils.toInstant
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.select
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("IfModifiedHelper")

fun <ID: Any, T: Entity<ID>> lastUpdateKeyFor(
    entity: EntityClass<ID, T>,
    id: ID,
) = "lastUpdate:${entity.table.tableName}:${id}"

fun <ID: Any, T: Entity<ID>> lastUpdateKeyForType(
    entity: EntityClass<ID, T>,
) = "lastUpdate:${entity.table.tableName}"

/**
 * Notifies that the given entity and ID has been updated by storing the current timestamp in Redis.
 *
 * @param entity The Exposed entity class.
 * @param id The ID of the entity that has been updated.
 */
suspend fun <ID: Any, T: Entity<ID>> notifyUpdateForEntity(
    entity: EntityClass<ID, T>,
    id: ID,
) {
    val now = now()
    RedisStoreMap.fromEnv.put(lastUpdateKeyFor(entity, id), now.toEpochMilli().toString())
    RedisStoreMap.fromEnv.put(lastUpdateKeyForType(entity), now.toEpochMilli().toString())
}

/**
 * Notifies that the given entity and ID has been updated by storing the current timestamp in Redis.
 *
 * @param entity The Exposed entity class.
 * @param id The ID of the entity that has been updated.
 */
suspend fun <ID: Any, T: Entity<ID>> notifyUpdateForEntity(
    entity: EntityClass<ID, T>,
    id: EntityID<ID>,
) = notifyUpdateForEntity(entity, id.value)

/**
 * Handles the `If-Modified-Since` header for the given entity and ID.
 *
 * If the entity has not been modified since the date provided in the header, responds with a 304 Not Modified status code.
 *
 * @param entity The Exposed entity class.
 * @param id The ID of the entity to check.
 *
 * @return `Unit` if the entity has not been modified, or `null` if a `304` response has been sent.
 */
suspend fun <ID: Any, T: Entity<ID>> RoutingContext.handleIfModified(
    entity: EntityClass<ID, T>,
    id: ID,
): Unit? {
    val lastUpdate = RedisStoreMap.fromEnv.get(lastUpdateKeyFor(entity, id))
        ?.toLongOrNull()
        ?.toInstant()
        ?: Database {
            // Fallback to database lastUpdate if not found in Redis
            entity.findById(id)?.let { it as? LastUpdateEntity }?.lastUpdate
        }
    if (lastUpdate != null) {
        call.response.header("CEA-Last-Update", lastUpdate.toEpochMilli())

        // Handle If-Modified-Since header
        val ifModifiedSince = call.request.ifModifiedSince()?.toInstant()
        if (ifModifiedSince != null) {
            if (lastUpdate <= ifModifiedSince) {
                call.respond(HttpStatusCode.NotModified)
                return null
            }
        }
    }
    return Unit
}

/**
 * Handles the `If-Modified-Since` header for the given entity type.
 *
 * If any entity of the given type has not been modified since the date provided in the header, responds with a 304 Not Modified status code.
 * @param entity The Exposed entity class.
 * @return `Unit` if the entity type has not been modified, or `null` if a `304` response has been sent.
 */
suspend fun <ID: Any, T: Entity<ID>> RoutingContext.handleIfModifiedForType(entity: EntityClass<ID, T>): Unit? {
    val lastUpdate = RedisStoreMap.fromEnv.get(lastUpdateKeyForType(entity))
        ?.toLongOrNull()
        ?.toInstant()
        ?: Database {
            // Fallback to database lastUpdate if not found in Redis
            val lastUpdateColumn: Expression<*>? = entity.table.columns.find { it.name == "lastUpdate" }
            if (lastUpdateColumn == null) {
                logger.warn("Could not find lastUpdate entity type ${entity.table.tableName}")
                return@Database null
            }
            // Query the maximum lastUpdate value from the table
            entity.table.select(lastUpdateColumn)
                .toList()
                .mapNotNull { row ->
                    when (val value = row[lastUpdateColumn]) {
                        is Timestamp -> value.toInstant()
                        is Instant -> value
                        is Long -> value.toInstant()
                        is String -> value.toLongOrNull()?.toInstant()
                        else -> null
                    }
                }
                .maxOrNull()
        }
    if (lastUpdate != null) {
        call.response.header("CEA-Last-Update", lastUpdate.toEpochMilli())

        // Handle If-Modified-Since header
        val ifModifiedSince = call.request.ifModifiedSince()?.toInstant()
        if (ifModifiedSince != null) {
            if (lastUpdate <= ifModifiedSince) {
                call.respond(HttpStatusCode.NotModified)
                return null
            }
        }
    }
    return Unit
}
