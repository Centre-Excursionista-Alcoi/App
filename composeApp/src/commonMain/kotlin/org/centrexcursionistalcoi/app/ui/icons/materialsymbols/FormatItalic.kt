package org.centrexcursionistalcoi.app.ui.icons.materialsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MaterialSymbols.FormatItalic: ImageVector
    get() {
        if (_FormatItalic != null) {
            return _FormatItalic!!
        }
        _FormatItalic = ImageVector.Builder(
            name = "FormatItalic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(250f, 760f)
                quadToRelative(-21f, 0f, -35.5f, -14.5f)
                reflectiveQuadTo(200f, 710f)
                quadToRelative(0f, -21f, 14.5f, -35.5f)
                reflectiveQuadTo(250f, 660f)
                horizontalLineToRelative(110f)
                lineToRelative(120f, -360f)
                lineTo(370f, 300f)
                quadToRelative(-21f, 0f, -35.5f, -14.5f)
                reflectiveQuadTo(320f, 250f)
                quadToRelative(0f, -21f, 14.5f, -35.5f)
                reflectiveQuadTo(370f, 200f)
                horizontalLineToRelative(300f)
                quadToRelative(21f, 0f, 35.5f, 14.5f)
                reflectiveQuadTo(720f, 250f)
                quadToRelative(0f, 21f, -14.5f, 35.5f)
                reflectiveQuadTo(670f, 300f)
                horizontalLineToRelative(-90f)
                lineTo(460f, 660f)
                horizontalLineToRelative(90f)
                quadToRelative(21f, 0f, 35.5f, 14.5f)
                reflectiveQuadTo(600f, 710f)
                quadToRelative(0f, 21f, -14.5f, 35.5f)
                reflectiveQuadTo(550f, 760f)
                lineTo(250f, 760f)
                close()
            }
        }.build()

        return _FormatItalic!!
    }

@Suppress("ObjectPropertyName")
private var _FormatItalic: ImageVector? = null
