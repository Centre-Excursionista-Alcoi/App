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
    actual fun RowScope.PositiveButton(text: String, onClick: () -> Unit) {
        TextButton(
            colors = ButtonDefaults.textButtonColors(),
            onClick = onClick
        ) {
            Text(text)
        }
    }

    @Composable
    actual fun RowScope.NeutralButton(text: String, onClick: () -> Unit) {
        TextButton(
            colors = ButtonDefaults.textButtonColors(),
            onClick = onClick
        ) {
            Text(text)
        }
    }

    @Composable
    actual fun RowScope.DestructiveButton(text: String, onClick: () -> Unit) {
        TextButton(
            colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            onClick = onClick
        ) {
            Text(text)
        }
    }
}
