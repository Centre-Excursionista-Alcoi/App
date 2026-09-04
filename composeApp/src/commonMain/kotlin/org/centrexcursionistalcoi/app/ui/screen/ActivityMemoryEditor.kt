package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Badge
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.name
import io.ktor.client.request.invoke
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ImageFileListContainer
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.data.displayName
import org.centrexcursionistalcoi.app.data.rememberImageFiles
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.ui.data.PastSelectableDates
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.AttachFile
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Delete
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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

@Composable
fun ActivityMemoryEditor(
    lendingId: Uuid?,
    memoryId: Uuid?,
    model: ActivityMemoryEditorViewModel = koinViewModel {
        parametersOf(ActivityMemoryEditorViewModel.Params(lendingId, memoryId))
    },
    onBack: () -> Unit
) {
    val memory by model.memory.collectAsState()
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
        memory = memory,
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
    memory: ReferencedMemory?,
    isSaving: Boolean,
    saveProgress: Progress?,
    members: List<Member>?,
    departments: List<Department>?,
    onSave: (from: ZonedDateTime?, to: ZonedDateTime?, place: String, memberUsers: List<Member>, externalUsers: String, sport: Sports?, department: Department?, description: RichTextState, files: List<PlatformFile>, removedFiles: List<Uuid>) -> Unit,
    onBack: () -> Unit,
) {
    val state = rememberRichTextState()
    var from by remember(memory) { mutableStateOf(memory?.from) }
    var to by remember(memory) { mutableStateOf(memory?.to) }
    var place by remember(memory) { mutableStateOf(memory?.place.orEmpty()) }
    var memberUsers by remember(memory) { mutableStateOf(memory?.members.orEmpty()) }
    var externalUsers by remember(memory) { mutableStateOf(memory?.externalUsers.orEmpty()) }
    var sport by remember(memory) { mutableStateOf(memory?.sport) }
    var department by remember(memory) { mutableStateOf(memory?.department) }

    var images by remember { mutableStateOf<List<PlatformFile>>(emptyList()) }
    var removedImages by remember { mutableStateOf<List<Uuid>>(emptyList()) }

    LaunchedEffect(memory) {
        memory?.let {
            state.setMarkdown(it.text)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(onBack)
                },
                title = { Text(stringResource(Res.string.memory_editor_title)) },
                actions = {
                    IconButton(
                        enabled = !isSaving,
                        onClick = {
                            onSave(
                                from,
                                to,
                                place,
                                memberUsers,
                                externalUsers,
                                sport,
                                department,
                                state,
                                images,
                                removedImages
                            )
                        }
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
        MemoryEditor_Content(
            isForLending = isForLending,
            isSaving = isSaving,
            members = members,
            departments = departments,
            state = state,
            from = from,
            to = to,
            place = place,
            memberUsers = memberUsers,
            externalUsers = externalUsers,
            sport = sport,
            department = department,
            previousImagesContainer = memory,
            removedPreviousImages = removedImages,
            images = images,
            onFromChange = { from = it },
            onToChange = { to = it },
            onPlaceChange = { place = it },
            onMemberUsersChange = { memberUsers = it },
            onExternalUsersChange = { externalUsers = it },
            onSportChange = { sport = it },
            onDepartmentChange = { department = it },
            onImagesChange = { images = it },
            onModifyRemovedPreviousImages = { removedImages = it },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        )
    }
}

@Preview
@Composable
fun ActivityMemoryEditor_Preview() {
    ActivityMemoryEditor(
        isForLending = false,
        memory = null,
        isSaving = false,
        saveProgress = null,
        departments = null,
        members = listOf(
            Member(1u, Member.Status.ACTIVE, "Alice", "87654321X", "alice@example.com"),
            Member(2u, Member.Status.ACTIVE, "Bob", "12345678Z", "bob@example.com"),
            Member(3u, Member.Status.ACTIVE, "Charlie", "11223344B", "charlie@example.com"),
        ),
        onSave = { _, _, _, _, _, _, _, _, _, _ -> },
        onBack = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryEditor_Content(
    isForLending: Boolean,
    isSaving: Boolean,
    members: List<Member>?,
    departments: List<Department>?,
    state: RichTextState,
    from: ZonedDateTime?,
    to: ZonedDateTime?,
    place: String,
    memberUsers: List<Member>,
    externalUsers: String,
    sport: Sports?,
    department: Department?,
    previousImagesContainer: ImageFileListContainer?,
    removedPreviousImages: List<Uuid>,
    images: List<PlatformFile>,
    onFromChange: (ZonedDateTime?) -> Unit,
    onToChange: (ZonedDateTime?) -> Unit,
    onPlaceChange: (String) -> Unit,
    onMemberUsersChange: (List<Member>) -> Unit,
    onExternalUsersChange: (String) -> Unit,
    onSportChange: (Sports?) -> Unit,
    onDepartmentChange: (Department?) -> Unit,
    onImagesChange: (List<PlatformFile>) -> Unit,
    onModifyRemovedPreviousImages: (List<Uuid>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Column(modifier = modifier) {
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
                onValueChange = { onFromChange(ZonedDateTime.forSystemDefault(it)) },
                label = stringResource(Res.string.memory_editor_from),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                enabled = !isSaving,
                selectableDates = PastSelectableDates.today(),
            )
            DateTimePickerFormField(
                value = to?.dateTime,
                onValueChange = { onToChange(ZonedDateTime.forSystemDefault(it)) },
                label = stringResource(Res.string.memory_editor_to),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                enabled = !isSaving,
                selectableDates = PastSelectableDates.today(),
            )
        }

        // Place
        OutlinedTextField(
            value = place,
            onValueChange = onPlaceChange,
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
                .filter { member ->
                    member.fullName.uppercase().unaccent()
                        .contains(searchingForUser.uppercase().unaccent())
                }
                .toSet(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            toString = { it.fullName },
            enabled = !isSaving,
            onSuggestionClicked = {
                onMemberUsersChange(memberUsers + it)
                searchingForUser = ""
            },
        )
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            item { Spacer(Modifier.width(8.dp)) }
            items(memberUsers) { user ->
                AssistChip(
                    enabled = !isSaving,
                    onClick = { onMemberUsersChange(memberUsers - user) },
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
            onValueChange = onExternalUsersChange,
            label = { Text(stringResource(Res.string.memory_editor_external_participants)) },
            supportingText = { Text(stringResource(Res.string.memory_editor_external_participants_help)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            enabled = !isSaving,
        )

        // Sport
        DropdownField(
            value = sport,
            onValueChange = onSportChange,
            options = Sports.entries,
            label = stringResource(Res.string.memory_editor_sport),
            itemToString = { it?.displayName ?: "" },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )

        // Department
        DropdownField(
            value = department,
            onValueChange = onDepartmentChange,
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
        val imagePicker = rememberFilePickerLauncher(
            FileKitType.Image,
            mode = FileKitMode.Multiple()
        ) { pickedFiles ->
            if (pickedFiles == null) return@rememberFilePickerLauncher
            onImagesChange(images + pickedFiles)
        }

        val previousImages = previousImagesContainer.rememberImageFiles()

        LazyRow(modifier = Modifier.fillMaxWidth()) {
            item { Spacer(Modifier.width(8.dp)) }

            items(previousImages.toList(), key = { it.first }, contentType = { "existing-image" }) { (uuid, bytes) ->
                val isRemoved = removedPreviousImages.contains(uuid)
                Box(
                    modifier = Modifier.padding(horizontal = 4.dp).height(200.dp)
                ) {
                    var imageWidth by remember { mutableStateOf(0.dp) }

                    if (isRemoved) {
                        // Show an overlay to indicate that the image is removed (and will be removed when saving)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(imageWidth)
                                .zIndex(1f)
                                .clickable { onModifyRemovedPreviousImages(removedPreviousImages - uuid) }
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                MaterialSymbols.Delete,
                                stringResource(Res.string.memory_editor_previous_image_delete),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else {
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .zIndex(1f)
                                .padding(4.dp)
                                .clickable { onModifyRemovedPreviousImages(removedPreviousImages + uuid) }
                        ) {
                            Icon(MaterialSymbols.Remove, stringResource(Res.string.remove))
                        }
                    }

                    AsyncImage(
                        model = bytes,
                        contentDescription = stringResource(Res.string.memory_editor_previous_image_uuid, uuid.toString()),
                        modifier = Modifier
                            .fillMaxHeight()
                            .onGloballyPositioned { imageWidth = with(density) { it.size.width.toDp() } }
                    )
                }
            }

            items(images, key = { it.name }, contentType = { "new-image" }) { file ->
                AssistChip(
                    enabled = !isSaving,
                    onClick = { onImagesChange(images - file) },
                    label = { Text(file.name) },
                    trailingIcon = {
                        Icon(MaterialSymbols.Remove, stringResource(Res.string.remove))
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item { Spacer(Modifier.width(8.dp)) }
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

        Spacer(Modifier.height(56.dp))
    }
}
