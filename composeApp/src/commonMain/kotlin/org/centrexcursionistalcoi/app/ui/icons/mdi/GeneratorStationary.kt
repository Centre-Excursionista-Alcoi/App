package org.centrexcursionistalcoi.app.ui.icons.mdi

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MDI.GeneratorStationary: ImageVector
    get() {
        if (_GeneratorStationary != null) {
            return _GeneratorStationary!!
        }
        _GeneratorStationary = ImageVector.Builder(
            name = "GeneratorStationary",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 3f)
                curveTo(4.89f, 3f, 4f, 3.9f, 4f, 5f)
                verticalLineTo(16f)
                horizontalLineTo(6f)
                verticalLineTo(17f)
                curveTo(6f, 17.55f, 6.45f, 18f, 7f, 18f)
                horizontalLineTo(8f)
                curveTo(8.55f, 18f, 9f, 17.55f, 9f, 17f)
                verticalLineTo(16f)
                horizontalLineTo(15f)
                verticalLineTo(17f)
                curveTo(15f, 17.55f, 15.45f, 18f, 16f, 18f)
                horizontalLineTo(17f)
                curveTo(17.55f, 18f, 18f, 17.55f, 18f, 17f)
                verticalLineTo(16f)
                horizontalLineTo(20f)
                verticalLineTo(5f)
                curveTo(20f, 3.9f, 19.11f, 3f, 18f, 3f)
                horizontalLineTo(6f)
                moveTo(12f, 7f)
                verticalLineTo(5f)
                horizontalLineTo(18f)
                verticalLineTo(7f)
                horizontalLineTo(12f)
                moveTo(12f, 9f)
                horizontalLineTo(18f)
                verticalLineTo(11f)
                horizontalLineTo(12f)
                verticalLineTo(9f)
                moveTo(8f, 5f)
                verticalLineTo(9f)
                horizontalLineTo(10f)
                lineTo(7f, 15f)
                verticalLineTo(11f)
                horizontalLineTo(5f)
                lineTo(8f, 5f)
                moveTo(22f, 20f)
                verticalLineTo(22f)
                horizontalLineTo(2f)
                verticalLineTo(20f)
                horizontalLineTo(22f)
                close()
            }
        }.build()

        return _GeneratorStationary!!
    }

@Suppress("ObjectPropertyName")
private var _GeneratorStationary: ImageVector? = null
