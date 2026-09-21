package org.centrexcursionistalcoi.app.ui.page.main.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.event_add_to_calendar
import cea_app.composeapp.generated.resources.event_assistance_closed
import cea_app.composeapp.generated.resources.event_by
import cea_app.composeapp.generated.resources.event_confirm_assistance
import cea_app.composeapp.generated.resources.event_department_generic
import cea_app.composeapp.generated.resources.event_not_part_of_department
import cea_app.composeapp.generated.resources.event_place
import cea_app.composeapp.generated.resources.event_qualification_unknown
import cea_app.composeapp.generated.resources.event_qualifications_missing
import cea_app.composeapp.generated.resources.event_qualifications_or
import cea_app.composeapp.generated.resources.event_qualifications_required
import cea_app.composeapp.generated.resources.event_reject_assistance
import cea_app.composeapp.generated.resources.event_requires_confirmation
import cea_app.composeapp.generated.resources.event_requires_insurance_none
import cea_app.composeapp.generated.resources.event_requires_insurance_period
import cea_app.composeapp.generated.resources.event_requires_insurance_valid
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.Job
import kotlin.time.Clock
import org.centrexcursionistalcoi.app.data.EventRequirement
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.canChangeAssistance
import org.centrexcursionistalcoi.app.data.requirements
import org.centrexcursionistalcoi.app.data.addCalendarEvent
import org.centrexcursionistalcoi.app.data.localizedDateRange
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.platform.PlatformCalendarSync
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.CheckCircle
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Close
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Distance
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Event
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EventItem(
    profile: ProfileResponse,
    event: ReferencedEvent,
    qualifications: List<Qualification>,
    myQualificationGrants: List<QualificationGrant>,
    onConfirmAssistanceRequest: () -> Job,
    onRejectAssistanceRequest: () -> Job,
) {
    val platformCalendarSync = koinInject<PlatformCalendarSync>()

    FeedItem(
        icon = MaterialSymbols.Event,
        title = event.title,
        dateString = event.localizedDateRange(),
        content = event.description,
        dialogContent = {
            Text(
                text = stringResource(
                    Res.string.event_by,
                    event.department?.displayName ?: stringResource(Res.string.event_department_generic)
                ),
            )
            Text(
                text = event.localizedDateRange(),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(MaterialSymbols.Distance, stringResource(Res.string.event_place))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = event.place,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (platformCalendarSync.isSupported) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        platformCalendarSync.addCalendarEvent(event)
                    }
                ) {
                    Text(stringResource(Res.string.event_add_to_calendar))
                }
            }

            if (event.image != null) {
                val image by event.rememberImageFile()
                AsyncByteImage(
                    bytes = image,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    canBeMaximized = true,
                )
            }

            val activeInsurances = remember(profile) { profile.activeInsurances() }
            val activeInsurancesForEvent = remember(profile) { profile.activeInsurances(event.start) }
            if (event.requiresInsurance) {
                val (text, color) = if (activeInsurancesForEvent.isEmpty()) {
                    if (activeInsurances.isEmpty()) {
                        stringResource(Res.string.event_requires_insurance_none)
                    } else {
                        stringResource(Res.string.event_requires_insurance_period, event.localizedDateRange())
                    } to MaterialTheme.colorScheme.error
                } else {
                    stringResource(Res.string.event_requires_insurance_valid) to Color(0xFF29BA2D)
                }
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                )
            }

            val requirements = remember(event, qualifications, myQualificationGrants) {
                event.requirements(qualifications, myQualificationGrants, Clock.System.now())
            }
            if (requirements.isNotEmpty()) {
                EventQualificationRequirements(requirements)
            }
            val meetsRequirements = requirements.all { it.isMet }
            // The server refuses to confirm or withdraw once the event has started, so the buttons aren't offered then
            val canChangeAssistance = remember(event) { event.canChangeAssistance(Clock.System.now()) }

            if (event.requiresConfirmation) {
                val assistanceConfirmed = event.userSubList.find { it.sub == profile.sub } != null
                Text(
                    text = stringResource(Res.string.event_requires_confirmation),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                )

                val isUserInDepartment = event.department?.let { department ->
                    profile.departments.contains(department.id)
                } ?: true

                var isLoading by remember { mutableStateOf(false) }
                if (assistanceConfirmed) {
                    OutlinedButton(
                        enabled = canChangeAssistance && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        onClick = {
                            isLoading = true
                            onRejectAssistanceRequest().invokeOnCompletion {
                                isLoading = false
                            }
                        },
                    ) { Text(stringResource(Res.string.event_reject_assistance)) }
                } else {
                    Button(
                        enabled = canChangeAssistance && isUserInDepartment && !isLoading && meetsRequirements && (!event.requiresInsurance || activeInsurancesForEvent.isNotEmpty()),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            isLoading = true
                            onConfirmAssistanceRequest().invokeOnCompletion {
                                isLoading = false
                            }
                        },
                    ) { Text(stringResource(Res.string.event_confirm_assistance)) }
                }

                if (!isUserInDepartment) {
                    Text(
                        text = stringResource(Res.string.event_not_part_of_department),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (!canChangeAssistance) {
                    Text(
                        text = stringResource(Res.string.event_assistance_closed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                // Someone already signed up (before the requirements changed, say) can still withdraw, so this only
                // matters while they can't confirm. Once the event has started nothing can change, which is said above.
                if (!meetsRequirements && !assistanceConfirmed && canChangeAssistance) {
                    Text(
                        text = stringResource(Res.string.event_qualifications_missing),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            event.description?.let { description ->
                Markdown(description, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }

            Spacer(Modifier.height(56.dp))
        },
    )
}

/** What the event requires of whoever attends, one line per group that has to be met, and whether the user meets it. */
@Composable
private fun EventQualificationRequirements(requirements: List<EventRequirement>) {
    // The word only: spaces at the edge of a string resource are not reliably kept, so they're added here
    val orSeparator = " ${stringResource(Res.string.event_qualifications_or)} "
    val unknown = stringResource(Res.string.event_qualification_unknown)
    val metColor = Color(0xFF29BA2D)

    Text(
        text = stringResource(Res.string.event_qualifications_required),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
    )
    for (requirement in requirements) {
        val color = if (requirement.isMet) metColor else MaterialTheme.colorScheme.error
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            Icon(
                imageVector = if (requirement.isMet) MaterialSymbols.CheckCircle else MaterialSymbols.Close,
                contentDescription = null,
                tint = color,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = requirement.alternatives.joinToString(orSeparator) { it ?: unknown },
                color = color,
            )
        }
    }
}
