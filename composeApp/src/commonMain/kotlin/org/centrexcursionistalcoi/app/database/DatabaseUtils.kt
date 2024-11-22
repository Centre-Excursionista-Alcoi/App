package org.centrexcursionistalcoi.app.database

import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.database.entity.DatabaseEntity

suspend fun <Entity: DatabaseEntity<*>> updateEntities(
    list: List<Entity>,
    getAll: suspend () -> List<Entity>,
    create: suspend (Entity) -> Unit,
    update: suspend (Entity) -> Unit,
    delete: suspend (Entity) -> Unit
) {
    updateEntities(
        list = list,
        idExtractor = { it.id },
        getAll = getAll,
        create = create,
        update = update,
        delete = delete
    )
}

suspend fun <Entity: Any, IdType: Any> updateEntities(
    list: List<Entity>,
    idExtractor: (Entity) -> IdType,
    getAll: suspend () -> List<Entity>,
    create: suspend (Entity) -> Unit,
    update: suspend (Entity) -> Unit,
    delete: suspend (Entity) -> Unit
) {
    val entities = getAll()
    for (entity in list) {
        val existing = entities.find { idExtractor(it) == idExtractor(entity) }
        if (existing == null) {
            Napier.d { "Inserting ${entity::class.simpleName}#${idExtractor(entity)}..." }
            create(entity)
        } else {
            Napier.d { "Updating ${entity::class.simpleName}#${idExtractor(entity)}..." }
            update(entity)
        }
    }
    for (entity in entities) {
        if (list.find { idExtractor(it) == idExtractor(entity) } == null) {
            Napier.d { "Deleting ${entity::class.simpleName}#${idExtractor(entity)}..." }
            delete(entity)
        }
    }
}
