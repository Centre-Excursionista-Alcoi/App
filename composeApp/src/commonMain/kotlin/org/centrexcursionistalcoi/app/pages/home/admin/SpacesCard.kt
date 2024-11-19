package org.centrexcursionistalcoi.app.pages.home.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.PlatformTextArea
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.server.response.data.Address
import org.centrexcursionistalcoi.app.server.response.data.Location
import org.centrexcursionistalcoi.app.server.response.data.MoneyD
import org.centrexcursionistalcoi.app.server.response.data.SpaceD
import org.centrexcursionistalcoi.app.utils.humanReadableSize
import org.jetbrains.compose.resources.stringResource

@Composable
fun SpacesCard(
    spaces: List<SpaceD>?,
    isCreating: Boolean,
    onOperationRequested: (SpaceD, onCreate: () -> Unit) -> Unit,
) {
    var showingCreationDialog: SpaceD? by remember { mutableStateOf(null) }
    CreationDialog(
        showingCreationDialog = showingCreationDialog,
        title = Res.string.spaces_create,
        isCreating = isCreating,
        isEnabled = SpaceD::validate,
        onCreateRequested = onOperationRequested,
        onDismissRequested = { if (!isCreating) showingCreationDialog = null }
    ) { data ->
        var images by remember { mutableStateOf(mapOf<String, ByteArray>()) }

        val filePicker = rememberFilePickerLauncher(mode = PickerMode.Single, type = PickerType.Image) { file ->
            file ?: return@rememberFilePickerLauncher
            CoroutineScope(Dispatchers.Main).launch {
                val bytes = file.readBytes()
                images = images + (file.name to bytes)
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

        BasicText(
            text = stringResource(Res.string.spaces_images),
            style = getPlatformTextStyles().label,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
        )
        for ((name, image) in images) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "$name (${image.humanReadableSize()})",
                    style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                PlatformButton(
                    text = stringResource(Res.string.remove),
                    onClick = { images = images - name },
                )
            }
        }
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
            value = data.memberPrice?.amount?.toString() ?: "",
            onValueChange = { amount ->
                showingCreationDialog = data.copy(
                    memberPrice = amount.toDoubleOrNull()?.let { MoneyD(amount = it) } ?: data.memberPrice
                )
            },
            label = stringResource(Res.string.spaces_member_price),
            supportingText = stringResource(Res.string.spaces_member_price_info),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp),
            enabled = !isCreating,
            keyboardType = KeyboardType.Decimal
        )
        PlatformFormField(
            value = data.externalPrice?.amount?.toString() ?: "",
            onValueChange = { amount ->
                showingCreationDialog = data.copy(
                    externalPrice = amount.toDoubleOrNull()?.let { MoneyD(amount = it) } ?: data.externalPrice
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
                    location = (data.location ?: Location()).let { loc ->
                        loc.copy(latitude = latitude.toDoubleOrNull() ?: loc.latitude).orNull()
                    }
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
                    location = (data.location ?: Location()).let { loc ->
                        loc.copy(longitude = longitude.toDoubleOrNull() ?: loc.longitude).orNull()
                    }
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
            value = data.address?.address ?: "",
            onValueChange = { value ->
                showingCreationDialog = data.copy(
                    address = (data.address ?: Address()).copy(address = value).orNull()
                )
            },
            label = stringResource(Res.string.spaces_address),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp),
            enabled = !isCreating
        )
        PlatformFormField(
            value = data.address?.city ?: "",
            onValueChange = { value ->
                showingCreationDialog = data.copy(
                    address = (data.address ?: Address()).copy(city = value).orNull()
                )
            },
            label = stringResource(Res.string.spaces_city),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            enabled = !isCreating
        )
        PlatformFormField(
            value = data.address?.postalCode ?: "",
            onValueChange = { value ->
                showingCreationDialog = data.copy(
                    address = (data.address ?: Address()).copy(postalCode = value).orNull()
                )
            },
            label = stringResource(Res.string.spaces_postal_code),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            enabled = !isCreating
        )
        PlatformFormField(
            value = data.address?.country ?: "",
            onValueChange = { value ->
                showingCreationDialog = data.copy(
                    address = (data.address ?: Address()).copy(country = value).orNull()
                )
            },
            label = stringResource(Res.string.spaces_country),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp),
            enabled = !isCreating
        )
    }

    PlatformCard(
        title = stringResource(Res.string.spaces_title),
        action = Triple(Icons.Default.Add, stringResource(Res.string.add), { showingCreationDialog = SpaceD() }),
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
                        BasicText(
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
                            BasicText(
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
