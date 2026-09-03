package org.centrexcursionistalcoi.app.database.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import kotlin.uuid.Uuid

@Entity(
    tableName = "Memories",
    foreignKeys = [
        ForeignKey(
            entity = DepartmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["department"],
        ),
        ForeignKey(
            entity = LendingEntity::class,
            parentColumns = ["id"],
            childColumns = ["lending"],
        ),
    ],
    indices = [Index(value = ["department"]), Index(value = ["lending"])],
)
data class MemoryEntity(
    @PrimaryKey
    val id: Uuid,
    val place: String?,
    val externalUsers: String?,
    val text: String,
    val sport: Sports?,
    val department: Uuid?,
    val attachments: List<Uuid>?,
    val submittedBy: String,
    val fromDate: ZonedDateTime,
    val toDate: ZonedDateTime,
    val pdf: Uuid?,
    val lending: Uuid?,
) {
    companion object {
        /** [Memory.members] is persisted separately, as [MemoryMemberCrossRef] rows. */
        fun Memory.toEntity() = MemoryEntity(
            id = id,
            place = place,
            externalUsers = externalUsers,
            text = text,
            sport = sport,
            department = department,
            attachments = attachments,
            submittedBy = submittedBy,
            fromDate = from,
            toDate = to,
            pdf = pdf,
            lending = lending,
        )
    }
}
