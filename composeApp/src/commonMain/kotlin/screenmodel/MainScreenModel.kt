package screenmodel

import backend.Backend
import backend.data.database.Category
import backend.data.database.InventoryItem
import backend.data.user.Role
import backend.wrapper.SupabaseWrapper
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.aakira.napier.Napier
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import utils.toLocalDate

class MainScreenModel : ScreenModel {
    val userLoggedOut = MutableStateFlow(false)

    val currentUser = MutableStateFlow<UserInfo?>(null)

    val userRoles = MutableStateFlow<List<Role>?>(null)

    /** Whether the user is authorized to use the lending system. */
    val lendingAuth = MutableStateFlow<Boolean?>(null)

    init {
        val user = SupabaseWrapper.auth.currentUserOrNull()
        if (user == null) {
            userLoggedOut.tryEmit(true)
        } else {
            currentUser.tryEmit(user)

            screenModelScope.launch(Dispatchers.IO) {
                loadUserRoles().await()
                loadUserLendingForm().await()
                loadCategories().await()
                loadInventoryItems().await()
                Napier.i { "Finished loading data." }
            }
        }
    }

    fun loadUserRoles() = screenModelScope.async(Dispatchers.IO) {
        Napier.i { "Loading user roles..." }
        val user = SupabaseWrapper.auth.currentUserOrNull()!!
        val rows = SupabaseWrapper.postgrest
            .selectList("user_roles", JsonElement.serializer()) {
                filter { eq("user_id", user.id) }
            }
            .map { it.jsonObject }
        val roles = rows
            // Convert to string
            .map { it.getValue("role").jsonPrimitive.toString().trim('"') }
            // Convert to Role
            .map { Role.valueOf(it) }
        userRoles.emit(roles)
        Napier.i { "Roles: $roles" }

        // Ony for development
        /*if (!roles.contains(Role.INVENTORY_MANAGER)) {
            Napier.i { "Inserting inventory_manager role..." }
            supabase.postgrest
                .from("user_roles")
                .insert(
                    buildJsonObject {
                        put("user_id", user.id)
                        put("role", Role.INVENTORY_MANAGER.name)
                    }
                )
        }*/
    }

    fun loadUserLendingForm() = screenModelScope.async(Dispatchers.IO) {
        val user = SupabaseWrapper.auth.currentUserOrNull()!!
        val year = Clock.System.now().toLocalDate().year
        val form = SupabaseWrapper.postgrest
            .selectOrNull("lending_users", JsonElement.serializer()) {
                filter { eq("user_id", user.id) }
                filter { eq("year", year) }
            }
        lendingAuth.emit(form != null)
    }

    fun loadCategories() = screenModelScope.async(Dispatchers.IO) {
        Backend.loadCategories()
    }

    fun loadInventoryItems() = screenModelScope.async(Dispatchers.IO) {
        Backend.loadInventoryItems()
    }

    private suspend inline fun <reified Type: Any> updateInventoryItem(
        item: InventoryItem,
        property: String,
        value: Type?
    ) {
        SupabaseWrapper.postgrest.update(
            "inventory",
            {
                set(property, value)
            }
        ) {
            filter { eq("id", item.id) }
        }
    }

    fun updateIcon(item: InventoryItem, icon: String?) = screenModelScope.launch(Dispatchers.IO) {
        updateInventoryItem(item, "icon", icon)
        loadInventoryItems().await()
    }

    fun updateDisplayName(item: InventoryItem, displayName: String?) =
        screenModelScope.launch(Dispatchers.IO) {
            updateInventoryItem(item, "display_name", displayName)
            loadInventoryItems().await()
        }

    fun updateCategory(item: InventoryItem, category: Category?) =
        screenModelScope.launch(Dispatchers.IO) {
            updateInventoryItem(item, "category", category?.id)
            loadInventoryItems().await()
        }
}
