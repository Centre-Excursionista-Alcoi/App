package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diamondedge.logging.logging
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.nav.DeepLinks
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.platform.PlatformLoadLogic
import org.centrexcursionistalcoi.app.push.LocalNotifications
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.push.SSENotificationsListener
import org.centrexcursionistalcoi.app.settings.LanguagePreference
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.centrexcursionistalcoi.app.storage.SETTINGS_LANGUAGE
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class PlatformInitializerViewModel(
    @InjectedParam url: Url?,
    @InjectedParam pushNotification: PushNotification?,
    dispatcherProvider: DispatcherProvider,
    private val sseNotificationsListener: SSENotificationsListener,
    private val localNotifications: LocalNotifications,
    private val settings: SettingsStore,
    private val languagePreference: LanguagePreference,
) : ViewModel() {
    private val log = logging()

    private val _isReady = MutableStateFlow(false)
    val isReady get() = _isReady.asStateFlow()

    val targetDestination: StateFlow<Destination?>
        field = MutableStateFlow<Destination?>(null)

    init {
        viewModelScope.launch(dispatcherProvider.io + GlobalAsyncErrorHandler.coroutineExceptionHandler) {
            log.d { "Running platform loading logic..." }
            PlatformLoadLogic.load()

            val startDestination = if (url != null) {
                log.d { "Processing destination for url: $url" }
                DeepLinks.fromUrl(url)
            } else {
                null
            }

            log.d { "Listening for SSE notifications..." }
            sseNotificationsListener.startListening()

            settings.get(SETTINGS_LANGUAGE)?.let { lang ->
                log.i { "Setting locale to: $lang" }
                LocaleUpdater.updateLocale(lang)
            }

            log.d { "Calculating target destination..." }
            targetDestination.value = calculateDestination(pushNotification) ?: startDestination

            log.d { "Platform is ready." }
            _isReady.emit(true)

            // Not waited for: the app is ready without it, and it only matters if the app has no language of its own
            launch { languagePreference.restoreFromServerIfUnset() }
        }
    }

    private suspend fun <N: PushNotification.LendingUpdated> destination(
        notification: N,
        forAdmin: (N) -> Destination? = { null },
        forUser: (N) -> Destination? = { null }
    ): Destination? {
        val isSelf = with(localNotifications) { notification.checkIsSelf() }
        return if (isSelf) forUser(notification)
        else forAdmin(notification)
    }

    private suspend fun calculateDestination(pushNotification: PushNotification?): Destination? {
        return when (pushNotification) {
            // always admin notifications
            is PushNotification.NewLendingRequest -> Destination.Admin.LendingManagement(pushNotification.lendingId)
            is PushNotification.NewMemoryUpload -> Destination.Admin.LendingManagement(pushNotification.lendingId)
            // always user notifications
            is PushNotification.LendingCancelled -> null // the lending is cancelled, cannot show any info
            is PushNotification.LendingConfirmed -> Destination.LendingDetails(
                lendingId = pushNotification.lendingId
            )
            // could be either
            is PushNotification.LendingTaken -> destination(
                pushNotification,
                forAdmin = { Destination.Admin.LendingManagement(pushNotification.lendingId) },
                forUser = { Destination.LendingDetails(it.lendingId) },
            )
            is PushNotification.LendingPartiallyReturned -> destination(
                pushNotification,
                forAdmin = { Destination.Admin.LendingManagement(pushNotification.lendingId) },
                forUser = { Destination.LendingDetails(it.lendingId) },
            )
            is PushNotification.LendingReturned -> destination(
                pushNotification,
                forAdmin = { Destination.Admin.LendingManagement(pushNotification.lendingId) },
                forUser = { Destination.LendingDetails(it.lendingId) },
            )
            else -> null
        }
    }
}
