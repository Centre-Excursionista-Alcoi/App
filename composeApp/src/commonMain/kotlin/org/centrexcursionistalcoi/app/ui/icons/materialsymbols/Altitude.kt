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
val MaterialSymbols.Altitude: ImageVector
    get() {
        if (_altitude != null) {
            return _altitude!!
        }
        _altitude =
            ImageVector.Builder(
                name = "altitude",
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
                        moveTo(18.29f, 11.71f)
                        quadTo(18f, 11.43f, 18f, 11f)
                        verticalLineTo(7.8f)
                        lineTo(17.08f, 8.7f)
                        quadTo(16.8f, 8.98f, 16.4f, 8.99f)
                        reflectiveQuadTo(15.7f, 8.7f)
                        quadTo(15.43f, 8.42f, 15.43f, 8f)
                        quadToRelative(0f, -0.43f, 0.28f, -0.7f)
                        lineTo(18.3f, 4.7f)
                        quadTo(18.6f, 4.4f, 19f, 4.4f)
                        reflectiveQuadToRelative(0.7f, 0.3f)
                        lineToRelative(2.6f, 2.6f)
                        quadTo(22.6f, 7.6f, 22.6f, 8f)
                        reflectiveQuadTo(22.3f, 8.7f)
                        reflectiveQuadTo(21.59f, 9f)
                        reflectiveQuadTo(20.88f, 8.7f)
                        lineTo(20f, 7.82f)
                        verticalLineTo(11f)
                        quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                        reflectiveQuadTo(19f, 12f)
                        reflectiveQuadTo(18.29f, 11.71f)
                        close()
                        moveTo(3f, 22f)
                        quadTo(2.38f, 22f, 2.1f, 21.45f)
                        reflectiveQuadTo(2.2f, 20.4f)
                        lineToRelative(4f, -5.32f)
                        quadToRelative(0.15f, -0.2f, 0.36f, -0.3f)
                        reflectiveQuadTo(7f, 14.68f)
                        reflectiveQuadToRelative(0.44f, 0.1f)
                        reflectiveQuadToRelative(0.36f, 0.3f)
                        lineTo(11.5f, 20f)
                        horizontalLineTo(19f)
                        lineTo(14f, 13.35f)
                        lineTo(12.3f, 15.6f)
                        quadTo(12f, 16f, 11.6f, 16.01f)
                        reflectiveQuadTo(10.9f, 15.8f)
                        reflectiveQuadTo(10.5f, 15.19f)
                        reflectiveQuadTo(10.7f, 14.4f)
                        lineToRelative(2.5f, -3.32f)
                        quadToRelative(0.15f, -0.2f, 0.36f, -0.3f)
                        quadToRelative(0.21f, -0.1f, 0.44f, -0.1f)
                        reflectiveQuadToRelative(0.44f, 0.1f)
                        quadToRelative(0.21f, 0.1f, 0.36f, 0.3f)
                        lineToRelative(7f, 9.32f)
                        quadToRelative(0.38f, 0.5f, 0.1f, 1.05f)
                        reflectiveQuadTo(21f, 22f)
                        horizontalLineTo(3f)
                        close()
                        moveToRelative(8.5f, -2f)
                        close()
                    }
                }
                .build()
        return _altitude!!
    }

private var _altitude: ImageVector? = null
