package org.centrexcursionistalcoi.app.ui.icons.material

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols

@Suppress("UnusedReceiverParameter")
val MaterialSymbols.CalendarEndOutline: ImageVector
    get() {
        if (_CalendarEndOutline != null) {
            return _CalendarEndOutline!!
        }
        _CalendarEndOutline = ImageVector.Builder(
            name = "Filled.CalendarEndOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 9f)
                horizontalLineTo(19f)
                verticalLineTo(12f)
                horizontalLineTo(21f)
                verticalLineTo(5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
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
                verticalLineTo(19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                horizontalLineTo(9f)
                verticalLineTo(19f)
                horizontalLineTo(5f)
                verticalLineTo(9f)
                moveTo(19f, 5f)
                verticalLineTo(7f)
                horizontalLineTo(5f)
                verticalLineTo(5f)
                horizontalLineTo(19f)
                moveTo(16f, 17f)
                horizontalLineTo(11f)
                verticalLineTo(19f)
                horizontalLineTo(16f)
                verticalLineTo(22f)
                lineTo(20f, 18f)
                lineTo(16f, 14f)
                verticalLineTo(17f)
                moveTo(20f, 14f)
                verticalLineTo(22f)
                horizontalLineTo(22f)
                verticalLineTo(14f)
                horizontalLineTo(20f)
                close()
            }
        }.build()

        return _CalendarEndOutline!!
    }

@Suppress("ObjectPropertyName")
private var _CalendarEndOutline: ImageVector? = null
