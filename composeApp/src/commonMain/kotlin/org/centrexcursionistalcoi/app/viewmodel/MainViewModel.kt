package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.centrexcursionistalcoi.app.permission.HelperHolder
import org.centrexcursionistalcoi.app.permission.Permission
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.storage.settings
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.BackgroundJobState
import org.centrexcursionistalcoi.app.sync.SyncAllDataBackgroundJobLogic

class MainViewModel: ViewModel() {
    val isSyncing = BackgroundJobCoordinator.observeUnique(SyncAllDataBackgroundJobLogic.UNIQUE_NAME)
        .stateFlow()
        .map { it == BackgroundJobState.RUNNING }
        .stateInViewModel()

    val profile = ProfileRepository.profile.stateInViewModel()

    val departments = DepartmentsRepository.selectAllAsFlow().stateInViewModel()

    val users = UsersRepository.selectAllAsFlow().stateInViewModel()

    val inventoryItemTypes = InventoryItemTypesRepository.selectAllAsFlow().stateInViewModel()
    val inventoryItemTypesCategories = InventoryItemTypesRepository.categoriesAsFlow().stateInViewModel()

    val inventoryItems = InventoryItemsRepository.selectAllAsFlow().stateInViewModel()

    val lendings = LendingsRepository.selectAllAsFlow().stateInViewModel()

    /**
     * A map of InventoryItemType ID to amount in the shopping list.
     */
    private val _shoppingList = MutableStateFlow(emptyMap<Uuid, Int>())
    val shoppingList = _shoppingList.asStateFlow()

    private val permissionHelper = HelperHolder.getPermissionHelperInstance()
    private val _notificationPermissionResult = MutableStateFlow<NotificationPermissionResult?>(null)
    val notificationPermissionResult = _notificationPermissionResult.asStateFlow()

    fun refreshPermissions() = launch {
        val denied = settings.getBooleanOrNull("permission.notifications.denied") == true
        if (denied) _notificationPermissionResult.value = null
        else _notificationPermissionResult.value = permissionHelper.checkIsPermissionGranted(Permission.Notification)
    }

    fun requestNotificationsPermission() = launch {
        _notificationPermissionResult.value = permissionHelper.requestForPermission(Permission.Notification)
    }

    fun denyNotificationsPermission() = launch {
        settings.putBoolean("permission.notifications.denied", true)
        _notificationPermissionResult.value = null
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
        LoadingViewModel.syncAll(force = true)
    }

    fun connectFEMECV(username: String, password: CharArray) = viewModelScope.async<Throwable?>(defaultAsyncDispatcher) {
        try {
            ProfileRemoteRepository.connectFEMECV(username, password)
            null
        } catch (e: ServerException) {
            Napier.e(e) { "Could not connect to FEMECV." }
            e
        }
    }

    fun disconnectFEMECV() = viewModelScope.launch(defaultAsyncDispatcher) {
        ProfileRemoteRepository.disconnectFEMECV()
    }
}
