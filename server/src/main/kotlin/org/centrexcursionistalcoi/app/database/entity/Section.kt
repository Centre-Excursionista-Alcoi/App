package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.SectionD
import org.centrexcursionistalcoi.app.database.common.SerializableEntity
import org.centrexcursionistalcoi.app.database.table.SectionsTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Section(id: EntityID<Int>) : IntEntity(id), SerializableEntity<SectionD> {
    companion object : IntEntityClass<Section>(SectionsTable)

    val createdAt by SectionsTable.createdAt

    var displayName by SectionsTable.displayName

    override fun serializable() = SectionD(
        id.value,
        createdAt.toEpochMilli(),
        displayName
    )
}
