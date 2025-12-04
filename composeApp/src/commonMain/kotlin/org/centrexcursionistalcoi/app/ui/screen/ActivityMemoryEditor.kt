package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.displayName
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.*
import org.centrexcursionistalcoi.app.ui.reusable.DropdownField
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.editor.RichTextStyleRow
import org.centrexcursionistalcoi.app.ui.reusable.form.AutocompleteFormField
import org.centrexcursionistalcoi.app.utils.unaccent
import org.centrexcursionistalcoi.app.viewmodel.ActivityMemoryEditorViewModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.uuid.Uuid

@Composable
fun ActivityMemoryEditor(
    lendingId: Uuid,
    model: ActivityMemoryEditorViewModel = viewModel { ActivityMemoryEditorViewModel(lendingId) },
    onBack: () -> Unit
) {
    val users by model.users.collectAsState()
    val isSaving by model.isSaving.collectAsState()
    val saveProgress by model.saveProgress.collectAsState()
    val uploadSuccessful by model.uploadSuccessful.collectAsState()

    LaunchedEffect(uploadSuccessful) {
        if (uploadSuccessful) {
            onBack()
        }
    }

    ActivityMemoryEditor(
        isSaving = isSaving,
        saveProgress = saveProgress,
        users = users,
        onSave = model::save,
        onBack = onBack,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ActivityMemoryEditor(
    isSaving: Boolean,
    saveProgress: Progress?,
    users: List<UserData>?,
    onSave: (place: String, memberUsers: List<UserData>, externalUsers: String, sport: Sports?, description: RichTextState, files: List<PlatformFile>) -> Unit,
    onBack: () -> Unit,
) {
    val state = rememberRichTextState()
    var place by remember { mutableStateOf("") }
    var memberUsers by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var externalUsers by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf<Sports?>(null) }
    var files by remember { mutableStateOf<List<PlatformFile>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                    ) {
                        Icon(MaterialSymbols.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                title = { Text(stringResource(Res.string.memory_editor_title)) },
                actions = {
                    IconButton(
                        enabled = !isSaving,
                        onClick = { onSave(place, memberUsers, externalUsers, sport, state, files) }
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
                suggestions = users.orEmpty()
                    .filter { user -> user.fullName.uppercase().unaccent().contains(searchingForUser.uppercase().unaccent()) }
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
        isSaving = false,
        saveProgress = null,
        users = listOf(
            UserData("000", "Alice", "alice@example.com", emptyList(), emptyList(), null, emptyList(), false, null),
            UserData("001", "Bob", "bob@example.com", emptyList(), emptyList(), null, emptyList(), false, null),
            UserData("002", "Charlie", "charlie@example.com", emptyList(), emptyList(), null, emptyList(), false, null),
        ),
        onSave = { _, _, _, _, _, _ -> },
        onBack = {},
    )
}
