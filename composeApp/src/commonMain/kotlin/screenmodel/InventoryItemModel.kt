package screenmodel

import backend.wrapper.SupabaseWrapper
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import utils.putEncoded

class InventoryItemModel : ScreenModel {
    fun create(displayName: String, description: String?) = screenModelScope.async(Dispatchers.IO) {
        SupabaseWrapper.postgrest
            .insert(
                "inventory",
                buildJsonObject {
                    putEncoded("created_at", Clock.System.now())
                    put("display_name", displayName)
                    put("description", description)
                }
            )
    }
}
