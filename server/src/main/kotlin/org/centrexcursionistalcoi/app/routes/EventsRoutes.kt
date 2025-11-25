package org.centrexcursionistalcoi.app.routes

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.uuid.toKotlinUuid
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.table.Events
import org.centrexcursionistalcoi.app.integration.Telegram
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.UpdateEventRequest
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.jetbrains.exposed.v1.core.eq

fun Route.eventsRoutes() {
    provideEntityRoutes(
        base = "events",
        entityClass = EventEntity,
        idTypeConverter = { UUID.fromString(it) },
        listProvider = { session -> EventEntity.forSession(session) },
        creator = { formParameters ->
            var start: LocalDateTime? = null
            var end: LocalDateTime? = null
            var place: String? = null
            var title: String? = null
            var description: String? = null
            var maxPeople: Int? = null
            var requiresConfirmation = false
            var departmentId: UUID? = null
            val image = FileRequestData()

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        when (partData.name) {
                            "start" -> start = LocalDateTime.parse(partData.value)
                            "end" -> end = LocalDateTime.parse(partData.value)
                            "place" -> place = partData.value
                            "title" -> title = partData.value
                            "description" -> description = partData.value
                            "maxPeople" -> maxPeople = partData.value.toIntOrNull()
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
        val events = EventEntity.forSession(session)

        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//Centre Excursionista d'Alcoi//Events Calendar//EN\r\n")
        for (event in events) {
            val eventData = Database { event.toData() }
            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:${eventData.id}\r\n")
            // example: 20231025T120000Z
            val formattedStart = eventData.start.toJavaLocalDateTime().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
            val formattedEnd = (eventData.end?.toJavaLocalDateTime() ?: LocalDateTime.of(eventData.start.date.toJavaLocalDate(), LocalTime.of(23, 59))).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
            sb.append("DTSTART:$formattedStart\r\n")
            sb.append("DTEND:$formattedEnd\r\n")
            sb.append("SUMMARY:${eventData.title}\r\n")
            sb.append("DESCRIPTION:${eventData.description}\r\n")
            sb.append("LOCATION:${eventData.place}\r\n")
            sb.append("END:VEVENT\r\n")
        }
        sb.append("END:VCALENDAR\r\n")
    }
}
