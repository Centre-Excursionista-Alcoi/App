package org.centrexcursionistalcoi.app.database.entity

import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.assertJsonEquals
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.utils.encodeOne
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.utils.toUuid
import kotlin.test.Test

class TestInventoryItem {
    @Test
    fun `test entity serializes the same as data class`() = runTest {
        Database.initForTests()

        val inventoryItemId = "f857f38b-a401-4328-b181-5bfa4fde4698".toUuid()
        val inventoryItemTypeId = "a53092b1-b9cd-40c9-a3c5-88f595b6b001".toUuid()

        val inventoryItemEntity = Database {
            InventoryItemEntity.new(inventoryItemId) {
                type = InventoryItemTypeEntity.new(inventoryItemTypeId) {
                    displayName = "Test Type"
                }
                variation = "abc"
                nfcId = byteArrayOf(0, 1, 2, 3)
                manufacturerTraceabilityCode = "abc"
            }
        }
        val inventoryItemClass = InventoryItem(
            id = inventoryItemId,
            type = inventoryItemTypeId,
            variation = "abc",
            nfcId = byteArrayOf(0, 1, 2, 3),
            manufacturerTraceabilityCode = "abc"
        )

        assertJsonEquals(
            encodeOne(InventoryItem.serializer(), inventoryItemEntity),
            json.encodeToString(InventoryItem.serializer(), inventoryItemClass)
        )
    }
}
