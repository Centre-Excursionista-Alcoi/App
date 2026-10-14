package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Deferred
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.data.sortedForDisplay
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.koin.core.annotation.KoinViewModel

/** One insurance as the profile lists it: what it is, and where it stands today (see [UserInsurance.Status]). */
data class InsuranceRow(
    val id: Uuid,
    val company: String,
    val policyNumber: String,
    val status: UserInsurance.Status,
    /** When it ends (active, expired) or starts (upcoming). */
    val date: LocalDate,
    val isFemecvSync: Boolean,
)

/** A department the user belongs to, or asked to join ([confirmed] is `false` until a manager accepts). */
data class DepartmentMembership(val department: Department, val confirmed: Boolean)

@KoinViewModel
class ProfilePageModel(
    profileRepository: ProfileRepository,
    departmentsRepository: DepartmentsRepository,
    private val departmentsRemoteRepository: DepartmentsRemoteRepository,
    private val profileRemoteRepository: ProfileRemoteRepository,
) : ViewModel() {
    val profile = profileRepository.profile.stateInViewModel()
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()

    /** The user's insurances, ordered as the profile shows them. */
    val insuranceRows = profile.map { profile ->
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        profile?.insurances?.sortedForDisplay(today)?.map {
            val status = it.status(today)
            InsuranceRow(
                id = it.id,
                company = it.insuranceCompany,
                policyNumber = it.policyNumber,
                status = status,
                date = if (status == UserInsurance.Status.UPCOMING) it.validFrom else it.validTo,
                isFemecvSync = it.femecvLicense != null,
            )
        }
    }.stateInViewModel()

    /** Whether the user has an insurance that covers today, `null` until the profile is loaded. */
    val hasActiveInsurance = profile.map { it?.activeInsurances()?.isNotEmpty() }.stateInViewModel()

    val memberships = combine(profile, departments) { profile, departments ->
        if (profile == null || departments == null) return@combine null
        departments.mapNotNull { department ->
            department.members.orEmpty().find { it.userSub == profile.sub }?.let { DepartmentMembership(department, it.confirmed) }
        }
    }.stateInViewModel()

    /** The departments the user can ask to join: the ones they aren't in. */
    val joinableDepartments = combine(departments, memberships) { departments, memberships ->
        if (departments == null || memberships == null) return@combine null
        departments.filter { department -> memberships.none { it.department.id == department.id } }
    }.stateInViewModel()

    /**
     * Creates the insurance, then refreshes the profile in the background.
     *
     * Completes with `true` once the server has stored the insurance, or `null` if that failed (the error is reported
     * through [async]'s global handler). The refresh isn't awaited: failing it doesn't undo the insurance, and treating
     * it as a failure would invite submitting the same insurance again.
     */
    fun createInsurance(
        company: String,
        policyNumber: String,
        validFrom: LocalDate,
        validTo: LocalDate,
        documents: List<PlatformFile>,
    ): Deferred<Boolean?> = async {
        profileRemoteRepository.createInsurance(company, policyNumber, validFrom, validTo, documents)
        this@ProfilePageModel.launch { profileRemoteRepository.synchronize() }
        true
    }

    /**
     * [createInsurance] for the native UI: dates as `2026-01-31`, documents as the paths of PDF files. Completes with
     * whether it was stored.
     */
    suspend fun addInsurance(
        company: String,
        policyNumber: String,
        validFrom: String,
        validTo: String,
        documentPaths: List<String>,
    ): Boolean = createInsurance(
        company, policyNumber, LocalDate.parse(validFrom), LocalDate.parse(validTo), documentPaths.map { PlatformFile(it) }
    ).await() == true

    /** [connectFEMECV] for the native UI: completes with why FEMECV said no, or `null` once it's connected. */
    suspend fun connectFemecv(username: String, password: String): String? =
        connectFEMECV(username, password.toCharArray()).await()?.let { it.message ?: it.toString() }

    fun connectFEMECV(username: String, password: CharArray): Deferred<Throwable?> = async {
        try {
            profileRemoteRepository.connectFEMECV(username, password)
            null
        } catch (e: ServerException) {
            e
        }
    }

    fun disconnectFEMECV() = launch {
        profileRemoteRepository.disconnectFEMECV()
    }

    fun requestJoinDepartment(department: Department) = launch {
        departmentsRemoteRepository.requestJoin(department.id)
    }

    fun leaveDepartment(department: Department) = launch {
        departmentsRemoteRepository.leave(department.id)
    }
}
