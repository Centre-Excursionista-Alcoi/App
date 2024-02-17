package backend

import backend.data.database.Category
import backend.data.database.InventoryItem
import backend.wrapper.SupabaseWrapper
import io.github.aakira.napier.Napier
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
    suspend fun loadCategories(): List<Category> {
        Napier.i { "Loading categories..." }
        val categoryList = SupabaseWrapper.postgrest
            .selectList("categories", Category::class)
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
            val result = SupabaseWrapper.postgrest.insert("categories", createCategories)
            Napier.d { "Creation result: ${result.data}" }
            categories.value = categoryList.toMutableList().apply { addAll(createCategories) }
            Napier.i { "Categories created!" }
        }

        return categories.value!!
    }

    /**
     * Get the categories from the database or the local cache if they are already loaded.
     */
    suspend fun getCategories(): List<Category> {
        return categories.value ?: loadCategories()
    }

    suspend fun loadInventoryItems(): List<InventoryItem> {
        val categories = getCategories()
        Napier.d { "There are ${categories.size} categories available." }
        Napier.i { "Loading inventory items..." }
        val items = SupabaseWrapper.postgrest
            .selectList("inventory", InventoryItem::class)
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

        return items
    }

    /**
     * Get the inventory items from the database or the local cache if they are already loaded.
     */
    suspend fun getInventoryItems(): List<InventoryItem> {
        return inventoryItems.value ?: loadInventoryItems()
    }
}
