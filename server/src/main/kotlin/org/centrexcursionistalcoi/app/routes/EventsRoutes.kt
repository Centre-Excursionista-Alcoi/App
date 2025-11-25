package org.centrexcursionistalcoi.app.routes

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.routing.Route
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.table.Events
import org.centrexcursionistalcoi.app.integration.Telegram
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.UpdateEventRequest
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("EventsRoutes")

fun Route.eventsRoutes() {
    provideEntityRoutes(
        base = "events",
        entityClass = EventEntity,
        idTypeConverter = { UUID.fromString(it) },
        listProvider = { session ->
            if (session == null) {
                // Not logged in, only show public events (without department)
                logger.debug("Unauthenticated user, fetching public events...")
                EventEntity.find { Events.department eq null }
            } else if (session.isAdmin()) {
                // If admin, show all events
                logger.debug("Admin user ${session.sub} fetching all events...")
                EventEntity.all()
            } else {
                // Logged in, show public events, and events for the user's department
                logger.debug("Fetching events for user ${session.sub}")
                logger.debug("Fetching user departments for user ${session.sub}")
                val userDepartments = transaction {
                    DepartmentMemberEntity.getUserDepartments(session.sub, isConfirmed = true)
                        .map { it.department.id.value }
                }
                logger.debug("User {} is in departments {}. Fetching events...", session.sub, userDepartments)
                EventEntity.find {
                    (Events.department eq null) or (Events.department inList userDepartments)
                }
            }
        },
        creator = { formParameters ->
            var date: LocalDate? = null
            var time: LocalTime? = null
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
                            "date" -> date = LocalDate.parse(partData.value)
                            "time" -> time = LocalTime.parse(partData.value)
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

            date ?: throw NullPointerException("Missing date")
            place ?: throw NullPointerException("Missing place")
            title ?: throw NullPointerException("Missing title")

            // Check that the department exists if departmentId is provided
            val department = departmentId?.let {
                Database { DepartmentEntity.findById(it) }  ?: throw NoSuchElementException("Department with id $it does not exist")
            }
            val imageEntity = if (image.isNotEmpty()) image.newEntity() else null

            Database {
                EventEntity.new {
                    this.date = date
                    this.time = time
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
}
