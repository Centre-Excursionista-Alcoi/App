package org.centrexcursionistalcoi.app.ui.icons.materialsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val MaterialSymbols.WindPower: ImageVector
    get() {
        if (_wind_power != null) {
            return _wind_power!!
        }
        _wind_power =
            ImageVector.Builder(
                name = "wind_power",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(10f, 23f)
                        quadToRelative(0f, -0.82f, 0.59f, -1.41f)
                        reflectiveQuadTo(12f, 21f)
                        verticalLineTo(15.8f)
                        quadTo(11.7f, 15.68f, 11.44f, 15.51f)
                        reflectiveQuadTo(10.95f, 15.15f)
                        lineToRelative(-2.2f, 1.4f)
                        quadTo(8.4f, 16.75f, 7.99f, 16.81f)
                        reflectiveQuadTo(7.15f, 16.75f)
                        lineTo(2.65f, 15.48f)
                        quadTo(1.93f, 15.28f, 1.46f, 14.66f)
                        reflectiveQuadTo(1f, 13.27f)
                        quadTo(1f, 12.33f, 1.66f, 11.66f)
                        reflectiveQuadTo(3.28f, 11f)
                        horizontalLineTo(10.8f)
                        quadToRelative(0.25f, -0.28f, 0.55f, -0.48f)
                        quadTo(11.65f, 10.33f, 12f, 10.2f)
                        verticalLineTo(6.77f)
                        quadToRelative(0f, -0.43f, 0.16f, -0.8f)
                        reflectiveQuadTo(12.63f, 5.32f)
                        lineToRelative(3.43f, -3.2f)
                        quadTo(16.63f, 1.57f, 17.39f, 1.5f)
                        quadTo(18.15f, 1.42f, 18.8f, 1.82f)
                        quadToRelative(0.8f, 0.5f, 1.04f, 1.41f)
                        reflectiveQuadTo(19.58f, 4.95f)
                        lineTo(15.6f, 11.52f)
                        quadToRelative(0.17f, 0.3f, 0.26f, 0.65f)
                        reflectiveQuadToRelative(0.11f, 0.72f)
                        lineToRelative(2.7f, 0.65f)
                        quadToRelative(0.4f, 0.1f, 0.73f, 0.35f)
                        reflectiveQuadToRelative(0.52f, 0.6f)
                        lineToRelative(2.28f, 4.1f)
                        quadToRelative(0.38f, 0.67f, 0.27f, 1.42f)
                        reflectiveQuadToRelative(-0.65f, 1.3f)
                        quadTo(21.15f, 22f, 20.21f, 22f)
                        reflectiveQuadTo(18.6f, 21.33f)
                        lineTo(14f, 16.73f)
                        verticalLineTo(21f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        quadTo(16f, 22.18f, 16f, 23f)
                        horizontalLineTo(10f)
                        close()
                        moveTo(5f, 5f)
                        quadTo(4.58f, 5f, 4.29f, 4.71f)
                        reflectiveQuadTo(4f, 4f)
                        quadTo(4f, 3.57f, 4.29f, 3.29f)
                        reflectiveQuadTo(5f, 3f)
                        horizontalLineTo(9f)
                        quadTo(9.43f, 3f, 9.71f, 3.29f)
                        reflectiveQuadTo(10f, 4f)
                        quadTo(10f, 4.42f, 9.71f, 4.71f)
                        reflectiveQuadTo(9f, 5f)
                        horizontalLineTo(5f)
                        close()
                        moveToRelative(9f, 1.77f)
                        verticalLineTo(10.2f)
                        quadToRelative(0.03f, 0f, 0.04f, 0.01f)
                        quadToRelative(0.01f, 0.01f, 0.04f, 0.01f)
                        lineTo(17.88f, 3.9f)
                        quadTo(17.93f, 3.8f, 17.9f, 3.69f)
                        quadTo(17.88f, 3.57f, 17.78f, 3.52f)
                        quadTo(17.7f, 3.47f, 17.59f, 3.5f)
                        quadTo(17.48f, 3.52f, 17.43f, 3.57f)
                        lineTo(14f, 6.77f)
                        close()
                        moveTo(2f, 9f)
                        quadTo(1.58f, 9f, 1.29f, 8.71f)
                        reflectiveQuadTo(1f, 8f)
                        quadTo(1f, 7.57f, 1.29f, 7.29f)
                        reflectiveQuadTo(2f, 7f)
                        horizontalLineTo(5f)
                        quadTo(5.43f, 7f, 5.71f, 7.29f)
                        reflectiveQuadTo(6f, 8f)
                        quadTo(6f, 8.42f, 5.71f, 8.71f)
                        reflectiveQuadTo(5f, 9f)
                        horizontalLineTo(2f)
                        close()
                        moveToRelative(11f, 5f)
                        quadToRelative(0.43f, 0f, 0.71f, -0.29f)
                        quadTo(14f, 13.43f, 14f, 13f)
                        reflectiveQuadTo(13.71f, 12.29f)
                        reflectiveQuadTo(13f, 12f)
                        reflectiveQuadToRelative(-0.71f, 0.29f)
                        reflectiveQuadTo(12f, 13f)
                        reflectiveQuadToRelative(0.29f, 0.71f)
                        reflectiveQuadTo(13f, 14f)
                        close()
                        moveTo(7.73f, 14.85f)
                        lineToRelative(2.33f, -1.4f)
                        quadTo(10.03f, 13.33f, 10.03f, 13.23f)
                        reflectiveQuadToRelative(0f, -0.22f)
                        horizontalLineTo(3.28f)
                        quadToRelative(-0.13f, 0f, -0.2f, 0.07f)
                        reflectiveQuadTo(3f, 13.27f)
                        quadToRelative(0f, 0.1f, 0.05f, 0.18f)
                        reflectiveQuadToRelative(0.15f, 0.1f)
                        lineToRelative(4.53f, 1.3f)
                        close()
                        moveTo(18.2f, 15.48f)
                        lineTo(15.35f, 14.83f)
                        quadToRelative(-0.05f, 0.05f, -0.1f, 0.13f)
                        quadToRelative(-0.05f, 0.08f, -0.1f, 0.13f)
                        lineToRelative(4.88f, 4.85f)
                        quadTo(20.1f, 20f, 20.23f, 20f)
                        reflectiveQuadToRelative(0.2f, -0.07f)
                        reflectiveQuadToRelative(0.09f, -0.16f)
                        quadToRelative(0.01f, -0.09f, -0.04f, -0.19f)
                        lineTo(18.2f, 15.48f)
                        close()
                        moveTo(4f, 21f)
                        quadTo(3.58f, 21f, 3.29f, 20.71f)
                        quadTo(3f, 20.43f, 3f, 20f)
                        reflectiveQuadTo(3.29f, 19.29f)
                        reflectiveQuadTo(4f, 19f)
                        horizontalLineTo(7f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(8f, 20f)
                        reflectiveQuadTo(7.71f, 20.71f)
                        reflectiveQuadTo(7f, 21f)
                        horizontalLineTo(4f)
                        close()
                        moveToRelative(9f, -8f)
                        close()
                        moveToRelative(1.08f, -2.78f)
                        close()
                        moveTo(10.03f, 13f)
                        close()
                        moveToRelative(5.13f, 2.07f)
                        close()
                    }
                }
                .build()
        return _wind_power!!
    }

private var _wind_power: ImageVector? = null
