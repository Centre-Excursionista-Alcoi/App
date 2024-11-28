package org.centrexcursionistalcoi.app.pages.home.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import org.centrexcursionistalcoi.app.component.ImagesCarousel
import org.centrexcursionistalcoi.app.data.SpaceKeyD
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.PlatformTextArea
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.utils.humanReadableSize
import org.jetbrains.compose.resources.stringResource

@Composable
fun SpacesCard(
    spaces: List<Space>?,
    isCreating: Boolean,
    onOperationRequested: (Space, onCreate: () -> Unit) -> Unit,
) {
    var showingCreationDialog: Space? by remember { mutableStateOf(null) }
    CreationDialog(
        showingCreationDialog = showingCreationDialog,
        title = Res.string.spaces_create,
        isCreating = isCreating,
        isEnabled = Space::validate,
        onCreateRequested = onOperationRequested,
        onDismissRequested = { if (!isCreating) showingCreationDialog = null }
    ) { data ->
        val filePicker = rememberFilePickerLauncher(mode = PickerMode.Multiple(), type = PickerType.Image) { files ->
            files?.takeIf { it.isNotEmpty() } ?: return@rememberFilePickerLauncher
            CoroutineScope(Dispatchers.Main).launch {
                val newImages = data.images.orEmpty().toMutableList()
                for (file in files) {
                    val bytes = file.readBytes()
                    newImages += bytes
                }
                showingCreationDialog = data.copy(images = newImages)
            }
        }

        PlatformFormField(
            value = data.name,
            onValueChange = { showingCreationDialog = data.copy(name = it) },
            label = stringResource(Res.string.spaces_name),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp),
            enabled = !isCreating
        )
        PlatformTextArea(
            value = data.description ?: "",
            onValueChange = { showingCreationDialog = data.copy(description = it) },
            label = stringResource(Res.string.spaces_description),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp),
            enabled = !isCreating
        )

        AppText(
            text = stringResource(Res.string.spaces_images),
            style = getPlatformTextStyles().label,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
        )
        ImagesCarousel(
            images = data.images.orEmpty(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            supportingContent = { image ->
                AppText(
                    text = image.humanReadableSize(),
                    style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                )
            },
            onRemove = { index ->
                showingCreationDialog = data.copy(images = data.images?.toMutableList()?.apply { removeAt(index) })
            }
        )
        PlatformButton(
            text = stringResource(Res.string.select),
            onClick = { filePicker.launch() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)
        )

        PlatformFormField(
            value = data.capacity?.toString() ?: "",
            onValueChange = { showingCreationDialog = data.copy(capacity = it.toIntOrNull() ?: data.capacity) },
            label = stringResource(Res.string.spaces_capacity),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating,
            keyboardType = KeyboardType.Number
        )

        PlatformFormField(
            value = data.memberPrice?.toString() ?: "",
            onValueChange = { amount ->
                showingCreationDialog = data.copy(
                    memberPrice = amount.toDoubleOrNull() ?: data.memberPrice
                )
            },
            label = stringResource(Res.string.spaces_member_price),
            supportingText = stringResource(Res.string.spaces_member_price_info),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp),
            enabled = !isCreating,
            keyboardType = KeyboardType.Decimal
        )
        PlatformFormField(
            value = data.externalPrice?.toString() ?: "",
            onValueChange = { amount ->
                showingCreationDialog = data.copy(
                    externalPrice = amount.toDoubleOrNull() ?: data.externalPrice
                )
            },
            label = stringResource(Res.string.spaces_non_member_price),
            supportingText = stringResource(Res.string.spaces_non_member_price_info),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp),
            enabled = !isCreating,
            keyboardType = KeyboardType.Decimal
        )

        PlatformFormField(
            value = data.location?.latitude?.toString() ?: "",
            onValueChange = { latitude ->
                showingCreationDialog = data.copy(
                    latitude = latitude.toDoubleOrNull() ?: data.latitude
                )
            },
            label = stringResource(Res.string.spaces_latitude),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating,
            keyboardType = KeyboardType.Decimal,
            error = stringResource(Res.string.error_latitude_not_set)
                .takeIf { data.location?.let { l -> l.latitude == null && l.longitude != null } == true }
        )
        PlatformFormField(
            value = data.location?.longitude?.toString() ?: "",
            onValueChange = { longitude ->
                showingCreationDialog = data.copy(
                    longitude = longitude.toDoubleOrNull() ?: data.longitude
                )
            },
            label = stringResource(Res.string.spaces_longitude),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating,
            keyboardType = KeyboardType.Decimal,
            error = stringResource(Res.string.error_longitude_not_set)
                .takeIf { data.location?.let { l -> l.latitude != null && l.longitude == null } == true }
        )

        PlatformFormField(
            value = data.address ?: "",
            onValueChange = { value ->
                showingCreationDialog = data.copy(
                    address = value.takeIf { it.isNotBlank() }
                )
            },
            label = stringResource(Res.string.spaces_address),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp),
            enabled = !isCreating
        )
        PlatformFormField(
            value = data.city ?: "",
            onValueChange = { value ->
                showingCreationDialog = data.copy(
                    city = value.takeIf { it.isNotBlank() }
                )
            },
            label = stringResource(Res.string.spaces_city),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            enabled = !isCreating
        )
        PlatformFormField(
            value = data.postalCode ?: "",
            onValueChange = { value ->
                showingCreationDialog = data.copy(
                    postalCode = value.takeIf { it.isNotBlank() }
                )
            },
            label = stringResource(Res.string.spaces_postal_code),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            enabled = !isCreating
        )
        PlatformFormField(
            value = data.country ?: "",
            onValueChange = { value ->
                showingCreationDialog = data.copy(
                    country = value.takeIf { it.isNotBlank() }
                )
            },
            label = stringResource(Res.string.spaces_country),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp),
            enabled = !isCreating
        )

        HorizontalDivider()

        AppText(
            text = stringResource(Res.string.spaces_keys),
            style = getPlatformTextStyles().label,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
        )
        for (key in data.keys.orEmpty()) {
            PlatformCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.delete),
                        modifier = Modifier.padding(8.dp).clickable {
                            showingCreationDialog = data.copy(
                                keys = data.keys?.toMutableList()?.apply { remove(key) }
                            )
                        }
                    )
                }
                PlatformFormField(
                    value = key.name,
                    onValueChange = { value ->
                        showingCreationDialog = data.copy(
                            keys = data.keys?.toMutableList()?.apply { set(indexOf(key), key.copy(name = value)) }
                        )
                    },
                    label = stringResource(Res.string.spaces_key_name),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
                PlatformTextArea(
                    value = key.description ?: "",
                    onValueChange = { value ->
                        showingCreationDialog = data.copy(
                            keys = data.keys?.toMutableList()?.apply { set(indexOf(key), key.copy(description = value.takeIf { it.isNotBlank() })) }
                        )
                    },
                    label = stringResource(Res.string.spaces_key_description),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }
        }
        PlatformButton(
            text = stringResource(Res.string.create),
            onClick = {
                showingCreationDialog = data.copy(
                    keys = data.keys.orEmpty().toMutableList().apply { add(SpaceKeyD()) }
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)
        )
    }

    PlatformCard(
        title = stringResource(Res.string.spaces_title),
        action = Triple(Icons.Default.Add, stringResource(Res.string.add), { showingCreationDialog = Space() }),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        AnimatedContent(
            targetState = spaces,
            modifier = Modifier.fillMaxWidth()
        ) { list ->
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (list == null) {
                    PlatformLoadingIndicator(large = false)
                } else {
                    if (list.isEmpty()) {
                        AppText(
                            text = stringResource(Res.string.spaces_empty),
                            style = getPlatformTextStyles().label.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    }
                    for (item in list) {
                        PlatformCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { showingCreationDialog = item }
                        ) {
                            AppText(
                                text = item.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
