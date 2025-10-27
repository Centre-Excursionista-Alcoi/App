package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.ui.reusable.editor.RichTextStyleRow
import org.centrexcursionistalcoi.app.viewmodel.ActivityMemoryEditorViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun ActivityMemoryEditor(
    lendingId: Uuid,
    model: ActivityMemoryEditorViewModel = viewModel { ActivityMemoryEditorViewModel(lendingId) },
    onBack: () -> Unit
) {
    val isSaving by model.isSaving.collectAsState()
    val uploadSuccessful by model.uploadSuccessful.collectAsState()

    LaunchedEffect(uploadSuccessful) {
        if (uploadSuccessful) {
            onBack()
        }
    }

    ActivityMemoryEditor(
        isSaving = isSaving,
        onSave = model::save,
        onBack = onBack,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ActivityMemoryEditor(
    isSaving: Boolean,
    onSave: (RichTextState) -> Unit,
    onBack: () -> Unit,
) {
    val state = rememberRichTextState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                    ) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                title = { Text(stringResource(Res.string.memory_editor_title)) },
                actions = {
                    if (!state.toText().isBlank()) {
                        IconButton(
                            enabled = !isSaving,
                            onClick = { onSave(state) }
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                stringResource(Res.string.memory_editor_save)
                            )
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(paddingValues)) {
            RichTextStyleRow(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                enabled = !isSaving,
            )

            OutlinedRichTextEditor(
                state = state,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                enabled = !isSaving,
            )

            Spacer(Modifier.height(56.dp))
        }
    }
}
