package org.centrexcursionistalcoi.app.ui.reusable.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.file_picker_pick
import cea_app.composeapp.generated.resources.remove
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.AttachFile
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Close
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.OutlinedButtonWithIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun FormFilePicker(
    file: PlatformFile?,
    onFilePicked: (PlatformFile?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    pickerType: FileKitType = FileKitType.File(),
    pickerTitle: String? = null,
    canClear: Boolean = true,
) {
    val picker = rememberFilePickerLauncher(pickerType, pickerTitle) { file ->
        file ?: return@rememberFilePickerLauncher
        onFilePicked(file)
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
        file?.let {
            AssistChip(
                onClick = { if (!canClear) onFilePicked(null) },
                label = { Text(it.name) },
                trailingIcon = { Icon(MaterialSymbols.Close, stringResource(Res.string.remove)) },
            )
        }
    }
}
