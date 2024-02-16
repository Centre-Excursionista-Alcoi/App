package ui.icons.exticons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import ui.icons.ExtIcons

val ExtIcons.ClimbingShoes: ImageVector
    get() {
        if (_ClimbingShoes != null) {
            return _ClimbingShoes!!
        }
        _ClimbingShoes = Builder(
            name = "ClimbingShoes",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(19.5f, 2.0f)
                curveTo(18.8008f, 2.0f, 17.8125f, 2.3125f, 17.8125f, 2.3125f)
                curveTo(20.4141f, 4.8125f, 11.9063f, 7.6016f, 12.9063f, 11.5f)
                curveTo(8.8047f, 7.8008f, 20.7109f, 3.0078f, 15.8125f, 2.9063f)
                curveTo(10.2109f, 4.6055f, 6.0f, 9.0f, 6.0f, 9.0f)
                curveTo(6.0f, 9.0f, 6.3008f, 10.3125f, 8.5f, 10.3125f)
                curveTo(10.8984f, 10.3125f, 9.9063f, 12.207f, 8.9063f, 14.4063f)
                curveTo(8.1797f, 15.8633f, 7.2383f, 16.7852f, 5.9063f, 16.7188f)
                curveTo(5.6719f, 16.5977f, 5.4063f, 16.4258f, 5.2188f, 16.3125f)
                curveTo(4.3828f, 15.7969f, 4.0f, 15.5313f, 4.0f, 14.0f)
                lineTo(2.0f, 14.0f)
                curveTo(2.0f, 16.0703f, 3.0742f, 17.3477f, 4.1875f, 18.0313f)
                curveTo(5.3008f, 18.7148f, 6.3438f, 19.0742f, 7.1563f, 19.9688f)
                lineTo(7.1875f, 19.9375f)
                curveTo(8.1563f, 21.043f, 9.4023f, 22.0f, 10.9063f, 22.0f)
                curveTo(14.1055f, 22.0f, 13.3125f, 15.7891f, 14.3125f, 13.6875f)
                curveTo(16.2109f, 9.5859f, 22.0f, 8.3125f, 22.0f, 3.8125f)
                curveTo(22.0f, 2.8125f, 21.1992f, 2.0f, 19.5f, 2.0f)
                close()
            }
        }.build()
        return _ClimbingShoes!!
    }

private var _ClimbingShoes: ImageVector? = null
