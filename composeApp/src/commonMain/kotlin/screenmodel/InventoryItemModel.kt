package screenmodel

import backend.database.InventoryItem
import backend.supabase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.datetime.Clock

class InventoryItemModel : ScreenModel {
    fun create(displayName: String) = screenModelScope.async(Dispatchers.IO) {
        val item = InventoryItem(
            id = 0L,
            createdAt = Clock.System.now(),
            displayName = displayName,
            localizedDisplayName = null,
            categoryId = null
        )
        supabase.postgrest.from("inventory").insert(item)
    }
}
