package screenmodel

import backend.data.database.Category
import backend.data.database.InventoryItem
import backend.data.user.Role
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
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import utils.toLocalDate

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

    val userRoles = MutableStateFlow<List<Role>?>(null)

    /** Whether the user is authorized to use the lending system. */
    val lendingAuth = MutableStateFlow<Boolean?>(null)

    init {
        val user = supabase.auth.currentUserOrNull()
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
        val user = supabase.auth.currentUserOrNull()!!
        val rows = supabase.postgrest
            .from("user_roles")
            .select {
                filter { eq("user_id", user.id) }
            }
            .decodeList<JsonElement>()
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
        val user = supabase.auth.currentUserOrNull()!!
        val year = Clock.System.now().toLocalDate().year
        val form = supabase.postgrest
            .from("lending_users")
            .select {
                filter { eq("user_id", user.id) }
                filter { eq("year", year) }
            }
            .decodeSingleOrNull<JsonElement>()
        lendingAuth.emit(form != null)
    }

    fun loadCategories() = screenModelScope.async(Dispatchers.IO) {
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
    }

    fun loadInventoryItems() = screenModelScope.async(Dispatchers.IO) {
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

    fun updateIcon(item: InventoryItem, icon: String?) = screenModelScope.launch(Dispatchers.IO) {
        supabase.postgrest
            .from("inventory")
            .update(
                {
                    set("icon", icon)
                }
            ) {
                filter { eq("id", item.id) }
            }
        loadInventoryItems().await()
    }
}
