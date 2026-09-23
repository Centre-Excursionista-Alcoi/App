package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.permission.HelperHolder
import org.centrexcursionistalcoi.app.permission.Permission
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.storage.settings
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HomePageModel(
    postsRepository: PostsRepository,
    eventsRepository: EventsRepository,
    departmentsRepository: DepartmentsRepository,
    private val eventsRemoteRepository: EventsRemoteRepository,
) : ViewModel() {
    val profile = ProfileRepository.profile.stateInViewModel()

    val posts = postsRepository.selectAllAsFlow().stateInViewModel()
    val events = eventsRepository.selectAllAsFlow().stateInViewModel()

    /**
     * What events can require, and which of those the user holds, to tell whether they can confirm assistance.
     */
    val qualifications = departmentsRepository.selectAllAsFlow()
        .map { departments -> departments.flatMap { it.qualifications.orEmpty() } }
        .stateInViewModel()
    val myQualificationGrants = combine(departmentsRepository.selectAllAsFlow(), profile) { departments, profile ->
        val sub = profile?.sub ?: return@combine emptyList()
        departments.flatMap { it.qualificationGrants.orEmpty() }.filter { it.userSub == sub }
    }.stateInViewModel()

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

    fun confirmEventAssistance(event: ReferencedEvent) = launch {
        eventsRemoteRepository.confirmAssistance(event.id)
    }

    fun rejectEventAssistance(event: ReferencedEvent) = launch {
        eventsRemoteRepository.rejectAssistance(event.id)
    }
}
