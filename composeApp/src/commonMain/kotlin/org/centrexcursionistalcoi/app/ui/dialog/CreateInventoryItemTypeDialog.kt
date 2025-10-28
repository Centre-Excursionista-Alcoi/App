package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.ui.utils.optional
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInventoryItemTypeDialog(
    /** Existing categories, for autocomplete. */
    categories: Set<String>,
    onCreate: (displayName: String, description: String, category: String, image: PlatformFile?) -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var showingCategories by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("") }

    var image by remember { mutableStateOf<PlatformFile?>(null) }
    val imagePicker = rememberFilePickerLauncher(
        type = FileKitType.File("png", "jpg", "jpeg", "webp")
    ) { file -> image = file }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text(stringResource(Res.string.management_inventory_item_type_create)) },
        text = {
            Column {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(Res.string.management_inventory_item_type_display_name)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.management_inventory_item_type_description).optional()) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded =showingCategories,
                    onExpandedChange = { showingCategories = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(stringResource(Res.string.management_inventory_item_type_category).optional()) },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        readOnly = false,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showingCategories) }
                    )

                    ExposedDropdownMenu(
                        expanded = showingCategories,
                        onDismissRequest = { showingCategories = false }
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    showingCategories = false
                                }
                            )
                        }
                    }
                }
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !isLoading,
                    onClick = { imagePicker.launch() }
                ) {
                    image?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                    } ?: Text(
                        stringResource(Res.string.management_inventory_item_type_image).optional(),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && displayName.isNotBlank(),
                onClick = {
                    isLoading = true
                    onCreate(displayName, description, category, image).invokeOnCompletion { onDismissRequested() }
                }
            ) { Text(stringResource(Res.string.create)) }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
