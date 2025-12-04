package org.centrexcursionistalcoi.app.ui.icons.materialsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MaterialSymbols.Mail: ImageVector
    get() {
        if (_Mail != null) {
            return _Mail!!
        }
        _Mail = ImageVector.Builder(
            name = "Mail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(160f, 800f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(80f, 720f)
                verticalLineToRelative(-480f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(160f, 160f)
                horizontalLineToRelative(640f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(880f, 240f)
                verticalLineToRelative(480f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(800f, 800f)
                lineTo(160f, 800f)
                close()
                moveTo(800f, 320f)
                lineTo(501f, 507f)
                quadToRelative(-5f, 3f, -10.5f, 4.5f)
                reflectiveQuadTo(480f, 513f)
                quadToRelative(-5f, 0f, -10.5f, -1.5f)
                reflectiveQuadTo(459f, 507f)
                lineTo(160f, 320f)
                verticalLineToRelative(400f)
                horizontalLineToRelative(640f)
                verticalLineToRelative(-400f)
                close()
                moveTo(480f, 440f)
                lineToRelative(320f, -200f)
                lineTo(160f, 240f)
                lineToRelative(320f, 200f)
                close()
                moveTo(160f, 320f)
                verticalLineToRelative(10f)
                verticalLineToRelative(-59f)
                verticalLineToRelative(1f)
                verticalLineToRelative(-32f)
                verticalLineToRelative(32f)
                verticalLineToRelative(-0.5f)
                verticalLineToRelative(58.5f)
                verticalLineToRelative(-10f)
                verticalLineToRelative(400f)
                verticalLineToRelative(-400f)
                close()
            }
        }.build()

        return _Mail!!
    }

@Suppress("ObjectPropertyName")
private var _Mail: ImageVector? = null
