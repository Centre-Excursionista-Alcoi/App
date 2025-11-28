package org.centrexcursionistalcoi.app.ui.page.main.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.addCalendarEvent
import org.centrexcursionistalcoi.app.data.localizedDateRange
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.platform.PlatformCalendarSync
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EventItem(event: ReferencedEvent) {
    FeedItem(
        icon = Icons.Default.Event,
        title = event.title,
        dateString = event.localizedDateRange(),
        content = event.description,
        dialogHeadline = {
            Text(
                text = stringResource(Res.string.event_by, event.department?.displayName ?: stringResource(Res.string.event_by)),
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Place, stringResource(Res.string.event_place))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = event.place,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (event.requiresConfirmation) {
                Text(
                    text = stringResource(Res.string.event_requires_confirmation),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(56.dp))
        }
    )
}
