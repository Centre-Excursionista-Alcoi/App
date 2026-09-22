package org.centrexcursionistalcoi.app.routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.unmetRequirements
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.table.EventMembers
import org.centrexcursionistalcoi.app.database.table.Events
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.database.table.UserQualifications
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.integration.Telegram
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.request.CreateEventRequest
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.UpdateEventRequest
import org.centrexcursionistalcoi.app.security.validatedQualificationRequirements
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Clock.System.now
import kotlin.time.toJavaInstant
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

private val eventAssistanceMutex = Mutex()

/**
 * Parses the `qualificationRequirements` form field: a JSON array of groups, each a JSON array of qualification
 * ids (e.g. `[["<id>"],["<id>","<id>"]]`).
 * @throws IllegalArgumentException if it isn't that.
 */
private fun parseQualificationRequirements(value: String): List<List<UUID>> {
    val groups = try {
        json.decodeFromString(ListSerializer(ListSerializer(String.serializer())), value)
    } catch (e: SerializationException) {
        throw IllegalArgumentException("Malformed qualificationRequirements", e)
    }
    return groups.map { group -> group.map { it.toUUIDOrNull() ?: throw IllegalArgumentException("Malformed qualification id: $it") } }
}

fun Route.eventsRoutes() {
    provideEntityRoutes(
        base = "events",
        entityClass = EventEntity,
        idTypeConverter = { it.toUUIDOrNull() },
        listProvider = { session -> EventEntity.forSession(session) },
        visibleTo = { event, session -> event.isVisibleTo(session) },
        // TODO(#659): multipart creation, kept only for app installs predating jsonCreator below -- the app
        //   always sends JSON for events now. Delete this whole `creator` lambda once the app version requiring
        //   it is unsupported. Note it never read requiresInsurance (see jsonCreator's comment) -- don't port
        //   that gap forward if this ever needs touching before removal.
        creator = { formParameters ->
            var start: Instant? = null
            var end: Instant? = null
            var place: String? = null
            var title: String? = null
            var description: String? = null
            var maxPeople: Long? = null
            var requiresConfirmation = false
            var departmentId: UUID? = null
            var qualificationRequirements: List<List<UUID>> = emptyList()
            val image = FileRequestData()

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        when (partData.name) {
                            "start" -> start = partData.value.toLong().let(Instant::ofEpochMilli)
                            "end" -> end = partData.value.toLong().let(Instant::ofEpochMilli)
                            "place" -> place = partData.value
                            "title" -> title = partData.value
                            "description" -> description = partData.value
                            "maxPeople" -> maxPeople = partData.value.toLongOrNull()
                            "requiresConfirmation" -> requiresConfirmation = partData.value.toBoolean()
                            "department" -> departmentId = partData.value.toUUIDOrNull()
                            "qualificationRequirements" -> qualificationRequirements = parseQualificationRequirements(partData.value)
                            "image" -> {
                                image.populate(partData)
                            }
                        }
                    }
                    is PartData.FileItem -> {
                        image.populate(partData)
                    }
                    else -> { /* nothing */ }
                }
            }

            start ?: throw NullPointerException("Missing start")
            place ?: throw NullPointerException("Missing place")
            title ?: throw NullPointerException("Missing title")

            // Check that the department exists if departmentId is provided
            val department = departmentId?.let {
                Database { DepartmentEntity.findById(it) }  ?: throw NoSuchElementException("Department with id $it does not exist")
            }

            // Checked before anything is created, so an invalid requirement can't leave a half-created event (or
            // an orphaned image) behind. Throws an IllegalArgumentException, which is reported as a 400.
            val requirements = Database { validatedQualificationRequirements(department?.id?.value, qualificationRequirements) }

            val imageEntity = if (image.isNotEmpty()) image.newEntity() else null

            Database {
                EventEntity.new {
                    this.start = start
                    this.end = end
                    this.place = place
                    this.title = title
                    this.description = description
                    this.maxPeople = maxPeople
                    this.requiresConfirmation = requiresConfirmation
                    this.department = department
                    this.image = imageEntity
                }.also { it.setQualificationRequirements(requirements) }
            }
        },
        afterCreate = { eventEntity ->
            Telegram.launch {
                val event = Database { eventEntity.toData() }
                Telegram.sendEvent(event)
            }
        },
        onWriteRejected = { event ->
            // The image (if any) was uploaded and persisted before the department could be authorized -- clean
            // it up too, or a rejected creation would leave it orphaned in the files table.
            val image = event.image
            event.delete()
            image?.delete()
        },
        deleteReferencesCheck = { department ->
            // departments are referenced in events, make sure no events reference the department before deleting
            EventEntity.find { Events.department eq department.id }.empty()
        },
        updater = UpdateEventRequest.serializer(),
        createRequestSerializer = CreateEventRequest.serializer(),
        jsonCreator = { request ->
            // Mirrors the multipart creator above -- same department lookup, same requirement validation, same
            // image creation (#659) -- except requiresInsurance is actually wired up here: the multipart creator
            // never read it at all, even though the client already sent it (Event.toMap()) and PATCH already
            // supports it (UpdateEventRequest.requiresInsurance), so it silently had no effect at creation time.
            val department = request.department?.let {
                Database { DepartmentEntity.findById(it.toJavaUuid()) } ?: throw NoSuchElementException("Department with id $it does not exist")
            }

            val requirements = Database {
                validatedQualificationRequirements(
                    department?.id?.value,
                    request.qualificationRequirements.map { group -> group.map { id -> id.toJavaUuid() } },
                )
            }

            val imageEntity = request.image?.let { Database { FileEntity.newFrom(it) } }

            Database {
                EventEntity.new {
                    this.start = request.start.toJavaInstant()
                    this.end = request.end?.toJavaInstant()
                    this.place = request.place
                    this.title = request.title
                    this.description = request.description
                    this.maxPeople = request.maxPeople
                    this.requiresConfirmation = request.requiresConfirmation
                    this.requiresInsurance = request.requiresInsurance
                    this.department = department
                    this.image = imageEntity
                }.also { it.setQualificationRequirements(requirements) }
            }
        },
        writePermission = EntityWritePermission(
            role = DepartmentRole.CONTENT_MANAGER,
            departmentOfEntity = { it.department?.id?.value },
        ),
    )
    get("/events/calendar") {
        val session = call.getUserSession()
        val events = Database { EventEntity.forSession(session).toList() }

        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//Centre Excursionista d'Alcoi//Events Calendar//EN\r\n")
        for (event in events) {
            val eventData = Database { event.toData() }
            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:${eventData.id}\r\n")
            // example: 20231025T120000Z
            val formattedStart = eventData.start.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
            val formattedEnd = (
                    eventData.end?.toLocalDateTime(TimeZone.currentSystemDefault())?.toJavaLocalDateTime()
                        ?: LocalDateTime.of(eventData.start.toLocalDateTime(TimeZone.currentSystemDefault()).date.toJavaLocalDate(), LocalTime.of(23, 59))
                    ).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
            sb.append("DTSTART:$formattedStart\r\n")
            sb.append("DTEND:$formattedEnd\r\n")
            sb.append("SUMMARY:${eventData.title}\r\n")
            sb.append("DESCRIPTION:${eventData.description}\r\n")
            sb.append("LOCATION:${eventData.place}\r\n")
            sb.append("END:VEVENT\r\n")
        }
        sb.append("END:VCALENDAR\r\n")
    }

    postWithLock("/events/{id}/confirm", eventAssistanceMutex) {
        val session = getUserSessionOrFail() ?: return@postWithLock
        val eventId = call.parameters["id"]?.toUUIDOrNull() ?: return@postWithLock call.respondError(Error.InvalidArgument("id"))

        // Make sure the event exists
        val event = Database {
            EventEntity.findById(eventId)
        } ?: return@postWithLock call.respondError(Error.EntityNotFound(EventEntity::class, eventId))

        // Make sure the event is in the future
        val now = now().toJavaInstant()
        if (event.start < now) {
            return@postWithLock call.respondError(Error.EventInThePast())
        }

        // Make sure the user reference exists
        session.getReference() ?: return@postWithLock call.respondError(Error.UserReferenceNotFound())

        // Check that the user has not already been confirmed for the event
        val alreadyConfirmed = Database {
            EventMembers.selectAll()
                .where { (EventMembers.event eq eventId) and (EventMembers.userReference eq session.sub) }
                .count()
        }
        if (alreadyConfirmed > 0) {
            call.respondError(Error.AssistanceAlreadyConfirmed())
            return@postWithLock
        }

        // If the event requires insurance, check that the user has a valid insurance for the event dates
        if (event.requiresInsurance) {
            val from = LocalDateTime.ofInstant(event.start, ZoneId.systemDefault()).toLocalDate()
            val to = if (event.end != null) {
                LocalDateTime.ofInstant(event.end!!, ZoneId.systemDefault()).toLocalDate()
            } else {
                from
            }
            val validInsurances = Database {
                UserInsuranceEntity
                    .find { (UserInsurances.userSub eq session.sub) and (UserInsurances.validFrom lessEq from) and (UserInsurances.validTo greaterEq to) }
                    .count()
            }
            if (validInsurances <= 0) {
                call.respondError(Error.UserDoesNotHaveInsurance())
                return@postWithLock
            }
        }

        // If the event requires qualifications, check that the user holds them (and that they haven't expired)
        val requirements = Database { event.qualificationRequirements() }.map { group -> group.map { it.toKotlinUuid() } }
        if (requirements.isNotEmpty()) {
            val required = requirements.flatten().map { it.toJavaUuid() }
            val held = Database {
                UserQualifications.selectAll()
                    .where {
                        (UserQualifications.userSub eq session.sub) and
                            (UserQualifications.qualification inList required) and
                            (UserQualifications.expiresAt.isNull() or (UserQualifications.expiresAt greater now().toJavaInstant()))
                    }
                    .map { it[UserQualifications.qualification].value.toKotlinUuid() }
                    .toSet()
            }
            val unmet = requirements.unmetRequirements(held)
            if (unmet.isNotEmpty()) {
                call.respondError(Error.MissingQualifications(unmet))
                return@postWithLock
            }
        }

        // Check that the event is not full
        if (event.maxPeople != null) {
            val currentMembers = Database {
                EventMembers.selectAll().where { EventMembers.event eq eventId }.count()
            }
            if (currentMembers >= event.maxPeople!!) {
                call.respondError(Error.EventFull())
                return@postWithLock
            }
        }

        Database {
            EventMembers.insert {
                it[this.event] = eventId
                it[this.userReference] = session.sub
            }
        }
        event.updated()

        Push.launch {
            val department = event.department
            if (department != null) {
                Push.sendPushNotificationToDepartment(
                    event.assistanceConfirmedNotification(session),
                    department.id.value,
                )
            } else {
                Push.sendPushNotificationToAll(
                    event.assistanceConfirmedNotification(session),
                )
            }
        }

        call.respond(HttpStatusCode.NoContent)
    }
    postWithLock("/events/{id}/reject", eventAssistanceMutex) {
        val session = getUserSessionOrFail() ?: return@postWithLock
        val eventId = call.parameters["id"]?.toUUIDOrNull() ?: return@postWithLock call.respondError(Error.InvalidArgument("id"))

        // Make sure the event exists
        val event = Database {
            EventEntity.findById(eventId)
        } ?: return@postWithLock call.respondError(Error.EntityNotFound(EventEntity::class, eventId))

        // Make sure the event is in the future
        val now = now().toJavaInstant()
        if (event.start < now) {
            return@postWithLock call.respondError(Error.EventInThePast())
        }

        // Make sure the user reference exists
        session.getReference() ?: return@postWithLock call.respondError(Error.UserReferenceNotFound())

        // Remove the user from the event members if they were confirmed
        Database {
            EventMembers.deleteWhere { (EventMembers.event eq eventId) and (EventMembers.userReference eq session.sub) }
        }
        event.updated()

        Push.launch {
            val department = event.department
            if (department != null) {
                Push.sendPushNotificationToDepartment(
                    event.assistanceRejectedNotification(session),
                    department.id.value,
                )
            } else {
                Push.sendPushNotificationToAll(
                    event.assistanceRejectedNotification(session),
                )
            }
        }

        call.respond(HttpStatusCode.NoContent)
    }
}
