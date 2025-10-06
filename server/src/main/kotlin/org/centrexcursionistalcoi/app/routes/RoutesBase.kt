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
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.utils.encodeEntityListToString
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedIterable

fun <ID : Any, E : Entity<ID>> Route.provideEntityRoutes(
    base: String,
    entityClass: EntityClass<ID, E>,
    idTypeConverter: (String) -> ID?,
    creator: suspend (MultiPartData) -> E,
    listProvider: JdbcTransaction.(UserSession?) -> SizedIterable<E> = { entityClass.all() }
) {
    require(!base.startsWith("/")) { "Base path must not start with '/'" }
    require(!base.endsWith("/")) { "Base path must not end with '/'" }

    get("/$base") {
        val session = getUserSession()
        val list = Database { listProvider(session).toList() }

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityListToString(list, entityClass)
        }
    }

    get("/$base/{id}") {
        val id = call.parameters["id"]?.let(idTypeConverter)
        if (id == null) {
            return@get call.respondText("Malformed id", status = HttpStatusCode.BadRequest)
        }

        val item = Database { entityClass.findById(id) }
        if (item == null) {
            return@get call.respondText("$base #$id not found", status = HttpStatusCode.NotFound)
        }

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityToString(item, entityClass)
        }
    }

    post("/$base") {
        val session = getUserSessionOrFail() ?: return@post
        if (!session.isAdmin()) {
            call.respondText("You don't have permission to create $base", status = HttpStatusCode.Forbidden)
            return@post
        }

        if (!call.request.contentType().match(ContentType.MultiPart.FormData)) {
            call.respondText("Content-Type must be multipart/form-data", status = HttpStatusCode.BadRequest)
            return@post
        }

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

    delete("$base/{id}") {
        val id = call.parameters["id"]?.let(idTypeConverter)
        if (id == null) {
            return@delete call.respondText("Malformed id", status = HttpStatusCode.BadRequest)
        }

        val session = getUserSessionOrFail() ?: return@delete
        if (!session.isAdmin()) {
            call.respondText("You don't have permission to delete $base", status = HttpStatusCode.Forbidden)
            return@delete
        }

        val item = Database { entityClass.findById(id) }
        if (item == null) {
            return@delete call.respondText("$base #$id not found", status = HttpStatusCode.NotFound)
        }

        Database { item.delete() }

        call.respondText("$base deleted", status = HttpStatusCode.NoContent)
    }
}
