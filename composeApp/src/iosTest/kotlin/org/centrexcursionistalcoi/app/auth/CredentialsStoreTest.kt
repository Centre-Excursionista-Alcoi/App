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
    fun `get returns null when nothing is saved`() {
        store.clear()
        assertNull(store.get())
        assertNull(store.current.value)
    }

    @Test
    fun `save persists credentials retrievable via both get and current`() {
        store.save("credentials-test@example.com", "s3cr3t-P@ss")

        val saved = store.get()
        assertEquals("credentials-test@example.com", saved?.email)
        assertEquals("s3cr3t-P@ss", saved?.password?.concatToString())

        assertEquals(saved?.email, store.current.value?.email)
        assertEquals(saved?.password?.concatToString(), store.current.value?.password?.concatToString())
    }

    @Test
    fun `saving again for the same email updates the password`() {
        store.save("credentials-test@example.com", "first-password")
        store.save("credentials-test@example.com", "second-password")

        assertEquals("second-password", store.get()?.password?.concatToString())
    }

    @Test
    fun `only one account is kept -- saving a different email replaces the previous one`() {
        store.save("first@example.com", "first-password")
        store.save("second@example.com", "second-password")

        assertEquals("second@example.com", store.get()?.email)
        assertEquals("second-password", store.get()?.password?.concatToString())
        assertEquals(store.get(), store.current.value)
    }

    @Test
    fun `clear removes the saved account`() {
        store.save("credentials-test@example.com", "s3cr3t-P@ss")
        store.clear()

        assertNull(store.get())
        assertNull(store.current.value)
    }

    @Test
    fun `new instance reads persisted credentials and initializes current`() {
        store.save("persisted@example.com", "persisted-password")
        val reopened = CredentialsStore(service)
        assertEquals("persisted@example.com", reopened.get()?.email)
        assertEquals("persisted-password", reopened.get()?.password?.concatToString())
        assertEquals(store.get(), reopened.get())
        assertEquals(store.get(), reopened.current.value)
    }

    @Test
    fun `unicode and embedded null characters round trip`() {
        val email = "excursió@example.com"
        val password = "密碼🔑é\u0000final"
        store.save(email, password)
        assertEquals(email, store.get()?.email)
        assertEquals(password, store.get()?.password?.concatToString())
    }

    @Test
    fun `empty password round trips`() {
        store.save("empty@example.com", "")
        assertEquals("", store.get()?.password?.concatToString())
    }

    @Test
    fun `save and clear do not affect another service`() {
        val other = CredentialsStore("$service.other")
        try {
            other.save("other@example.com", "other-password")
            store.save("first@example.com", "first-password")
            store.save("second@example.com", "second-password")
            store.clear()
            store.clear()
            assertEquals("other@example.com", other.get()?.email)
            assertEquals("other-password", other.get()?.password?.concatToString())
        } finally {
            other.clear()
        }
    }
}
