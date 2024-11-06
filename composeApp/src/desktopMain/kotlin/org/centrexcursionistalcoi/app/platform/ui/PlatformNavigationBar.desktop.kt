package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.gabrieldrn.carbon.button.Button
import com.gabrieldrn.carbon.button.ButtonType

@Composable
actual fun PlatformNavigationBar(
    selection: Int,
    onSelectionChanged: (Int) -> Unit,
    items: List<Pair<ImageVector, String>>
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        for ((index, item) in items.withIndex()) {
            val (_, text) = item
            Button(
                label = text,
                modifier = Modifier.weight(1f),
                buttonType = if (selection == index) {
                    ButtonType.Primary
                } else {
                    ButtonType.Secondary
                },
                onClick = { onSelectionChanged(index) }
            )
        }
    }
}
