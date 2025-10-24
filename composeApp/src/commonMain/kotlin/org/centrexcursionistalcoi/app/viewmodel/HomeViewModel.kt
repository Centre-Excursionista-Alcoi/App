package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlin.uuid.Uuid
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.centrexcursionistalcoi.app.storage.settings
import tech.kotlinlang.permission.HelperHolder
import tech.kotlinlang.permission.Permission
import tech.kotlinlang.permission.result.NotificationPermissionResult

class HomeViewModel: ViewModel() {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    val profile = ProfileRepository.profile.stateInViewModel()

    val departments = DepartmentsRepository.selectAllAsFlow().stateInViewModel()

    val users = UsersRepository.selectAllAsFlow().stateInViewModel()

    val inventoryItemTypes = InventoryItemTypesRepository.selectAllAsFlow().stateInViewModel()

    val inventoryItems = InventoryItemsRepository.selectAllAsFlow().stateInViewModel()

    val lendings = combine(LendingsRepository.selectAllAsFlow(), ProfileRepository.profile) { lendings, profile ->
        lendings.filter { lending -> lending.user.sub == profile?.sub }
    }.map { list -> list.sortedBy { it.from } }.stateInViewModel()

    /**
     * A map of InventoryItemType ID to amount in the shopping list.
     */
    private val _shoppingList = MutableStateFlow(emptyMap<Uuid, Int>())
    val shoppingList = _shoppingList.asStateFlow()

    private val _memoryUploadProgress = MutableStateFlow<Pair<Long, Long>?>(null)
    val memoryUploadProgress = _memoryUploadProgress.asStateFlow()

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

    fun delete(item: ReferencedInventoryItem) = viewModelScope.launch(defaultAsyncDispatcher) {
        InventoryItemsRemoteRepository.delete(item.id)
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

    fun cancelLending(lending: ReferencedLending) = viewModelScope.launch(defaultAsyncDispatcher) {
        LendingsRemoteRepository.cancel(lending.id)
    }

    fun submitMemory(lending: ReferencedLending, file: PlatformFile) = viewModelScope.async(defaultAsyncDispatcher) {
        try {
            _memoryUploadProgress.emit(0L to file.size())
            LendingsRemoteRepository.submitMemory(lending.id, file) { current, max ->
                _memoryUploadProgress.value = current to (max ?: file.size())
            }
        } catch (e: ServerException) {
            Napier.e(e) { "Could not submit memory." }
        } finally {
            _memoryUploadProgress.value = null
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
