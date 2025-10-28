package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.form.AutocompleteFormField
import org.centrexcursionistalcoi.app.ui.utils.optional
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditInventoryItemTypeDialog(
    item: InventoryItemType,
    categories: Set<String>,
    onSubmit: (id: Uuid, displayName: String?, description: String?, category: String?, image: PlatformFile?) -> Job,
    onDismissRequest: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    var displayName by remember { mutableStateOf(item.displayName) }
    var description by remember { mutableStateOf(item.description.orEmpty()) }
    var category by remember { mutableStateOf(item.category.orEmpty()) }
    val imageBytes by item.rememberImageFile()
    var image by remember { mutableStateOf<PlatformFile?>(null) }
    val imagePicker = rememberFilePickerLauncher(
        type = FileKitType.File("png", "jpg", "jpeg", "webp")
    ) { file -> image = file }

    val dirty = displayName != item.displayName || description != (item.description ?: "") || category != (item.category ?: "") || image != null

    EditDialog(
        enabled = dirty && !isLoading,
        onSubmit = {
            isLoading = true
            onSubmit(
                item.id,
                displayName.takeIf { it != item.displayName && it.isNotEmpty() },
                description.takeIf { it != item.description && it.isNotEmpty() },
                category.takeIf { it != item.category && it.isNotEmpty() },
                image,
            ).invokeOnCompletion {
                isLoading = false
                onDismissRequest()
            }
        },
        onDismissRequest = onDismissRequest,
    ) {
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(stringResource(Res.string.form_display_name)) },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(Res.string.form_description).optional()) },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        AutocompleteFormField(
            value = category,
            onValueChange = { category = it },
            suggestions = categories,
            label = { Text(stringResource(Res.string.management_inventory_item_type_category).optional()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        )
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
            } ?: imageBytes?.let {
                AsyncByteImage(
                    bytes = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            } ?: Text(
                stringResource(Res.string.form_select_image).optional(),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
