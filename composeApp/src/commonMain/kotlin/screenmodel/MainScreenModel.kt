package screenmodel

import backend.database.InventoryItem
import backend.supabase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainScreenModel : ScreenModel {
    val userLoggedOut = MutableStateFlow(false)

    val currentUser = MutableStateFlow<UserInfo?>(null)

    val items = MutableStateFlow<List<InventoryItem>?>(null)

    init {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            userLoggedOut.tryEmit(true)
        } else {
            currentUser.tryEmit(user)
        }
        loadInventoryItems()
    }

    private fun loadInventoryItems() = screenModelScope.launch(Dispatchers.IO) {
        println("Loading inventory items...")
        val inventoryItems = supabase.postgrest
            .from("inventory")
            .select()
            .decodeList<InventoryItem>()
        items.tryEmit(inventoryItems)
        println("Decoded ${inventoryItems.size} inventory items.")
    }
}
