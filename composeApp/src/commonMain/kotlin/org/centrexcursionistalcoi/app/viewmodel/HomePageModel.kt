package org.centrexcursionistalcoi.app.viewmodel

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import org.centrexcursionistalcoi.app.data.isUpcoming
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
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.koin.core.annotation.KoinViewModel

/** What to offer the user about the notification permission (only for users registered for lendings). */
enum class NotificationPrompt {
    NONE,

    /** They haven't granted it: ask again. */
    ASK,

    /** The system won't ask again: only the settings can grant it. */
    OPEN_SETTINGS,
}

@KoinViewModel
class HomePageModel(
    profileRepository: ProfileRepository,
    postsRepository: PostsRepository,
    eventsRepository: EventsRepository,
    departmentsRepository: DepartmentsRepository,
    private val eventsRemoteRepository: EventsRemoteRepository,
    private val settings: SettingsStore,
) : ViewModel() {
    val profile = profileRepository.profile.stateInViewModel()

    val posts = postsRepository.selectAllAsFlow().stateInViewModel()
    val events = eventsRepository.selectAllAsFlow().stateInViewModel()

    /** The events that haven't ended yet, the ones the home page shows. */
    val upcomingEvents = eventsRepository.selectAllAsFlow()
        .map { events -> Clock.System.now().let { now -> events.filter { it.isUpcoming(now) } } }
        .stateInViewModel()

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

    /** What the home page asks about notifications, in terms of what the user can do, see [NotificationPrompt]. */
    val notificationPrompt = notificationPermissionResult.map {
        when (it) {
            NotificationPermissionResult.Denied -> NotificationPrompt.ASK
            NotificationPermissionResult.NotAllowed -> NotificationPrompt.OPEN_SETTINGS
            else -> NotificationPrompt.NONE
        }
    }.stateInViewModel(NotificationPrompt.NONE)

    fun openNotificationSettings() {
        permissionHelper.openSettings()
    }

    fun refreshPermissions() = launch {
        val denied = settings.get(NOTIFICATION_PERMISSION_DENIED_KEY, false) == true
        if (denied) _notificationPermissionResult.value = null
        else _notificationPermissionResult.value = permissionHelper.checkIsPermissionGranted(Permission.Notification)
    }

    fun requestNotificationsPermission() = launch {
        _notificationPermissionResult.value = permissionHelper.requestForPermission(Permission.Notification)
    }

    fun denyNotificationsPermission() = launch {
        settings.set(NOTIFICATION_PERMISSION_DENIED_KEY, true)
        _notificationPermissionResult.value = null
    }

    fun confirmEventAssistance(event: ReferencedEvent) = launch {
        eventsRemoteRepository.confirmAssistance(event.id)
    }

    fun rejectEventAssistance(event: ReferencedEvent) = launch {
        eventsRemoteRepository.rejectAssistance(event.id)
    }

    companion object {
        private val NOTIFICATION_PERMISSION_DENIED_KEY = booleanPreferencesKey("permission_notifications_denied")
    }
}
