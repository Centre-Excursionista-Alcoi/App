package org.centrexcursionistalcoi.app.bridge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import org.centrexcursionistalcoi.app.push.PlatformSSEConfiguration
import org.centrexcursionistalcoi.app.push.SSENotificationsListener
import org.centrexcursionistalcoi.app.viewmodel.HomePageModel
import org.centrexcursionistalcoi.app.viewmodel.LoadingViewModel
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel
import org.centrexcursionistalcoi.app.viewmodel.LogoutViewModel
import org.centrexcursionistalcoi.app.viewmodel.MainScreenViewModel
import org.centrexcursionistalcoi.app.viewmodel.PlatformInitializerViewModel
import org.centrexcursionistalcoi.app.viewmodel.SecurityViewModel
import org.centrexcursionistalcoi.app.viewmodel.SessionExpiryViewModel
import org.centrexcursionistalcoi.app.viewmodel.SettingsViewModel
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

/**
 * Gives the native SwiftUI screens the same ViewModels the Compose screens use.
 *
 * Koin's types aren't exported to Swift (see [org.centrexcursionistalcoi.app.di.initKoinIos]), so every ViewModel
 * Swift needs gets a function here, which resolves it from Koin. There is no logic in this file: anything a screen
 * decides belongs in the ViewModel, so both UIs behave the same.
 *
 * Unlike Compose, nothing ties a ViewModel to the screen showing it: Swift owns it, and must call [dispose] when
 * the screen goes away, or its coroutines keep running.
 */
object ViewModelFactory {
    fun settingsViewModel(onDeleteAccount: () -> Unit): SettingsViewModel =
        KoinPlatform.getKoin().get { parametersOf(onDeleteAccount) }

    /** Prepares the app: runs the platform's loading logic and works out where to start. */
    fun platformInitializerViewModel(): PlatformInitializerViewModel =
        KoinPlatform.getKoin().get { parametersOf(null, null) }

    fun loadingViewModel(): LoadingViewModel = KoinPlatform.getKoin().get()

    fun homePageModel(): HomePageModel = KoinPlatform.getKoin().get()

    fun securityViewModel(): SecurityViewModel = KoinPlatform.getKoin().get()

    /** Whether the app listens for live updates from the server, see [startListeningForNotifications]. */
    val isLiveUpdatesEnabled: Boolean get() = PlatformSSEConfiguration.enableSSE

    /** Connects to the server's live updates again, e.g. after they failed. */
    fun startListeningForNotifications() {
        KoinPlatform.getKoin().get<SSENotificationsListener>().startListening()
    }

    fun loginViewModel(): LoginViewModel = KoinPlatform.getKoin().get()

    fun mainScreenViewModel(): MainScreenViewModel = KoinPlatform.getKoin().get()

    /** Logs the user out as soon as it is created, and calls [afterLogout] when it's done. */
    fun logoutViewModel(afterLogout: () -> Unit): LogoutViewModel =
        KoinPlatform.getKoin().get { parametersOf(afterLogout) }

    /** Watches for the session expiring while the user is in, and calls [onLoggedOut] if it can't be recovered. */
    fun sessionExpiryViewModel(onLoggedOut: () -> Unit): SessionExpiryViewModel =
        KoinPlatform.getKoin().get { parametersOf(onLoggedOut) }

    /** Cancels everything [viewModel] was doing. Call it once, when the screen that owns it is gone. */
    fun dispose(viewModel: ViewModel) {
        viewModel.viewModelScope.cancel()
    }
}
