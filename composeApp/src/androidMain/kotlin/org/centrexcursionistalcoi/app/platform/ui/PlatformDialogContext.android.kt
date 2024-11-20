package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

actual abstract class PlatformDialogContext : RowScope {
    actual abstract fun dismiss()

    @Composable
    actual fun RowScope.PositiveButton(text: String, enabled: Boolean, onClick: () -> Unit) {
        TextButton(
            colors = ButtonDefaults.textButtonColors(),
            enabled = enabled,
            onClick = onClick
        ) {
            Text(text)
        }
    }

    @Composable
    actual fun RowScope.NeutralButton(text: String, enabled: Boolean, onClick: () -> Unit) {
        TextButton(
            colors = ButtonDefaults.textButtonColors(),
            enabled = enabled,
            onClick = onClick
        ) {
            Text(text)
        }
    }

    @Composable
    actual fun RowScope.DestructiveButton(text: String, enabled: Boolean, onClick: () -> Unit) {
        TextButton(
            colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            enabled = enabled,
            onClick = onClick
        ) {
            Text(text)
        }
    }
}
