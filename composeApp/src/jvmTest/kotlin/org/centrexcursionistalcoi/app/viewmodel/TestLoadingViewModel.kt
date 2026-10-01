package org.centrexcursionistalcoi.app.viewmodel

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.push.FCMTokenManager
import org.centrexcursionistalcoi.app.response.ProfileResponse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Starting the app with its profile stored, but no session: e.g. after the server rejected the session's refresh
 * token. Everything was then requested without logging in, and failed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestLoadingViewModel {
    private val dispatcherProvider = object : DispatcherProvider {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
    }

    private val profileRepository = mockk<ProfileRepository> {
        coEvery { getProfile() } returns mockk<ProfileResponse>(relaxed = true)
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private val tokenManager = mockk<FCMTokenManager>(relaxed = true)

    private fun viewModel(authBackend: AuthBackend) = LoadingViewModel(
        dispatcherProvider = dispatcherProvider,
        backgroundJobCoordinator = mockk(relaxed = true),
        databaseIntegrityVerifier = mockk(relaxed = true),
        authBackend = authBackend,
        server = mockk(relaxed = true),
        tokenManager = tokenManager,
        profileRepository = profileRepository,
        settings = mockk(relaxed = true),
    )

    @Test
    fun `without a session that can't be recovered, the account is forgotten`() = runTest {
        val authBackend = mockk<AuthBackend>(relaxed = true) {
            coEvery { hasSession() } returns false
            coEvery { tryAutoRelogin() } returns false
        }
        var loggedIn = false
        var notLoggedIn = false

        viewModel(authBackend).load(onLoggedIn = { loggedIn = true }, onNotLoggedIn = { notLoggedIn = true }).join()

        assertTrue(notLoggedIn, "Should go to the login screen")
        assertFalse(loggedIn)
        coVerify { authBackend.forgetLocalAccount() }
    }

    @Test
    fun `without a session, a recovered one is used`() = runTest {
        val authBackend = mockk<AuthBackend>(relaxed = true) {
            coEvery { hasSession() } returns false
            coEvery { tryAutoRelogin() } returns true
        }
        var notLoggedIn = false

        viewModel(authBackend).load(onLoggedIn = {}, onNotLoggedIn = { notLoggedIn = true }).join()

        assertFalse(notLoggedIn)
        coVerify(exactly = 0) { authBackend.forgetLocalAccount() }
        // Loading went on with the stored profile
        coVerify { tokenManager.renovate() }
    }
}
