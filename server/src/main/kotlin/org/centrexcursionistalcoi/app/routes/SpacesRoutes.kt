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
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyTypeEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import org.centrexcursionistalcoi.app.database.table.SpaceKeys
import org.centrexcursionistalcoi.app.database.table.SpaceLendingKeyRequests
import org.centrexcursionistalcoi.app.database.table.SpaceLendingKeys
import org.centrexcursionistalcoi.app.database.table.SpaceLendings
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateSpaceKeyRequest
import org.centrexcursionistalcoi.app.request.CreateSpaceKeyTypeRequest
import org.centrexcursionistalcoi.app.request.CreateSpaceRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceKeyTypeRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceKeyRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceRequest
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.isSpaceLendingsManager
import org.centrexcursionistalcoi.app.security.isSpacesManager
import org.centrexcursionistalcoi.app.today
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.EmptySizedIterable
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.selectAll

fun Route.spacesRoutes() {
    // Writes are for admins only
    provideEntityRoutes(
        resources = Api.Spaces.resources,
        entityClass = SpaceEntity,
        syncKey = "spaces",
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

    // The types of keys, and the spaces they are for. Any logged-in user needs them to choose keys when booking
    provideEntityRoutes(
        resources = Api.SpaceKeyTypes.resources,
        entityClass = SpaceKeyTypeEntity,
        syncKey = "space_key_types",
        idTypeConverter = { it.toUuidOrNull() },
        writeGroup = SPACES_MANAGER_GROUP_NAME,
        listProvider = { session ->
            if (session == null) EmptySizedIterable()
            else SizedCollection(SpaceKeyTypeEntity.withDataPreloaded(SpaceKeyTypeEntity.all().toList()))
        },
        visibleTo = { _, session -> session != null },
        deleteReferencesCheck = { type ->
            SpaceKeys.selectAll().where { SpaceKeys.type eq type.id }.empty() &&
                SpaceLendingKeyRequests.selectAll().where { SpaceLendingKeyRequests.keyType eq type.id }.empty()
        },
        createRequestSerializer = CreateSpaceKeyTypeRequest.serializer(),
        updater = UpdateSpaceKeyTypeRequest.serializer(),
        creator = { request ->
            require(request.name.isNotBlank()) { "name cannot be blank" }
            Database {
                SpaceKeyTypeEntity.validateSpaces(request.spaces)
                SpaceKeyTypeEntity.new {
                    this.name = request.name
                    this.description = request.description?.takeIf { it.isNotEmpty() }
                }.also { it.setSpaces(request.spaces) }
            }
        },
    )

    // The keys of the club. Only those who hand them out and manage them can see them
    provideEntityRoutes(
        resources = Api.SpaceKeys.resources,
        entityClass = SpaceKeyEntity,
        syncKey = "space_keys",
        idTypeConverter = { it.toUuidOrNull() },
        writeGroup = SPACES_MANAGER_GROUP_NAME,
        listProvider = { session ->
            if (session != null && (session.isSpacesManager() || session.isSpaceLendingsManager())) SpaceKeyEntity.all()
            else EmptySizedIterable()
        },
        visibleTo = { _, session -> session != null && (session.isSpacesManager() || session.isSpaceLendingsManager()) },
        deleteReferencesCheck = { key ->
            SpaceLendingKeys.selectAll().where { SpaceLendingKeys.key eq key.id }.empty()
        },
        createRequestSerializer = CreateSpaceKeyRequest.serializer(),
        updater = UpdateSpaceKeyRequest.serializer(),
        creator = { request ->
            Database {
                val type = SpaceKeyTypeEntity.findById(request.type)
                    ?: throw NoSuchElementException("Key type with id ${request.type} does not exist")
                SpaceKeyEntity.new {
                    this.type = type
                    this.label = request.label?.takeIf { it.isNotEmpty() }
                    this.nfcId = request.nfcId?.takeUnless { it.isEmpty() }
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
