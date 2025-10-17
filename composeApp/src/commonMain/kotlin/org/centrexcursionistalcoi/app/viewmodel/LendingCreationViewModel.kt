package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.typing.ShoppingList

class LendingCreationViewModel(
    private val shoppingList: ShoppingList
) : ViewModel() {
    val inventoryItemTypes = InventoryItemTypesRepository.selectAllAsFlow().stateInViewModel()

    val inventoryItems = InventoryItemsRepository.selectAllAsFlow().stateInViewModel()

    private val _from = MutableStateFlow<LocalDate?>(null)
    val from = _from.asStateFlow()

    private val _to = MutableStateFlow<LocalDate?>(null)
    val to = _to.asStateFlow()

    private val _allocatedItems = MutableStateFlow<List<InventoryItem>?>(null)
    val allocatedItems = _allocatedItems.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error = _error.asStateFlow()

    fun setFrom(date: LocalDate) {
        _from.value = date
        allocateItems()
    }

    fun setTo(date: LocalDate) {
        _to.value = date
        allocateItems()
    }

    private fun allocateItems() = viewModelScope.launch(defaultAsyncDispatcher) {
        val from = from.value ?: return@launch
        val to = to.value ?: return@launch

        val allocatedItemsIds = mutableListOf<Uuid>()
        for ((typeId, amount) in shoppingList) {
            try {
                Napier.i { "Trying to allocate x$amount of $typeId from $from to $to..." }
                val items = LendingsRemoteRepository.allocate(typeId, from, to, amount)
                Napier.d { "Items allocated. IDs: $items" }
                allocatedItemsIds.addAll(items)
            } catch (e: IllegalStateException) {
                // Not enough items available
                _error.emit(e)
                _allocatedItems.emit(null)
                return@launch
            } catch (e: IllegalArgumentException) {
                // Some other error
                Napier.e(e) { "Failed to allocate $typeId" }
                _error.emit(e)
                _allocatedItems.emit(null)
                return@launch
            }
        }
        val inventoryItems = inventoryItems.value.orEmpty()

        val allocatedItems = allocatedItemsIds.map { uuid ->
            inventoryItems.first { it.id == uuid }
        }
        _allocatedItems.emit(allocatedItems)
    }

    fun createLending(onSuccess: () -> Unit) {
        val from = from.value ?: return Napier.w { "From date not set" }
        val to = to.value ?: return Napier.w { "To date not set" }
        val items = allocatedItems.value ?: return Napier.w { "Items allocation not ready" }

        viewModelScope.launch(defaultAsyncDispatcher) {
            val itemIds = items.map { it.id }

            LendingsRemoteRepository.create(from, to, itemIds, null)
            Napier.i { "Lending created" }
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }
}
