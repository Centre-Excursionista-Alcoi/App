package org.centrexcursionistalcoi.app.database.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.EntityDataConverter
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.security.UserSession
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * The JSON list of [entities] as their data classes, for the user of [session]. It reads them in a transaction, so what
 * converting needs is loaded once for the whole list when it has been preloaded in that same transaction.
 */
fun <D : Entity<*>> encodeList(
    serializer: KSerializer<D>,
    entities: Iterable<EntityDataConverter<D, *>>,
    session: UserSession? = null,
): String = Database { json.encodeToString(ListSerializer(serializer), entities.map { it.toData(session) }) }

/**
 * Like [encodeList], for entities read by [entities], which runs in the same transaction.
 */
fun <D : Entity<*>> encodeList(
    serializer: KSerializer<D>,
    session: UserSession?,
    entities: JdbcTransaction.() -> Iterable<EntityDataConverter<D, *>>,
): String = Database { json.encodeToString(ListSerializer(serializer), entities().map { it.toData(session) }) }

/** The JSON of [entity] as its data class, for the user of [session]. */
fun <D : Entity<*>> encodeOne(
    serializer: KSerializer<D>,
    entity: EntityDataConverter<D, *>,
    session: UserSession? = null,
): String = Database { json.encodeToString(serializer, entity.toData(session)) }
