package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class LendingDetailsModel(
    @InjectedParam private val lendingId: Uuid,
    lendingsRepository: LendingsRepository,
    private val lendingsRemoteRepository: LendingsRemoteRepository,
): ViewModel() {
    val lending = lendingsRepository.getAsFlow(lendingId).stateInViewModel()

    fun cancelLending() = async {
        try {
            doAsync { lendingsRemoteRepository.cancel(lendingId) }
            null
        } catch (error: ServerException) {
            error
        }
    }
}
