package screenmodel

import backend.database.Category
import backend.database.InventoryItem
import backend.supabase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.aakira.napier.Napier
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock

class MainScreenModel : ScreenModel {
    companion object {
        private val defaultCategories = listOf(
            Category(1000, Clock.System.now(), "Muntanyisme", "hiking"),
            Category(1001, Clock.System.now(), "Escalada", "carabiner"),
            Category(1002, Clock.System.now(), "Espeleologia", "cave"),
        )
    }

    val userLoggedOut = MutableStateFlow(false)

    val currentUser = MutableStateFlow<UserInfo?>(null)

    private var categories: List<Category>? = null
    val items = MutableStateFlow<List<InventoryItem>?>(null)

    init {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            userLoggedOut.tryEmit(true)
        } else {
            currentUser.tryEmit(user)
        }
        loadCategories().invokeOnCompletion {
            Napier.i { "Finished loading data." }
        }
    }

    private fun loadCategories() = screenModelScope.async(Dispatchers.IO) {
        Napier.i { "Loading categories..." }
        val categoryList = supabase.postgrest
            .from("categories")
            .select()
            .decodeList<Category>()
            .also { categories = it }
        Napier.d { "Decoded ${categoryList.size} categories." }

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
            categories = categoryList.toMutableList().apply { addAll(createCategories) }
            Napier.i { "Categories created!" }
        }

        loadInventoryItems()
    }

    private suspend fun loadInventoryItems() {
        val categories = categories ?: emptyList()
        println("Loading inventory items...")
        val inventoryItems = supabase.postgrest
            .from("inventory")
            .select()
            .decodeList<InventoryItem>()
            .onEach { item ->
                val category = categories.find { it.id == item.id }
                item.category = category
            }
        items.tryEmit(inventoryItems)
        println("Decoded ${inventoryItems.size} inventory items.")
    }
}
