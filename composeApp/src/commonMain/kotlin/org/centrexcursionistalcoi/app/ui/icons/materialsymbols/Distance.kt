package org.centrexcursionistalcoi.app.ui.icons.materialsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MaterialSymbols.Distance: ImageVector
    get() {
        if (_Distance != null) {
            return _Distance!!
        }
        _Distance = ImageVector.Builder(
            name = "Distance",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(480f, 880f)
                quadToRelative(-106f, 0f, -173f, -33.5f)
                reflectiveQuadTo(240f, 760f)
                quadToRelative(0f, -19f, 8.5f, -35f)
                reflectiveQuadToRelative(25.5f, -30f)
                quadToRelative(14f, -10f, 30.5f, -8f)
                reflectiveQuadToRelative(26.5f, 16f)
                quadToRelative(10f, 14f, 7.5f, 30.5f)
                reflectiveQuadTo(322f, 760f)
                quadToRelative(13f, 16f, 60f, 28f)
                reflectiveQuadToRelative(98f, 12f)
                quadToRelative(51f, 0f, 98f, -12f)
                reflectiveQuadToRelative(60f, -28f)
                quadToRelative(-14f, -10f, -16.5f, -26.5f)
                reflectiveQuadTo(629f, 703f)
                quadToRelative(10f, -14f, 26.5f, -16f)
                reflectiveQuadToRelative(30.5f, 8f)
                quadToRelative(17f, 14f, 25.5f, 30f)
                reflectiveQuadToRelative(8.5f, 35f)
                quadToRelative(0f, 53f, -67f, 86.5f)
                reflectiveQuadTo(480f, 880f)
                close()
                moveTo(481f, 660f)
                quadToRelative(99f, -73f, 149f, -146.5f)
                reflectiveQuadTo(680f, 366f)
                quadToRelative(0f, -102f, -65f, -154f)
                reflectiveQuadToRelative(-135f, -52f)
                quadToRelative(-70f, 0f, -135f, 52f)
                reflectiveQuadToRelative(-65f, 154f)
                quadToRelative(0f, 67f, 49f, 139.5f)
                reflectiveQuadTo(481f, 660f)
                close()
                moveTo(480f, 741f)
                quadToRelative(-12f, 0f, -24f, -4f)
                reflectiveQuadToRelative(-22f, -12f)
                quadToRelative(-118f, -94f, -176f, -183.5f)
                reflectiveQuadTo(200f, 366f)
                quadToRelative(0f, -71f, 25.5f, -124.5f)
                reflectiveQuadTo(291f, 152f)
                quadToRelative(40f, -36f, 90f, -54f)
                reflectiveQuadToRelative(99f, -18f)
                quadToRelative(49f, 0f, 99f, 18f)
                reflectiveQuadToRelative(90f, 54f)
                quadToRelative(40f, 36f, 65.5f, 89.5f)
                reflectiveQuadTo(760f, 366f)
                quadToRelative(0f, 86f, -58f, 175.5f)
                reflectiveQuadTo(526f, 725f)
                quadToRelative(-10f, 8f, -22f, 12f)
                reflectiveQuadToRelative(-24f, 4f)
                close()
                moveTo(480f, 440f)
                quadToRelative(33f, 0f, 56.5f, -23.5f)
                reflectiveQuadTo(560f, 360f)
                quadToRelative(0f, -33f, -23.5f, -56.5f)
                reflectiveQuadTo(480f, 280f)
                quadToRelative(-33f, 0f, -56.5f, 23.5f)
                reflectiveQuadTo(400f, 360f)
                quadToRelative(0f, 33f, 23.5f, 56.5f)
                reflectiveQuadTo(480f, 440f)
                close()
                moveTo(480f, 360f)
                close()
            }
        }.build()

        return _Distance!!
    }

@Suppress("ObjectPropertyName")
private var _Distance: ImageVector? = null
