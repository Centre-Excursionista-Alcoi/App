package org.centrexcursionistalcoi.app.ui.icons.materialsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MaterialSymbols.FormatUnderlined: ImageVector
    get() {
        if (_FormatUnderlined != null) {
            return _FormatUnderlined!!
        }
        _FormatUnderlined = ImageVector.Builder(
            name = "FormatUnderlined",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(240f, 840f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(200f, 800f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(240f, 760f)
                horizontalLineToRelative(480f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(760f, 800f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(720f, 840f)
                lineTo(240f, 840f)
                close()
                moveTo(480f, 680f)
                quadToRelative(-101f, 0f, -157f, -63f)
                reflectiveQuadToRelative(-56f, -167f)
                verticalLineToRelative(-279f)
                quadToRelative(0f, -21f, 15.5f, -36f)
                reflectiveQuadToRelative(36.5f, -15f)
                quadToRelative(21f, 0f, 36f, 15f)
                reflectiveQuadToRelative(15f, 36f)
                verticalLineToRelative(285f)
                quadToRelative(0f, 56f, 28f, 91f)
                reflectiveQuadToRelative(82f, 35f)
                quadToRelative(54f, 0f, 82f, -35f)
                reflectiveQuadToRelative(28f, -91f)
                verticalLineToRelative(-285f)
                quadToRelative(0f, -21f, 15.5f, -36f)
                reflectiveQuadToRelative(36.5f, -15f)
                quadToRelative(21f, 0f, 36f, 15f)
                reflectiveQuadToRelative(15f, 36f)
                verticalLineToRelative(279f)
                quadToRelative(0f, 104f, -56f, 167f)
                reflectiveQuadToRelative(-157f, 63f)
                close()
            }
        }.build()

        return _FormatUnderlined!!
    }

@Suppress("ObjectPropertyName")
private var _FormatUnderlined: ImageVector? = null
