package org.centrexcursionistalcoi.app.settings

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.network.PreferencesRemoteRepository
import org.centrexcursionistalcoi.app.storage.SETTINGS_LANGUAGE

class TestLanguagePreference {
    /** The language stored in the app, if any. */
    private var stored: String? = null

    private val settings = mockk<SettingsStore> {
        coEvery { get(SETTINGS_LANGUAGE) } answers { stored }
        coEvery { set(SETTINGS_LANGUAGE, any<String>()) } answers { stored = secondArg() }
    }
    private val remote = mockk<PreferencesRemoteRepository>(relaxed = true)
    private val profileRepository = mockk<ProfileRepository> {
        coEvery { isLoggedIn() } returns true
    }

    private val preference = LanguagePreference(settings, remote, profileRepository)

    @Test
    fun `changing the language changes it in the app and tells the server`() = runTest {
        preference.change("ca")

        assertEquals("ca", stored)
        coVerify { remote.setLanguage("ca") }
    }

    @Test
    fun `if the server can't be told, the app has it anyway`() = runTest {
        coEvery { remote.setLanguage(any()) } returns false

        preference.change("es")

        assertEquals("es", stored)
    }

    @Test
    fun `without a language of its own, the app uses the one of the server`() = runTest {
        coEvery { remote.getLanguage() } returns "ca-ES"

        preference.restoreFromServerIfUnset()

        assertEquals("ca", stored)
    }

    @Test
    fun `the language of the app is kept`() = runTest {
        stored = "es"
        coEvery { remote.getLanguage() } returns "ca"

        preference.restoreFromServerIfUnset()

        assertEquals("es", stored)
        // And the server isn't even asked
        coVerify(exactly = 0) { remote.getLanguage() }
    }

    @Test
    fun `a language the app doesn't have is not used`() = runTest {
        coEvery { remote.getLanguage() } returns "ta"

        preference.restoreFromServerIfUnset()

        assertEquals(null, stored)
    }

    @Test
    fun `nothing is set if the server has none or can't be reached`() = runTest {
        coEvery { remote.getLanguage() } returns null

        preference.restoreFromServerIfUnset()

        assertEquals(null, stored)
    }

    @Test
    fun `the server isn't asked if the user isn't logged in`() = runTest {
        coEvery { profileRepository.isLoggedIn() } returns false

        preference.restoreFromServerIfUnset()

        coVerify(exactly = 0) { remote.getLanguage() }
    }

    @Test
    fun `a language chosen while the server answered wins`() = runTest {
        coEvery { remote.getLanguage() } coAnswers {
            stored = "es"
            "ca"
        }

        preference.restoreFromServerIfUnset()

        assertEquals("es", stored)
    }
}
