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

val ExtIcons.Climbing: ImageVector
    get() {
        if (_Climbing != null) {
            return _Climbing!!
        }
        _Climbing = Builder(
            name = "Climbing",
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
                moveTo(7.4063f, 2.0f)
                curveTo(4.207f, 2.0f, 3.0f, 5.5f, 3.0f, 8.0f)
                curveTo(3.0f, 17.1992f, 4.5117f, 22.0f, 7.3125f, 22.0f)
                lineTo(7.9063f, 21.9063f)
                curveTo(7.207f, 21.6055f, 5.0f, 19.6992f, 5.0f, 8.0f)
                curveTo(5.0f, 5.5f, 5.6953f, 2.1992f, 8.5938f, 2.0f)
                close()
                moveTo(10.3125f, 2.0938f)
                curveTo(7.1133f, 1.9922f, 6.1992f, 5.5f, 6.0f, 8.0f)
                curveTo(5.8984f, 10.1992f, 6.293f, 14.0078f, 7.0938f, 17.9063f)
                curveTo(8.0938f, 22.4063f, 9.7891f, 22.1055f, 10.1875f, 21.9063f)
                curveTo(12.0859f, 21.207f, 13.6133f, 19.2109f, 14.3125f, 18.3125f)
                curveTo(15.0117f, 17.5117f, 17.9883f, 14.0f, 19.1875f, 12.5f)
                curveTo(21.0859f, 10.3008f, 21.207f, 9.0078f, 20.9063f, 7.4063f)
                curveTo(20.6055f, 5.707f, 19.4141f, 4.4063f, 16.8125f, 3.4063f)
                curveTo(14.2109f, 2.4063f, 12.2109f, 2.0938f, 10.3125f, 2.0938f)
                close()
                moveTo(16.0f, 6.0f)
                curveTo(17.1016f, 6.0f, 18.0f, 6.8984f, 18.0f, 8.0f)
                curveTo(18.0f, 9.1016f, 17.1016f, 10.0f, 16.0f, 10.0f)
                curveTo(14.8984f, 10.0f, 14.0f, 9.1016f, 14.0f, 8.0f)
                curveTo(14.0f, 6.8984f, 14.8984f, 6.0f, 16.0f, 6.0f)
                close()
                moveTo(9.0313f, 6.1875f)
                curveTo(9.2578f, 6.1328f, 9.4609f, 6.4609f, 9.6875f, 6.6875f)
                curveTo(9.8867f, 6.9883f, 10.3867f, 7.8867f, 10.6875f, 9.1875f)
                curveTo(10.9883f, 10.4883f, 11.207f, 11.8945f, 11.4063f, 12.5938f)
                curveTo(11.5078f, 13.4922f, 11.3008f, 14.5938f, 11.0f, 15.0938f)
                curveTo(10.6992f, 15.5938f, 10.3867f, 15.9141f, 10.1875f, 16.3125f)
                curveTo(9.9883f, 16.6133f, 9.5117f, 17.0117f, 9.3125f, 16.8125f)
                curveTo(9.1133f, 16.7109f, 8.6055f, 14.8945f, 8.4063f, 13.5938f)
                curveTo(8.1055f, 12.3945f, 7.9922f, 11.8008f, 8.0938f, 9.5f)
                curveTo(8.1953f, 7.3008f, 8.8125f, 6.4063f, 8.8125f, 6.4063f)
                curveTo(8.8867f, 6.2813f, 8.957f, 6.207f, 9.0313f, 6.1875f)
                close()
            }
        }.build()
        return _Climbing!!
    }

private var _Climbing: ImageVector? = null
