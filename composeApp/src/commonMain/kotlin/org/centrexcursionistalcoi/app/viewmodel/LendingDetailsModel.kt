package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository

class LendingDetailsModel(private val lendingId: Uuid): ViewModel() {
    val lending = LendingsRepository.getAsFlow(lendingId).stateInViewModel()

    fun cancelLending() = async {
        try {
            doAsync { LendingsRemoteRepository.cancel(lendingId) }
            null
        } catch (error: ServerException) {
            error
        }
    }
}
