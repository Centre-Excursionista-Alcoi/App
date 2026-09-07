package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LendingPageModel(
    lendingsRepository: LendingsRepository,
) : ViewModel() {
    val lendings = lendingsRepository.selectAllAsFlow().stateInViewModel()
    val activeLending = lendings
        .map { list -> list?.find { it.status().isPending() } }
        .stateInViewModel()
}
