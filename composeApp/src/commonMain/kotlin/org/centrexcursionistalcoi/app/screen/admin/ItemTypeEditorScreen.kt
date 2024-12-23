package org.centrexcursionistalcoi.app.screen.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.platform.ui.Action
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformDropdown
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.PlatformScaffold
import org.centrexcursionistalcoi.app.platform.ui.PlatformTextArea
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.route.ItemTypeEditorRoute
import org.centrexcursionistalcoi.app.screen.Screen
import org.centrexcursionistalcoi.app.utils.humanReadableSize
import org.centrexcursionistalcoi.app.viewmodel.admin.ItemTypeEditorViewModel
import org.jetbrains.compose.resources.stringResource

object ItemTypeEditorScreen : Screen<ItemTypeEditorRoute, ItemTypeEditorViewModel>(::ItemTypeEditorViewModel) {
    @Composable
    override fun Content(viewModel: ItemTypeEditorViewModel) {
        val navController = LocalNavController.current

        val itemTypeId = route.itemTypeId

        LaunchedEffect(Unit) {
            viewModel.load(itemTypeId)
        }

        val isLoading by viewModel.isLoading.collectAsState()
        val itemType by viewModel.itemType.collectAsState()
        val sections by viewModel.sections.collectAsState()

        PlatformScaffold(
            onBack = navController::navigateUp,
            title = stringResource(if (itemTypeId != null) Res.string.types_edit else Res.string.types_create),
            actions = listOf(
                Action(
                    Icons.Default.Check,
                    stringResource(if (itemTypeId != null) Res.string.update else Res.string.create),
                    enabled = !isLoading
                ) { viewModel.createOrUpdate { navController.navigateUp() } }
            )
        ) {
            AnimatedVisibility(visible = itemType == null, modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PlatformLoadingIndicator()
                }
            }
            AnimatedVisibility(visible = itemType != null, modifier = Modifier.fillMaxSize()) {
                itemType?.let { Content(isLoading, it, viewModel::setItemType, sections) }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalComposeUiApi::class)
    private fun Content(
        isLoading: Boolean,
        data: ItemType,
        onDataChange: (ItemType) -> Unit,
        sections: List<Section>?
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlatformFormField(
                value = data.title,
                onValueChange = { onDataChange(data.copy(title = it)) },
                label = stringResource(Res.string.types_name),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                enabled = !isLoading
            )
            PlatformTextArea(
                value = data.description ?: "",
                onValueChange = { onDataChange(data.copy(description = it.takeIf(String::isNotBlank))) },
                label = stringResource(Res.string.types_description),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                enabled = !isLoading
            )
            PlatformFormField(
                value = data.brand ?: "",
                onValueChange = { onDataChange(data.copy(brand = it.takeIf(String::isNotBlank))) },
                label = stringResource(Res.string.types_brand),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                enabled = !isLoading
            )
            PlatformFormField(
                value = data.model ?: "",
                onValueChange = { onDataChange(data.copy(model = it.takeIf(String::isNotBlank))) },
                label = stringResource(Res.string.types_model),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                enabled = !isLoading
            )

            val filePicker = rememberFilePickerLauncher(mode = PickerMode.Single, type = PickerType.Image) { file ->
                CoroutineScope(Dispatchers.Main).launch {
                    val bytes = file?.readBytes()
                    onDataChange(data.copy(image = bytes))
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
                enabled = !isLoading
            ) { filePicker.launch() }

            PlatformDropdown(
                value = data.sectionId.let { sectionId -> sections?.find { it.id == sectionId } },
                onValueChange = { onDataChange(data.copy(sectionId = it.id)) },
                options = sections ?: emptyList(),
                label = stringResource(Res.string.types_section),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                enabled = !isLoading,
                toString = { it?.displayName ?: "" }
            )
        }
    }
}
