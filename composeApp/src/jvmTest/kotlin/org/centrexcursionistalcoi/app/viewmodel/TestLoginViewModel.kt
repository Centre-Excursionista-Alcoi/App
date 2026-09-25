package org.centrexcursionistalcoi.app.viewmodel

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.auth.CredentialsStore
import org.centrexcursionistalcoi.app.auth.SavedAccount
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Regression coverage for the Login screen's "you already have an account saved" dialog, which used to reappear
 * for the account the user had *just* finished logging into: [LoginViewModel.existingAccountEmail] mirrored
 * [CredentialsStore.current] live, and [AuthBackend.login] saves the new credentials (updating `current`) well
 * before its caller navigates away, so the same successful login that was meant to proceed to the app instead
 * momentarily re-triggered its own "existing account" warning.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestLoginViewModel {
    private val dispatcherProvider = object : DispatcherProvider {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkObject(ProfileRemoteRepository)
        coEvery { ProfileRemoteRepository.synchronize(any(), any(), any()) } returns true
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `existingAccountEmail does not flip non-null from this screen's own successful login`() = runTest {
        val current = MutableStateFlow<SavedAccount?>(null)
        val credentialsStore = mockk<CredentialsStore> {
            every { this@mockk.current } returns current
        }
        val authBackend = mockk<AuthBackend>()
        coEvery { authBackend.login("user@example.com", "password") } coAnswers {
            // Mirrors AuthBackend.login's real side effect: credentialsStore.saveSession(...) updates `current` on
            // success, before login() returns and its caller gets a chance to navigate away.
            current.value = SavedAccount("user@example.com")
        }

        val viewModel = LoginViewModel(authBackend, dispatcherProvider, credentialsStore)
        assertNull(viewModel.existingAccountEmail.value, "No account was saved before the screen loaded")

        var loggedIn = false
        viewModel.login("user@example.com", "password") { loggedIn = true }

        assert(loggedIn) { "login() should have completed and invoked afterLogin" }
        assertNull(
            viewModel.existingAccountEmail.value,
            "Should stay null -- this is the account the user just logged into, not a pre-existing one",
        )
    }
}
