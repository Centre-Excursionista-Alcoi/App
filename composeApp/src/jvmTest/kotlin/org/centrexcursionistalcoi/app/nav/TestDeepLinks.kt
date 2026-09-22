package org.centrexcursionistalcoi.app.nav

import io.ktor.http.Url
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A link handed over by the platform waits until the app opens it, and can't be lost or opened twice. */
class TestDeepLinks {
    private val link = "https://centrexcursionistalcoi.app/admin/lendings/1f0e5c2a-0000-4000-8000-000000000001"

    @AfterTest
    fun tearDown() {
        DeepLinks.pending.value?.let { DeepLinks.consume(it) }
    }

    @Test
    fun aReceivedLink_staysPending_untilItIsConsumed() {
        assertNull(DeepLinks.pending.value)

        DeepLinks.receive(link)

        assertEquals(Url(link), DeepLinks.pending.value)
        DeepLinks.consume(Url(link))
        assertNull(DeepLinks.pending.value)
    }

    @Test
    fun consumingALinkThatIsNotTheCurrentOne_keepsTheNewerLink() {
        DeepLinks.receive(link)
        val newer = "https://centrexcursionistalcoi.app/admin/lendings"

        DeepLinks.receive(newer)
        DeepLinks.consume(Url(link)) // the older one finishes being handled

        assertEquals(Url(newer), DeepLinks.pending.value)
    }

    @Test
    fun theSameLinkCanBeReceivedAgain_onceItWasConsumed() {
        DeepLinks.receive(link)
        DeepLinks.consume(Url(link))

        DeepLinks.receive(link)

        assertEquals(Url(link), DeepLinks.pending.value)
    }

    // ---- Where a link can be opened from ----

    @Test
    fun linksAreNotOpened_whileLoadingOrLoggingInOrOut() {
        assertFalse(Destination.Loading.canOpenLinks())
        assertFalse(Destination.Login().canOpenLinks())
        assertFalse(Destination.Logout.canOpenLinks())
    }

    @Test
    fun linksAreOpened_onceTheUserIsIn() {
        assertTrue(Destination.Main().canOpenLinks())
        assertTrue(Destination.Settings.canOpenLinks())
    }
}
