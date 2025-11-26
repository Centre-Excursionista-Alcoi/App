package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.MultiPartData
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.utils.encodeEntityListToString
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.assertAdmin
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.request.UpdateEntityRequest
import org.centrexcursionistalcoi.app.routes.helper.handleIfModified
import org.centrexcursionistalcoi.app.routes.helper.handleIfModifiedForType
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.dao.Entity as ExposedEntity

suspend fun RoutingContext.assertContentType(contentType: ContentType = ContentType.MultiPart.FormData): Unit? {
    val requestContentType = call.request.contentType()
    if (!requestContentType.match(contentType)) {
        respondError(Error.InvalidContentType(contentType, requestContentType))
        return null
    }
    return Unit
}

@Suppress("USELESS_CAST")
inline fun <EID : Any, reified EE : ExposedEntity<EID>> Route.provideEntityRoutes(
    base: String,
    entityClass: EntityClass<EID, EE>,
    noinline idTypeConverter: (String) -> EID?,
    noinline creator: suspend (MultiPartData) -> EE,
    noinline listProvider: JdbcTransaction.(UserSession?) -> SizedIterable<EE> = { entityClass.all() },
    /**
     * A check to be performed before deleting an entity.
     * Verifies whether there are references to this entity that would prevent its deletion.
     * If it returns `false`, the deletion is aborted and an error is returned.
     */
    noinline deleteReferencesCheck: JdbcTransaction.(EE) -> Boolean = { true },
) = provideEntityRoutes<EID, EE, Any, Entity<Any>, UpdateEntityRequest<Any, Entity<Any>>>(base, entityClass, EE::class as KClass<EE>, idTypeConverter, creator, null, listProvider, deleteReferencesCheck)

@Suppress("USELESS_CAST")
inline fun <EID : Any, reified EE : ExposedEntity<EID>, ID: Any, E : Entity<ID>, UER: UpdateEntityRequest<ID, E>> Route.provideEntityRoutes(
    base: String,
    entityClass: EntityClass<EID, EE>,
    noinline idTypeConverter: (String) -> EID?,
    /**
     * Creates a new entity from the provided [MultiPartData].
     * @throws NullPointerException if a required argument is missing.
     * @throws IllegalArgumentException if an argument is malformed.
     * @throws NoSuchElementException if a referenced entity is not found.
     */
    noinline creator: suspend (MultiPartData) -> EE,
    /**
     * If null, the PATCH endpoint will not be created.
     *
     * Otherwise, the entity class must implement [EntityPatcher].
     */
    updater: KSerializer<UER>,
    noinline listProvider: JdbcTransaction.(UserSession?) -> SizedIterable<EE> = { entityClass.all() },
    /**
     * A check to be performed before deleting an entity.
     * Verifies whether there are references to this entity that would prevent its deletion.
     * If it returns `false`, the deletion is aborted and an error is returned.
     */
    noinline deleteReferencesCheck: JdbcTransaction.(EE) -> Boolean = { true },
) = provideEntityRoutes(base, entityClass, EE::class as KClass<EE>, idTypeConverter, creator, updater, listProvider, deleteReferencesCheck)

@OptIn(InternalSerializationApi::class)
fun <EID : Any, EE : ExposedEntity<EID>, ID: Any, E : Entity<ID>, UER: UpdateEntityRequest<ID, E>> Route.provideEntityRoutes(
    base: String,
    entityClass: EntityClass<EID, EE>,
    entityKClass: KClass<EE>,
    idTypeConverter: (String) -> EID?,
    /**
     * Creates a new entity from the provided [MultiPartData].
     * @throws NullPointerException if a required argument is missing.
     * @throws IllegalArgumentException if an argument is malformed.
     * @throws NoSuchElementException if a referenced entity is not found.
     */
    creator: suspend (MultiPartData) -> EE,
    /**
     * If null, the PATCH endpoint will not be created.
     *
     * Otherwise, [entityKClass] must implement [EntityPatcher].
     */
    updater: KSerializer<UER>? = null,
    listProvider: JdbcTransaction.(UserSession?) -> SizedIterable<EE> = { entityClass.all() },
    /**
     * A check to be performed before deleting an entity.
     * Verifies whether there are references to this entity that would prevent its deletion.
     * If it returns `false`, the deletion is aborted and an error is returned.
     */
    deleteReferencesCheck: JdbcTransaction.(EE) -> Boolean = { true },
) {
    require(!base.startsWith("/")) { "Base path must not start with '/'" }
    require(!base.endsWith("/")) { "Base path must not end with '/'" }
    require(updater == null || entityKClass.isSubclassOf(EntityPatcher::class)) { "${entityKClass.simpleName} doesn't extend EntityPatcher" }

    suspend fun RoutingContext.getId(): EID? {
        val id = call.parameters["id"]?.let(idTypeConverter)
        if (id == null) {
            respondError(Error.MalformedId())
            return null
        }
        return id
    }
    suspend fun RoutingContext.assertEntity(id: EID): EE? {
        val item = Database { entityClass.findById(id) }
        if (item == null) {
            respondError(Error.EntityNotFound(entityKClass, id))
            return null
        }
        return item
    }

    get("/$base") {
        val session = getUserSession()
        handleIfModifiedForType(entityClass) ?: return@get
        val list = Database { listProvider(session).toList() }

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityListToString(list, entityClass)
        }
    }

    get("/$base/{id}") {
        val id = getId() ?: return@get
        handleIfModified(entityClass, id) ?: return@get
        val item = assertEntity(id) ?: return@get

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityToString(item, entityClass)
        }
    }

    post("/$base") {
        assertContentType() ?: return@post
        assertAdmin() ?: return@post

        val multipart = call.receiveMultipart()
        val item = try {
            creator(multipart)
        } catch (_: NullPointerException) {
            respondError(Error.MissingArgument())
            return@post
        } catch (_: IllegalArgumentException) {
            respondError(Error.MalformedRequest())
            return@post
        } catch (_: NoSuchElementException) {
            respondError(Error.EntityNotFound(entityKClass, "N/A"))
            return@post
        }

        if (item is LastUpdateEntity) {
            item.updated()
        }

        call.response.header(HttpHeaders.Location, "/$base/${item.id.value}")
        call.respondText("$base created", status = HttpStatusCode.Created)
    }

    patch("$base/{id}") {
        if (updater == null) {
            respondError(Error.OperationNotSupported())
            return@patch
        }

        val id = getId() ?: return@patch
        assertContentType(ContentType.Application.Json) ?: return@patch
        assertAdmin() ?: return@patch
        val item = assertEntity(id) ?: return@patch

        val body = call.receiveText()
        val request = try {
            json.decodeFromString(updater, body)
        } catch (_: Exception) {
            respondError(Error.MalformedRequest())
            return@patch
        }
        if (request.isEmpty()) {
            respondError(Error.NothingToUpdate())
            return@patch
        }
        @Suppress("UNCHECKED_CAST")
        val patcher = item as EntityPatcher<UER>
        Database { patcher.patch(request) }

        if (item is LastUpdateEntity) {
            item.updated()
        }

        call.response.header(HttpHeaders.Location, "/$base/${item.id.value}")
        call.respondText("$base created", status = HttpStatusCode.OK)
    }

    delete("$base/{id}") {
        val id = getId() ?: return@delete
        assertAdmin() ?: return@delete
        val item = assertEntity(id) ?: return@delete

        val referencesCheck = Database { deleteReferencesCheck(item) }
        if (!referencesCheck) {
            respondError(Error.EntityDeleteReferencesExist())
            return@delete
        }

        Database { item.delete() }

        call.respondText("$base deleted", status = HttpStatusCode.NoContent)
    }
}
