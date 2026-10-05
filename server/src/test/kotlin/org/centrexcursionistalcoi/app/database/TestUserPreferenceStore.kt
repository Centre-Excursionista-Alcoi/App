package org.centrexcursionistalcoi.app.database

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser

class TestUserPreferenceStore {
    @AfterTest
    fun clearDatabase() = Database.clear()

    @Test
    fun `any preference can be stored, by name`() = runTest {
        Database.initForTests()
        Database { FakeUser.provideEntity(); FakeAdminUser.provideEntity() }

        Database {
            assertNull(UserPreferenceStore[FakeUser.SUB, "theme"])

            UserPreferenceStore.set(FakeUser.SUB, "theme", "dark")
            UserPreferenceStore.set(FakeUser.SUB, "anything.else", "1")
            UserPreferenceStore.set(FakeAdminUser.SUB, "theme", "light")

            assertEquals("dark", UserPreferenceStore.get(FakeUser.SUB, "theme"))
            assertEquals("light", UserPreferenceStore.get(FakeAdminUser.SUB, "theme"))
            assertEquals(mapOf("theme" to "dark", "anything.else" to "1"), UserPreferenceStore.all(FakeUser.SUB))

            // Setting it again replaces it
            UserPreferenceStore.set(FakeUser.SUB, "theme", "system")
            assertEquals("system", UserPreferenceStore.get(FakeUser.SUB, "theme"))
            assertEquals(2, UserPreferenceStore.all(FakeUser.SUB).size)

            UserPreferenceStore.remove(FakeUser.SUB, "theme")
            assertNull(UserPreferenceStore.get(FakeUser.SUB, "theme"))
            assertEquals("light", UserPreferenceStore.get(FakeAdminUser.SUB, "theme"))
        }
    }

    @Test
    fun `setting if missing keeps what the user chose`() = runTest {
        Database.initForTests()
        Database { FakeUser.provideEntity() }

        Database {
            assertTrue(UserPreferenceStore.setIfMissing(FakeUser.SUB, "theme", "dark"))
            assertFalse(UserPreferenceStore.setIfMissing(FakeUser.SUB, "theme", "light"))
            assertEquals("dark", UserPreferenceStore.get(FakeUser.SUB, "theme"))
        }
    }

    @Test
    fun `a key with a type is stored as text and read back`() = runTest {
        Database.initForTests()
        Database { FakeUser.provideEntity() }

        Database {
            assertNull(UserPreferenceStore[FakeUser.SUB, UserPreferenceKey.Language])

            UserPreferenceStore[FakeUser.SUB, UserPreferenceKey.Language] = Locale.forLanguageTag("ca-ES")
            assertEquals("ca-ES", UserPreferenceStore.get(FakeUser.SUB, "language"))
            assertEquals(Locale.forLanguageTag("ca-ES"), UserPreferenceStore[FakeUser.SUB, UserPreferenceKey.Language])

            // A value that isn't valid for the key is as if it wasn't set
            UserPreferenceStore.set(FakeUser.SUB, "language", "*")
            assertNull(UserPreferenceStore[FakeUser.SUB, UserPreferenceKey.Language])
        }
    }
}
