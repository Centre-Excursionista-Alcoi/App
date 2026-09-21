package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import com.diamondedge.logging.logging
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.DepartmentRosterMember
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.QualificationsRemoteRepository
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel

/**
 * Backs the qualifications management tab. Qualification data isn't stored locally, so everything here is loaded
 * from the server on demand; call [refresh] when the screen is shown (not from `init`, so creating the view model
 * never touches the network).
 */
@KoinViewModel
class QualificationsManagementViewModel(
    private val dispatcherProvider: DispatcherProvider,
    departmentsRepository: DepartmentsRepository,
    private val qualificationsRemoteRepository: QualificationsRemoteRepository,
) : ViewModel() {
    companion object {
        private val log = logging()

        /** How many members to load per department to put names to the holders of a qualification. */
        private const val ROSTER_LIMIT = 200

        /** How many matches to show while searching for a member to grant a qualification to. */
        private const val SEARCH_LIMIT = 20
    }

    val profile = ProfileRepository.profile.stateInViewModel()
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()

    /** Every qualification of every department, or `null` until first loaded. */
    val qualifications: StateFlow<List<Qualification>?>
        field = MutableStateFlow<List<Qualification>?>(null)

    /** The holders of each qualification whose holders have been loaded (see [loadGrants]), by qualification id. */
    val grants: StateFlow<Map<Uuid, List<QualificationGrant>>>
        field = MutableStateFlow<Map<Uuid, List<QualificationGrant>>>(emptyMap())

    /** The members of each department whose roster has been loaded (see [loadRoster]), by department id. */
    val roster: StateFlow<Map<Uuid, List<DepartmentRosterMember>>>
        field = MutableStateFlow<Map<Uuid, List<DepartmentRosterMember>>>(emptyMap())

    fun refresh() = launch {
        val loaded = withContext(dispatcherProvider.io) { qualificationsRemoteRepository.list() }
        qualifications.value = loaded
    }

    fun create(departmentId: Uuid, name: String, description: String?) = launch {
        withContext(dispatcherProvider.io) { qualificationsRemoteRepository.create(departmentId, name, description) }
        refresh()
    }

    fun update(qualification: Qualification, name: String, description: String?) = launch {
        val newName = name.takeIf { it != qualification.name }
        // A blank description clears it on the server, so "" is a real value here; only skip it if unchanged
        val newDescription = (description ?: "").takeIf { it != (qualification.description ?: "") }
        // Nothing changed: the server would answer NothingToUpdate
        if (newName == null && newDescription == null) return@launch

        withContext(dispatcherProvider.io) { qualificationsRemoteRepository.update(qualification.id, newName, newDescription) }
        refresh()
    }

    fun delete(qualification: Qualification) = launch {
        withContext(dispatcherProvider.io) { qualificationsRemoteRepository.delete(qualification.id) }
        grants.update { it - qualification.id }
        refresh()
    }

    fun loadGrants(qualification: Qualification) = launch {
        val loaded = withContext(dispatcherProvider.io) { qualificationsRemoteRepository.grants(qualification.id) }
        grants.update { it + (qualification.id to loaded) }
    }

    fun grant(qualification: Qualification, userSub: String, expiresAt: Instant?) = launch {
        withContext(dispatcherProvider.io) { qualificationsRemoteRepository.grant(qualification.id, userSub, expiresAt) }
        loadGrants(qualification)
    }

    fun revoke(qualification: Qualification, userSub: String) = launch {
        withContext(dispatcherProvider.io) { qualificationsRemoteRepository.revoke(qualification.id, userSub) }
        loadGrants(qualification)
    }

    /** Loads the department's members, to show names instead of ids for the holders of its qualifications. */
    fun loadRoster(departmentId: Uuid) = launch {
        val loaded = withContext(dispatcherProvider.io) { qualificationsRemoteRepository.roster(departmentId, limit = ROSTER_LIMIT) }
        roster.update { it + (departmentId to loaded) }
    }

    /**
     * Searches [departmentId]'s members by name, for picking who to grant a qualification to. Never throws: if the
     * search fails the error is reported and there are simply no results.
     */
    suspend fun searchRoster(departmentId: Uuid, query: String): List<DepartmentRosterMember> = try {
        withContext(dispatcherProvider.io) { qualificationsRemoteRepository.roster(departmentId, query, SEARCH_LIMIT) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.e(e) { "Could not search the department's members." }
        emptyList()
    }
}
