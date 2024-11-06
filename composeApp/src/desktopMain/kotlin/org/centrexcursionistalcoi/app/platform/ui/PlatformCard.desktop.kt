package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gabrieldrn.carbon.button.Button
import com.gabrieldrn.carbon.button.ButtonType
import com.gabrieldrn.carbon.foundation.color.CarbonLayer
import com.gabrieldrn.carbon.foundation.color.containerBackground

@Composable
actual fun PlatformCard(
    title: String,
    modifier: Modifier,
    action: Triple<ImageVector, String, () -> Unit>?,
    content: @Composable ColumnScope.() -> Unit
) {
    CarbonLayer {
        Column(
            modifier = modifier.containerBackground()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = title,
                    style = getPlatformTextStyles().heading,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)
                )

                action?.let { (_, text, onClick) ->
                    Button(
                        label = text,
                        onClick = onClick,
                        buttonType = ButtonType.Tertiary
                    )
                }
            }

            Column(
                modifier = modifier.fillMaxWidth().padding(8.dp)
            ) {
                content()
            }
        }
    }
}
