package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalTime
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.table.EventMembers
import org.centrexcursionistalcoi.app.database.table.Events
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.request.UpdateEventRequest
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class EventEntity(id: EntityID<UUID>) : UUIDEntity(id), EntityDataConverter<Event, Uuid>, EntityPatcher<UpdateEventRequest> {
    companion object: UUIDEntityClass<EventEntity>(Events)

    val created by Events.created
    var updated by Events.updated
    private set

    var date by Events.date
    var time by Events.time

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
        date = date.toKotlinLocalDate(),
        time = time?.toKotlinLocalTime(),
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
        request.date?.let { date = it.toJavaLocalDate() }
        request.time?.let { time = it.toJavaLocalTime() }
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
