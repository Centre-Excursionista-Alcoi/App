package org.centrexcursionistalcoi.app.database.entity

import kotlin.test.Test
import kotlin.uuid.toKotlinUuid
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.assertJsonEquals
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.utils.toUUID

class TestInventoryItem {
    @Test
    fun `test entity serializes the same as data class`() = runTest {
        Database.init(TEST_URL)

        val inventoryItemId = "f857f38b-a401-4328-b181-5bfa4fde4698".toUUID()
        val inventoryItemTypeId = "a53092b1-b9cd-40c9-a3c5-88f595b6b001".toUUID()

        val inventoryItemEntity = Database {
            InventoryItemEntity.new(inventoryItemId) {
                type = InventoryItemTypeEntity.new(inventoryItemTypeId) {
                    displayName = "Test Type"
                }
                variation = "abc"
                nfcId = byteArrayOf(0, 1, 2, 3)
            }
        }
        val inventoryItemClass = InventoryItem(
            id = inventoryItemId.toKotlinUuid(),
            type = inventoryItemTypeId.toKotlinUuid(),
            variation = "abc",
            nfcId = byteArrayOf(0, 1, 2, 3)
        )

        assertJsonEquals(
            json.encodeEntityToString(inventoryItemEntity),
            json.encodeToString(InventoryItem.serializer(), inventoryItemClass)
        )
    }
}
