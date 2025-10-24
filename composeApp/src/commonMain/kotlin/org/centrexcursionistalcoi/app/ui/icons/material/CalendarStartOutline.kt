package org.centrexcursionistalcoi.app.ui.icons.material

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Filled.CalendarStartOutline: ImageVector
    get() {
        if (_CalendarStartOutline != null) {
            return _CalendarStartOutline!!
        }
        _CalendarStartOutline = ImageVector.Builder(
            name = "Filled.CalendarStartOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13f, 18f)
                lineTo(9f, 14f)
                verticalLineTo(17f)
                horizontalLineTo(4f)
                verticalLineTo(14f)
                horizontalLineTo(2f)
                verticalLineTo(22f)
                horizontalLineTo(4f)
                verticalLineTo(19f)
                horizontalLineTo(9f)
                verticalLineTo(22f)
                lineTo(13f, 18f)
                moveTo(19f, 3f)
                horizontalLineTo(18f)
                verticalLineTo(1f)
                horizontalLineTo(16f)
                verticalLineTo(3f)
                horizontalLineTo(8f)
                verticalLineTo(1f)
                horizontalLineTo(6f)
                verticalLineTo(3f)
                horizontalLineTo(5f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                verticalLineTo(12f)
                horizontalLineTo(5f)
                verticalLineTo(9f)
                horizontalLineTo(19f)
                verticalLineTo(19f)
                horizontalLineTo(14.8f)
                lineTo(12.8f, 21f)
                horizontalLineTo(19f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                verticalLineTo(5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
                moveTo(5f, 7f)
                verticalLineTo(5f)
                horizontalLineTo(19f)
                verticalLineTo(7f)
                horizontalLineTo(5f)
                close()
            }
        }.build()

        return _CalendarStartOutline!!
    }

@Suppress("ObjectPropertyName")
private var _CalendarStartOutline: ImageVector? = null
