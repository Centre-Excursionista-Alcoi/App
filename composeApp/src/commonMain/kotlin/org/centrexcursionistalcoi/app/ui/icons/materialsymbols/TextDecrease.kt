package org.centrexcursionistalcoi.app.ui.icons.materialsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MaterialSymbols.TextDecrease: ImageVector
    get() {
        if (_TextDecrease != null) {
            return _TextDecrease!!
        }
        _TextDecrease = ImageVector.Builder(
            name = "TextDecrease",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(640f, 520f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(600f, 480f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(640f, 440f)
                horizontalLineToRelative(240f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(920f, 480f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(880f, 520f)
                lineTo(640f, 520f)
                close()
                moveTo(187f, 617f)
                lineTo(146f, 731f)
                quadToRelative(-5f, 14f, -16f, 21.5f)
                reflectiveQuadToRelative(-25f, 7.5f)
                quadToRelative(-23f, 0f, -36.5f, -19.5f)
                reflectiveQuadTo(63f, 699f)
                lineToRelative(176f, -469f)
                quadToRelative(5f, -14f, 17f, -22f)
                reflectiveQuadToRelative(26f, -8f)
                horizontalLineToRelative(36f)
                quadToRelative(15f, 0f, 26.5f, 8f)
                reflectiveQuadToRelative(16.5f, 22f)
                lineToRelative(177f, 470f)
                quadToRelative(8f, 22f, -5.5f, 41f)
                reflectiveQuadTo(496f, 760f)
                quadToRelative(-14f, 0f, -26f, -8f)
                reflectiveQuadToRelative(-17f, -22f)
                lineToRelative(-40f, -113f)
                lineTo(187f, 617f)
                close()
                moveTo(216f, 536f)
                horizontalLineToRelative(168f)
                lineToRelative(-82f, -232f)
                horizontalLineToRelative(-4f)
                lineToRelative(-82f, 232f)
                close()
            }
        }.build()

        return _TextDecrease!!
    }

@Suppress("ObjectPropertyName")
private var _TextDecrease: ImageVector? = null
