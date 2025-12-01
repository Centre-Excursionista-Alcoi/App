package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import com.mikepenz.markdown.m3.Markdown
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.localizedDateRange
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.DropdownField
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.reusable.editor.RichTextStyleRow
import org.centrexcursionistalcoi.app.ui.reusable.form.DatePickerFormField
import org.centrexcursionistalcoi.app.ui.reusable.form.DateTimePickerFormField
import org.centrexcursionistalcoi.app.ui.reusable.form.FormImagePicker
import org.centrexcursionistalcoi.app.ui.reusable.form.FormSwitchRow
import org.centrexcursionistalcoi.app.ui.utils.optional
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsListView(
    windowSizeClass: WindowSizeClass,
    events: List<ReferencedEvent>?,
    departments: List<Department>?,
    onCreate: (start: LocalDateTime, end: LocalDateTime?, place: String, title: String, description: RichTextState, maxPeople: String, requiresConfirmation: Boolean, department: Department?, image: PlatformFile?, progressNotifier: (Progress) -> Unit) -> Job,
    onUpdate: (eventId: Uuid, start: LocalDateTime?, end: LocalDateTime?, place: String?, title: String?, description: RichTextState?, maxPeople: String?, requiresConfirmation: Boolean?, department: Department?, image: PlatformFile?, progressNotifier: (Progress) -> Unit) -> Job,
    onDelete: (ReferencedEvent) -> Job,
) {
    ListView(
        windowSizeClass = windowSizeClass,
        items = events,
        itemIdProvider = { it.id },
        itemDisplayName = { it.title },
        itemSupportingContent = { Text(it.localizedDateRange()) },
        emptyItemsText = stringResource(Res.string.management_no_events),
        isCreatingSupported = true,
        createTitle = stringResource(Res.string.management_event_create),
        onDeleteRequest = onDelete,
        editItemContent = { event ->
            var isLoading by remember { mutableStateOf(false) }
            var progress by remember { mutableStateOf<Progress?>(null) }

            var title by remember { mutableStateOf(event?.title ?: "") }
            var place by remember { mutableStateOf(event?.place ?: "") }
            var start by remember { mutableStateOf(event?.start?.toLocalDateTime(TimeZone.currentSystemDefault())) }
            var end by remember { mutableStateOf(event?.end?.toLocalDateTime(TimeZone.currentSystemDefault())) }
            val description = rememberRichTextState()
            var maxPeople by remember { mutableStateOf(event?.maxPeople?.toString() ?: "") }
            var requiresConfirmation by remember { mutableStateOf(event?.requiresConfirmation ?: false) }
            var department by remember { mutableStateOf(event?.department) }
            var image by remember { mutableStateOf<PlatformFile?>(null) }

            LaunchedEffect(event) {
                if (event != null) {
                    description.setMarkdown(event.description ?: "")
                }
            }

            val isValid = title.isNotBlank() && place.isNotBlank() && start != null
            val isDirty =
                event == null || title != event.title ||
                        department?.id != event.department?.id ||
                        description.toMarkdown() != event.description ||
                        place != event.place ||
                        start != event.start.toLocalDateTime(TimeZone.currentSystemDefault()) ||
                        end != event.end?.toLocalDateTime(TimeZone.currentSystemDefault()) ||
                        maxPeople != event.maxPeople?.toString() ||
                        requiresConfirmation != event.requiresConfirmation ||
                        image != null

            FormImagePicker(
                image = image,
                container = event,
                onImagePicked = { image = it },
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp)),
                isLoading = isLoading,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(Res.string.event_title)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = place,
                onValueChange = { place = it },
                label = { Text(stringResource(Res.string.event_place)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            place = "Centre Excursionista Alcoi\nDiego Fernàndez Montañés, 3. Alcoi 03801 (Alacant)"
                        },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.icon_monochrome),
                            contentDescription = stringResource(Res.string.event_place_cea),
                            tint = LocalContentColor.current,
                        )
                    }
                },
            )

            DateTimePickerFormField(
                value = start,
                onValueChange = { start = it },
                label = stringResource(Res.string.event_start),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                enabled = !isLoading,
            )
            DateTimePickerFormField(
                value = end,
                onValueChange = { end = it },
                label = stringResource(Res.string.event_end).optional(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                enabled = !isLoading,
            )

            OutlinedTextField(
                value = maxPeople,
                onValueChange = { value ->
                    value.toIntOrNull() ?: return@OutlinedTextField
                    maxPeople = value
                },
                label = { Text(stringResource(Res.string.event_max_people).optional()) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            FormSwitchRow(
                checked = requiresConfirmation,
                onCheckedChange = { requiresConfirmation = it },
                label = stringResource(Res.string.event_requires_confirmation),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                enabled = !isLoading,
            )

            DropdownField(
                value = department,
                onValueChange = { department = it },
                options = departments.orEmpty(),
                label = stringResource(Res.string.event_department).optional(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                itemToString = { it?.displayName ?: stringResource(Res.string.event_department_generic) },
                allowNull = true,
            )

            RichTextStyleRow(
                modifier = Modifier.fillMaxWidth(),
                state = description,
                enabled = !isLoading,
            )
            OutlinedRichTextEditor(
                state = description,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                enabled = !isLoading,
            )

            Spacer(Modifier.height(64.dp))

            LinearLoadingIndicator(progress)

            OutlinedButton(
                enabled = !isLoading && isDirty && isValid,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                onClick = {
                    isLoading = true
                    val job = if (event == null) {
                        onCreate(
                            start!!,
                            end,
                            place,
                            title,
                            description,
                            maxPeople,
                            requiresConfirmation,
                            department,
                            image
                        ) {
                            progress = it
                        }
                    } else {
                        onUpdate(
                            event.id,
                            start.takeIf { it != event.start },
                            end.takeIf { it != event.end },
                            place.takeIf { it != event.place },
                            title.takeIf { it != event.title },
                            description.takeIf { it.toMarkdown() != event.description },
                            maxPeople.takeIf { it != event.maxPeople?.toString() },
                            requiresConfirmation.takeIf { it != event.requiresConfirmation },
                            department.takeIf { it?.id != event.department?.id },
                            image
                        ) { progress = it }
                    }
                    job.invokeOnCompletion {
                        isLoading = false
                        finishEdit()
                    }
                }
            ) {
                Text(stringResource(Res.string.submit))
            }
        },
    ) { event ->
        if (event.image != null) {
            val image by event.rememberImageFile()
            AsyncByteImage(
                bytes = image,
                contentDescription = event.title,
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp))
            )
        }

        Text(
            text = event.title,
            style = MaterialTheme.typography.titleLarge,
        )
        Row {
            Text(
                text = stringResource(
                    Res.string.event_by,
                    event.department?.displayName ?: stringResource(Res.string.event_department_generic)
                ),
            )
            Text(" - ")
            Text(
                text = event.localizedDateRange(),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = stringResource(Res.string.event_place),
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = event.place,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        event.maxPeople?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = stringResource(Res.string.event_max_people),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = pluralStringResource(Res.plurals.event_max_people_value, it.toInt(), it),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        if (event.requiresConfirmation) {
            Text(
                text = stringResource(Res.string.event_requires_confirmation),
                fontWeight = FontWeight.Bold,
            )
        }

        event.description?.let {
            Markdown(
                content = it,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
