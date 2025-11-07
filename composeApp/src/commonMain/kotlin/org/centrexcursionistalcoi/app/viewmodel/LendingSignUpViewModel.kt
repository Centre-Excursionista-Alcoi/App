package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository

class LendingSignUpViewModel : ViewModel() {
    fun signUpForLending(
        phoneNumber: String,
        sports: List<Sports>,
    ) = viewModelScope.launch(defaultAsyncDispatcher) {
        ProfileRemoteRepository.signUpForLending(phoneNumber, sports)
        ProfileRemoteRepository.synchronize()
    }
}
