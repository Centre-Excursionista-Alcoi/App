package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun <Type: Any> PlatformDropdown(
    value: Type?,
    onValueChange: (Type) -> Unit,
    options: List<Type>,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    toString: (Type?) -> String = { it?.toString() ?: "" },
)
