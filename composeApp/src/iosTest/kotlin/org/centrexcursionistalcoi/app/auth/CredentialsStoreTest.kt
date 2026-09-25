package org.centrexcursionistalcoi.app.auth

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun tearDown() {
        store.clear()
    }

    @Test
    fun `nothing is returned when nothing is saved`() {
        store.clear()
        assertNull(store.getSession())
        assertNull(store.getLegacyCredentials())
        assertNull(store.current.value)
    }

    @Test
    fun `saveSession persists the session retrievable via both getSession and current`() {
        store.saveSession("credentials-test@example.com", "refresh-token")

        val saved = store.getSession()
        assertEquals("credentials-test@example.com", saved?.email)
        assertEquals("refresh-token", saved?.refreshToken)
        assertEquals("credentials-test@example.com", store.current.value?.email)
    }

    @Test
    fun `saving again replaces the refresh token and the account`() {
        store.saveSession("first@example.com", "first-token")
        store.saveSession("second@example.com", "second-token")

        assertEquals("second@example.com", store.getSession()?.email)
        assertEquals("second-token", store.getSession()?.refreshToken)
        assertEquals("second@example.com", store.current.value?.email)
    }

    @Test
    fun `legacy credentials are readable until a session is saved`() {
        store.saveLegacyCredentialsForTests("legacy@example.com", "s3cr3t-P@ss")
        assertEquals("legacy@example.com", store.getLegacyCredentials()?.email)
        assertEquals("s3cr3t-P@ss", store.getLegacyCredentials()?.password?.concatToString())
        assertEquals("legacy@example.com", store.current.value?.email)
        assertNull(store.getSession())

        store.saveSession("legacy@example.com", "refresh-token")
        assertNull(store.getLegacyCredentials())
        assertEquals("refresh-token", store.getSession()?.refreshToken)
    }

    @Test
    fun `clear removes both the session and legacy credentials`() {
        store.saveLegacyCredentialsForTests("legacy@example.com", "s3cr3t-P@ss")
        store.clear()
        assertNull(store.getLegacyCredentials())

        store.saveSession("credentials-test@example.com", "refresh-token")
        store.clear()
        assertNull(store.getSession())
        assertNull(store.current.value)
    }

    @Test
    fun `new instance reads the persisted session and initializes current`() {
        store.saveSession("persisted@example.com", "persisted-token")
        val reopened = CredentialsStore(service)
        assertEquals("persisted@example.com", reopened.getSession()?.email)
        assertEquals("persisted-token", reopened.getSession()?.refreshToken)
        assertEquals("persisted@example.com", reopened.current.value?.email)
    }

    @Test
    fun `unicode round trips`() {
        val email = "excursió@example.com"
        store.saveSession(email, "密碼🔑é-token")
        assertEquals(email, store.getSession()?.email)
        assertEquals("密碼🔑é-token", store.getSession()?.refreshToken)
    }

    @Test
    fun `save and clear do not affect another service`() {
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
