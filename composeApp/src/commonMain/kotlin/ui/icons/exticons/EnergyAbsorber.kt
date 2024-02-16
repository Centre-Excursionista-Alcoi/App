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

val ExtIcons.EnergyAbsorber: ImageVector
    get() {
        if (_EnergyAbsorber != null) {
            return _EnergyAbsorber!!
        }
        _EnergyAbsorber = Builder(
            name = "EnergyAbsorber",
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
                moveTo(8.5938f, 2.0938f)
                curveTo(4.293f, 1.9922f, 2.0f, 4.3867f, 2.0f, 6.6875f)
                curveTo(2.0f, 7.8867f, 2.9883f, 8.9063f, 4.1875f, 8.9063f)
                curveTo(5.3867f, 8.9063f, 6.4063f, 8.0117f, 6.4063f, 6.8125f)
                lineTo(6.4063f, 6.6875f)
                curveTo(6.5078f, 6.2891f, 6.5f, 6.0f, 8.0f, 6.0f)
                lineTo(8.0f, 4.0f)
                lineTo(8.5938f, 4.0f)
                curveTo(10.1953f, 4.0f, 11.5938f, 4.8008f, 12.5938f, 6.0f)
                lineTo(13.9063f, 6.0f)
                curveTo(14.4063f, 6.0f, 14.7891f, 5.9922f, 15.1875f, 6.0938f)
                curveTo(13.6875f, 3.793f, 11.4922f, 2.0938f, 8.5938f, 2.0938f)
                close()
                moveTo(5.0f, 5.0f)
                curveTo(4.6992f, 5.3984f, 4.5f, 5.8867f, 4.5f, 6.6875f)
                curveTo(4.5f, 6.8867f, 4.1953f, 6.9063f, 4.0938f, 6.9063f)
                curveTo(3.9922f, 6.8047f, 4.0f, 6.7891f, 4.0f, 6.6875f)
                curveTo(4.0f, 6.1875f, 4.3008f, 5.5f, 5.0f, 5.0f)
                close()
                moveTo(13.9063f, 6.9063f)
                curveTo(11.332f, 6.9063f, 8.457f, 7.5625f, 6.125f, 9.0313f)
                curveTo(3.793f, 10.5f, 2.0f, 12.8867f, 2.0f, 16.0938f)
                curveTo(2.0f, 17.5977f, 2.5f, 19.125f, 3.6875f, 20.3125f)
                curveTo(3.6992f, 20.3242f, 3.707f, 20.332f, 3.7188f, 20.3438f)
                curveTo(4.9023f, 21.4102f, 6.4297f, 22.0f, 7.9063f, 22.0f)
                lineTo(7.9063f, 22.0938f)
                curveTo(13.8047f, 22.0938f, 17.0f, 16.8125f, 17.0f, 11.8125f)
                curveTo(17.0f, 11.5117f, 17.0078f, 11.2109f, 16.9063f, 10.8125f)
                curveTo(16.3047f, 10.3125f, 15.5117f, 10.1016f, 14.8125f, 10.0f)
                curveTo(15.0117f, 10.6016f, 15.0f, 11.2109f, 15.0f, 11.8125f)
                curveTo(15.0f, 15.9141f, 12.6055f, 20.0938f, 7.9063f, 20.0938f)
                lineTo(7.9063f, 20.0f)
                curveTo(6.9844f, 20.0f, 5.8789f, 19.5781f, 5.0625f, 18.8438f)
                curveTo(4.293f, 18.0469f, 4.0f, 17.1641f, 4.0f, 16.0938f)
                curveTo(4.0f, 13.6016f, 5.2695f, 11.9258f, 7.1875f, 10.7188f)
                curveTo(9.1055f, 9.5117f, 11.6797f, 8.9063f, 13.9063f, 8.9063f)
                curveTo(16.4375f, 8.9063f, 17.8398f, 9.8672f, 18.75f, 11.0313f)
                curveTo(19.6602f, 12.1953f, 20.0f, 13.6563f, 20.0f, 14.4063f)
                curveTo(20.0f, 15.0469f, 19.8867f, 15.4844f, 19.8125f, 16.0f)
                lineTo(18.0f, 16.0f)
                curveTo(18.0f, 16.7695f, 17.832f, 17.1641f, 17.7188f, 17.3125f)
                curveTo(17.6055f, 17.4609f, 17.5781f, 17.5f, 17.3125f, 17.5f)
                curveTo(16.1367f, 17.5f, 15.0938f, 18.5117f, 15.0938f, 19.6875f)
                curveTo(15.0938f, 20.8633f, 16.1367f, 21.9063f, 17.3125f, 21.9063f)
                curveTo(18.5352f, 21.9063f, 19.7656f, 21.2031f, 20.625f, 19.9375f)
                curveTo(21.4844f, 18.6719f, 22.0f, 16.8516f, 22.0f, 14.4063f)
                curveTo(22.0f, 13.1563f, 21.5859f, 11.3672f, 20.3438f, 9.7813f)
                curveTo(19.1016f, 8.1953f, 16.9727f, 6.9063f, 13.9063f, 6.9063f)
                close()
                moveTo(18.9688f, 18.8125f)
                curveTo(18.4023f, 19.6445f, 17.7891f, 19.9063f, 17.3125f, 19.9063f)
                curveTo(17.0898f, 19.9063f, 17.0938f, 19.9102f, 17.0938f, 19.6875f)
                curveTo(17.0938f, 19.4648f, 17.0898f, 19.5f, 17.3125f, 19.5f)
                curveTo(17.9063f, 19.5f, 18.4883f, 19.2383f, 18.9688f, 18.8125f)
                close()
            }
        }.build()
        return _EnergyAbsorber!!
    }

private var _EnergyAbsorber: ImageVector? = null
