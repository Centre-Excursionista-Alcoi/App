package backend

import backend.data.database.Category
import backend.data.database.InventoryItem
import io.github.aakira.napier.Napier
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock

object Backend {
    private val defaultCategories = listOf(
        Category(1000, Clock.System.now(), "Muntanyisme", "hiking"),
        Category(1001, Clock.System.now(), "Escalada", "carabiner"),
        Category(1002, Clock.System.now(), "Espeleologia", "cave"),
    )

    val categories = MutableStateFlow<List<Category>?>(null)
    val inventoryItems = MutableStateFlow<List<InventoryItem>?>(null)

    /**
     * Load the categories from the database and create the default ones if they don't exist.
     */
    suspend fun loadCategories() {
        Napier.i { "Loading categories..." }
        val categoryList = supabase.postgrest
            .from("categories")
            .select()
            .decodeList<Category>()
            .also { categories.value = it }
        Napier.d { "Decoded ${categoryList.size} categories." }
        Napier.d {
            "Categories:\n${categoryList.joinToString("\n") { "- ${it.id} :: ${it.displayName}" }}"
        }

        val createCategories = mutableListOf<Category>()
        for (defaultCategory in defaultCategories) {
            val exists = categoryList.find { it.id == defaultCategory.id }
            if (exists != null) continue
            else createCategories.add(defaultCategory)
        }
        if (createCategories.isNotEmpty()) {
            Napier.i { "Creating ${createCategories.size} categories..." }
            val result = supabase.postgrest.from("categories").insert(createCategories)
            Napier.d { "Creation result: ${result.data}" }
            categories.value = categoryList.toMutableList().apply { addAll(createCategories) }
            Napier.i { "Categories created!" }
        }
    }

    suspend fun loadInventoryItems() {
        val categories = categories.value ?: emptyList()
        Napier.d { "There are ${categories.size} categories available." }
        Napier.i { "Loading inventory items..." }
        val items = supabase.postgrest
            .from("inventory")
            .select()
            .decodeList<InventoryItem>()
            .map { item ->
                val category = categories.find { it.id == item.categoryId }
                if (category == null) {
                    Napier.w {
                        "Got an item (#${item.id}) with an invalid category (#${item.categoryId})."
                    }
                }
                item.copy(category = category)
            }
        inventoryItems.emit(items)
        Napier.d { "Decoded ${items.size} inventory items." }
        Napier.d {
            "Inventory Items:\n${items.joinToString("\n") { "- ${it.categoryId} :: ${it.category != null}" }}"
        }
    }
}
