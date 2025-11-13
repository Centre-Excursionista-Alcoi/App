package org.centrexcursionistalcoi.app.ui.reusable.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.centrexcursionistalcoi.app.data.ImageFileContainer
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.jetbrains.compose.resources.stringResource

@Composable
fun FormImagePicker(
    image: PlatformFile?,
    container: ImageFileContainer?,
    onImagePicked: (PlatformFile) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val filePicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        file ?: return@rememberFilePickerLauncher
        onImagePicked(file)
    }

    if (container?.image != null && image == null) {
        val originalImage by container.rememberImageFile()
        AsyncByteImage(
            bytes = originalImage,
            contentDescription = null,
            modifier = modifier.clickable(enabled = !isLoading) { filePicker.launch() }
        )
    } else if (image != null) {
        AsyncImage(
            model = image,
            contentDescription = null,
            modifier = modifier.clickable(enabled = !isLoading) { filePicker.launch() }
        )
    } else {
        OutlinedCard(
            modifier = modifier,
            enabled = !isLoading,
            onClick = { filePicker.launch() },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.form_select_image), modifier = Modifier.padding(8.dp))
            }
        }
    }
}
