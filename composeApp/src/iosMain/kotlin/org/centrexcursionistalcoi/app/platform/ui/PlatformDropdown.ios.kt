package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.cupertino.CupertinoDropdownMenu
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTextField
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.MenuAction

@OptIn(ExperimentalCupertinoApi::class)
@Composable
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

    Box {
        CupertinoTextField(
            value = value?.let(toString) ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = modifier,
            interactionSource = remember { MutableInteractionSource() }
                .also { source ->
                    LaunchedEffect(source) {
                        source.interactions.collect {
                            if (it is PressInteraction.Release) {
                                expanded = true
                            }
                        }
                    }
                }
        )

        CupertinoDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for (option in options) {
                MenuAction(
                    onClick = { onValueChange(option); expanded = false },
                    title = { CupertinoText(option.let(toString)) }
                )
            }
        }
    }
}
