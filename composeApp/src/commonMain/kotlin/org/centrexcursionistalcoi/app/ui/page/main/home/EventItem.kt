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
        dialogHeadline = {
            Text(
                text = stringResource(
                    Res.string.event_by,
                    event.department?.displayName ?: stringResource(Res.string.event_by)
                ),
            )
            Text(
                text = event.localizedDateRange(),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

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

            Spacer(Modifier.height(12.dp))
        },
        dialogBottom = {
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(MaterialSymbols.Distance, stringResource(Res.string.event_place))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = event.place,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            val activeInsurances = remember(profile) { profile.activeInsurances() }
            val activeInsurancesForEvent = remember(profile) { profile.activeInsurances(event.start) }
            if (event.requiresInsurance) {
                if (activeInsurancesForEvent.isEmpty()) {
                    if (activeInsurances.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.event_requires_insurance_none),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.event_requires_insurance_period, event.localizedDateRange()),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.event_requires_insurance_valid),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF29BA2D),
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }
            }

            if (event.requiresConfirmation) {
                val assistanceConfirmed = event.userReferences.find { it.sub == profile.sub } != null
                Text(
                    text = stringResource(Res.string.event_requires_confirmation),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                )

                var isLoading by remember { mutableStateOf(false) }
                if (assistanceConfirmed) {
                    Button(
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            isLoading = true
                            onRejectAssistanceRequest().invokeOnCompletion {
                                isLoading = false
                            }
                        },
                    ) { Text(stringResource(Res.string.event_reject_assistance)) }
                } else {
                    OutlinedButton(
                        enabled = !isLoading && (!event.requiresInsurance || activeInsurancesForEvent.isNotEmpty()),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            isLoading = true
                            onConfirmAssistanceRequest().invokeOnCompletion {
                                isLoading = false
                            }
                        },
                    ) { Text(stringResource(Res.string.event_confirm_assistance)) }
                }
            }

            Spacer(Modifier.height(56.dp))
        }
    )
}
