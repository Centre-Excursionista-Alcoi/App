package org.centrexcursionistalcoi.app.ui.icons.materialsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MaterialSymbols.FormatStrikethrough: ImageVector
    get() {
        if (_FormatStrikethrough != null) {
            return _FormatStrikethrough!!
        }
        _FormatStrikethrough = ImageVector.Builder(
            name = "FormatStrikethrough",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(120f, 560f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(80f, 520f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(120f, 480f)
                horizontalLineToRelative(720f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(880f, 520f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(840f, 560f)
                lineTo(120f, 560f)
                close()
                moveTo(420f, 400f)
                verticalLineToRelative(-120f)
                lineTo(260f, 280f)
                quadToRelative(-25f, 0f, -42.5f, -17.5f)
                reflectiveQuadTo(200f, 220f)
                quadToRelative(0f, -25f, 17.5f, -42.5f)
                reflectiveQuadTo(260f, 160f)
                horizontalLineToRelative(440f)
                quadToRelative(25f, 0f, 42.5f, 17.5f)
                reflectiveQuadTo(760f, 220f)
                quadToRelative(0f, 25f, -17.5f, 42.5f)
                reflectiveQuadTo(700f, 280f)
                lineTo(540f, 280f)
                verticalLineToRelative(120f)
                lineTo(420f, 400f)
                close()
                moveTo(420f, 640f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(100f)
                quadToRelative(0f, 25f, -17.5f, 42.5f)
                reflectiveQuadTo(480f, 800f)
                quadToRelative(-25f, 0f, -42.5f, -17.5f)
                reflectiveQuadTo(420f, 740f)
                verticalLineToRelative(-100f)
                close()
            }
        }.build()

        return _FormatStrikethrough!!
    }

@Suppress("ObjectPropertyName")
private var _FormatStrikethrough: ImageVector? = null
