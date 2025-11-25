package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.table.EventMembers
import org.centrexcursionistalcoi.app.database.table.Events
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.request.UpdateEventRequest
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

class EventEntity(id: EntityID<UUID>) : UUIDEntity(id), EntityDataConverter<Event, Uuid>, EntityPatcher<UpdateEventRequest> {
    companion object: UUIDEntityClass<EventEntity>(Events) {
        private val logger = LoggerFactory.getLogger("EventEntity")

        fun forSession(session: UserSession?) = if (session == null) {
            // Not logged in, only show public events (without department)
            logger.debug("Unauthenticated user, fetching public events...")
            find { Events.department eq null }
        } else if (session.isAdmin()) {
            // If admin, show all events
            logger.debug("Admin user ${session.sub} fetching all events...")
            all()
        } else {
            // Logged in, show public events, and events for the user's department
            logger.debug("Fetching events for user ${session.sub}")
            logger.debug("Fetching user departments for user ${session.sub}")
            val userDepartments = transaction {
                DepartmentMemberEntity.getUserDepartments(session.sub, isConfirmed = true)
                    .map { it.department.id.value }
            }
            logger.debug("User {} is in departments {}. Fetching events...", session.sub, userDepartments)
            find {
                (Events.department eq null) or (Events.department inList userDepartments)
            }
        }
    }

    val created by Events.created
    var updated by Events.updated
    private set

    var start by Events.start
    var end by Events.end

    var place by Events.place

    var title by Events.title
    var description by Events.description

    var maxPeople by Events.maxPeople
    var requiresConfirmation by Events.requiresConfirmation

    var department by DepartmentEntity optionalReferencedOn Events.department
    var image by FileEntity optionalReferencedOn Events.image

    val userReferences by UserReferenceEntity via EventMembers

    context(_: JdbcTransaction)
    override fun toData(): Event = Event(
        id = id.value.toKotlinUuid(),
        start = start.toKotlinLocalDateTime(),
        end = end?.toKotlinLocalDateTime(),
        place = place,
        title = title,
        description = description,
        maxPeople = maxPeople,
        requiresConfirmation = requiresConfirmation,
        department = department?.id?.value?.toKotlinUuid(),
        image = image?.id?.value?.toKotlinUuid(),
        userSubList = userReferences.map { it.sub.value },
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateEventRequest) {
        request.start?.let { start = it.toJavaLocalDateTime() }
        request.end?.let { end = it.toJavaLocalDateTime() }
        request.place?.let { place = it }
        request.title?.let { title = it }
        request.description?.let { description = it }
        request.maxPeople?.let { maxPeople = it }
        request.requiresConfirmation?.let { requiresConfirmation = it }
        request.department?.let { department = DepartmentEntity.findById(it.toJavaUuid()) }
        request.image?.let { image = FileEntity.updateOrCreate(it) }

        updated = now()
    }
}
