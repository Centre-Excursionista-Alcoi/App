package backend

import backend.data.app.StockInfo
import backend.data.database.Entry
import backend.data.database.InventoryEntry
import backend.data.database.InventoryItem
import backend.wrapper.SupabaseWrapper
import kotlinx.coroutines.flow.MutableStateFlow

object StockManagement {
    val availableStock = MutableStateFlow<Map<InventoryItem, StockInfo>>(emptyMap())

    suspend fun loadAvailableStock() {
        // Load inventory items from the database
        val items = Backend.getInventoryItems()

        val inventoryEntries = SupabaseWrapper.postgrest
            .selectList("inventory_entries", InventoryEntry::class)
        val entries = SupabaseWrapper.postgrest
            .selectList("entries", Entry::class)

        val stockInfo = items
            .associateWith { item -> inventoryEntries.filter { it.inventoryItemId == item.id } }
            .mapValues {  (item, list) ->
                val inUse = entries
                    .filter { it.inventoryItemId == item.id }
                    .count { it.returned == false }
                val reserved = entries
                    .filter { it.inventoryItemId == item.id }
                    .count { it.returned == null }
                StockInfo(
                    available = list.count { !it.exit }.toULong(),
                    inUse = inUse.toULong(),
                    reserved = reserved.toULong()
                )
            }
        availableStock.value = stockInfo
    }
}
