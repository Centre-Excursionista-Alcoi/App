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

val ExtIcons.Rope: ImageVector
    get() {
        if (_Rope != null) {
            return _Rope!!
        }
        _Rope = Builder(
            name = "Rope",
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
                moveTo(16.5f, 1.9688f)
                curveTo(15.0898f, 1.9688f, 13.6758f, 2.5117f, 12.5938f, 3.5938f)
                curveTo(12.2852f, 3.9023f, 12.0938f, 4.2148f, 11.7188f, 4.6875f)
                curveTo(11.3438f, 5.1602f, 10.8438f, 5.7188f, 10.2813f, 6.2813f)
                lineTo(11.7188f, 7.7188f)
                curveTo(12.3594f, 7.0781f, 12.8828f, 6.4414f, 13.2813f, 5.9375f)
                curveTo(13.6797f, 5.4336f, 14.0078f, 4.9922f, 14.0f, 5.0f)
                curveTo(15.4336f, 3.5664f, 17.5664f, 3.5664f, 19.0f, 5.0f)
                curveTo(20.4336f, 6.4336f, 20.4336f, 8.5664f, 19.0f, 10.0f)
                curveTo(19.0078f, 9.9922f, 18.4727f, 10.4102f, 17.9375f, 10.8438f)
                curveTo(17.4023f, 11.2773f, 16.7852f, 11.7773f, 16.2813f, 12.2813f)
                lineTo(17.7188f, 13.7188f)
                curveTo(18.1172f, 13.3203f, 18.7031f, 12.8242f, 19.2188f, 12.4063f)
                curveTo(19.7344f, 11.9883f, 20.0977f, 11.7148f, 20.4063f, 11.4063f)
                curveTo(22.5742f, 9.2383f, 22.5742f, 5.7617f, 20.4063f, 3.5938f)
                curveTo(19.3242f, 2.5117f, 17.9102f, 1.9688f, 16.5f, 1.9688f)
                close()
                moveTo(16.25f, 5.0f)
                curveTo(15.5391f, 5.0f, 14.8164f, 5.2461f, 14.2813f, 5.7813f)
                curveTo(14.0938f, 5.9688f, 14.1406f, 5.9648f, 14.0938f, 6.0313f)
                curveTo(14.0469f, 6.0977f, 14.0078f, 6.1836f, 13.9375f, 6.2813f)
                curveTo(13.8008f, 6.4727f, 13.6289f, 6.7109f, 13.4375f, 6.9688f)
                curveTo(13.0508f, 7.4883f, 12.5313f, 8.0859f, 12.3438f, 8.25f)
                lineTo(13.6563f, 9.75f)
                curveTo(14.168f, 9.3125f, 14.6172f, 8.7109f, 15.0313f, 8.1563f)
                curveTo(15.2383f, 7.8789f, 15.4258f, 7.6289f, 15.5625f, 7.4375f)
                curveTo(15.625f, 7.3516f, 15.6797f, 7.2695f, 15.7188f, 7.2188f)
                curveTo(16.0469f, 6.8906f, 16.4531f, 6.8906f, 16.7813f, 7.2188f)
                curveTo(17.1094f, 7.5469f, 17.1094f, 7.9531f, 16.7813f, 8.2813f)
                curveTo(16.8789f, 8.1836f, 16.7148f, 8.3633f, 16.5313f, 8.5f)
                curveTo(16.3477f, 8.6367f, 16.1055f, 8.8008f, 15.8438f, 9.0f)
                curveTo(15.3203f, 9.3945f, 14.7148f, 9.8477f, 14.2813f, 10.2813f)
                lineTo(15.7188f, 11.7188f)
                curveTo(15.9844f, 11.4531f, 16.5273f, 11.0039f, 17.0313f, 10.625f)
                curveTo(17.2813f, 10.4336f, 17.5234f, 10.2383f, 17.7188f, 10.0938f)
                curveTo(17.9141f, 9.9492f, 18.0195f, 9.918f, 18.2188f, 9.7188f)
                curveTo(19.2891f, 8.6484f, 19.2891f, 6.8516f, 18.2188f, 5.7813f)
                curveTo(17.6836f, 5.2461f, 16.9609f, 5.0f, 16.25f, 5.0f)
                close()
                moveTo(9.0938f, 6.9063f)
                lineTo(7.6875f, 8.3125f)
                lineTo(13.9063f, 14.5f)
                curveTo(17.1484f, 17.7422f, 17.0f, 19.5f, 17.0f, 22.0f)
                lineTo(19.0f, 22.0f)
                curveTo(19.0f, 19.5f, 18.8711f, 16.6523f, 15.3125f, 13.0938f)
                close()
                moveTo(6.8125f, 9.1875f)
                lineTo(5.4063f, 10.5938f)
                lineTo(5.6563f, 10.8438f)
                curveTo(5.3672f, 11.0977f, 5.0742f, 11.3555f, 4.7813f, 11.5938f)
                curveTo(4.2656f, 12.0117f, 3.9023f, 12.2852f, 3.5938f, 12.5938f)
                curveTo(1.4258f, 14.7617f, 1.4258f, 18.2383f, 3.5938f, 20.4063f)
                curveTo(5.7617f, 22.5742f, 9.2383f, 22.5742f, 11.4063f, 20.4063f)
                curveTo(11.6758f, 20.1367f, 11.918f, 19.7969f, 12.3125f, 19.3125f)
                curveTo(12.5469f, 19.0273f, 12.8594f, 18.6719f, 13.1563f, 18.3438f)
                lineTo(13.4063f, 18.5938f)
                lineTo(14.8125f, 17.1875f)
                close()
                moveTo(7.0938f, 12.2813f)
                lineTo(7.6563f, 12.8438f)
                curveTo(7.4414f, 13.0195f, 7.1953f, 13.2031f, 6.9688f, 13.375f)
                curveTo(6.7188f, 13.5664f, 6.4766f, 13.7617f, 6.2813f, 13.9063f)
                curveTo(6.0859f, 14.0508f, 5.9805f, 14.082f, 5.7813f, 14.2813f)
                curveTo(4.7109f, 15.3516f, 4.7109f, 17.1484f, 5.7813f, 18.2188f)
                curveTo(6.8516f, 19.2891f, 8.6484f, 19.2891f, 9.7188f, 18.2188f)
                curveTo(9.9063f, 18.0313f, 9.8594f, 18.0352f, 9.9063f, 17.9688f)
                curveTo(9.9531f, 17.9023f, 9.9922f, 17.8164f, 10.0625f, 17.7188f)
                curveTo(10.1992f, 17.5273f, 10.3711f, 17.2891f, 10.5625f, 17.0313f)
                curveTo(10.7383f, 16.793f, 10.9375f, 16.5352f, 11.125f, 16.3125f)
                lineTo(11.75f, 16.9375f)
                curveTo(11.4023f, 17.3516f, 11.0547f, 17.7305f, 10.7813f, 18.0625f)
                curveTo(10.3789f, 18.5547f, 10.0313f, 18.9688f, 10.0f, 19.0f)
                curveTo(8.5664f, 20.4336f, 6.4336f, 20.4336f, 5.0f, 19.0f)
                curveTo(3.5664f, 17.5664f, 3.5664f, 15.4336f, 5.0f, 14.0f)
                curveTo(4.9922f, 14.0078f, 5.5273f, 13.5898f, 6.0625f, 13.1563f)
                curveTo(6.3945f, 12.8867f, 6.7461f, 12.5859f, 7.0938f, 12.2813f)
                close()
                moveTo(9.0938f, 14.2813f)
                lineTo(9.7188f, 14.9063f)
                curveTo(9.457f, 15.2109f, 9.2031f, 15.5313f, 8.9688f, 15.8438f)
                curveTo(8.7617f, 16.1211f, 8.5742f, 16.3711f, 8.4375f, 16.5625f)
                curveTo(8.375f, 16.6484f, 8.3203f, 16.7305f, 8.2813f, 16.7813f)
                curveTo(7.9531f, 17.1094f, 7.5469f, 17.1094f, 7.2188f, 16.7813f)
                curveTo(6.8906f, 16.4531f, 6.8906f, 16.0469f, 7.2188f, 15.7188f)
                curveTo(7.1211f, 15.8164f, 7.2852f, 15.6367f, 7.4688f, 15.5f)
                curveTo(7.6523f, 15.3633f, 7.8945f, 15.1992f, 8.1563f, 15.0f)
                curveTo(8.457f, 14.7734f, 8.7852f, 14.5352f, 9.0938f, 14.2813f)
                close()
            }
        }.build()
        return _Rope!!
    }

private var _Rope: ImageVector? = null
