package org.centrexcursionistalcoi.app.viewmodel.spaces

import androidx.lifecycle.ViewModel
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

/**
 * What the booking form is for: [spaceId], and, to modify a lending that hasn't been picked up yet, [lendingId].
 *
 * Handed to the view model as a single parameter: Koin matches injected parameters by type, so two ids of the same
 * type would both receive the first one.
 */
data class SpaceLendingFormTarget(val spaceId: Uuid, val lendingId: Uuid? = null)

/**
 * Creates a lending of a space, or, if the target has a lending, modifies it.
 */
@KoinViewModel
class SpaceLendingCreationViewModel(
    @InjectedParam target: SpaceLendingFormTarget,
    spacesRepository: SpacesRepository,
    spaceKeysRepository: SpaceKeysRepository,
    private val spaceLendingsRepository: SpaceLendingsRepository,
    private val spaceLendingsRemoteRepository: SpaceLendingsRemoteRepository,
) : ViewModel() {
    private val log = logging()

    private val spaceId = target.spaceId
    private val lendingId = target.lendingId

    val space = spacesRepository.getAsFlow(spaceId).stateInViewModel()
    val keys = spaceKeysRepository.getBySpaceAsFlow(spaceId).stateInViewModel()

    /**
     * The dates of the lending being modified, or `null` when creating one. They are only the starting point: the
     * dates being chosen live in the date picker, and are handed to [submit].
     */
    var initialDates: Pair<LocalDate, LocalDate>? = null
        private set

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

    private val _isLoaded = MutableStateFlow(lendingId == null)
    /** Whether the lending being modified has been loaded. Always `true` when creating. */
    val isLoaded = _isLoaded.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking = _isWorking.asStateFlow()

    val isEditing: Boolean get() = lendingId != null

    init {
        launch {
            if (lendingId != null) {
                spaceLendingsRepository.get(lendingId)?.let { lending ->
                    initialDates = lending.checkIn to lending.checkOut
                    _attendees.value = lending.attendees
                    _keyQuantities.value = lending.keys.associate { it.key to it.quantity }
                    _notes.value = lending.notes.orEmpty()
                    _acceptedConditions.value = lending.acceptedConditionsAt != null
                }
                _isLoaded.value = true
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

    fun setAttendees(category: Category, count: Int) = _attendees.update { it + (category to count.coerceAtLeast(0)) }

    fun setKeyQuantity(key: Uuid, quantity: Int) = _keyQuantities.update { it + (key to quantity.coerceAtLeast(0)) }

    fun setNotes(notes: String) {
        _notes.value = notes
    }

    fun setAcceptedConditions(accepted: Boolean) {
        _acceptedConditions.value = accepted
    }

    /**
     * Sends the lending for the stay from [checkIn] to [checkOut]. [onDone] is called with the lending's id on success.
     */
    fun submit(checkIn: LocalDate, checkOut: LocalDate, onDone: (Uuid) -> Unit) = launch {
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
