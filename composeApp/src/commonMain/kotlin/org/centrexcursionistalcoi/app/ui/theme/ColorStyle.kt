package org.centrexcursionistalcoi.app.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import org.centrexcursionistalcoi.app.ui.theme.font.PlusJakartaSans

data class ColorStyle(
    val contentColor: Color,
    val backgroundColor: Color,
    val backgroundBorderColor: Color = Color.Unspecified
) {
    @Composable
    fun Providing(
        color: Color = Color.Unspecified,
        fontSize: TextUnit? = null,
        fontWeight: FontWeight? = null,
        block: @Composable () -> Unit
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides LocalTextStyle.current.copy(
                color = if (color.isUnspecified) contentColor else color,
                fontFamily = PlusJakartaSans,
                fontSize = fontSize ?: LocalTextStyle.current.fontSize,
                fontWeight = fontWeight ?: LocalTextStyle.current.fontWeight,
            )
        ) {
            block()
        }
    }
}
