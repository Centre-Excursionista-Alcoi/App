package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gabrieldrn.carbon.button.ButtonType
import org.centrexcursionistalcoi.app.component.CarbonButton

actual abstract class PlatformDialogContext : RowScope {
    actual abstract fun dismiss()

    @Composable
    actual fun RowScope.PositiveButton(text: String, onClick: () -> Unit) {
        CarbonButton(
            text = text,
            buttonType = ButtonType.Primary,
            onClick = onClick,
            modifier = Modifier.weight(1f)
        )
    }

    @Composable
    actual fun RowScope.NeutralButton(text: String, onClick: () -> Unit) {
        CarbonButton(
            text = text,
            buttonType = ButtonType.Secondary,
            onClick = onClick,
            modifier = Modifier.weight(1f)
        )
    }

    @Composable
    actual fun RowScope.DestructiveButton(text: String, onClick: () -> Unit) {
        CarbonButton(
            text = text,
            buttonType = ButtonType.PrimaryDanger,
            onClick = onClick,
            modifier = Modifier.weight(1f)
        )
    }
}
