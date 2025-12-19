package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.diamondedge.logging.logging
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.database.*
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.centrexcursionistalcoi.app.permission.HelperHolder
import org.centrexcursionistalcoi.app.permission.Permission
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.storage.settings
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.BackgroundJobState
import org.centrexcursionistalcoi.app.sync.SyncAllDataBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncAllDataBackgroundJobLogic
import kotlin.uuid.Uuid

class MainViewModel: ViewModel() {
    companion object {
        private val log = logging()
    }

    val isSyncing = BackgroundJobCoordinator.observeUnique(SyncAllDataBackgroundJobLogic.UNIQUE_NAME)
        .stateFlow()
        .map { it in listOf(BackgroundJobState.RUNNING) }
        .stateInViewModel()

    val profile = ProfileRepository.profile.stateInViewModel()

    val departments = DepartmentsRepository.selectAllAsFlow().stateInViewModel()

    val users = UsersRepository.selectAllAsFlow().stateInViewModel()

    val members = MembersRepository.selectAllAsFlow().stateInViewModel()

    val inventoryItemTypes = InventoryItemTypesRepository.selectAllAsFlow().stateInViewModel()
    val inventoryItemTypesCategories = InventoryItemTypesRepository.categoriesAsFlow().stateInViewModel()

    val inventoryItems = InventoryItemsRepository.selectAllAsFlow().stateInViewModel()

    val lendings = LendingsRepository.selectAllAsFlow().stateInViewModel()

    val posts = PostsRepository.selectAllAsFlow().stateInViewModel()

    val events = EventsRepository.selectAllAsFlow().stateInViewModel()

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

    fun cancelLending(lending: ReferencedLending) = launch {
        LendingsRemoteRepository.cancel(lending.id)
    }

    fun createInsurance(company: String, policyNumber: String, validFrom: LocalDate, validTo: LocalDate, document: PlatformFile?) = launch {
        ProfileRemoteRepository.createInsurance(company, policyNumber, validFrom, validTo, document)
        ProfileRemoteRepository.synchronize()
    }

    fun addItemToShoppingList(type: ReferencedInventoryItemType) {
        val currentList = _shoppingList.value.toMutableMap()
        val currentAmount = currentList[type.id] ?: 0
        currentList[type.id] = currentAmount + 1
        _shoppingList.value = currentList
    }

    fun removeItemFromShoppingList(type: ReferencedInventoryItemType) {
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

    fun sync() = launch {
        log.d { "Scheduling data sync..." }
        BackgroundJobCoordinator.schedule<SyncAllDataBackgroundJobLogic, SyncAllDataBackgroundJob>(
            input = mapOf(SyncAllDataBackgroundJobLogic.EXTRA_FORCE_SYNC to "true"),
            requiresInternet = true,
            uniqueName = SyncAllDataBackgroundJobLogic.UNIQUE_NAME,
            logic = SyncAllDataBackgroundJobLogic,
        )
    }

    fun connectFEMECV(username: String, password: CharArray) = async<Throwable?> {
        try {
            ProfileRemoteRepository.connectFEMECV(username, password)
            null
        } catch (e: ServerException) {
            log.e(e) { "Could not connect to FEMECV." }
            e
        }
    }

    fun disconnectFEMECV() = launch {
        ProfileRemoteRepository.disconnectFEMECV()
    }

    fun requestJoinDepartment(department: Department) = launch {
        DepartmentsRemoteRepository.requestJoin(department.id)
    }

    fun leaveDepartment(department: Department) = launch {
        DepartmentsRemoteRepository.leave(department.id)
    }

    fun confirmEventAssistance(event: ReferencedEvent) = launch {
        EventsRemoteRepository.confirmAssistance(event.id)
    }

    fun rejectEventAssistance(event: ReferencedEvent) = launch {
        EventsRemoteRepository.rejectAssistance(event.id)
    }
}
