package org.centrexcursionistalcoi.app.database.relation

import androidx.room3.Embedded
import androidx.room3.Junction
import androidx.room3.Relation
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.database.room.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.room.entity.EventEntity
import org.centrexcursionistalcoi.app.database.room.entity.EventUserCrossRef
import org.centrexcursionistalcoi.app.database.room.entity.UserEntity

data class EventWithRelations(
    @Embedded val event: EventEntity,
    @Relation(parentColumns = ["department"], entityColumns = ["id"])
    val department: DepartmentEntity?,
    // Inner-joined via EventUserCrossRef: a userSub with no matching local Users row (e.g. non-admins only get a
    // small subset of Users synced) is silently excluded, matching the previous manual `filter` behavior.
    @Relation(
        entity = UserEntity::class,
        parentColumns = ["id"],
        entityColumns = ["sub"],
        associateBy = Junction(EventUserCrossRef::class, parentColumns = ["eventId"], entityColumns = ["userSub"]),
    )
    val userSubList: List<UserEntity>,
)

fun EventWithRelations.toReferenced(): ReferencedEvent {
    return ReferencedEvent(
        id = event.id,
        start = event.start,
        end = event.end,
        place = event.place,
        title = event.title,
        description = event.description,
        maxPeople = event.maxPeople,
        requiresConfirmation = event.requiresConfirmation,
        requiresInsurance = event.requiresInsurance,
        department = department?.toDepartment(),
        image = event.image,
        userSubList = userSubList.map { it.toUser() },
    )
}
