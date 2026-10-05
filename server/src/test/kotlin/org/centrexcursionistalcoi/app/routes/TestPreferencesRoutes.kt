package org.centrexcursionistalcoi.app.routes

import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.patch
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.UserPreferenceKey
import org.centrexcursionistalcoi.app.database.UserPreferenceStore
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.request.UpdatePreferencesRequest
import org.centrexcursionistalcoi.app.response.PreferencesResponse
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType

class TestPreferencesRoutes : ApplicationTestBase() {
    private suspend fun io.ktor.client.HttpClient.update(request: UpdatePreferencesRequest) = patch(Api.Profile.Preferences()) {
        contentType(ContentType.Application.Json)
        setBody(request)
    }

    @Test
    fun test_notLoggedIn() = runApplicationTest {
        client.get(Api.Profile.Preferences()).assertStatusCode(HttpStatusCode.Unauthorized)
        client.update(UpdatePreferencesRequest(language = "ca")).assertStatusCode(HttpStatusCode.Unauthorized)
    }

    @Test
    fun test_nothingIsSetByDefault() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.get(Api.Profile.Preferences()).assertBody(PreferencesResponse.serializer()) {
            assertNull(it.language)
        }
    }

    @Test
    fun test_languageIsChanged_andReplacesWhatWasRemembered() = runApplicationTest(shouldLogIn = LoginType.USER) {
        // The first request with a language is remembered
        client.get(Api.Profile.Preferences()) { header(HttpHeaders.AcceptLanguage, "es") }
            .assertBody(PreferencesResponse.serializer()) { assertEquals("es", it.language) }

        // But what the user chooses replaces it
        client.update(UpdatePreferencesRequest(language = "ca-ES")).assertStatusCode(HttpStatusCode.NoContent)
        client.get(Api.Profile.Preferences()).assertBody(PreferencesResponse.serializer()) {
            assertEquals("ca-ES", it.language)
        }
        assertEquals(Locale.forLanguageTag("ca-ES"), Database { UserPreferenceStore[FakeUser.SUB, UserPreferenceKey.Language] })
    }

    @Test
    fun test_aLanguageOfAnotherUserIsNotChanged() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { FakeAdminUser.provideEntity() },
    ) {
        client.update(UpdatePreferencesRequest(language = "ca")).assertStatusCode(HttpStatusCode.NoContent)
        assertNull(Database { UserPreferenceStore[FakeAdminUser.SUB, UserPreferenceKey.Language] })
    }

    @Test
    fun test_invalidLanguage() = runApplicationTest(shouldLogIn = LoginType.USER) {
        for (language in listOf("", "*", "not a language!")) {
            client.update(UpdatePreferencesRequest(language = language)).assertError(Error.InvalidArgument("language"))
        }
        assertNull(Database { UserPreferenceStore[FakeUser.SUB, UserPreferenceKey.Language] })
    }

    @Test
    fun test_nothingToUpdate() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.update(UpdatePreferencesRequest()).assertError(Error.NothingToUpdate())
    }
}
