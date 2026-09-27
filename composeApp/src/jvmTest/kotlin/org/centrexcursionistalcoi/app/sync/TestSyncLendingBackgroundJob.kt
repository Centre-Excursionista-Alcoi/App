package org.centrexcursionistalcoi.app.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.push.PushNotification

class TestSyncLendingBackgroundJob {
    private val lendingId = Uuid.random()

    @Test
    fun test_isRemoval_deletedAndCancelled() {
        // The lending no longer exists on the server: fetching it again would fail
        assertTrue(SyncLendingBackgroundJob.isRemoval(PushNotification.LendingDeleted(lendingId, "sub", null)))
        assertTrue(SyncLendingBackgroundJob.isRemoval(PushNotification.LendingCancelled(lendingId, "sub")))
    }

    @Test
    fun test_isRemoval_otherUpdates() {
        assertFalse(SyncLendingBackgroundJob.isRemoval(PushNotification.LendingTaken(lendingId, "sub")))
    }
}
