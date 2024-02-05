package screenmodel

import backend.supabase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import utils.putEncoded

class InventoryItemModel : ScreenModel {
    fun create(displayName: String, description: String?) = screenModelScope.async(Dispatchers.IO) {
        supabase.postgrest.from("inventory")
            .insert(
                buildJsonObject {
                    putEncoded("created_at", Clock.System.now())
                    put("display_name", displayName)
                    put("description", description)
                }
            )
    }
}
