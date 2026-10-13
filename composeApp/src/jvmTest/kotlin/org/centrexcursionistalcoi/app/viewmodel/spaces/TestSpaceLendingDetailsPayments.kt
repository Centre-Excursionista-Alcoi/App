package org.centrexcursionistalcoi.app.viewmodel.spaces

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.data.PaymentLaunch
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.data.PaymentMethod
import org.centrexcursionistalcoi.app.data.ServerInfo
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.ServerInfoRepository
import org.centrexcursionistalcoi.app.network.SpaceLendingsRemoteRepository
import org.centrexcursionistalcoi.app.viewmodel.spaces.SpaceLendingDetailsViewModel.PaymentIssue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * What the app does with the answers of the online payment endpoints: open the page, or tell the user what they can
 * do about it, instead of the generic error dialog meant for failures to report.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestSpaceLendingDetailsPayments {
    private val lendingId = Uuid.random()
    private val remote = mockk<SpaceLendingsRemoteRepository>()

    private val dispatcherProvider = object : DispatcherProvider {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
    }

    @BeforeTest
    fun setUp() {
        startKoin { modules(module { single<DispatcherProvider> { dispatcherProvider } }) }
        Dispatchers.setMain(UnconfinedTestDispatcher())
        GlobalAsyncErrorHandler.clearError()
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        stopKoin()
        GlobalAsyncErrorHandler.clearError()
        Dispatchers.resetMain()
    }

    private fun viewModel(methods: List<PaymentMethod> = listOf(PaymentMethod.CARD)): SpaceLendingDetailsViewModel {
        val info = mockk<ServerInfoRepository> {
            every { this@mockk.info } returns ServerInfo(
                version = ServerInfo.Version("1.0.0", 15, 1),
                lastCEASync = 0,
                lastFEMECVSync = 0,
                paymentMethods = methods,
            )
        }
        return SpaceLendingDetailsViewModel(
            lendingId = lendingId,
            spaceLendingsRepository = mockk(relaxed = true) { every { getAsFlow(any()) } returns flowOf(null) },
            spacesRepository = mockk(relaxed = true),
            spaceKeyTypesRepository = mockk(relaxed = true),
            spaceKeysRepository = mockk(relaxed = true),
            profileRepository = mockk(relaxed = true),
            remote = remote,
            serverInfo = info,
        )
    }

    private fun error(code: Int) = ServerException("error", 500, null, code)

    @Test
    fun `the methods offered by the server are the ones shown`() {
        assertEquals(listOf(PaymentMethod.CARD, PaymentMethod.BIZUM), viewModel(listOf(PaymentMethod.CARD, PaymentMethod.BIZUM)).paymentMethods)
        assertTrue(viewModel(emptyList()).paymentMethods.isEmpty())
    }

    @Test
    fun `paying opens the page and waits for the result`() {
        coEvery { remote.startPayment(lendingId, PaymentMethod.CARD) } returns PaymentLaunch("https://app.example/pay?id=abc", 1800)
        val opened = mutableListOf<String>()
        val model = viewModel()

        model.pay(PaymentMethod.CARD, opened::add)

        assertEquals(listOf("https://app.example/pay?id=abc"), opened)
        assertTrue(model.awaitingPayment.value)
        assertNull(model.paymentIssue.value)
    }

    @Test
    fun `an unavailable gateway is explained, not reported as a failure`() {
        coEvery { remote.startPayment(any(), any(), any()) } throws error(Error.ERROR_PAYMENT_GATEWAY_UNAVAILABLE)
        val opened = mutableListOf<String>()
        val model = viewModel()

        model.pay(PaymentMethod.CARD, opened::add)

        assertEquals(PaymentIssue.GATEWAY_UNAVAILABLE, model.paymentIssue.value)
        assertTrue(opened.isEmpty())
        assertFalse(model.awaitingPayment.value)
        assertNull(GlobalAsyncErrorHandler.error.value)
    }

    @Test
    fun `any other failure is reported`() {
        coEvery { remote.startPayment(any(), any(), any()) } throws error(Error.ERROR_INVALID_SPACE_LENDING_STATE)
        val model = viewModel()

        model.pay(PaymentMethod.CARD) { }

        assertNull(model.paymentIssue.value)
        assertNotNull(GlobalAsyncErrorHandler.error.value)
    }

    @Test
    fun `a refused refund and one without an online payment are told apart`() {
        val model = viewModel()

        coEvery { remote.refund(lendingId, any()) } throws error(Error.ERROR_PAYMENT_REJECTED)
        model.refundOnline()
        assertEquals(PaymentIssue.REFUND_REJECTED, model.paymentIssue.value)

        coEvery { remote.refund(lendingId, any()) } throws error(Error.ERROR_INVALID_SPACE_LENDING_STATE)
        model.refundOnline()
        assertEquals(PaymentIssue.NOT_PAID_ONLINE, model.paymentIssue.value)

        coEvery { remote.refund(lendingId, any()) } returns Unit
        model.refundOnline()
        assertNull(model.paymentIssue.value)
        coVerify(exactly = 3) { remote.refund(lendingId, any()) }
    }
}
