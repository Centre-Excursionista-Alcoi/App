package org.centrexcursionistalcoi.app.ui.icons.mdi

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MDI.FuelCell: ImageVector
    get() {
        if (_FuelCell != null) {
            return _FuelCell!!
        }
        _FuelCell = ImageVector.Builder(
            name = "FuelCell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 3f)
                verticalLineTo(5f)
                horizontalLineTo(4f)
                curveTo(2.9f, 5f, 2f, 5.9f, 2f, 7f)
                verticalLineTo(17f)
                curveTo(2f, 18.1f, 2.9f, 19f, 4f, 19f)
                verticalLineTo(20f)
                curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
                horizontalLineTo(7f)
                curveTo(8.1f, 22f, 9f, 21.1f, 9f, 20f)
                verticalLineTo(19f)
                horizontalLineTo(15f)
                verticalLineTo(20f)
                curveTo(15f, 21.1f, 15.9f, 22f, 17f, 22f)
                horizontalLineTo(18f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                verticalLineTo(19f)
                curveTo(21.1f, 19f, 22f, 18.1f, 22f, 17f)
                horizontalLineTo(11f)
                verticalLineTo(5f)
                horizontalLineTo(8f)
                verticalLineTo(3f)
                horizontalLineTo(6f)
                moveTo(16f, 3f)
                verticalLineTo(5f)
                horizontalLineTo(13f)
                verticalLineTo(7f)
                horizontalLineTo(22f)
                curveTo(22f, 5.9f, 21.1f, 5f, 20f, 5f)
                horizontalLineTo(18f)
                verticalLineTo(3f)
                horizontalLineTo(16f)
                moveTo(7f, 7f)
                verticalLineTo(11f)
                horizontalLineTo(9f)
                lineTo(6f, 17f)
                verticalLineTo(13f)
                horizontalLineTo(4f)
                lineTo(7f, 7f)
                moveTo(13f, 9f)
                verticalLineTo(11f)
                horizontalLineTo(22f)
                verticalLineTo(9f)
                horizontalLineTo(13f)
                moveTo(13f, 13f)
                verticalLineTo(15f)
                horizontalLineTo(22f)
                verticalLineTo(13f)
                horizontalLineTo(13f)
                close()
            }
        }.build()

        return _FuelCell!!
    }

@Suppress("ObjectPropertyName")
private var _FuelCell: ImageVector? = null
