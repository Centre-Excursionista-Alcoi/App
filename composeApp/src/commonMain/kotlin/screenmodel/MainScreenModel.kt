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
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainScreenModel : ScreenModel {
    val userLoggedOut = MutableStateFlow(false)

    val currentUser = MutableStateFlow<UserInfo?>(null)

    val categories = MutableStateFlow<List<Category>?>(null)
    val items = MutableStateFlow<List<InventoryItem>?>(null)

    init {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            userLoggedOut.tryEmit(true)
        } else {
            currentUser.tryEmit(user)
        }
        loadInventoryItems().invokeOnCompletion {
            Napier.i { "Finished loading data." }
        }
    }

    private fun loadCategories() = screenModelScope.async(Dispatchers.IO) {
        Napier.i { "Loading categories..." }
        val categories = supabase.postgrest
            .from("categories")
            .select()
            .decodeList<Category>()
            .also(categories::tryEmit)
        Napier.d { "Decoded ${categories.size} categories." }

        loadInventoryItems().await()
    }

    private fun loadInventoryItems() = screenModelScope.async(Dispatchers.IO) {
        val categories = categories.value ?: emptyList()
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
