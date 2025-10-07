package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object LendingItems : Table() {
    val lending = reference("lendingId", Lendings, onDelete = ReferenceOption.CASCADE)
    val item = reference("itemId", InventoryItems, onDelete = ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(lending, item, name = "PK_LendingItem_Lending_Item")

    init {
        index(true, lending, item)
    }
}
