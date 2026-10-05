package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.SPACES_MANAGER_GROUP_NAME
import org.centrexcursionistalcoi.app.data.SpaceOccupancy
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import org.centrexcursionistalcoi.app.database.table.SpaceLendings
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateSpaceKeyRequest
import org.centrexcursionistalcoi.app.request.CreateSpaceRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceKeyRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceRequest
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.today
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq

fun Route.spacesRoutes() {
    // Writes are for admins only
    provideEntityRoutes(
        resources = Api.Spaces.resources,
        entityClass = SpaceEntity,
        idTypeConverter = { it.toUuidOrNull() },
        writeGroup = SPACES_MANAGER_GROUP_NAME,
        createRequestSerializer = CreateSpaceRequest.serializer(),
        updater = UpdateSpaceRequest.serializer(),
        creator = { request ->
            require(request.name.isNotBlank()) { "name cannot be blank" }
            Database {
                SpaceEntity.new {
                    this.name = request.name
                    this.description = request.description
                    this.conditionsOfUse = request.conditionsOfUse?.takeIf { it.isNotEmpty() }
                    this.requiresKeys = request.requiresKeys
                    this.prices = request.prices
                }
            }
        },
    )

    provideEntityRoutes(
        resources = Api.SpaceKeys.resources,
        entityClass = SpaceKeyEntity,
        idTypeConverter = { it.toUuidOrNull() },
        writeGroup = SPACES_MANAGER_GROUP_NAME,
        createRequestSerializer = CreateSpaceKeyRequest.serializer(),
        updater = UpdateSpaceKeyRequest.serializer(),
        creator = { request ->
            require(request.name.isNotBlank()) { "name cannot be blank" }
            require(request.maxQuantity > 0) { "maxQuantity must be positive" }
            Database {
                val space = SpaceEntity.findById(request.space)
                    ?: throw NoSuchElementException("Space with id ${request.space} does not exist")
                SpaceKeyEntity.new {
                    this.space = space
                    this.name = request.name
                    this.maxQuantity = request.maxQuantity
                }
            }
        },
    )

    // The nights of a space that are taken. Doesn't tell who by: any logged-in user may see it, to choose dates.
    get<Api.Spaces.Id.Occupancy> { occupancy ->
        getUserSessionOrFail() ?: return@get
        val spaceId = occupancy.parent.id.toUuidOrNull() ?: return@get respondError(Error.MalformedId())
        val result = Database {
            if (SpaceEntity.findById(spaceId) == null) return@Database null
            SpaceLendingEntity
                .find { (SpaceLendings.space eq spaceId) and (SpaceLendings.cancelled eq false) and (SpaceLendings.checkOut greaterEq today()) }
                .map { SpaceOccupancy(it.checkIn, it.checkOut) }
        } ?: return@get respondError(Error.EntityNotFound(SpaceEntity::class, spaceId))
        call.respondText(ContentType.Application.Json) {
            json.encodeToString(ListSerializer(SpaceOccupancy.serializer()), result)
        }
    }
}
