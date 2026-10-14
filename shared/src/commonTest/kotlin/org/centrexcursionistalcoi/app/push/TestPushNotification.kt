package org.centrexcursionistalcoi.app.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class TestPushNotification {
    @Test
    fun `the FEMECV credentials notification survives being sent`() {
        val sent = PushNotification.FemecvCredentialsInvalid(userSub = "abc").toMap()

        assertEquals(mapOf("type" to "FemecvCredentialsInvalid", "userSub" to "abc"), sent)
        val received = PushNotification.fromData(sent)
        assertIs<PushNotification.FemecvCredentialsInvalid>(received)
        assertEquals("abc", received.userSub)
    }

    @Test
    fun `the FEMECV credentials notification needs to say who it is for`() {
        assertFailsWith<IllegalArgumentException> {
            PushNotification.fromData(mapOf("type" to "FemecvCredentialsInvalid"))
        }
    }
}
