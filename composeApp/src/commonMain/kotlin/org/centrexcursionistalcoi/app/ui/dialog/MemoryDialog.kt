package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.memory_external
import cea_app.composeapp.generated.resources.memory_from
import cea_app.composeapp.generated.resources.memory_images
import cea_app.composeapp.generated.resources.memory_members
import cea_app.composeapp.generated.resources.memory_place
import cea_app.composeapp.generated.resources.memory_text
import cea_app.composeapp.generated.resources.memory_title
import cea_app.composeapp.generated.resources.memory_to
import coil3.compose.AsyncImage
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.data.rememberImageFiles
import org.centrexcursionistalcoi.app.ui.reusable.buttons.CloseButton
import org.centrexcursionistalcoi.app.ui.screen.MemoryViewButtons
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MemoryDialog(memory: ReferencedMemory, onDismissRequest: () -> Unit) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismissRequest
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.memory_title)) },
                    navigationIcon = {
                        CloseButton(onDismissRequest)
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                memory.department?.let { department ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val image by department.rememberImageFile()
                            AsyncImage(
                                model = image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).padding(end = 12.dp)
                            )
                            Text(
                                text = department.displayName,
                                style = MaterialTheme.typography.titleMediumEmphasized,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        LabelWithTitle(title = stringResource(Res.string.memory_from), text = memory.from.toStringCompact())
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        LabelWithTitle(title = stringResource(Res.string.memory_to), text = memory.to.toStringCompact())
                    }
                }

                memory.place?.let { place ->
                    LabelWithTitle(title = stringResource(Res.string.memory_place), text = place)
                }

                LabelWithTitle(title = stringResource(Res.string.memory_text), text = memory.text)

                if (memory.attachments.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.memory_images).uppercase(),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    )
                    val images = memory.rememberImageFiles()
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    ) {
                        items(images.toList(), key = { it }) { (_, attachment) ->
                            AsyncImage(
                                model = attachment,
                                contentDescription = null,
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (memory.members.isNotEmpty()) {
                    LabelWithTitle(
                        title = stringResource(Res.string.memory_members),
                        text = memory.members.joinToString(", ") { it.fullName },
                    )
                }
                memory.externalUsers?.let { externals ->
                    LabelWithTitle(
                        title = stringResource(Res.string.memory_external),
                        text = externals
                    )
                }

                MemoryViewButtons(memory)
            }
        }
    }
}

@Composable
fun LabelWithTitle(title: String, text: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLargeEmphasized,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 4.dp)
    )
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    )
}
