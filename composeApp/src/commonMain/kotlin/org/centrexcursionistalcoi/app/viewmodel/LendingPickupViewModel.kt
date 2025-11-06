package org.centrexcursionistalcoi.app.viewmodel

import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository

class LendingPickupViewModel(lendingId: Uuid): LendingPickupReturnViewModel(lendingId) {
    fun pickup(onSuccess: () -> Unit) = launch {
        try {
            doAsync {
                val dismissedItems = dismissedItems.value

                Napier.i { "Marking lending as picked up..." }
                Napier.d { "Dismissing ${dismissedItems.size} items: ${dismissedItems.joinToString()}" }
                LendingsRemoteRepository.pickup(lendingId, dismissItemsIds = dismissedItems.toList())
                Napier.i { "Lending has been marked as picked up." }
            }
            onSuccess()
        } catch (e: ServerException) {
            Napier.e("Error picking up lending", e)
            setError(e)
        }
    }
}
