package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import org.centrexcursionistalcoi.app.ui.reusable.editor.RichTextStyleRow
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ActivityMemoryEditor(onBack: () -> Unit) {
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
                title = { Text(stringResource(Res.string.memory_editor_title)) }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(paddingValues)) {
            val state = rememberRichTextState()

            RichTextStyleRow(
                modifier = Modifier.fillMaxWidth(),
                state = state,
            )

            RichTextEditor(
                state = state,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
        }
    }
}
