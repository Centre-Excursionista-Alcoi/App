package org.centrexcursionistalcoi.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.centrexcursionistalcoi.app.data.SectionD

@Entity
data class Section(
    @PrimaryKey override val id: Int = 0,
    override val createdAt: Instant = Clock.System.now(),
    val displayName: String = ""
): DatabaseEntity<SectionD> {
    companion object : EntityDeserializer<SectionD, Section> {
        override fun deserialize(source: SectionD): Section {
            return Section(
                id = source.id!!,
                createdAt = Instant.fromEpochMilliseconds(source.createdAt!!),
                displayName = source.displayName
            )
        }
    }

    override fun serializable(): SectionD = SectionD(
        id = id.takeIf { it > 0 },
        createdAt = createdAt.toEpochMilliseconds(),
        displayName = displayName
    )

    override fun validate(): Boolean = serializable().validate()
}
