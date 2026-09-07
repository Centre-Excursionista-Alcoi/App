package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getBooleanStateFlow
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.push.FCMTokenManager
import org.centrexcursionistalcoi.app.push.SSENotificationsListener
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_ANALYTICS
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_ERRORS
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_SESSION_REPLAY
import org.centrexcursionistalcoi.app.storage.settings
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@OptIn(ExperimentalSettingsApi::class)
@KoinViewModel
class SettingsViewModel(
    private val authBackend: AuthBackend,
    private val dispatcherProvider: DispatcherProvider,
    sseNotificationsListener: SSENotificationsListener,
    @InjectedParam private val onDeleteAccount: () -> Unit,
) : ErrorViewModel() {
    val fcmToken = FCMTokenManager.tokenFlow.stateInViewModel()

    val sseConnected = sseNotificationsListener.isConnected.stateInViewModel(initialValue = false)
    val sseError = sseNotificationsListener.sseException.stateInViewModel()

    val privacyErrors = settings.getBooleanStateFlow(viewModelScope, SETTINGS_PRIVACY_ERRORS, true)
    val privacyAnalytics = settings.getBooleanStateFlow(viewModelScope, SETTINGS_PRIVACY_ANALYTICS, true)
    val privacySessionReplay = settings.getBooleanStateFlow(viewModelScope, SETTINGS_PRIVACY_SESSION_REPLAY, true)

    fun deleteAccount() = launch {
        authBackend.deleteAccount()
        withContext(dispatcherProvider.main) { onDeleteAccount() }
    }

    fun onPrivacyErrorsChange(state: Boolean) {
        settings.putBoolean(SETTINGS_PRIVACY_ERRORS, state)
    }

    fun onPrivacyAnalyticsChange(state: Boolean) {
        settings.putBoolean(SETTINGS_PRIVACY_ANALYTICS, state)
    }

    fun onPrivacySessionReplayChange(state: Boolean) {
        settings.putBoolean(SETTINGS_PRIVACY_SESSION_REPLAY, state)
    }
}
