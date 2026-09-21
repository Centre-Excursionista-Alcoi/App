package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.event_assisting_users
import cea_app.composeapp.generated.resources.event_by
import cea_app.composeapp.generated.resources.event_department
import cea_app.composeapp.generated.resources.event_department_generic
import cea_app.composeapp.generated.resources.event_end
import cea_app.composeapp.generated.resources.event_qualification_unknown
import cea_app.composeapp.generated.resources.event_qualifications_or
import cea_app.composeapp.generated.resources.event_qualifications_required
import cea_app.composeapp.generated.resources.event_requires_confirmation_forced
import cea_app.composeapp.generated.resources.event_max_people
import cea_app.composeapp.generated.resources.event_max_people_value
import cea_app.composeapp.generated.resources.event_place
import cea_app.composeapp.generated.resources.event_place_cea
import cea_app.composeapp.generated.resources.event_requires_confirmation
import cea_app.composeapp.generated.resources.event_requires_insurance
import cea_app.composeapp.generated.resources.event_start
import cea_app.composeapp.generated.resources.event_title
import cea_app.composeapp.generated.resources.icon_monochrome
import cea_app.composeapp.generated.resources.management_event_create
import cea_app.composeapp.generated.resources.management_no_events
import cea_app.composeapp.generated.resources.submit
import com.mikepenz.markdown.m3.Markdown
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Job
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Department.Companion.departmentsWithRole
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.localizedDateRange
import org.centrexcursionistalcoi.app.data.normalizedRequirements
import org.centrexcursionistalcoi.app.data.requirements
import org.centrexcursionistalcoi.app.data.sameRequirementsAs
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Distance
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Groups
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.HealthAndSafety
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.PersonCheck
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.DropdownField
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.reusable.editor.RichTextStyleRow
import org.centrexcursionistalcoi.app.ui.reusable.form.DateTimePickerFormField
import org.centrexcursionistalcoi.app.ui.reusable.form.FormImagePicker
import org.centrexcursionistalcoi.app.ui.reusable.form.FormSwitchRow
import org.centrexcursionistalcoi.app.ui.reusable.form.QualificationRequirementsField
import org.centrexcursionistalcoi.app.ui.utils.optional
import org.centrexcursionistalcoi.app.viewmodel.management.EventsManagementViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Composable
fun EventsListView(model: EventsManagementViewModel = koinViewModel()) {
    val profile by model.profile.collectAsState()
    val events by model.events.collectAsState()
    val departments by model.departments.collectAsState()
    val qualifications by model.qualifications.collectAsState()

    val profileValue = profile
    if (profileValue == null) {
        LoadingBox()
        return
    }

    EventsListView(
        profile = profileValue,
        events = events,
        departments = departments,
        qualifications = qualifications.orEmpty(),
        onCreate = model::createEvent,
        onUpdate = model::updateEvent,
        onDelete = model::deleteEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventsListView(
    profile: ProfileResponse,
    events: List<ReferencedEvent>?,
    departments: List<Department>?,
    qualifications: List<Qualification>,
    onCreate: (start: LocalDateTime, end: LocalDateTime?, place: String, title: String, description: RichTextState, maxPeople: String, requiresConfirmation: Boolean, requiresInsurance: Boolean, department: Department?, qualificationRequirements: List<List<Uuid>>, image: PlatformFile?, progressNotifier: ProgressNotifier) -> Job,
    onUpdate: (eventId: Uuid, start: LocalDateTime?, end: LocalDateTime?, place: String?, title: String?, description: RichTextState?, maxPeople: String?, requiresConfirmation: Boolean?, requiresInsurance: Boolean?, department: Department?, qualificationRequirements: List<List<Uuid>>?, image: PlatformFile?, progressNotifier: ProgressNotifier) -> Job,
    onDelete: (ReferencedEvent) -> Job,
) {
    // Mirrors the server's CONTENT_MANAGER check for events: creating/editing an event scoped to a department
    // requires that role there (or global admin); an event with no department (public/global) requires admin.
    val managedDepartments = remember(profile, departments) {
        departments.orEmpty().departmentsWithRole(profile, DepartmentRole.CONTENT_MANAGER)
    }
    val departmentOptions = if (profile.isAdmin) departments.orEmpty() else managedDepartments
    val canCreate = profile.isAdmin || managedDepartments.isNotEmpty()

    ListView(
        items = events,
        itemIdProvider = { it.id },
        itemDisplayName = { it.title },
        itemSupportingContent = { Text(it.localizedDateRange()) },
        emptyItemsText = stringResource(Res.string.management_no_events),
        isCreatingSupported = canCreate,
        createTitle = stringResource(Res.string.management_event_create),
        onDeleteRequest = onDelete,
        canModify = { event ->
            profile.isAdmin || event.department?.let { d -> managedDepartments.any { it.id == d.id } } == true
        },
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
            var requiresInsurance by remember { mutableStateOf(event?.requiresInsurance ?: false) }
            // For a non-admin creating a new event, default to their (only allowed) department instead of
            // leaving it at "no department" -- global/public events require admin.
            var department by remember { mutableStateOf(event?.department ?: managedDepartments.firstOrNull().takeIf { !profile.isAdmin }) }
            var image by remember { mutableStateOf<PlatformFile?>(null) }
            // Groups of alternatives that must all be met to attend, see Event.qualificationRequirements. A group
            // with nothing picked yet is kept while editing, and dropped when saving.
            var requirements by remember { mutableStateOf(event?.qualificationRequirements.orEmpty()) }
            val savedRequirements = requirements.normalizedRequirements()
            // The requirements can only be enforced through the sign-up, so an event with any needs one
            val effectiveRequiresConfirmation = requiresConfirmation || savedRequirements.isNotEmpty()

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
                        effectiveRequiresConfirmation != event.requiresConfirmation ||
                        !(savedRequirements sameRequirementsAs event.qualificationRequirements) ||
                        requiresInsurance != event.requiresInsurance ||
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
                checked = effectiveRequiresConfirmation,
                onCheckedChange = { requiresConfirmation = it },
                label = stringResource(Res.string.event_requires_confirmation),
                description = if (savedRequirements.isNotEmpty()) stringResource(Res.string.event_requires_confirmation_forced) else null,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                enabled = !isLoading && savedRequirements.isEmpty(),
            )
            FormSwitchRow(
                checked = requiresInsurance,
                onCheckedChange = { requiresInsurance = it },
                label = stringResource(Res.string.event_requires_insurance),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                enabled = !isLoading,
            )

            DropdownField(
                value = department,
                onValueChange = {
                    // An event can only require its own department's qualifications
                    if (it?.id != department?.id) requirements = emptyList()
                    department = it
                },
                options = departmentOptions,
                label = stringResource(Res.string.event_department).optional(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                itemToString = { it?.displayName ?: stringResource(Res.string.event_department_generic) },
                // Only a global admin may leave an event without a department (public/global content).
                allowNull = profile.isAdmin,
            )

            QualificationRequirementsField(
                groups = requirements,
                onGroupsChange = { requirements = it },
                qualifications = department?.let { d -> qualifications.filter { it.departmentId == d.id } },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                enabled = !isLoading,
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
                            effectiveRequiresConfirmation,
                            requiresInsurance,
                            department,
                            savedRequirements,
                            image,
                            ProgressNotifier { progress = it }
                        )
                    } else {
                        onUpdate(
                            event.id,
                            start.takeIf { it != event.start },
                            end.takeIf { it != event.end },
                            place.takeIf { it != event.place },
                            title.takeIf { it != event.title },
                            description.takeIf { it.toMarkdown() != event.description },
                            maxPeople.takeIf { it != event.maxPeople?.toString() },
                            effectiveRequiresConfirmation.takeIf { it != event.requiresConfirmation },
                            requiresInsurance.takeIf { it != event.requiresInsurance },
                            department.takeIf { it?.id != event.department?.id },
                            // Also sent (as an empty list) when a department change dropped them all
                            savedRequirements.takeIf { !(it sameRequirementsAs event.qualificationRequirements) },
                            image,
                            ProgressNotifier { progress = it }
                        )
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
            Spacer(Modifier.height(12.dp))
        }

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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = MaterialSymbols.Distance,
                contentDescription = stringResource(Res.string.event_place),
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = event.place,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        event.maxPeople?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = MaterialSymbols.Groups,
                    contentDescription = stringResource(Res.string.event_max_people),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = pluralStringResource(Res.plurals.event_max_people_value, it.toInt(), it),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (event.requiresConfirmation) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = MaterialSymbols.PersonCheck,
                    contentDescription = stringResource(Res.string.event_requires_confirmation),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = stringResource(Res.string.event_requires_confirmation),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (event.requiresInsurance) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = MaterialSymbols.HealthAndSafety,
                    contentDescription = stringResource(Res.string.event_requires_insurance),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = stringResource(Res.string.event_requires_insurance),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        val requirements = remember(event, qualifications) { event.requirements(qualifications, emptyList(), Clock.System.now()) }
        if (requirements.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.event_qualifications_required),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            val orSeparator = " ${stringResource(Res.string.event_qualifications_or)} "
            val unknown = stringResource(Res.string.event_qualification_unknown)
            for (requirement in requirements) {
                Text(
                    text = "- " + requirement.alternatives.joinToString(orSeparator) { it ?: unknown },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        event.description?.let { description ->
            Markdown(description, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }

        if (event.userSubList.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.event_assisting_users) + " (${event.userSubList.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            for (user in event.userSubList) {
                Text(
                    text = "- " + user.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
