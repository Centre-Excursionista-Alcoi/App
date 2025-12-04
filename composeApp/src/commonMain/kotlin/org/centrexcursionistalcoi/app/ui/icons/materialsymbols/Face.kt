package org.centrexcursionistalcoi.app.ui.icons.materialsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MaterialSymbols.Face: ImageVector
    get() {
        if (_Face != null) {
            return _Face!!
        }
        _Face = ImageVector.Builder(
            name = "Face",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(360f, 570f)
                quadToRelative(-21f, 0f, -35.5f, -14.5f)
                reflectiveQuadTo(310f, 520f)
                quadToRelative(0f, -21f, 14.5f, -35.5f)
                reflectiveQuadTo(360f, 470f)
                quadToRelative(21f, 0f, 35.5f, 14.5f)
                reflectiveQuadTo(410f, 520f)
                quadToRelative(0f, 21f, -14.5f, 35.5f)
                reflectiveQuadTo(360f, 570f)
                close()
                moveTo(600f, 570f)
                quadToRelative(-21f, 0f, -35.5f, -14.5f)
                reflectiveQuadTo(550f, 520f)
                quadToRelative(0f, -21f, 14.5f, -35.5f)
                reflectiveQuadTo(600f, 470f)
                quadToRelative(21f, 0f, 35.5f, 14.5f)
                reflectiveQuadTo(650f, 520f)
                quadToRelative(0f, 21f, -14.5f, 35.5f)
                reflectiveQuadTo(600f, 570f)
                close()
                moveTo(480f, 800f)
                quadToRelative(134f, 0f, 227f, -93f)
                reflectiveQuadToRelative(93f, -227f)
                quadToRelative(0f, -24f, -3f, -46.5f)
                reflectiveQuadTo(786f, 390f)
                quadToRelative(-21f, 5f, -42f, 7.5f)
                reflectiveQuadToRelative(-44f, 2.5f)
                quadToRelative(-91f, 0f, -172f, -39f)
                reflectiveQuadTo(390f, 252f)
                quadToRelative(-32f, 78f, -91.5f, 135.5f)
                reflectiveQuadTo(160f, 474f)
                verticalLineToRelative(6f)
                quadToRelative(0f, 134f, 93f, 227f)
                reflectiveQuadToRelative(227f, 93f)
                close()
                moveTo(480f, 880f)
                quadToRelative(-83f, 0f, -156f, -31.5f)
                reflectiveQuadTo(197f, 763f)
                quadToRelative(-54f, -54f, -85.5f, -127f)
                reflectiveQuadTo(80f, 480f)
                quadToRelative(0f, -83f, 31.5f, -156f)
                reflectiveQuadTo(197f, 197f)
                quadToRelative(54f, -54f, 127f, -85.5f)
                reflectiveQuadTo(480f, 80f)
                quadToRelative(83f, 0f, 156f, 31.5f)
                reflectiveQuadTo(763f, 197f)
                quadToRelative(54f, 54f, 85.5f, 127f)
                reflectiveQuadTo(880f, 480f)
                quadToRelative(0f, 83f, -31.5f, 156f)
                reflectiveQuadTo(763f, 763f)
                quadToRelative(-54f, 54f, -127f, 85.5f)
                reflectiveQuadTo(480f, 880f)
                close()
                moveTo(426f, 165f)
                quadToRelative(42f, 70f, 114f, 112.5f)
                reflectiveQuadTo(700f, 320f)
                quadToRelative(14f, 0f, 27f, -1.5f)
                reflectiveQuadToRelative(27f, -3.5f)
                quadToRelative(-42f, -70f, -114f, -112.5f)
                reflectiveQuadTo(480f, 160f)
                quadToRelative(-14f, 0f, -27f, 1.5f)
                reflectiveQuadToRelative(-27f, 3.5f)
                close()
                moveTo(177f, 379f)
                quadToRelative(51f, -29f, 89f, -75f)
                reflectiveQuadToRelative(57f, -103f)
                quadToRelative(-51f, 29f, -89f, 75f)
                reflectiveQuadToRelative(-57f, 103f)
                close()
                moveTo(426f, 165f)
                close()
                moveTo(323f, 201f)
                close()
            }
        }.build()

        return _Face!!
    }

@Suppress("ObjectPropertyName")
private var _Face: ImageVector? = null
