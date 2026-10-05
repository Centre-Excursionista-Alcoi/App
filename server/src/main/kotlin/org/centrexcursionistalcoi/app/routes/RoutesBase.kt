package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.resources.serialization.ResourcesFormat
import io.ktor.server.request.contentType
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.utils.encodeEntityListToString
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.request.MissingPartException
import org.centrexcursionistalcoi.app.request.RequestWithFiles
import org.centrexcursionistalcoi.app.request.UpdateEntityRequest
import org.centrexcursionistalcoi.app.request.assertRequestWithFilesContentType
import org.centrexcursionistalcoi.app.request.receiveRequestWithFiles
import org.centrexcursionistalcoi.app.routes.helper.handleIfModified
import org.centrexcursionistalcoi.app.routes.helper.handleIfModifiedForType
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.assertDepartmentRole
import org.centrexcursionistalcoi.app.security.hasAnyDepartmentRole
import org.centrexcursionistalcoi.app.security.hasDepartmentRole
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.dao.Entity as ExposedEntity

private val logger = LoggerFactory.getLogger("RoutesBase")

/**
 * Thrown from inside a [Database] transaction block to abort and roll it back when a permission check fails
 * after some mutation within that same block has already been applied (e.g. a patch that reassigns an entity's
 * department). Never escapes past the `try`/`catch` that wraps the transaction it was thrown from.
 */
class PermissionDeniedException : Exception()

/**
 * Thrown from inside [EntityPatcher.patch] to reject the PATCH with [error] (e.g. a value that's invalid in
 * combination with the entity's other fields, which can only be checked once the patch is being applied). Like
 * [PermissionDeniedException], it aborts and rolls back the transaction the patch runs in, so nothing is left
 * half-applied, and never escapes past the `try`/`catch` that wraps that transaction.
 */
class PatchRejectedException(val error: Error) : Exception(error.description)

/**
 * Describes the department-scoped role required to create/patch/delete an entity via [provideEntityRoutes].
 *
 * @param role The [DepartmentRole] required (or [UserSession.isAdmin], which always suffices regardless of role).
 * @param departmentOfEntity Resolves the department id the entity belongs to, for an already-existing entity
 *   (used for PATCH/DELETE, and -- since a brand-new entity's department can only be known once it has been
 *   created -- also for POST, checked immediately after creation). Return `null` if the entity isn't
 *   department-scoped (e.g. a shared/global entity with no owning department), in which case the write falls
 *   back to requiring global admin.
 */
class EntityWritePermission<EE>(
    val role: DepartmentRole,
    val departmentOfEntity: (EE) -> Uuid?,
)

suspend fun RoutingContext.assertContentType(contentType: ContentType): Unit? {
    val requestContentType = call.request.contentType()
    if (!requestContentType.match(contentType)) {
        respondError(Error.InvalidContentType(contentType, requestContentType))
        return null
    }
    return Unit
}

/**
 * Asserts that the "id" parameter in the call's parameters is a valid UUID.
 * If not, responds with an [Error.MalformedId] error.
 * @return The UUID if valid, or null if invalid.
 */
suspend fun RoutingContext.assertIdParameter(): Uuid? {
    val id = call.parameters["id"]?.toUuidOrNull()
    if (id == null) {
        call.respondError(Error.MalformedId())
        return null
    }
    return id
}

@Suppress("USELESS_CAST")
inline fun <EID : Any, reified EE : ExposedEntity<EID>, ID: Any, E : Entity<ID>, UER: UpdateEntityRequest<ID, E>, CR : Any, C : Any, I : Any> Route.provideEntityRoutes(
    resources: EntityResources<C, I>,
    entityClass: EntityClass<EID, EE>,
    noinline idTypeConverter: (String) -> EID?,
    createRequestSerializer: KSerializer<CR>,
    noinline creator: suspend (CR) -> EE,
    updater: KSerializer<UER>,
    noinline listProvider: JdbcTransaction.(UserSession?) -> SizedIterable<EE> = { entityClass.all() },
    noinline visibleTo: JdbcTransaction.(EE, UserSession?) -> Boolean = { entity, session -> listProvider(session).any { it.id.value == entity.id.value } },
    noinline deleteReferencesCheck: JdbcTransaction.(EE) -> Boolean = { true },
    writePermission: EntityWritePermission<EE>? = null,
    noinline afterCreate: suspend (EE) -> Unit = {},
    noinline onWriteRejected: JdbcTransaction.(EE) -> Unit = { it.delete() },
    writeGroup: String? = null,
) = provideEntityRoutes(resources, entityClass, EE::class as KClass<EE>, idTypeConverter, createRequestSerializer, creator, updater, listProvider, visibleTo, deleteReferencesCheck, writePermission, afterCreate, onWriteRejected, writeGroup)

@OptIn(InternalSerializationApi::class)
fun <EID : Any, EE : ExposedEntity<EID>, ID: Any, E : Entity<ID>, UER: UpdateEntityRequest<ID, E>, CR : Any, C : Any, I : Any> Route.provideEntityRoutes(
    resources: EntityResources<C, I>,
    entityClass: EntityClass<EID, EE>,
    entityKClass: KClass<EE>,
    idTypeConverter: (String) -> EID?,
    /**
     * Decodes the body of a `POST` on the collection: sent as JSON, or as multipart with its files in parts of their
     * own (see [RequestWithFiles]).
     */
    createRequestSerializer: KSerializer<CR>,
    /**
     * Creates a new entity from a request decoded with [createRequestSerializer].
     * @throws NullPointerException if a required argument is missing.
     * @throws IllegalArgumentException if an argument is malformed.
     * @throws NoSuchElementException if a referenced entity is not found.
     * @throws NumberFormatException if a numeric argument is malformed.
     */
    creator: suspend (CR) -> EE,
    /**
     * If null, the PATCH endpoint will not be created.
     *
     * Otherwise, [entityKClass] must implement [EntityPatcher].
     */
    updater: KSerializer<UER>? = null,
    listProvider: JdbcTransaction.(UserSession?) -> SizedIterable<EE> = { entityClass.all() },
    /**
     * Cheap, targeted check for whether a single already-fetched entity is visible to [session] -- must agree
     * with [listProvider] but without scanning its whole result. Defaults to doing exactly that scan (correct
     * but potentially O(n) per single-item GET); override with a direct predicate whenever [listProvider] does
     * more than trivial filtering.
     */
    visibleTo: JdbcTransaction.(EE, UserSession?) -> Boolean = { entity, session -> listProvider(session).any { it.id.value == entity.id.value } },
    /**
     * A check to be performed before deleting an entity.
     * Verifies whether there are references to this entity that would prevent its deletion.
     * If it returns `false`, the deletion is aborted and an error is returned.
     */
    deleteReferencesCheck: JdbcTransaction.(EE) -> Boolean = { true },
    writePermission: EntityWritePermission<EE>? = null,
    /**
     * Runs once a newly created entity has passed [writePermission]'s fine-grained check -- the place for side
     * effects that must not fire for a rejected (department-unauthorized) creation, such as external notifications.
     */
    afterCreate: suspend (EE) -> Unit = {},
    /**
     * Cleans up a newly created entity that failed [writePermission]'s fine-grained check. Defaults to just
     * deleting the entity; override when [creator] also persists dependent rows (e.g. uploaded files) that would
     * otherwise be orphaned by a rejected creation.
     */
    onWriteRejected: JdbcTransaction.(EE) -> Unit = { it.delete() },
    /**
     * A global group (besides admins) whose members may write any entity, e.g. a "spaces manager".
     */
    writeGroup: String? = null,
) {
    fun UserSession.isWriteGroupMember() = writeGroup != null && writeGroup in groups

    val base = ResourcesFormat().encodeToPathPattern(resources.collectionSerializer).trim('/')
    require(updater == null || entityKClass.isSubclassOf(EntityPatcher::class)) { "${entityKClass.simpleName} doesn't extend EntityPatcher" }

    /**
     * Coarse pre-check, before the entity is looked up (PATCH/DELETE) or created (POST): requires global admin,
     * or -- for a department-scoped [writePermission] -- holding its role in at least one department. This must
     * run before touching the entity/request body at all, so that a caller who could never qualify is rejected
     * without leaking whether a given entity exists, and without running body/validation logic first.
     */
    suspend fun RoutingContext.assertMayWriteAtAll(): UserSession? {
        val session = getUserSessionOrFail() ?: return null
        if (session.isAdmin() || session.isWriteGroupMember()) return session
        val allowed = if (writePermission == null) false else session.hasAnyDepartmentRole(writePermission.role)
        return if (allowed) session else { respondError(if (writePermission == null) Error.NotAnAdmin() else Error.PermissionRejected()); null }
    }

    /**
     * Fine-grained check once the entity is available: resolves its department via [EntityWritePermission.departmentOfEntity]
     * and requires [EntityWritePermission.role] there. An entity with no resolvable department (e.g. a shared/global
     * entity) requires global admin. [session] is assumed to have already passed [assertMayWriteAtAll].
     */
    suspend fun RoutingContext.assertWritePermission(session: UserSession, entity: EE): UserSession? {
        if (session.isAdmin() || session.isWriteGroupMember()) return session
        val departmentId = writePermission?.let { wp -> Database { wp.departmentOfEntity(entity) } }
        return if (writePermission == null || departmentId == null) {
            respondError(Error.NotAnAdmin())
            null
        } else {
            assertDepartmentRole(session, departmentId, writePermission.role)
        }
    }

    suspend fun RoutingContext.getId(resource: I): EID? {
        val id = idTypeConverter(resources.idOf(resource))
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

    /**
     * Fetches the entity by [id], but only if it's also visible to [session] per [visibleTo] -- otherwise
     * responds [Error.EntityNotFound], exactly as if it didn't exist. Unlike [assertEntity] (used by
     * PATCH/DELETE, which are gated by [writePermission] instead), this is what `GET` on an item uses, so a
     * resource can never be read individually by ID if the caller couldn't also see it in the list.
     */
    suspend fun RoutingContext.assertVisibleEntity(id: EID, session: UserSession?): EE? {
        val item = Database {
            val entity = entityClass.findById(id) ?: return@Database null
            entity.takeIf { visibleTo(it, session) }
        }
        if (item == null) {
            respondError(Error.EntityNotFound(entityKClass, id))
            return null
        }
        return item
    }

    handle(resources.collectionSerializer, HttpMethod.Get) {
        val session = getUserSession()
        handleIfModifiedForType(entityClass) ?: return@handle
        val list = Database { listProvider(session).toList() }

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityListToString(list, entityClass, session)
        }
    }

    handle(resources.itemSerializer, HttpMethod.Get) { resource ->
        val id = getId(resource) ?: return@handle
        val session = getUserSession()
        // Visibility must be checked before handleIfModified: a 304 (or its Last-Modified header) would
        // otherwise confirm an invisible entity's existence/last-modified time to a caller who can't see it,
        // via a path that skips assertVisibleEntity entirely.
        val item = assertVisibleEntity(id, session) ?: return@handle
        handleIfModified(entityClass, id) ?: return@handle

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityToString(item, entityClass, session)
        }
    }

    /** Runs the [creator] call, mapping the exceptions it's documented to throw to the matching [Error]. */
    suspend fun RoutingContext.tryCreate(block: suspend () -> EE): EE? = try {
        block()
    } catch (e: NullPointerException) {
        logger.error("Missing argument during entity creation", e)
        respondError(Error.MissingArgument())
        null
    } catch (e: IllegalArgumentException) {
        logger.error("Illegal argument during entity creation", e)
        respondError(Error.MalformedRequest())
        null
    } catch (e: NoSuchElementException) {
        logger.error("Referenced entity not found during entity creation", e)
        respondError(Error.EntityNotFound(entityKClass, "N/A"))
        null
    } catch (e: NumberFormatException) {
        logger.error("Number format exception during entity creation", e)
        respondError(Error.MalformedRequest())
        null
    }

    handle(resources.collectionSerializer, HttpMethod.Post) {
        assertRequestWithFilesContentType() ?: return@handle
        val session = assertMayWriteAtAll() ?: return@handle

        val received = receiveRequestWithFiles(createRequestSerializer) ?: return@handle
        val item = tryCreate { received.withUploads { creator(received.request) } } ?: return@handle

        // The fine-grained check can only run once the entity (and thus its department) exists. If the caller isn't
        // allowed after all, roll the creation back -- including
        // any dependent rows (e.g. uploaded files) onWriteRejected cleans up, so a rejected creation never leaves
        // orphaned data behind.
        if (assertWritePermission(session, item) == null) {
            Database { onWriteRejected(item) }
            return@handle
        }

        if (item is LastUpdateEntity) {
            item.updated()
        }

        // Only reached once the department-scoped permission check above has passed, so external side effects
        // (e.g. Telegram announcements) never fire for a creation that ends up rejected.
        afterCreate(item)

        Push.launch {
            Push.sendPushNotificationToAll(
                PushNotification.EntityUpdated(entityKClass, item.id.value.toString(), true),
            )
        }

        call.response.header(HttpHeaders.Location, href(resources.itemSerializer, resources.item(item.id.value.toString())))
        call.respondText("$base created", status = HttpStatusCode.Created)
    }

    handle(resources.itemSerializer, HttpMethod.Patch) { resource ->
        if (updater == null) {
            respondError(Error.OperationNotSupported())
            return@handle
        }

        val id = getId(resource) ?: return@handle
        assertRequestWithFilesContentType() ?: return@handle
        val session = assertMayWriteAtAll() ?: return@handle
        val item = assertEntity(id) ?: return@handle
        assertWritePermission(session, item) ?: return@handle

        // As JSON, or as multipart with the files in parts of their own (see RequestWithFiles)
        val received = receiveRequestWithFiles(updater) ?: return@handle
        val request = received.request
        if (request.isEmpty()) {
            respondError(Error.NothingToUpdate())
            return@handle
        }
        @Suppress("UNCHECKED_CAST")
        val patcher = item as EntityPatcher<UER>
        // The patch may reassign the entity to a different department (or type, for inventory items) -- re-check
        // the *destination* inside the same transaction as the patch, so an unauthorized move is rolled back
        // atomically rather than left half-applied.
        try {
            received.withUploads {
                Database {
                    patcher.patch(request)
                    if (writePermission != null && !session.isAdmin()) {
                        val newDepartmentId = writePermission.departmentOfEntity(item)
                        val allowed = newDepartmentId != null && session.hasDepartmentRole(newDepartmentId, writePermission.role)
                        if (!allowed) throw PermissionDeniedException()
                    }
                }
            }
        } catch (e: MissingPartException) {
            logger.error("Update request refers to a missing part", e)
            respondError(Error.MalformedRequest())
            return@handle
        } catch (_: PermissionDeniedException) {
            respondError(Error.PermissionRejected())
            return@handle
        } catch (e: PatchRejectedException) {
            respondError(e.error)
            return@handle
        }

        if (item is LastUpdateEntity) {
            item.updated()
        }

        Push.launch {
            Push.sendPushNotificationToAll(
                PushNotification.EntityUpdated(entityKClass, item.id.value.toString(), false),
            )
        }

        call.response.header(HttpHeaders.Location, href(resources.itemSerializer, resources.item(item.id.value.toString())))
        call.respondText("$base created", status = HttpStatusCode.OK)
    }

    handle(resources.itemSerializer, HttpMethod.Delete) { resource ->
        val id = getId(resource) ?: return@handle
        val session = assertMayWriteAtAll() ?: return@handle
        val item = assertEntity(id) ?: return@handle
        assertWritePermission(session, item) ?: return@handle

        val referencesCheck = Database { deleteReferencesCheck(item) }
        if (!referencesCheck) {
            respondError(Error.EntityDeleteReferencesExist())
            return@handle
        }

        Database { item.delete() }

        Push.launch {
            Push.sendPushNotificationToAll(
                PushNotification.EntityDeleted(entityKClass, item.id.value.toString()),
            )
        }

        call.respondText("$base deleted", status = HttpStatusCode.NoContent)
    }
}
