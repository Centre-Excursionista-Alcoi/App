package org.centrexcursionistalcoi.app.viewmodel

import kotlin.time.Clock
import kotlin.time.Instant
import org.centrexcursionistalcoi.app.data.EventRequirement
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.canChangeAssistance
import org.centrexcursionistalcoi.app.data.requirements
import org.centrexcursionistalcoi.app.response.ProfileResponse

/** Whether the user's insurance covers an event that requires one. */
enum class EventInsuranceStatus {
    /** The event doesn't require insurance. */
    NOT_REQUIRED,

    /** It does, and the user has none that is active. */
    NONE,

    /** It does, and the user has an active one, but it doesn't cover the dates of the event. */
    NOT_VALID_FOR_EVENT,

    VALID,
}

/**
 * What a user can do about an event they might attend, and why they can't when they can't: what the events' details
 * show. The same rules as the Compose `EventItem`, in one place so a screen only words them.
 */
data class EventAssistanceState(
    val insurance: EventInsuranceStatus,
    /** One per group of qualifications the event asks for, see [EventRequirement]. */
    val requirements: List<EventRequirement>,
    val requiresConfirmation: Boolean,
    val isConfirmed: Boolean,
    /** Events of a department are for its members. */
    val isUserInDepartment: Boolean,
    /** The server refuses to confirm or withdraw once the event has started. */
    val canChangeAssistance: Boolean,
    val meetsRequirements: Boolean,
    /** Whether confirming is offered. Withdrawing is always possible while [canChangeAssistance]. */
    val canConfirm: Boolean,
    /** The user can't confirm because of the qualifications they lack: worth saying, as the button is disabled. */
    val showMissingQualifications: Boolean,
)

fun ReferencedEvent.assistanceState(
    profile: ProfileResponse,
    qualifications: List<Qualification>,
    myQualificationGrants: List<QualificationGrant>,
    now: Instant,
): EventAssistanceState {
    val insuranceForEvent = profile.activeInsurances(start)
    val insurance = when {
        !requiresInsurance -> EventInsuranceStatus.NOT_REQUIRED
        insuranceForEvent.isNotEmpty() -> EventInsuranceStatus.VALID
        profile.activeInsurances().isEmpty() -> EventInsuranceStatus.NONE
        else -> EventInsuranceStatus.NOT_VALID_FOR_EVENT
    }
    val requirements = requirements(qualifications, myQualificationGrants, now)
    val meetsRequirements = requirements.all { it.isMet }
    val canChange = canChangeAssistance(now)
    val isConfirmed = userSubList.any { it.sub == profile.sub }
    val isUserInDepartment = department?.let { profile.departments.contains(it.id) } ?: true
    return EventAssistanceState(
        insurance = insurance,
        requirements = requirements,
        requiresConfirmation = requiresConfirmation,
        isConfirmed = isConfirmed,
        isUserInDepartment = isUserInDepartment,
        canChangeAssistance = canChange,
        meetsRequirements = meetsRequirements,
        canConfirm = canChange && isUserInDepartment && meetsRequirements &&
            (!requiresInsurance || insuranceForEvent.isNotEmpty()),
        // Someone already signed up (before the requirements changed, say) can still withdraw
        showMissingQualifications = !meetsRequirements && !isConfirmed && canChange,
    )
}

/** [assistanceState] as of now. */
fun ReferencedEvent.assistanceState(
    profile: ProfileResponse,
    qualifications: List<Qualification>,
    myQualificationGrants: List<QualificationGrant>,
): EventAssistanceState = assistanceState(profile, qualifications, myQualificationGrants, Clock.System.now())
