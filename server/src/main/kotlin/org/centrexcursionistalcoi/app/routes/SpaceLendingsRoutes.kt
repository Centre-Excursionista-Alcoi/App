package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.sync.Mutex

import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.SPACE_LENDINGS_MANAGER_GROUP_NAME
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.data.SpaceLendingFileKind
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.SpaceLendingFiles
import org.centrexcursionistalcoi.app.database.table.SpaceLendingKeys
import org.centrexcursionistalcoi.app.database.table.SpaceLendings
import org.centrexcursionistalcoi.app.database.utils.encodeList
import org.centrexcursionistalcoi.app.database.utils.encodeOne
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.notifications.Email
import org.centrexcursionistalcoi.app.translation.locale
import org.centrexcursionistalcoi.app.notifications.EmailTemplate
import org.centrexcursionistalcoi.app.notifications.EmailRecipient
import java.util.Locale
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.request.AttachPaymentProofRequest
import org.centrexcursionistalcoi.app.request.CreateSpaceLendingRequest
import org.centrexcursionistalcoi.app.request.MissingPartException
import org.centrexcursionistalcoi.app.request.SetSpaceLendingPaymentRequest
import org.centrexcursionistalcoi.app.request.SubmitSpaceLendingReportRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceLendingAttendeesRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceLendingRequest
import org.centrexcursionistalcoi.app.request.receiveJson
import org.centrexcursionistalcoi.app.request.receiveRequestWithFiles
import org.centrexcursionistalcoi.app.routes.sync.SyncSection
import org.centrexcursionistalcoi.app.routes.sync.SyncSections
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.isSpaceLendingsManager
import org.centrexcursionistalcoi.app.today
import org.centrexcursionistalcoi.app.utils.SpacePricing
import org.centrexcursionistalcoi.app.utils.hasSpaceLendingConflict
import org.centrexcursionistalcoi.app.utils.isClosedDuring
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

/**
 * Space lendings are created and modified one at a time, so two requests can never take the same nights.
 */
private val spaceLendingsMutex = Mutex()

private val logger = LoggerFactory.getLogger("SpaceLendingsRoutes")

/**
 * Finds the lending of the call's `id`, if the user may see it (its owner, or a space lendings manager). Answers
 * [Error.EntityNotFound] otherwise, to not reveal that it exists.
 * @param ownerOnly Whether managers that aren't the owner are rejected.
 * @param managerOnly Whether only space lendings managers (and admins) are accepted.
 * @return `null` if an error has been responded.
 */
private suspend fun RoutingContext.spaceLendingFor(
    session: UserSession,
    id: String,
    ownerOnly: Boolean = false,
    managerOnly: Boolean = false,
): SpaceLendingEntity? {
    val lendingId = id.toUuidOrNull()
    if (lendingId == null) {
        respondError(Error.MalformedId())
        return null
    }
    val lending = Database { SpaceLendingEntity.findById(lendingId) }
    val isOwner = lending != null && Database { lending.userSub?.id?.value } == session.sub
    val allowed = lending != null && when {
        managerOnly -> session.isSpaceLendingsManager()
        ownerOnly -> isOwner
        else -> isOwner || session.isSpaceLendingsManager()
    }
    if (!allowed) {
        respondError(Error.EntityNotFound(SpaceLendingEntity::class, lendingId))
        return null
    }
    return lending
}

private fun Map<Category, Int>.isValidAttendees(): Boolean = values.all { it >= 0 } && values.sum() > 0

/** Recomputes the price of [lending] from the current prices of its space. */
context(_: JdbcTransaction)
private fun SpaceLendingEntity.recomputePrice() {
    totalPrice = SpacePricing.compute(space.prices, attendees, checkIn, checkOut)
}

/** A payment that is settled: nothing more is owed. */
private val settledPayments = setOf(PaymentStatus.COMPLETED, PaymentStatus.REFUNDED)

/**
 * Checks that a stay in [space] from [checkIn] to [checkOut] is valid, and resolves the keys wanted.
 * @param ignore A lending that is being modified, which doesn't collide with itself.
 * @return The keys and their quantity, or `null` if an error has been responded.
 */
private suspend fun RoutingContext.validateStay(
    space: SpaceEntity,
    checkIn: LocalDate,
    checkOut: LocalDate,
    attendees: Map<Category, Int>,
    keysWanted: Map<Uuid, Int>,
    ignore: SpaceLendingEntity? = null,
): List<Pair<SpaceKeyEntity, Int>>? {
    if (checkOut < checkIn) {
        respondError(Error.EndDateCannotBeBeforeStart())
        return null
    }
    if (!attendees.isValidAttendees()) {
        respondError(Error.InvalidArgument("attendees"))
        return null
    }
    if (Database { space.isClosedDuring(checkIn, checkOut) }) {
        respondError(Error.SpaceClosed())
        return null
    }
    // Keys: each must be a key of the space, in a quantity between 1 and its maximum
    val keys = Database {
        keysWanted.filterValues { it != 0 }.map { (keyId, quantity) ->
            val key = SpaceKeyEntity.findById(keyId)
            if (key == null || key.space.id != space.id || quantity !in 1..key.maxQuantity) return@Database null
            key to quantity
        }
    }
    if (keys == null) {
        respondError(Error.InvalidArgument("keys"))
        return null
    }
    if (Database { hasSpaceLendingConflict(space, checkIn, checkOut, ignore) }) {
        respondError(Error.SpaceConflict())
        return null
    }
    return keys
}

/**
 * Emails the user a confirmation of their new lending, in their language [locale], and the people who manage space
 * lendings (and admins) a notice of it, each in theirs. Both with a link to open it in the app.
 */
private fun sendNewSpaceLendingEmails(lending: SpaceLendingEntity, user: UserReferenceEntity, locale: Locale) = Email.launch {
    val id = lending.id.value
    val args = Database {
        mapOf(
            "id" to id.toString(),
            "userName" to user.fullName,
            "spaceName" to lending.space.name,
            "checkIn" to lending.checkIn.toString(),
            "checkOut" to lending.checkOut.toString(),
            "price" to "%.2f €".format(Locale.ROOT, lending.totalPrice),
            "notes" to lending.notes,
        )
    }

    Email.sendTemplate(
        recipients = listOf(EmailRecipient.of(user, locale)),
        template = EmailTemplate.SpaceLendingConfirmation,
        args = args + ("link" to AppLinks.spaceLending(id)),
    )
    Email.sendTemplate(
        recipients = EmailRecipient.staff(SPACE_LENDINGS_MANAGER_GROUP_NAME),
        template = EmailTemplate.NewSpaceLending,
        args = args + ("link" to AppLinks.adminSpaceLending(id)),
    )
}

/** The space lendings [session] can see: all for those who manage them, and only their own for everyone else. */
internal fun spaceLendingsFor(session: UserSession): List<SpaceLendingEntity> = Database {
    val lendings =
        if (session.isSpaceLendingsManager()) SpaceLendingEntity.all().toList()
        else SpaceLendingEntity.find { SpaceLendings.userSub eq session.sub }.toList()
    // Their keys and files loaded for all at once. Holds while the caller stays in this transaction
    SpaceLendingEntity.withDataPreloaded(lendings)
}

fun Route.spaceLendingsRoutes() {
    SyncSections.register(
        SyncSection(
            key = "space_lendings",
            snapshot = { session -> encodeList(SpaceLending.serializer(), spaceLendingsFor(session), session) },
        )
    )
    get<Api.SpaceLendings> {
        val session = getUserSessionOrFail() ?: return@get
        val lendings = spaceLendingsFor(session)
        call.respondText(ContentType.Application.Json) {
            encodeList(SpaceLending.serializer(), lendings, session)
        }
    }
    get<Api.SpaceLendings.Id> { resource ->
        val session = getUserSessionOrFail() ?: return@get
        val lending = spaceLendingFor(session, resource.id) ?: return@get
        call.respondText(ContentType.Application.Json) {
            encodeOne(SpaceLending.serializer(), lending, session)
        }
    }

    // Step 1: the user creates the lending
    postWithLock<Api.SpaceLendings>(spaceLendingsMutex) {
        val session = getUserSessionOrFail() ?: return@postWithLock
        val request = receiveJson(CreateSpaceLendingRequest.serializer()) ?: return@postWithLock

        if (request.checkIn < today()) return@postWithLock respondError(Error.DateMustBeInFuture())

        val userReference = Database { UserReferenceEntity.findById(session.sub) }
            ?: return@postWithLock respondError(Error.UserReferenceNotFound())
        val space = Database { SpaceEntity.findById(request.space) }
            ?: return@postWithLock respondError(Error.EntityNotFound(SpaceEntity::class, request.space))

        // Nobody can make a new lending until they have paid the previous one
        val hasUnpaid = Database {
            SpaceLendingEntity
                .find { (SpaceLendings.userSub eq session.sub) and (SpaceLendings.cancelled eq false) }
                .any { it.paymentStatus !in settledPayments }
        }
        if (hasUnpaid) return@postWithLock respondError(Error.PreviousSpaceLendingNotPaid())

        if (Database { space.conditionsOfUse != null } && !request.acceptConditions) {
            return@postWithLock respondError(Error.ConditionsNotAccepted())
        }
        val keys = validateStay(space, request.checkIn, request.checkOut, request.attendees, request.keys)
            ?: return@postWithLock

        val lending = Database {
            val entity = SpaceLendingEntity.new {
                this.userSub = userReference
                this.space = space
                this.checkIn = request.checkIn
                this.checkOut = request.checkOut
                this.attendees = request.attendees.filterValues { it > 0 }
                this.acceptedConditionsAt = if (request.acceptConditions) now() else null
                this.notes = request.notes?.takeIf { it.isNotBlank() }
            }
            entity.recomputePrice()
            for ((key, quantity) in keys) {
                SpaceLendingKeys.insert {
                    it[SpaceLendingKeys.lending] = entity.id
                    it[SpaceLendingKeys.key] = key.id
                    it[SpaceLendingKeys.quantity] = quantity
                }
            }
            entity
        }
        lending.updated()

        Push.launch {
            Push.sendPushNotificationToGroup(
                PushNotification.NewSpaceLending(spaceLendingId = lending.id.value, userSub = session.sub),
                SPACE_LENDINGS_MANAGER_GROUP_NAME,
            )
        }
        sendNewSpaceLendingEmails(lending, userReference, call.request.locale())

        call.response.header(HttpHeaders.Location, "/space_lendings/${lending.id.value}")
        call.respond(HttpStatusCode.Created)
    }

    // Until the keys are picked up, the user (or a manager) can change dates, attendees, keys and notes
    patchWithLock<Api.SpaceLendings.Id>(spaceLendingsMutex) { resource ->
        val session = getUserSessionOrFail() ?: return@patchWithLock
        val lending = spaceLendingFor(session, resource.id) ?: return@patchWithLock
        val request = receiveJson(UpdateSpaceLendingRequest.serializer()) ?: return@patchWithLock
        notPickedUpOrFail(lending) ?: return@patchWithLock

        val checkIn = request.checkIn ?: Database { lending.checkIn }
        val checkOut = request.checkOut ?: Database { lending.checkOut }
        val attendees = request.attendees ?: Database { lending.attendees }
        val space = Database { lending.space }
        if (request.checkIn != null && checkIn < today()) return@patchWithLock respondError(Error.DateMustBeInFuture())
        val keysWanted = request.keys ?: Database { lending.keys().associate { it.key to it.quantity } }
        val keys = validateStay(space, checkIn, checkOut, attendees, keysWanted, ignore = lending) ?: return@patchWithLock

        Database {
            lending.checkIn = checkIn
            lending.checkOut = checkOut
            lending.attendees = attendees.filterValues { it > 0 }
            request.notes?.let { lending.notes = it.takeIf { notes -> notes.isNotBlank() } }
            lending.recomputePrice()
            if (request.keys != null) {
                SpaceLendingKeys.deleteWhere { SpaceLendingKeys.lending eq lending.id }
                for ((key, quantity) in keys) {
                    SpaceLendingKeys.insert {
                        it[SpaceLendingKeys.lending] = lending.id
                        it[SpaceLendingKeys.key] = key.id
                        it[SpaceLendingKeys.quantity] = quantity
                    }
                }
            }
        }
        lending.updated()
        call.respond(HttpStatusCode.NoContent)
    }

    postWithLock<Api.SpaceLendings.Id.Cancel>(spaceLendingsMutex) { resource ->
        val session = getUserSessionOrFail() ?: return@postWithLock
        val lending = spaceLendingFor(session, resource.parent.id) ?: return@postWithLock
        notPickedUpOrFail(lending) ?: return@postWithLock
        Database { lending.cancelled = true }
        lending.updated()
        call.respond(HttpStatusCode.NoContent)
    }

    postWithLock<Api.SpaceLendings.Id.Attendees>(spaceLendingsMutex) { resource ->
        val session = getUserSessionOrFail() ?: return@postWithLock
        val lending = spaceLendingFor(session, resource.parent.id) ?: return@postWithLock
        val request = receiveJson(UpdateSpaceLendingAttendeesRequest.serializer()) ?: return@postWithLock
        if (!request.attendees.isValidAttendees()) return@postWithLock respondError(Error.InvalidArgument("attendees"))
        if (Database { lending.cancelled || lending.paymentStatus in settledPayments }) {
            return@postWithLock respondError(Error.InvalidSpaceLendingState("The attendees of a paid or cancelled lending cannot change"))
        }
        Database {
            lending.attendees = request.attendees.filterValues { it > 0 }
            lending.recomputePrice()
        }
        lending.updated()
        call.respond(HttpStatusCode.NoContent)
    }

    postWithLock<Api.SpaceLendings.Id.Report>(spaceLendingsMutex) { resource ->
        val session = getUserSessionOrFail() ?: return@postWithLock
        val lending = spaceLendingFor(session, resource.parent.id, ownerOnly = true) ?: return@postWithLock
        val received = receiveRequestWithFiles(SubmitSpaceLendingReportRequest.serializer()) ?: return@postWithLock
        val request = received.request
        // The notes and issues can only be left once the lending is over
        if (Database { lending.cancelled || today() < lending.checkOut }) {
            return@postWithLock respondError(Error.InvalidSpaceLendingState("The report can only be sent once the lending is over"))
        }
        val rules = FileReadWriteRules(readUsers = listOf(session.sub), readGroups = listOf(ADMIN_GROUP_NAME))
        try {
            received.withUploads {
                Database {
                    request.reportNotes?.takeIf { it.isNotBlank() }?.let { lending.reportNotes = it }
                    request.reportIssues?.takeIf { it.isNotBlank() }?.let { lending.reportIssues = it }
                    for ((files, kind) in listOf(
                        request.reportNotesFiles to SpaceLendingFileKind.REPORT_NOTES,
                        request.reportIssuesFiles to SpaceLendingFileKind.REPORT_ISSUES,
                    )) {
                        for (file in files.filterNot { it.isEmpty() }) {
                            val entity = FileEntity.newFrom(file, rules)
                            SpaceLendingFiles.insert {
                                it[SpaceLendingFiles.lending] = lending.id
                                it[SpaceLendingFiles.file] = entity.id
                                it[SpaceLendingFiles.kind] = kind
                            }
                        }
                    }
                    lending.reportSubmittedAt = now()
                }
            }
        } catch (e: MissingPartException) {
            logger.error("Space lending report refers to a missing part", e)
            return@postWithLock respondError(Error.MalformedRequest())
        }
        lending.updated()
        call.respond(HttpStatusCode.NoContent)
    }

    postWithLock<Api.SpaceLendings.Id.PaymentProof>(spaceLendingsMutex) { resource ->
        val session = getUserSessionOrFail() ?: return@postWithLock
        val lending = spaceLendingFor(session, resource.parent.id) ?: return@postWithLock
        val received = receiveRequestWithFiles(AttachPaymentProofRequest.serializer()) ?: return@postWithLock
        val files = received.request.files.filterNot { it.isEmpty() }
        if (files.isEmpty()) return@postWithLock respondError(Error.MissingFile())
        val ownerSub = Database { lending.userSub?.id?.value }
        val rules = FileReadWriteRules(readUsers = listOfNotNull(ownerSub), readGroups = listOf(ADMIN_GROUP_NAME))
        try {
            received.withUploads {
                Database {
                    for (file in files) {
                        val entity = FileEntity.newFrom(file, rules)
                        SpaceLendingFiles.insert {
                            it[SpaceLendingFiles.lending] = lending.id
                            it[SpaceLendingFiles.file] = entity.id
                            it[SpaceLendingFiles.kind] = SpaceLendingFileKind.PAYMENT_PROOF
                        }
                    }
                }
            }
        } catch (e: MissingPartException) {
            logger.error("Payment proof refers to a missing part", e)
            return@postWithLock respondError(Error.MalformedRequest())
        }
        lending.updated()
        call.respond(HttpStatusCode.NoContent)
    }

    postWithLock<Api.SpaceLendings.Id.Payment>(spaceLendingsMutex) { resource ->
        val session = getUserSessionOrFail() ?: return@postWithLock
        val lending = spaceLendingFor(session, resource.parent.id, managerOnly = true) ?: return@postWithLock
        val request = receiveJson(SetSpaceLendingPaymentRequest.serializer()) ?: return@postWithLock
        if (request.status in settledPayments && Database { lending.returnedAt == null }) {
            return@postWithLock respondError(Error.InvalidSpaceLendingState("The lending is paid once the keys have been returned"))
        }
        Database { lending.paymentStatus = request.status }
        lending.updated()
        call.respond(HttpStatusCode.NoContent)
    }

    // Step 2: a manager hands the keys over. From then on, the lending is locked.
    postWithLock<Api.SpaceLendings.Id.Pickup>(spaceLendingsMutex) { resource ->
        val session = getUserSessionOrFail() ?: return@postWithLock
        val lending = spaceLendingFor(session, resource.parent.id, managerOnly = true) ?: return@postWithLock
        notPickedUpOrFail(lending) ?: return@postWithLock
        Database {
            lending.pickedUpAt = now()
            lending.pickedUpBy = UserReferenceEntity.findById(session.sub)
            SpaceLendingKeys.update({ SpaceLendingKeys.lending eq lending.id }) {
                it[givenBy] = session.sub
                it[givenAt] = now()
            }
        }
        lending.updated()
        call.respond(HttpStatusCode.NoContent)
    }

    // Step 3: a manager takes the keys back, and then the lending gets paid
    postWithLock<Api.SpaceLendings.Id.Return>(spaceLendingsMutex) { resource ->
        val session = getUserSessionOrFail() ?: return@postWithLock
        val lending = spaceLendingFor(session, resource.parent.id, managerOnly = true) ?: return@postWithLock
        if (Database { lending.pickedUpAt == null || lending.returnedAt != null }) {
            return@postWithLock respondError(Error.InvalidSpaceLendingState("The keys must have been picked up, and not returned yet"))
        }
        Database {
            lending.returnedAt = now()
            lending.returnedBy = UserReferenceEntity.findById(session.sub)
            SpaceLendingKeys.update({ SpaceLendingKeys.lending eq lending.id }) {
                it[returnedTo] = session.sub
                it[returnedAt] = now()
            }
            // Nothing to pay
            if (lending.totalPrice <= 0.0) lending.paymentStatus = PaymentStatus.COMPLETED
        }
        lending.updated()
        call.respond(HttpStatusCode.NoContent)
    }
}

/**
 * Responds an error and returns `null` unless [lending] can still be modified: it hasn't been picked up, and it
 * isn't cancelled.
 */
private suspend fun RoutingContext.notPickedUpOrFail(lending: SpaceLendingEntity): Unit? {
    val locked = Database { lending.cancelled || lending.pickedUpAt != null }
    if (locked) {
        respondError(Error.InvalidSpaceLendingState("The lending cannot be modified once its keys have been picked up, or if it is cancelled"))
        return null
    }
    return Unit
}
