package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.koin.core.annotation.KoinViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.data.Department
import kotlin.uuid.Uuid

/** A type of gear the user can borrow, and how many of it there are. */
data class CatalogEntry(val type: ReferencedInventoryItemType, val available: Int)

/** The gear of a department ([department] is `null` for what belongs to none), as the lending page groups it. */
data class CatalogSection(val department: Department?, val entries: List<CatalogEntry>)

/** Whether the user can borrow gear yet, and if not, what is missing. */
enum class LendingAvailability {
    /** They haven't signed up for lendings. */
    NOT_REGISTERED,

    /** They have, but have no insurance that covers today. */
    NO_INSURANCE,

    READY,
}

@KoinViewModel
class LendingsPageModel(
    inventoryItemsRepository: InventoryItemsRepository,
    profileRepository: ProfileRepository,
    lendingsRepository: LendingsRepository,
) : ViewModel() {
    val profile = profileRepository.profile.stateInViewModel()
    val inventoryItems = inventoryItemsRepository.selectAllAsFlow().stateInViewModel()
    val lendings = lendingsRepository.selectAllAsFlow().stateInViewModel()

    private val _shoppingList = MutableStateFlow(emptyMap<Uuid, Int>())
    val shoppingList = _shoppingList.asStateFlow()

    val availability = profile.map { profile ->
        when {
            profile == null -> null
            profile.lendingUser == null -> LendingAvailability.NOT_REGISTERED
            profile.activeInsurances().isEmpty() -> LendingAvailability.NO_INSURANCE
            else -> LendingAvailability.READY
        }
    }.stateInViewModel()

    /** The gear, by department (in the order they appear, those without one last), each type once. */
    val catalog = inventoryItems.map { items ->
        items?.let {
            val byType = it.groupBy { item -> item.type }
            val departments = byType.keys.mapNotNull { type -> type.department }.distinctBy { department -> department.id }
            val sections = departments.map { department ->
                CatalogSection(department, byType.entries.filter { (type) -> type.department?.id == department.id }.entries())
            }
            val withoutDepartment = byType.entries.filter { (type) -> type.department == null }
            if (withoutDepartment.isEmpty()) sections else sections + CatalogSection(null, withoutDepartment.entries())
        }
    }.stateInViewModel()

    private fun List<Map.Entry<ReferencedInventoryItemType, List<ReferencedInventoryItem>>>.entries() =
        sortedBy { (type) -> type.displayName }.map { (type, items) -> CatalogEntry(type, items.size) }

    /** How many of each type the user picked so far, by the type's id (as text, which is what Swift can key on). */
    val selectedAmounts = shoppingList.map { list -> list.entries.associate { (id, amount) -> id.toString() to amount } }
        .stateInViewModel(emptyMap())

    /** How many items the user picked in all. */
    val selectedCount = shoppingList.map { list -> list.values.sum() }.stateInViewModel(0)

    /** The screen that confirms borrowing what the user picked. */
    fun checkoutDestination(): Destination = Destination.LendingCreation(shoppingList.value)

    /** The user's lendings that are over, newest first: the history. */
    val history = combine(lendings, profile) { lendings, profile ->
        lendings?.filter { it.user.sub == profile?.sub && !it.status().isPending() }?.sortedByDescending { it.from }
    }.stateInViewModel()

    fun addItemToShoppingList(type: ReferencedInventoryItemType) {
        val currentList = _shoppingList.value.toMutableMap()
        currentList[type.id] = (currentList[type.id] ?: 0) + 1
        _shoppingList.value = currentList
    }

    fun removeItemFromShoppingList(type: ReferencedInventoryItemType) {
        val currentList = _shoppingList.value.toMutableMap()
        val newAmount = (currentList[type.id] ?: 0) - 1
        when {
            newAmount > 0 -> currentList[type.id] = newAmount
            else -> currentList.remove(type.id)
        }
        _shoppingList.value = currentList
    }
}
