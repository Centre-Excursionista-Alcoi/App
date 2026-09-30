package org.centrexcursionistalcoi.app.auth

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.uuid.Uuid

/**
 * Exercises the real iOS Keychain on a simulator/device. Each test uses a unique service so it cannot
 * overwrite credentials saved by the app or by another test run.
 */
class CredentialsStoreTest {
    private val service = "org.centrexcursionistalcoi.app.credentials.test.${Uuid.random()}"
    private val store = CredentialsStore(service)

    @AfterTest
    fun tearDown() = runBlocking {
        store.clear()
    }

    @Test
    fun `nothing is returned when nothing is saved`() = runTest {
        store.clear()
        assertNull(store.getSession())
        assertNull(store.current.value)
    }

    @Test
    fun `saveSession persists the session retrievable via both getSession and current`() = runTest {
        store.saveSession("credentials-test@example.com", "refresh-token")

        val saved = store.getSession()
        assertEquals("credentials-test@example.com", saved?.email)
        assertEquals("refresh-token", saved?.refreshToken)
        assertEquals("credentials-test@example.com", store.current.value?.email)
    }

    @Test
    fun `saving again replaces the refresh token and the account`() = runTest {
        store.saveSession("first@example.com", "first-token")
        store.saveSession("second@example.com", "second-token")

        assertEquals("second@example.com", store.getSession()?.email)
        assertEquals("second-token", store.getSession()?.refreshToken)
        assertEquals("second@example.com", store.current.value?.email)
    }

    @Test
    fun `saving a session deletes the legacy credentials`() = runTest {
        store.saveLegacyCredentialsForTests("legacy@example.com", "s3cr3t-P@ss")
        assertNull(store.getSession())

        store.saveSession("legacy@example.com", "refresh-token")
        assertFalse(store.hasLegacyCredentialsForTests())
        assertEquals("refresh-token", store.getSession()?.refreshToken)
    }

    @Test
    fun `clear removes both the session and legacy credentials`() = runTest {
        store.saveLegacyCredentialsForTests("legacy@example.com", "s3cr3t-P@ss")
        store.clear()
        assertFalse(store.hasLegacyCredentialsForTests())

        store.saveSession("credentials-test@example.com", "refresh-token")
        store.clear()
        assertNull(store.getSession())
        assertNull(store.current.value)
    }

    @Test
    fun `new instance reads the persisted session and initializes current`() = runTest {
        store.saveSession("persisted@example.com", "persisted-token")
        val reopened = CredentialsStore(service)
        assertEquals("persisted@example.com", reopened.getSession()?.email)
        assertEquals("persisted-token", reopened.getSession()?.refreshToken)
        assertEquals("persisted@example.com", reopened.current.value?.email)
    }

    @Test
    fun `unicode round trips`() = runTest {
        val email = "excursió@example.com"
        store.saveSession(email, "密碼🔑é-token")
        assertEquals(email, store.getSession()?.email)
        assertEquals("密碼🔑é-token", store.getSession()?.refreshToken)
    }

    @Test
    fun `save and clear do not affect another service`() = runTest {
        val other = CredentialsStore("$service.other")
        try {
            other.saveSession("other@example.com", "other-token")
            store.saveSession("first@example.com", "first-token")
            store.clear()
            assertEquals("other@example.com", other.getSession()?.email)
            assertEquals("other-token", other.getSession()?.refreshToken)
        } finally {
            other.clear()
        }
    }
}
