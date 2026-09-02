package org.centrexcursionistalcoi.app.ui.icons.materialsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MaterialSymbols.Location: ImageVector
    get() {
        if (_Location != null) {
            return _Location!!
        }
        _Location = ImageVector.Builder(
            name = "Location",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF1F1F1F))) {
                moveTo(480f, 774f)
                quadToRelative(122f, -112f, 181f, -203.5f)
                reflectiveQuadTo(720f, 408f)
                quadToRelative(0f, -109f, -69.5f, -178.5f)
                reflectiveQuadTo(480f, 160f)
                quadToRelative(-101f, 0f, -170.5f, 69.5f)
                reflectiveQuadTo(240f, 408f)
                quadToRelative(0f, 71f, 59f, 162.5f)
                reflectiveQuadTo(480f, 774f)
                close()
                moveTo(452f, 848f)
                quadToRelative(-14f, -5f, -25f, -15f)
                quadToRelative(-65f, -60f, -115f, -117f)
                reflectiveQuadToRelative(-83.5f, -110.5f)
                quadToRelative(-33.5f, -53.5f, -51f, -103f)
                reflectiveQuadTo(160f, 408f)
                quadToRelative(0f, -150f, 96.5f, -239f)
                reflectiveQuadTo(480f, 80f)
                quadToRelative(127f, 0f, 223.5f, 89f)
                reflectiveQuadTo(800f, 408f)
                quadToRelative(0f, 45f, -17.5f, 94.5f)
                reflectiveQuadToRelative(-51f, 103f)
                quadTo(698f, 659f, 648f, 716f)
                reflectiveQuadTo(533f, 833f)
                quadToRelative(-11f, 10f, -25f, 15f)
                reflectiveQuadToRelative(-28f, 5f)
                quadToRelative(-14f, 0f, -28f, -5f)
                close()
                moveTo(480f, 400f)
                close()
                moveTo(536.5f, 456.5f)
                quadTo(560f, 433f, 560f, 400f)
                reflectiveQuadToRelative(-23.5f, -56.5f)
                quadTo(513f, 320f, 480f, 320f)
                reflectiveQuadToRelative(-56.5f, 23.5f)
                quadTo(400f, 367f, 400f, 400f)
                reflectiveQuadToRelative(23.5f, 56.5f)
                quadTo(447f, 480f, 480f, 480f)
                reflectiveQuadToRelative(56.5f, -23.5f)
                close()
            }
        }.build()

        return _Location!!
    }

@Suppress("ObjectPropertyName")
private var _Location: ImageVector? = null
