package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.memory_editor_department
import cea_app.composeapp.generated.resources.memory_editor_description
import cea_app.composeapp.generated.resources.memory_editor_external_participants
import cea_app.composeapp.generated.resources.memory_editor_external_participants_help
import cea_app.composeapp.generated.resources.memory_editor_from
import cea_app.composeapp.generated.resources.memory_editor_member_participants
import cea_app.composeapp.generated.resources.memory_editor_place
import cea_app.composeapp.generated.resources.memory_editor_place_help
import cea_app.composeapp.generated.resources.memory_editor_save
import cea_app.composeapp.generated.resources.memory_editor_sport
import cea_app.composeapp.generated.resources.memory_editor_title
import cea_app.composeapp.generated.resources.memory_editor_to
import cea_app.composeapp.generated.resources.memory_editor_upload_image
import cea_app.composeapp.generated.resources.memory_editor_warning_no_lending
import cea_app.composeapp.generated.resources.remove
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.data.displayName
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.ui.data.PastSelectableDates
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.AttachFile
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Remove
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Upload
import org.centrexcursionistalcoi.app.ui.reusable.DropdownField
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.ui.reusable.editor.RichTextStyleRow
import org.centrexcursionistalcoi.app.ui.reusable.form.AutocompleteFormField
import org.centrexcursionistalcoi.app.ui.reusable.form.DateTimePickerFormField
import org.centrexcursionistalcoi.app.utils.unaccent
import org.centrexcursionistalcoi.app.viewmodel.ActivityMemoryEditorViewModel
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

@Composable
fun ActivityMemoryEditor(
    lendingId: Uuid?,
    model: ActivityMemoryEditorViewModel = viewModel { ActivityMemoryEditorViewModel(lendingId) },
    onBack: () -> Unit
) {
    val members by model.members.collectAsState()
    val departments by model.departments.collectAsState()
    val isSaving by model.isSaving.collectAsState()
    val saveProgress by model.saveProgress.collectAsState()
    val uploadSuccessful by model.uploadSuccessful.collectAsState()

    LaunchedEffect(uploadSuccessful) {
        if (uploadSuccessful) {
            onBack()
        }
    }

    ActivityMemoryEditor(
        isForLending = model.isForLending,
        isSaving = isSaving,
        saveProgress = saveProgress,
        members = members,
        departments = departments,
        onSave = model::save,
        onBack = onBack,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ActivityMemoryEditor(
    isForLending: Boolean,
    isSaving: Boolean,
    saveProgress: Progress?,
    members: List<Member>?,
    departments: List<Department>?,
    onSave: (from: ZonedDateTime?, to: ZonedDateTime?, place: String, memberUsers: List<Member>, externalUsers: String, sport: Sports?, department: Department?, description: RichTextState, files: List<PlatformFile>) -> Unit,
    onBack: () -> Unit,
) {
    val state = rememberRichTextState()
    var from by remember { mutableStateOf<ZonedDateTime?>(null) }
    var to by remember { mutableStateOf<ZonedDateTime?>(null) }
    var place by remember { mutableStateOf("") }
    var memberUsers by remember { mutableStateOf<List<Member>>(emptyList()) }
    var externalUsers by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf<Sports?>(null) }
    var department by remember { mutableStateOf<Department?>(null) }
    var files by remember { mutableStateOf<List<PlatformFile>>(emptyList()) }

    // Form validation:
    // If not for lending, from and to must be provided, and description must not be empty.
    // If for lending, only description must not be empty.
    val formValid = isForLending || (from != null && to != null) && state.toMarkdown().isNotEmpty()

    // TODO: Instead of disabling the upload button, when clicking, scroll and highlight the first invalid field, and show a message

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(onBack)
                },
                title = { Text(stringResource(Res.string.memory_editor_title)) },
                actions = {
                    IconButton(
                        enabled = formValid && !isSaving,
                        onClick = { onSave(from, to, place, memberUsers, externalUsers, sport, department, state, files) }
                    ) {
                        Icon(
                            MaterialSymbols.Upload,
                            stringResource(Res.string.memory_editor_save)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (isSaving) saveProgress?.let { progress ->
                LinearLoadingIndicator(progress, modifier = Modifier.fillMaxWidth())
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(paddingValues)) {
            if (!isForLending) {
                OutlinedCard(
                    modifier = Modifier.padding(8.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.memory_editor_warning_no_lending),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }

                // TODO: Actually allow to select timezone
                DateTimePickerFormField(
                    value = from?.dateTime,
                    onValueChange = { from = ZonedDateTime.forSystemDefault(it) },
                    label = stringResource(Res.string.memory_editor_from),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    enabled = !isSaving,
                    selectableDates = PastSelectableDates.today(),
                )
                DateTimePickerFormField(
                    value = to?.dateTime,
                    onValueChange = { to = ZonedDateTime.forSystemDefault(it) },
                    label = stringResource(Res.string.memory_editor_to),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    enabled = !isSaving,
                    selectableDates = PastSelectableDates.today(),
                )
            }

            // Place
            OutlinedTextField(
                value = place,
                onValueChange = { place = it },
                label = { Text(stringResource(Res.string.memory_editor_place)) },
                supportingText = { Text(stringResource(Res.string.memory_editor_place_help)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                enabled = !isSaving,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            // Member Participants
            var searchingForUser by remember { mutableStateOf("") }
            AutocompleteFormField(
                value = searchingForUser,
                onValueChange = { searchingForUser = it },
                label = { Text(stringResource(Res.string.memory_editor_member_participants)) },
                suggestions = members.orEmpty()
                    .filter { member -> member.fullName.uppercase().unaccent().contains(searchingForUser.uppercase().unaccent()) }
                    .toSet(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                toString = { it.fullName },
                enabled = !isSaving,
                onSuggestionClicked = { memberUsers += it; searchingForUser = "" },
            )
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                item { Spacer(Modifier.width(8.dp)) }
                items(memberUsers) { user ->
                    AssistChip(
                        enabled = !isSaving,
                        onClick = { memberUsers -= user },
                        label = { Text(user.fullName) },
                        trailingIcon = {
                            Icon(MaterialSymbols.Remove, stringResource(Res.string.remove))
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                item { Spacer(Modifier.width(8.dp)) }
            }

            // External Participants
            OutlinedTextField(
                value = externalUsers,
                onValueChange = { externalUsers = it },
                label = { Text(stringResource(Res.string.memory_editor_external_participants)) },
                supportingText = { Text(stringResource(Res.string.memory_editor_external_participants_help)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                enabled = !isSaving,
            )

            // Sport
            DropdownField(
                value = sport,
                onValueChange = { sport = it },
                options = Sports.entries,
                label = stringResource(Res.string.memory_editor_sport),
                itemToString = { it?.displayName ?: "" },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )

            // Department
            DropdownField(
                value = department,
                onValueChange = { department = it },
                options = departments.orEmpty(),
                label = stringResource(Res.string.memory_editor_department),
                itemToString = { it?.displayName ?: "" },
                enabled = !isSaving,
                allowNull = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )

            // Activity description:
            Text(
                text = stringResource(Res.string.memory_editor_description),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 16.dp),
            )
            RichTextStyleRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp),
                state = state,
                enabled = !isSaving,
            )
            OutlinedRichTextEditor(
                state = state,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 16.dp),
                enabled = !isSaving,
            )

            // Images
            val imagePicker = rememberFilePickerLauncher(FileKitType.ImageAndVideo, mode = FileKitMode.Multiple()) { pickedFiles ->
                if (pickedFiles == null) return@rememberFilePickerLauncher
                files += pickedFiles
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                onClick = { imagePicker.launch() },
                enabled = !isSaving,
            ) {
                Icon(MaterialSymbols.AttachFile, stringResource(Res.string.memory_editor_upload_image))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.memory_editor_upload_image))
            }
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                item { Spacer(Modifier.width(8.dp)) }
                items(files) { file ->
                    AssistChip(
                        enabled = !isSaving,
                        onClick = { files -= file },
                        label = { Text(file.name) },
                        trailingIcon = {
                            Icon(MaterialSymbols.Remove, stringResource(Res.string.remove))
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                item { Spacer(Modifier.width(8.dp)) }
            }

            Spacer(Modifier.height(56.dp))
        }
    }
}

@Preview
@Composable
fun ActivityMemoryEditor_Preview() {
    ActivityMemoryEditor(
        isForLending = false,
        isSaving = false,
        saveProgress = null,
        departments = null,
        members = listOf(
            Member(1u, Member.Status.ACTIVE, "Alice", "87654321X", "alice@example.com"),
            Member(2u, Member.Status.ACTIVE, "Bob", "12345678Z", "bob@example.com"),
            Member(3u, Member.Status.ACTIVE, "Charlie", "11223344B", "charlie@example.com"),
        ),
        onSave = { _, _, _, _, _, _, _, _, _ -> },
        onBack = {},
    )
}
