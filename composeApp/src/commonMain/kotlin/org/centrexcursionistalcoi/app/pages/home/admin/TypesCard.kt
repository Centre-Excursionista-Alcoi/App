package org.centrexcursionistalcoi.app.pages.home.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.composition.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformDropdown
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.PlatformTextArea
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.utils.humanReadableSize
import org.jetbrains.compose.resources.stringResource

@Composable
fun TypesCard(
    itemTypes: List<ItemType>?,
    sections: List<Section>?,
    isCreating: Boolean,
    onCreateRequested: (ItemType, onCreate: () -> Unit) -> Unit
) {
    var showingCreationDialog: ItemType? by remember { mutableStateOf(null) }
    CreationDialog(
        showingCreationDialog = showingCreationDialog,
        title = Res.string.types_create,
        isCreating = isCreating,
        isEnabled = ItemType::validate,
        onCreateRequested = onCreateRequested,
        onDismissRequested = { if (!isCreating) showingCreationDialog = null }
    ) { data ->
        PlatformFormField(
            value = data.title,
            onValueChange = { showingCreationDialog = data.copy(title = it) },
            label = stringResource(Res.string.types_name),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        )
        PlatformTextArea(
            value = data.description ?: "",
            onValueChange = { showingCreationDialog = data.copy(description = it.takeIf(String::isNotBlank)) },
            label = stringResource(Res.string.types_description),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        )
        PlatformFormField(
            value = data.brand ?: "",
            onValueChange = { showingCreationDialog = data.copy(brand = it.takeIf(String::isNotBlank)) },
            label = stringResource(Res.string.types_brand),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        )
        PlatformFormField(
            value = data.model ?: "",
            onValueChange = { showingCreationDialog = data.copy(model = it.takeIf(String::isNotBlank)) },
            label = stringResource(Res.string.types_model),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        )

        val filePicker = rememberFilePickerLauncher(mode = PickerMode.Single, type = PickerType.Image) { file ->
            CoroutineScope(Dispatchers.Main).launch {
                val bytes = file?.readBytes()
                showingCreationDialog = data.copy(image = bytes)
            }
        }
        AppText(
            text = stringResource(Res.string.types_image),
            style = getPlatformTextStyles().label,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
        )
        PlatformButton(
            text = if (data.image == null)
                stringResource(Res.string.select)
            else
                stringResource(Res.string.selected_size, data.image.humanReadableSize()),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        ) { filePicker.launch() }

        PlatformDropdown(
            value = data.sectionId.let { sectionId -> sections?.find { it.id == sectionId } },
            onValueChange = { showingCreationDialog = data.copy(sectionId = it.id) },
            options = sections ?: emptyList(),
            label = stringResource(Res.string.types_section),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating,
            toString = { it?.displayName ?: "" }
        )
    }

    PlatformCard(
        title = stringResource(Res.string.types_title),
        action = Triple(Icons.Default.Add, stringResource(Res.string.add)) { showingCreationDialog = ItemType() },
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        AnimatedContent(
            targetState = itemTypes,
            modifier = Modifier.fillMaxWidth()
        ) { list ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (list == null) {
                    PlatformLoadingIndicator(large = false)
                } else if (list.isEmpty()) {
                    AppText(
                        text = stringResource(Res.string.types_empty),
                        style = getPlatformTextStyles().label.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                } else {
                    val windowSizeClass = calculateWindowSizeClass()
                    val groupCount = when (windowSizeClass.widthSizeClass) {
                        WindowWidthSizeClass.Compact -> 1
                        WindowWidthSizeClass.Medium -> 2
                        WindowWidthSizeClass.Expanded -> 3
                        else -> 1
                    }
                    // group the elements in list
                    val groups = list.chunked(groupCount)
                    for (group in groups) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (item in group) {
                                PlatformCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp)
                                        .clickable { showingCreationDialog = item }
                                ) {
                                    AppText(
                                        text = item.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp)
                                            .padding(top = 8.dp),
                                        style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                                    )
                                    AppText(
                                        text = (item.brand ?: "") + " " + (item.model ?: ""),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                        style = getPlatformTextStyles().label
                                    )
                                    AppText(
                                        text = item.description ?: "",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp)
                                            .padding(bottom = 8.dp),
                                        style = getPlatformTextStyles().body
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
