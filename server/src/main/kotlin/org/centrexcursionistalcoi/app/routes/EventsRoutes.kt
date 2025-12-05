package org.centrexcursionistalcoi.app.routes

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.time.Clock.System.now
import kotlin.time.toJavaInstant
import kotlin.uuid.toKotlinUuid
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.table.EventMembers
import org.centrexcursionistalcoi.app.database.table.Events
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.integration.Telegram
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.UpdateEventRequest
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

private val eventAssistanceMutex = Mutex()

fun Route.eventsRoutes() {
    provideEntityRoutes(
        base = "events",
        entityClass = EventEntity,
        idTypeConverter = { UUID.fromString(it) },
        listProvider = { session -> EventEntity.forSession(session) },
        creator = { formParameters ->
            var start: Instant? = null
            var end: Instant? = null
            var place: String? = null
            var title: String? = null
            var description: String? = null
            var maxPeople: Long? = null
            var requiresConfirmation = false
            var departmentId: UUID? = null
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
                }
            }.also { eventEntity ->
                Push.launch {
                    if (departmentId != null) {
                        Push.sendPushNotificationToDepartment(
                            PushNotification.NewEvent(
                                eventId = eventEntity.id.value.toKotlinUuid(),
                            ),
                            departmentId!!
                        )
                    } else {
                        Push.sendPushNotificationToAll(
                            PushNotification.NewEvent(
                                eventId = eventEntity.id.value.toKotlinUuid(),
                            )
                        )
                    }
                }
                Telegram.launch {
                    val event = Database { eventEntity.toData() }
                    Telegram.sendEvent(event)
                }
            }
        },
        deleteReferencesCheck = { department ->
            // departments are referenced in events, make sure no events reference the department before deleting
            EventEntity.find { Events.department eq department.id }.empty()
        },
        updater = UpdateEventRequest.serializer(),
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
                    event.assistanceConfirmedNotification(session, true),
                    department.id.value,
                )
            } else {
                Push.sendPushNotificationToAll(
                    event.assistanceConfirmedNotification(session, true),
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
                    event.assistanceRejectedNotification(session, true),
                    department.id.value,
                )
            } else {
                Push.sendPushNotificationToAll(
                    event.assistanceRejectedNotification(session, true),
                )
            }
        }

        call.respond(HttpStatusCode.NoContent)
    }
}
