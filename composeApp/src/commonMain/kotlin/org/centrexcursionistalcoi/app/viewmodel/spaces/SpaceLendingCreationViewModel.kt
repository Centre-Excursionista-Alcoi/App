package org.centrexcursionistalcoi.app.viewmodel.spaces

import androidx.lifecycle.ViewModel
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.SpaceOccupancy
import org.centrexcursionistalcoi.app.database.SpaceKeysRepository
import org.centrexcursionistalcoi.app.database.SpaceLendingsRepository
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.network.SpaceLendingsRemoteRepository
import org.centrexcursionistalcoi.app.request.CreateSpaceLendingRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceLendingRequest
import org.centrexcursionistalcoi.app.utils.SpacePricing
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

/**
 * Creates a lending of a space, or, if [lendingId] is given, modifies one that hasn't been picked up yet.
 */
@KoinViewModel
class SpaceLendingCreationViewModel(
    @InjectedParam private val spaceId: Uuid,
    @InjectedParam private val lendingId: Uuid?,
    spacesRepository: SpacesRepository,
    spaceKeysRepository: SpaceKeysRepository,
    private val spaceLendingsRepository: SpaceLendingsRepository,
    private val spaceLendingsRemoteRepository: SpaceLendingsRemoteRepository,
) : ViewModel() {
    private val log = logging()

    val space = spacesRepository.getAsFlow(spaceId).stateInViewModel()
    val keys = spaceKeysRepository.getBySpaceAsFlow(spaceId).stateInViewModel()

    private val _checkIn = MutableStateFlow<LocalDate?>(null)
    val checkIn = _checkIn.asStateFlow()

    private val _checkOut = MutableStateFlow<LocalDate?>(null)
    val checkOut = _checkOut.asStateFlow()

    private val _attendees = MutableStateFlow<Map<Category, Int>>(emptyMap())
    val attendees = _attendees.asStateFlow()

    private val _keyQuantities = MutableStateFlow<Map<Uuid, Int>>(emptyMap())
    val keyQuantities = _keyQuantities.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    private val _acceptedConditions = MutableStateFlow(false)
    val acceptedConditions = _acceptedConditions.asStateFlow()

    private val _occupancy = MutableStateFlow<List<SpaceOccupancy>?>(null)
    val occupancy = _occupancy.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking = _isWorking.asStateFlow()

    /** The price of the stay with the current choices, or `null` until the dates are chosen. */
    val price = combine(space, checkIn, checkOut, attendees) { space, checkIn, checkOut, attendees ->
        if (space == null || checkIn == null || checkOut == null) null
        else SpacePricing.compute(space.prices, attendees, checkIn, checkOut)
    }.stateInViewModel()

    val isEditing: Boolean get() = lendingId != null

    init {
        launch {
            if (lendingId != null) {
                spaceLendingsRepository.get(lendingId)?.let { lending ->
                    _checkIn.value = lending.checkIn
                    _checkOut.value = lending.checkOut
                    _attendees.value = lending.attendees
                    _keyQuantities.value = lending.keys.associate { it.key to it.quantity }
                    _notes.value = lending.notes.orEmpty()
                    _acceptedConditions.value = lending.acceptedConditionsAt != null
                }
            }
            try {
                _occupancy.value = spaceLendingsRemoteRepository.occupancy(spaceId)
                    .filterNot { occupancy ->
                        // The lending being modified doesn't collide with itself
                        val own = lendingId?.let { spaceLendingsRepository.get(it) }
                        own != null && own.checkIn == occupancy.checkIn && own.checkOut == occupancy.checkOut
                    }
                    .sortedBy { it.checkIn }
            } catch (e: Exception) {
                log.w(e) { "Could not load the occupancy of space $spaceId" }
                _occupancy.value = emptyList()
            }
        }
    }

    fun setDates(checkIn: LocalDate?, checkOut: LocalDate?) {
        _checkIn.value = checkIn
        _checkOut.value = checkOut ?: checkIn
    }

    fun setAttendees(category: Category, count: Int) = _attendees.update { it + (category to count.coerceAtLeast(0)) }

    fun setKeyQuantity(key: Uuid, quantity: Int) = _keyQuantities.update { it + (key to quantity.coerceAtLeast(0)) }

    fun setNotes(notes: String) {
        _notes.value = notes
    }

    fun setAcceptedConditions(accepted: Boolean) {
        _acceptedConditions.value = accepted
    }

    /**
     * Sends the lending. [onDone] is called with the lending's id on success.
     */
    fun submit(onDone: (Uuid) -> Unit) = launch {
        val checkIn = checkIn.value ?: return@launch
        val checkOut = checkOut.value ?: return@launch
        _isWorking.value = true
        try {
            val attendees = attendees.value.filterValues { it > 0 }
            val keys = keyQuantities.value.filterValues { it > 0 }
            val id = if (lendingId != null) {
                spaceLendingsRemoteRepository.modify(
                    lendingId,
                    UpdateSpaceLendingRequest(
                        checkIn = checkIn,
                        checkOut = checkOut,
                        attendees = attendees,
                        keys = keys,
                        notes = notes.value,
                    ),
                )
                lendingId
            } else {
                spaceLendingsRemoteRepository.create(
                    CreateSpaceLendingRequest(
                        space = spaceId,
                        checkIn = checkIn,
                        checkOut = checkOut,
                        attendees = attendees,
                        keys = keys,
                        acceptConditions = acceptedConditions.value,
                        notes = notes.value.takeIf { it.isNotBlank() },
                    ),
                )
            }
            onDone(id)
        } finally {
            _isWorking.value = false
        }
    }
}
