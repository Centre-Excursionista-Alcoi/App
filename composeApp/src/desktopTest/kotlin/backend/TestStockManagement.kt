package backend

import SupabaseMockingTestbench
import backend.data.app.StockInfo
import backend.data.database.Entry
import backend.data.database.InventoryEntry
import backend.data.database.InventoryItem
import io.github.jan.supabase.postgrest.Postgrest
import io.mockk.coEvery
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class TestStockManagement : SupabaseMockingTestbench(
    supabaseWrapperConfig = {
        install(Postgrest)
    }
) {
    @Test
    fun `loadAvailableStock should load stock info from the database`() {
        val instant = Instant.fromEpochMilliseconds(1708163967)

        // Given
        val items = listOf(
            InventoryItem(1, instant, "Item 1"),
            InventoryItem(2, instant, "Item 2")
        )
        val inventoryEntries = listOf(
            InventoryEntry(1, instant, 1, false),
            InventoryEntry(2, instant, 1, true),
            InventoryEntry(3, instant, 2, false),
            InventoryEntry(4, instant, 2, false),
            InventoryEntry(5, instant, 2, true)
        )
        val userUUID = UUID.randomUUID().toString()
        val collectionUUID = UUID.randomUUID().toString()
        val entries = listOf(
            Entry(1, instant, 1, userUUID, userUUID, collectionUUID, true),
            Entry(2, instant, 1, userUUID, null, collectionUUID, null),
            Entry(3, instant, 1, userUUID, null, collectionUUID, null),
            Entry(4, instant, 2, userUUID, userUUID, collectionUUID, false),
        )
        val expectedStockInfo = mapOf(
            items[0] to StockInfo(1u, 0u, 2u),
            items[1] to StockInfo(2u, 1u, 0u)
        )

        println("Mocking Backend...")
        mockkObject(Backend)
        println("  - Backend.getInventoryItems()...")
        coEvery { Backend.getInventoryItems() } returns items

        println("Replacing Postgrest methods...")
        selectList = { table, _, _, _, _ ->
            when (table) {
                "inventory_entries" -> inventoryEntries
                "entries" -> entries
                else -> error("Unsupported table requested: $table")
            }
        }

        // When
        println("Loading available stock...")
        runBlocking { StockManagement.loadAvailableStock() }

        // Then
        println("Running assertions...")
        val availableStock = StockManagement.availableStock.value
        assertEquals(expectedStockInfo.size, availableStock.size)
        for ((item, stockInfo) in expectedStockInfo) {
            val actualStockInfo = availableStock[item]
            assertEquals(stockInfo, actualStockInfo)
        }
    }
}
