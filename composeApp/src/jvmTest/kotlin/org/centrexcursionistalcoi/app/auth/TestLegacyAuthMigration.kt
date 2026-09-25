package org.centrexcursionistalcoi.app.auth

import com.russhwolf.settings.PreferencesSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLegacyAuthMigration {
    // An isolated node, so the tests never touch the machine's real app settings.
    private val preferences = Preferences.userRoot().node("cea-test-${UUID.randomUUID()}")
    private val settings = PreferencesSettings(preferences)

    private val credentialsStore = mockk<CredentialsStore>()
    private val authBackend = mockk<AuthBackend>(relaxUnitFun = true)
    private val migration = LegacyAuthMigration(credentialsStore, authBackend).also { it.settings = settings }

    @AfterTest
    fun tearDown() {
        preferences.removeNode()
    }

    private fun withLegacyPassword() {
        every { credentialsStore.getLegacyCredentials() } returns
            SavedCredentials("user@example.com", "password".toCharArray())
    }

    @Test
    fun `without legacy data nothing happens`() = runTest {
        every { credentialsStore.getLegacyCredentials() } returns null
        settings.putString("language", "ca")

        migration.run()

        coVerify(exactly = 0) { authBackend.authenticate(any(), any()) }
        coVerify(exactly = 0) { authBackend.clearLocalData() }
        assertEquals(setOf("language"), settings.keys)
    }

    @Test
    fun `the saved password is exchanged for a session and the cookies removed`() = runTest {
        withLegacyPassword()
        settings.putString("cookie.USER_SESSION-1", "{}")
        settings.putString("language", "ca")

        migration.run()

        coVerify { authBackend.authenticate("user@example.com", "password") }
        coVerify(exactly = 0) { authBackend.clearLocalData() }
        // The rest of the settings stay: it's still the same account.
        assertEquals(setOf("language"), settings.keys)
    }

    @Test
    fun `a failed login logs out`() = runTest {
        withLegacyPassword()
        coEvery { authBackend.authenticate(any(), any()) } throws IOException("offline")

        migration.run()

        coVerify { authBackend.clearLocalData() }
    }

    @Test
    fun `a cookie without a saved password logs out`() = runTest {
        every { credentialsStore.getLegacyCredentials() } returns null
        settings.putString("cookie.USER_SESSION-1", "{}")

        migration.run()

        coVerify(exactly = 0) { authBackend.authenticate(any(), any()) }
        coVerify { authBackend.clearLocalData() }
    }
}
