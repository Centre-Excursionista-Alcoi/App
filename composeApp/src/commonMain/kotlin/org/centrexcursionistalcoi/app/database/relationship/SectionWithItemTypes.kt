package org.centrexcursionistalcoi.app.database.relationship

import androidx.room.Embedded
import androidx.room.Relation
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.Section

data class SectionWithItemTypes(
    @Embedded val section: Section,
    @Relation(
        parentColumn = "id",
        entityColumn = "sectionId"
    )
    val itemTypes: List<ItemType>
)
