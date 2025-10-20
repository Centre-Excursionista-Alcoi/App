package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository

class LendingSignUpViewModel : ViewModel() {
    fun signUpForLending(
        fullName: String,
        nif: String,
        phoneNumber: String,
        sports: List<Sports>,
        address: String,
        postalCode: String,
        city: String,
        province: String,
        country: String
    ) = viewModelScope.launch(defaultAsyncDispatcher) {
        ProfileRemoteRepository.signUpForLending(fullName, nif, phoneNumber, sports, address, postalCode, city, province, country)
        ProfileRemoteRepository.synchronize()
    }
}
