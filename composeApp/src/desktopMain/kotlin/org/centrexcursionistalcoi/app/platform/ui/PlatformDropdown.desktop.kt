package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gabrieldrn.carbon.textinput.TextInput
import com.gabrieldrn.carbon.textinput.TextInputState

@Composable
@OptIn(ExperimentalMaterialApi::class)
actual fun <Type : Any> PlatformDropdown(
    value: Type?,
    onValueChange: (Type) -> Unit,
    options: List<Type>,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    toString: (Type?) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        TextInput(
            label = label,
            value = toString(value),
            onValueChange = { },
            state = if (enabled) TextInputState.ReadOnly else TextInputState.Disabled,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                ) {
                    BasicText(toString(option))
                }
            }
        }
    }
}
