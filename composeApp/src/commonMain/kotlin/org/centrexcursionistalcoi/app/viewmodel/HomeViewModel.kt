package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository

class HomeViewModel: ViewModel() {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    val profile = ProfileRepository.profile.stateInViewModel()

    val departments = DepartmentsRepository.selectAllAsFlow().stateInViewModel()

    val users = UsersRepository.selectAllAsFlow().stateInViewModel()

    val inventoryItemTypes = InventoryItemTypesRepository.selectAllAsFlow().stateInViewModel()

    val inventoryItems = InventoryItemsRepository.selectAllAsFlow().stateInViewModel()

    /**
     * A map of InventoryItemType ID to amount in the shopping list.
     */
    private val _shoppingList = MutableStateFlow(emptyMap<Uuid, Int>())
    val shoppingList = _shoppingList.asStateFlow()

    fun createDepartment(displayName: String, imageFile: PlatformFile?) = viewModelScope.launch(defaultAsyncDispatcher) {
        val image = imageFile?.readBytes()
        DepartmentsRemoteRepository.create(displayName, image)
    }

    fun delete(department: Department) = viewModelScope.launch(defaultAsyncDispatcher) {
        DepartmentsRemoteRepository.delete(department.id)
    }

    fun createInventoryItemType(displayName: String, description: String, imageFile: PlatformFile?) = viewModelScope.launch(defaultAsyncDispatcher) {
        val image = imageFile?.readBytes()
        InventoryItemTypesRemoteRepository.create(displayName, description.takeUnless { it.isEmpty() }, image)
    }

    fun updateInventoryItemType(id: Uuid, displayName: String?, description: String?, imageFile: PlatformFile?) = viewModelScope.launch(defaultAsyncDispatcher) {
        val image = imageFile?.readBytes()
        InventoryItemTypesRemoteRepository.update(id, displayName, description, image)
    }

    fun delete(item: InventoryItemType) = viewModelScope.launch(defaultAsyncDispatcher) {
        InventoryItemTypesRemoteRepository.delete(item.id)
    }

    fun createInventoryItem(variation: String, type: InventoryItemType, amount: Int) = viewModelScope.launch(defaultAsyncDispatcher) {
        InventoryItemsRemoteRepository.create(variation.takeUnless { it.isEmpty() }, type.id, amount)
    }

    fun delete(item: InventoryItem) = viewModelScope.launch(defaultAsyncDispatcher) {
        InventoryItemsRemoteRepository.delete(item.id)
    }

    fun signUpForLending(fullName: String, nif: String, phoneNumber: String, sports: List<Sports>, address: String, postalCode: String, city: String, province: String, country: String) = viewModelScope.launch(defaultAsyncDispatcher) {
        ProfileRemoteRepository.signUpForLending(fullName, nif, phoneNumber, sports, address, postalCode, city, province, country)
        ProfileRemoteRepository.synchronize()
    }

    fun createInsurance(company: String, policyNumber: String, validFrom: LocalDate, validTo: LocalDate) = viewModelScope.launch(defaultAsyncDispatcher) {
        ProfileRemoteRepository.createInsurance(company, policyNumber, validFrom, validTo)
        ProfileRemoteRepository.synchronize()
    }

    fun addItemToShoppingList(type: InventoryItemType) {
        val currentList = _shoppingList.value.toMutableMap()
        val currentAmount = currentList[type.id] ?: 0
        currentList[type.id] = currentAmount + 1
        _shoppingList.value = currentList
    }

    fun removeItemFromShoppingList(type: InventoryItemType) {
        val currentList = _shoppingList.value.toMutableMap()
        val currentAmount = currentList[type.id] ?: 0
        if (currentAmount > 0) {
            val newAmount = currentAmount - 1
            if (newAmount <= 0) {
                currentList.remove(type.id)
            } else {
                currentList[type.id] = newAmount
            }
            _shoppingList.value = currentList
        }
    }

    fun sync() = viewModelScope.launch(defaultAsyncDispatcher) {
        try {
            _isSyncing.emit(true)
            LoadingViewModel.syncAll(force = true)
        } finally {
            _isSyncing.emit(false)
        }
    }
}
