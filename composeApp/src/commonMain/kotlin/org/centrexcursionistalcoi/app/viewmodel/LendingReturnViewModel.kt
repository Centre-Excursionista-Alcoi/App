package org.centrexcursionistalcoi.app.viewmodel

import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository

class LendingReturnViewModel(lendingId: Uuid): LendingPickupReturnViewModel(lendingId) {
    fun `return`() = launch {
        doAsync {
            Napier.i { "Returning lending..." }
            LendingsRemoteRepository.`return`(lendingId)
            Napier.i { "Return of lending has been received." }
        }
    }
}
