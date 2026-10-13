package org.centrexcursionistalcoi.app.viewmodel.spaces

import androidx.lifecycle.ViewModel
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.PaymentMethod
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.database.SpaceKeyTypesRepository
import org.centrexcursionistalcoi.app.database.SpaceKeysRepository
import org.centrexcursionistalcoi.app.database.SpaceLendingsRepository
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.ServerInfoRepository
import org.centrexcursionistalcoi.app.network.SpaceLendingsRemoteRepository
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SpaceLendingDetailsViewModel(
    @InjectedParam private val lendingId: Uuid,
    spaceLendingsRepository: SpaceLendingsRepository,
    spacesRepository: SpacesRepository,
    spaceKeyTypesRepository: SpaceKeyTypesRepository,
    spaceKeysRepository: SpaceKeysRepository,
    profileRepository: ProfileRepository,
    private val remote: SpaceLendingsRemoteRepository,
    serverInfo: ServerInfoRepository,
) : ViewModel() {
    /** What can go wrong with a payment in a way the user can do something about, rather than a failure to report. */
    enum class PaymentIssue {
        /** The server can't take payments now: try later. */
        GATEWAY_UNAVAILABLE,

        /** The payment provider refused to return the money. */
        REFUND_REJECTED,

        /** The lending wasn't paid online, so there is nothing to return from here. */
        NOT_PAID_ONLINE,
    }

    /** The ways of paying online the server offers, empty if it doesn't take online payments. */
    val paymentMethods: List<PaymentMethod> = serverInfo.info?.paymentMethods.orEmpty()

    private val _paymentIssue = MutableStateFlow<PaymentIssue?>(null)
    val paymentIssue = _paymentIssue.asStateFlow()

    private val _awaitingPayment = MutableStateFlow(false)

    /** A payment was opened in the browser, and its result isn't known yet. */
    val awaitingPayment = _awaitingPayment.asStateFlow()

    val lending = spaceLendingsRepository.getAsFlow(lendingId).stateInViewModel()
    val profile = profileRepository.profile.stateInViewModel()

    val space = lending.flatMapLatest { lending ->
        if (lending == null) flowOf(null) else spacesRepository.getAsFlow(lending.space)
    }.stateInViewModel()

    /** The types of keys, to tell the names of the ones the lending asks for. */
    val keyTypes = spaceKeyTypesRepository.selectAllAsFlow().stateInViewModel()

    /** The keys of the club. Only managers get them. */
    val keys = spaceKeysRepository.selectAllAsFlow().stateInViewModel()

    /** The keys that are out with any lending, so they can't be given again. */
    val keysOut = spaceLendingsRepository.selectAllAsFlow()
        .map { lendings -> lendings.flatMap { it.keys }.filter { it.isOut }.map { it.key }.toSet() }
        .stateInViewModel()

    private val _isWorking = MutableStateFlow(false)
    val isWorking = _isWorking.asStateFlow()

    private fun work(block: suspend () -> Unit) = launch {
        _isWorking.value = true
        try {
            block()
        } finally {
            _isWorking.value = false
        }
    }

    fun cancel() = work { remote.cancel(lendingId) }

    fun updateAttendees(attendees: Map<Category, Int>) = work { remote.updateAttendees(lendingId, attendees) }

    /** @param keys The exact keys given to the user. */
    fun pickup(keys: List<Uuid>) = work { remote.pickup(lendingId, keys) }

    /** @param keys The keys that are back, or `null` for all that are still out. */
    fun returnKeys(keys: List<Uuid>?) = work { remote.returnKeys(lendingId, keys) }

    /**
     * Pays the lending online.
     * @param openUrl Opens the page of the payment in a browser.
     */
    fun pay(method: PaymentMethod, openUrl: (String) -> Unit) = work {
        _paymentIssue.value = null
        val launch = try {
            remote.startPayment(lendingId, method)
        } catch (e: ServerException) {
            if (e.errorCode != Error.ERROR_PAYMENT_GATEWAY_UNAVAILABLE) throw e
            _paymentIssue.value = PaymentIssue.GATEWAY_UNAVAILABLE
            return@work
        }
        _awaitingPayment.value = true
        openUrl(launch.url)
    }

    /** Fetches the lending again, to see whether the payment went through. */
    fun checkPayment() = work {
        remote.update(lendingId, ignoreIfModifiedSince = true)
        if (lending.value?.paymentStatus != PaymentStatus.PENDING) _awaitingPayment.value = false
    }

    /** For managers: returns the money of a lending that was paid online. */
    fun refundOnline() = work {
        _paymentIssue.value = null
        try {
            remote.refund(lendingId)
        } catch (e: ServerException) {
            _paymentIssue.value = when (e.errorCode) {
                Error.ERROR_PAYMENT_REJECTED -> PaymentIssue.REFUND_REJECTED
                Error.ERROR_INVALID_SPACE_LENDING_STATE -> PaymentIssue.NOT_PAID_ONLINE
                Error.ERROR_PAYMENT_GATEWAY_UNAVAILABLE -> PaymentIssue.GATEWAY_UNAVAILABLE
                else -> throw e
            }
        }
    }

    fun setPayment(status: PaymentStatus) = work { remote.setPayment(lendingId, status) }

    fun submitReport(notes: String?, issues: String?, notesFiles: List<PlatformFile>, issuesFiles: List<PlatformFile>, onDone: () -> Unit) = work {
        remote.submitReport(lendingId, notes, issues, notesFiles, issuesFiles)
        onDone()
    }

    fun attachPaymentProof(files: List<PlatformFile>, onDone: () -> Unit) = work {
        remote.attachPaymentProof(lendingId, files)
        onDone()
    }

    fun refresh() = work { remote.update(lendingId, ignoreIfModifiedSince = true) }
}
