package org.centrexcursionistalcoi.app.ui.reusable.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.AttachFile
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Close
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.OutlinedButtonWithIcon
import org.jetbrains.compose.resources.stringResource

/**
 * Picks any number of files: each pick adds to [files], and each picked file is shown as a chip that removes it.
 */
@Composable
fun FormFilesPicker(
    files: List<PlatformFile>,
    onFilesChange: (List<PlatformFile>) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    pickerType: FileKitType = FileKitType.File(),
) {
    val picker = rememberFilePickerLauncher(pickerType, FileKitMode.Multiple()) { picked ->
        if (picked.isNullOrEmpty()) return@rememberFilePickerLauncher
        onFilesChange(files + picked)
    }

    Column(
        modifier = modifier,
    ) {
        label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButtonWithIcon(
            icon = MaterialSymbols.AttachFile,
            text = stringResource(Res.string.file_picker_pick),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            onClick = { picker.launch() },
        )
        files.forEachIndexed { index, file ->
            InputChip(
                selected = false,
                enabled = enabled,
                onClick = { onFilesChange(files.filterIndexed { i, _ -> i != index }) },
                label = { Text(file.name) },
                trailingIcon = { Icon(MaterialSymbols.Close, stringResource(Res.string.remove)) },
            )
        }
    }
}
