package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.MultiPartData
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.utils.encodeEntityListToString
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.assertAdmin
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedIterable

suspend fun RoutingContext.assertContentType(contentType: ContentType = ContentType.MultiPart.FormData): Unit? {
    if (!call.request.contentType().match(contentType)) {
        call.respondText("Content-Type must be $contentType", status = HttpStatusCode.BadRequest)
        return null
    }
    return Unit
}

fun <ID : Any, E : Entity<ID>> Route.provideEntityRoutes(
    base: String,
    entityClass: EntityClass<ID, E>,
    idTypeConverter: (String) -> ID?,
    creator: suspend (MultiPartData) -> E,
    updater: (suspend (entity: E, MultiPartData) -> Unit)? = null,
    listProvider: JdbcTransaction.(UserSession?) -> SizedIterable<E> = { entityClass.all() }
) {
    require(!base.startsWith("/")) { "Base path must not start with '/'" }
    require(!base.endsWith("/")) { "Base path must not end with '/'" }

    suspend fun RoutingContext.getId(): ID? {
        val id = call.parameters["id"]?.let(idTypeConverter)
        if (id == null) {
            call.respondText("Malformed id", status = HttpStatusCode.BadRequest)
            return null
        }
        return id
    }
    suspend fun RoutingContext.assertEntity(id: ID): E? {
        val item = Database { entityClass.findById(id) }
        if (item == null) {
            call.respondText("$base #$id not found", status = HttpStatusCode.NotFound)
            return null
        }
        return item
    }

    get("/$base") {
        val session = getUserSession()
        val list = Database { listProvider(session).toList() }

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityListToString(list, entityClass)
        }
    }

    get("/$base/{id}") {
        val id = getId() ?: return@get
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
        } catch (e: NullPointerException) {
            call.respondText(e.message ?: "Missing argument", status = HttpStatusCode.BadRequest)
            return@post
        } catch (e: IllegalArgumentException) {
            call.respondText(e.message ?: "Malformed Request", status = HttpStatusCode.BadRequest)
            return@post
        } catch (e: NoSuchElementException) {
            call.respondText(e.message ?: "Not found", status = HttpStatusCode.NotFound)
            return@post
        }

        call.response.header(HttpHeaders.Location, "/$base/${item.id.value}")
        call.respondText("$base created", status = HttpStatusCode.Created)
    }

    patch("$base/{id}") {
        if (updater == null) {
            call.respondText("Operation not supported", status = HttpStatusCode.MethodNotAllowed)
            return@patch
        }

        val id = getId() ?: return@patch
        assertContentType() ?: return@patch
        assertAdmin() ?: return@patch
        val item = assertEntity(id) ?: return@patch

        val multipart = call.receiveMultipart()
        try {
            updater(item, multipart)
        } catch (e: NullPointerException) {
            call.respondText(e.message ?: "Missing argument", status = HttpStatusCode.BadRequest)
            return@patch
        } catch (e: IllegalArgumentException) {
            call.respondText(e.message ?: "Malformed Request", status = HttpStatusCode.BadRequest)
            return@patch
        } catch (e: NoSuchElementException) {
            call.respondText(e.message ?: "Not found", status = HttpStatusCode.NotFound)
            return@patch
        }

        call.response.header(HttpHeaders.Location, "/$base/${item.id.value}")
        call.respondText("$base created", status = HttpStatusCode.OK)
    }

    delete("$base/{id}") {
        val id = getId() ?: return@delete
        assertAdmin() ?: return@delete
        val item = assertEntity(id) ?: return@delete

        Database { item.delete() }

        call.respondText("$base deleted", status = HttpStatusCode.NoContent)
    }
}
