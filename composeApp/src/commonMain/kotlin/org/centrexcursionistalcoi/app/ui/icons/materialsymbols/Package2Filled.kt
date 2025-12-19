package org.centrexcursionistalcoi.app.ui.icons.materialsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MaterialSymbols.Package2Filled: ImageVector
    get() {
        if (_Package2Filled != null) {
            return _Package2Filled!!
        }
        _Package2Filled = ImageVector.Builder(
            name = "Package2Filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(440f, 869f)
                verticalLineToRelative(-366f)
                lineTo(120f, 318f)
                verticalLineToRelative(321f)
                quadToRelative(0f, 22f, 10.5f, 40f)
                reflectiveQuadToRelative(29.5f, 29f)
                lineTo(440f, 869f)
                close()
                moveTo(520f, 869f)
                lineTo(800f, 708f)
                quadToRelative(19f, -11f, 29.5f, -29f)
                reflectiveQuadToRelative(10.5f, -40f)
                verticalLineToRelative(-321f)
                lineTo(520f, 503f)
                verticalLineToRelative(366f)
                close()
                moveTo(679f, 319f)
                lineTo(797f, 250f)
                lineTo(520f, 91f)
                quadToRelative(-19f, -11f, -40f, -11f)
                reflectiveQuadToRelative(-40f, 11f)
                lineToRelative(-79f, 45f)
                lineToRelative(318f, 183f)
                close()
                moveTo(480f, 434f)
                lineToRelative(119f, -68f)
                lineToRelative(-317f, -184f)
                lineToRelative(-120f, 69f)
                lineToRelative(318f, 183f)
                close()
            }
        }.build()

        return _Package2Filled!!
    }

@Suppress("ObjectPropertyName")
private var _Package2Filled: ImageVector? = null
