package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.database.data.Spaces
import org.centrexcursionistalcoi.app.storage.databaseInstance

expect val SpacesRepository : Repository<Space, Uuid>

object SpacesSettingsRepository : SettingsRepository<Space, Uuid>("spaces", Space.serializer())

object SpacesDatabaseRepository : DatabaseRepository<Space, Uuid>() {
    override val queries by lazy { databaseInstance.spacesQueries }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher) = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)
        .map { list -> list.map { it.toSpace() } }

    override suspend fun selectAll() = queries.selectAll().awaitAsList().map { it.toSpace() }

    override suspend fun get(id: Uuid): Space? {
        return queries.get(id).awaitAsList().firstOrNull()?.toSpace()
    }

    override fun getAsFlow(id: Uuid, dispatcher: CoroutineDispatcher): Flow<Space?> {
        return queries
            .get(id)
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.firstOrNull()?.toSpace() }
    }

    override suspend fun insert(item: Space) = queries.insert(
        id = item.id,
        name = item.name,
        description = item.description,
        price = item.price?.first,
        priceDuration = item.price?.second,
        capacity = item.capacity?.toLong()
    )

    override suspend fun update(item: Space) = queries.update(
        id = item.id,
        name = item.name,
        description = item.description,
        price = item.price?.first,
        priceDuration = item.price?.second,
        capacity = item.capacity?.toLong()
    )

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    private fun Spaces.toSpace() = Space(
        id = id,
        name = name,
        description = description,
        price = if (price != null && priceDuration != null) price to priceDuration else null,
        capacity = capacity?.toInt()
    )
}
