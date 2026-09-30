package org.centrexcursionistalcoi.app.viewmodel

import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.di.GenderInflection
import org.centrexcursionistalcoi.app.di.GenderInflectionProvider
import org.centrexcursionistalcoi.app.di.globalGenderInflectionProvider
import org.centrexcursionistalcoi.app.push.FCMTokenManager
import org.centrexcursionistalcoi.app.push.SSENotificationsListener
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.centrexcursionistalcoi.app.storage.SETTINGS_LANGUAGE
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_ANALYTICS
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_ERRORS
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_SESSION_REPLAY
import org.centrexcursionistalcoi.app.ui.screen.Language
import org.centrexcursionistalcoi.app.ui.screen.languageFromCode
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    private val authBackend: AuthBackend,
    private val dispatcherProvider: DispatcherProvider,
    sseNotificationsListener: SSENotificationsListener,
    genderInflectionProvider: GenderInflectionProvider?,
    fcmTokenManager: FCMTokenManager,
    private val settings: SettingsStore,
    @InjectedParam private val onDeleteAccount: () -> Unit,
) : ErrorViewModel() {
    val gender = genderInflectionProvider?.observableGender?.stateInViewModel()

    val fcmToken = fcmTokenManager.tokenFlow.stateInViewModel()

    val sseConnected = sseNotificationsListener.isConnected.stateInViewModel(initialValue = false)
    val sseError = sseNotificationsListener.sseException.stateInViewModel()

    val language = settings.getFlow(SETTINGS_LANGUAGE)
        .map { lang -> lang?.let { languageFromCode(it) } }
        .stateInViewModel()
    val privacyErrors = settings.getFlow(SETTINGS_PRIVACY_ERRORS, true).stateInViewModel(true)
    val privacyAnalytics = settings.getFlow(SETTINGS_PRIVACY_ANALYTICS, true).stateInViewModel(true)
    val privacySessionReplay = settings.getFlow(SETTINGS_PRIVACY_SESSION_REPLAY, true).stateInViewModel(true)

    fun onGenderChange(gender: GenderInflection) = launch {
        globalGenderInflectionProvider?.setGenderInflection(gender)
    }

    fun deleteAccount() = launch {
        authBackend.deleteAccount()
        withContext(dispatcherProvider.main) { onDeleteAccount() }
    }

    fun onLanguageChange(language: Language) = launch {
        val (lang) = language
        settings.set(SETTINGS_LANGUAGE, lang)
        LocaleUpdater.updateLocale(lang)
    }

    fun onPrivacyErrorsChange(state: Boolean) = launch {
        settings.set(SETTINGS_PRIVACY_ERRORS, state)
    }

    fun onPrivacyAnalyticsChange(state: Boolean) = launch {
        settings.set(SETTINGS_PRIVACY_ANALYTICS, state)
    }

    fun onPrivacySessionReplayChange(state: Boolean) = launch {
        settings.set(SETTINGS_PRIVACY_SESSION_REPLAY, state)
    }
}
