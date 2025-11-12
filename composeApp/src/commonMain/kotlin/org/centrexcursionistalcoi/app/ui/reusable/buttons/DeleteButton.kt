package org.centrexcursionistalcoi.app.ui.reusable.buttons

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteButton(
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        onClick = onClick,
    ) {
        Text(stringResource(Res.string.delete))
    }
}
