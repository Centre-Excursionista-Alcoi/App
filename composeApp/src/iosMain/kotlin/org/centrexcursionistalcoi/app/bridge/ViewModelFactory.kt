package org.centrexcursionistalcoi.app.bridge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
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

    /** Cancels everything [viewModel] was doing. Call it once, when the screen that owns it is gone. */
    fun dispose(viewModel: ViewModel) {
        viewModel.viewModelScope.cancel()
    }
}
