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
    val entities = getAll()
    for (entity in list) {
        val existing = entities.find { it.id == entity.id }
        if (existing == null) {
            Napier.d { "Inserting ${entity::class.simpleName}#${entity.id}..." }
            create(entity)
        } else {
            Napier.d { "Updating ${entity::class.simpleName}#${entity.id}..." }
            update(entity)
        }
    }
    for (entity in entities) {
        if (list.find { it.id == entity.id } == null) {
            Napier.d { "Deleting ${entity::class.simpleName}#${entity.id}..." }
            delete(entity)
        }
    }
}
