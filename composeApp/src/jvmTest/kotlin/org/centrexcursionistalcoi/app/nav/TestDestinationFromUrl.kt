package org.centrexcursionistalcoi.app.nav

import io.ktor.http.Url
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Which screen a link opens. The links that matter are the ones the server puts in its emails to admins, like
 * `https://centrexcursionistalcoi.app/admin/lendings/<id>` ("Open in app"). There is no custom URL scheme: a plain
 * web link is the only way into the app, on every platform.
 */
class TestDestinationFromUrl {
    private val id = Uuid.parse("1f0e5c2a-0000-4000-8000-000000000001")

    @AfterTest
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    private suspend fun open(link: String) = Destination.fromUrl(Url(link))

    // ---- The links the server sends ----

    @Test
    fun adminLendingLink_opensThatLending() = runTest {
        assertEquals(Destination.Admin.LendingManagement(id), open("https://centrexcursionistalcoi.app/admin/lendings/$id"))
    }

    @Test
    fun adminLendingsLink_withoutALending_opensTheLendingsList() = runTest {
        assertEquals(Destination.Main(showingAdminLendingsScreen = true), open("https://centrexcursionistalcoi.app/admin/lendings"))
    }

    @Test
    fun adminItemsLink_opensThatItemType() = runTest {
        assertEquals(Destination.Main(showingAdminItemTypeId = id), open("https://centrexcursionistalcoi.app/admin/items/$id"))
    }

    @Test
    fun adminItemsLink_withoutAnItemType_opensNothing() = runTest {
        assertNull(open("https://centrexcursionistalcoi.app/admin/items"))
    }

    @Test
    fun linksAreMatchedIgnoringCase() = runTest {
        // not every app that shows a link keeps the case of its host
        assertEquals(Destination.Admin.LendingManagement(id), open("https://centrexcursionistalcoi.app/Admin/LENDINGS/$id"))
    }

    @Test
    fun aBadOrMissingId_opensNothing() = runTest {
        assertNull(open("https://centrexcursionistalcoi.app/admin/lendings/not-a-uuid"))
        assertNull(open("https://centrexcursionistalcoi.app/admin/lendings/$id/extra"))
    }

    // ---- Item types (needs the local repository) ----

    @Test
    fun itemTypeLink_ofAnItemTypeThatIsNotStored_opensNothing() = runTest {
        val repository = mockk<InventoryItemTypesRepository>()
        coEvery { repository.get(id) } returns null
        startKoin { modules(module { single { repository } }) }

        assertNull(open("https://centrexcursionistalcoi.app/itemType/$id"))
    }

    @Test
    fun itemTypeLink_withoutAValidId_opensNothing() = runTest {
        assertNull(open("https://centrexcursionistalcoi.app/itemType/not-a-uuid"))
        assertNull(open("https://centrexcursionistalcoi.app/itemType"))
    }

    // ---- Reset password ----

    @Test
    fun resetPasswordLink_succeeded_opensLogin() = runTest {
        assertEquals(Destination.Login(changedPassword = true), open("https://centrexcursionistalcoi.app/reset_password?success=true"))
    }

    @Test
    fun resetPasswordLink_withARequest_opensTheResetScreen() = runTest {
        assertEquals(Destination.External.ResetPassword("abc"), open("https://centrexcursionistalcoi.app/reset_password?request_id=abc"))
        // the same page, reached on the server's own host
        assertEquals(Destination.External.ResetPassword("abc"), open("https://server.centrexcursionistalcoi.app/reset_password?request_id=abc"))
    }

    @Test
    fun resetPasswordLink_withoutARequest_opensNothing() = runTest {
        assertNull(open("https://centrexcursionistalcoi.app/reset_password"))
    }

    // ---- Anything else ----

    @Test
    fun aLinkThatPointsNowhere_opensNothing_insteadOfCrashing() = runTest {
        // these used to throw, reaching for the first path segment of a link that has none
        assertNull(open("https://centrexcursionistalcoi.app/"))
        assertNull(open("https://centrexcursionistalcoi.app/foo"))
        assertNull(open("https://centrexcursionistalcoi.app/foo/bar/baz"))
        assertNull(open("https://server.centrexcursionistalcoi.app/"))
        assertNull(open("https://server.centrexcursionistalcoi.app/download/abc"))
    }

    @Test
    fun noLink_opensNothing() = runTest {
        assertNull(Destination.fromUrl(null))
    }

    // ---- The back stack for a link ----

    @Test
    fun aLendingLink_getsTheListBehindIt() {
        val lending = Destination.Admin.LendingManagement(id)
        assertEquals(listOf(Destination.Main(showingAdminLendingsScreen = true), lending), Destination.backStackFor(lending))
    }
}
