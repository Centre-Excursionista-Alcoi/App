package org.centrexcursionistalcoi.app.ui.reusable.buttons

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun CloseButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
    ) {
        Text(stringResource(Res.string.close))
    }
}
