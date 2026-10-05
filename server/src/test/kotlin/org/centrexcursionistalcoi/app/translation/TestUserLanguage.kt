package org.centrexcursionistalcoi.app.translation

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.UserPreferenceKey
import org.centrexcursionistalcoi.app.database.UserPreferenceStore
import org.centrexcursionistalcoi.app.database.table.UserPreferences
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.notifications.emailLocale
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType

class TestUserLanguage : ApplicationTestBase() {
    private fun language() = Database { UserPreferenceStore[FakeUser.SUB, UserPreferenceKey.Language] }

    @Test
    fun `the language is set by the first request that has one`() = runApplicationTest(shouldLogIn = LoginType.USER) {
        // Without a header, nothing is known
        client.get("/profile").assertStatusCode(HttpStatusCode.OK)
        assertNull(language())

        client.get("/profile") { header(HttpHeaders.AcceptLanguage, "ca-ES,ca;q=0.9") }.assertStatusCode(HttpStatusCode.OK)
        assertEquals(Locale.forLanguageTag("ca-ES"), language())

        // And it is not changed by the next ones
        client.get("/profile") { header(HttpHeaders.AcceptLanguage, "es") }.assertStatusCode(HttpStatusCode.OK)
        assertEquals(Locale.forLanguageTag("ca-ES"), language())
    }

    @Test
    fun `a request that is not logged in sets nothing`() = runApplicationTest(shouldLogIn = LoginType.NONE) {
        client.get("/profile") { header(HttpHeaders.AcceptLanguage, "ca") }
        assertEquals(0L, Database { UserPreferences.selectAll().count() })
    }

    @Test
    fun `any language is not a language`() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.get("/profile") { header(HttpHeaders.AcceptLanguage, "*") }.assertStatusCode(HttpStatusCode.OK)
        assertNull(language())
    }

    @Test
    fun `emails are sent in the language of the user, or in english`() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val user = Database { UserReferenceEntity[FakeUser.SUB] }
        assertEquals(Locale.ENGLISH, user.emailLocale())

        client.get("/profile") { header(HttpHeaders.AcceptLanguage, "es") }.assertStatusCode(HttpStatusCode.OK)
        assertEquals(Locale.forLanguageTag("es"), user.emailLocale())
    }

    @Test
    fun `deleting the user deletes their preferences`() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.get("/profile") { header(HttpHeaders.AcceptLanguage, "es") }.assertStatusCode(HttpStatusCode.OK)
        Database { UserReferenceEntity[FakeUser.SUB].delete() }
        assertEquals(0L, Database { UserPreferences.selectAll().count() })
    }
}
