package org.centrexcursionistalcoi.app.endpoints.shared_logic.list

import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.DatabaseData
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.common.SerializableEntity
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.space.SpacesListEndpoint.respondFailure
import org.centrexcursionistalcoi.app.endpoints.space.SpacesListEndpoint.respondSuccess
import org.centrexcursionistalcoi.app.server.response.Errors
import org.jetbrains.exposed.dao.IntEntityClass

suspend fun <Serializable : DatabaseData, Entity : SerializableEntity<Serializable>, EntityClass : IntEntityClass<Entity>> RoutingContext.listAllDatabaseEntries(
    user: User,
    entityClass: EntityClass,
    elementSerializer: KSerializer<Serializable>
) {
    if (!user.confirmed) {
        respondFailure(Errors.UserNotConfirmed)
        return
    }

    val list = ServerDatabase("List${entityClass::class.simpleName}sEndpoint", "listAllDatabaseEntries") {
        entityClass.all().map { it.serializable() }
    }
    respondSuccess(list, ListSerializer(elementSerializer))
}
