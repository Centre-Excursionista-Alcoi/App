package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.none
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.RadioButtonChecked
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.RadioButtonUnchecked
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun <T> DropdownField(
    value: T?,
    onValueChange: (T?) -> Unit,
    options: List<T>,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemToString: @Composable (T?) -> String = { it?.toString() ?: "" },
    itemDescriptionToString: (@Composable (T?) -> String)? = null,
    itemLeadingContent: (@Composable (T?) -> Unit)? = null,
    supportingText: String? = null,
    allowNull: Boolean = false,
    nullText: String? = stringResource(Res.string.none),
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            value = itemToString(value),
            onValueChange = {},
            label = { Text(label) },
            supportingText = (supportingText ?: itemDescriptionToString?.invoke(value))?.let {
                { Text(it) }
            },
            leadingIcon = itemLeadingContent?.let {
                { it(value) }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            if (allowNull) {
                DropdownMenuItem(
                    enabled = enabled,
                    text = {
                        Column {
                            Text(nullText ?: itemToString(null))
                            itemDescriptionToString?.invoke(null)?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    onClick = {
                        onValueChange(null)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (value == null) MaterialSymbols.RadioButtonChecked else MaterialSymbols.RadioButtonUnchecked,
                            contentDescription = if (value == null) "Selected" else "Not selected"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            options.forEach { option ->
                val isSelected = value == option
                DropdownMenuItem(
                    enabled = enabled,
                    text = {
                        Column {
                            Text(itemToString(option))
                            itemDescriptionToString?.invoke(option)?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    onClick = {
                        onValueChange(option)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSelected) MaterialSymbols.RadioButtonChecked else MaterialSymbols.RadioButtonUnchecked,
                            contentDescription = if (isSelected) "Selected" else "Not selected"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
