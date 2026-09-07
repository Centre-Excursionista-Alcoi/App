package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import kotlin.uuid.Uuid

/**
 * Junction row for the many-to-many relation between [EventEntity] and [UserEntity] (an event's confirmed
 * assistants). No FK to [UserEntity]: non-admins only get a small subset of the `Users` table synced locally, so a
 * `userSub` here may legitimately not have a matching local user row -- the relation query is an inner join, so
 * such rows are simply excluded from the result, matching the previous manual `filter` behavior.
 */
@Entity(
    tableName = "EventUsers",
    primaryKeys = ["eventId", "userSub"],
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["userSub"])],
)
data class EventUserCrossRef(
    val eventId: Uuid,
    val userSub: String,
)
