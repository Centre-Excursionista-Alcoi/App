package org.centrexcursionistalcoi.app.ui.page.main.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.addCalendarEvent
import org.centrexcursionistalcoi.app.data.localizedDateRange
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.platform.PlatformCalendarSync
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Distance
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Event
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EventItem(
    profile: ProfileResponse,
    event: ReferencedEvent,
    onConfirmAssistanceRequest: () -> Job,
    onRejectAssistanceRequest: () -> Job,
) {
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

            if (PlatformCalendarSync.isSupported) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        PlatformCalendarSync.addCalendarEvent(event)
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
                        enabled = !isLoading,
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
                        enabled = isUserInDepartment && !isLoading && (!event.requiresInsurance || activeInsurancesForEvent.isNotEmpty()),
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
            }

            event.description?.let { description ->
                Text(
                    text = description,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(56.dp))
        },
    )
}
