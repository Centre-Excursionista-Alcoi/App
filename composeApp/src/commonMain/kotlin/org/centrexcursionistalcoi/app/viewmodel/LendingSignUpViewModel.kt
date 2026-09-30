package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LendingSignUpViewModel(
    private val dispatcherProvider: DispatcherProvider,
    private val profileRemoteRepository: ProfileRemoteRepository,
) : ViewModel() {
    fun signUpForLending(
        phoneNumber: String,
        sports: List<Sports>,
        onComplete: () -> Unit
    ) = launch {
        profileRemoteRepository.signUpForLending(phoneNumber, sports)
        profileRemoteRepository.synchronize()
        withContext(dispatcherProvider.main) { onComplete() }
    }
}
