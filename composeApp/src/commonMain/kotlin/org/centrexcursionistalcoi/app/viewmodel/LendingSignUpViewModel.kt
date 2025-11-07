package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.doMain
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository

class LendingSignUpViewModel : ViewModel() {
    fun signUpForLending(
        phoneNumber: String,
        sports: List<Sports>,
        onComplete: () -> Unit
    ) = launch {
        ProfileRemoteRepository.signUpForLending(phoneNumber, sports)
        ProfileRemoteRepository.synchronize()
        doMain { onComplete() }
    }
}
