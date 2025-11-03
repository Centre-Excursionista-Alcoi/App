package org.centrexcursionistalcoi.app.ui.utils

import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Marks a form field as optional by appending the localized "optional" label.
 */
@Composable
fun String.optional(): String {
    return this + ' ' + stringResource(Res.string.form_label_optional)
}

@Composable
fun unknown(): String = stringResource(Res.string.unknown)

@Composable
fun String?.orUnknown(): String = this ?: unknown()
