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
val MaterialSymbols.Handyman: ImageVector
    get() {
        if (_handyman != null) {
            return _handyman!!
        }
        _handyman =
            ImageVector.Builder(
                name = "handyman",
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
                        moveTo(18.68f, 22.33f)
                        quadToRelative(-0.2f, -0.07f, -0.37f, -0.24f)
                        lineTo(13.09f, 16.86f)
                        quadTo(12.92f, 16.7f, 12.85f, 16.49f)
                        quadToRelative(-0.07f, -0.2f, -0.07f, -0.43f)
                        reflectiveQuadToRelative(0.07f, -0.43f)
                        reflectiveQuadToRelative(0.24f, -0.37f)
                        lineToRelative(2.14f, -2.14f)
                        quadTo(15.4f, 12.95f, 15.6f, 12.88f)
                        reflectiveQuadTo(16.03f, 12.8f)
                        reflectiveQuadToRelative(0.43f, 0.08f)
                        reflectiveQuadToRelative(0.37f, 0.25f)
                        lineToRelative(5.22f, 5.23f)
                        quadToRelative(0.17f, 0.17f, 0.24f, 0.37f)
                        reflectiveQuadToRelative(0.07f, 0.43f)
                        reflectiveQuadToRelative(-0.08f, 0.43f)
                        reflectiveQuadToRelative(-0.25f, 0.37f)
                        lineToRelative(-2.14f, 2.14f)
                        quadToRelative(-0.17f, 0.17f, -0.37f, 0.24f)
                        reflectiveQuadTo(19.11f, 22.4f)
                        reflectiveQuadTo(18.68f, 22.33f)
                        close()
                        moveToRelative(0.43f, -2.5f)
                        lineToRelative(0.68f, -0.68f)
                        lineTo(16.05f, 15.4f)
                        lineToRelative(-0.68f, 0.68f)
                        lineToRelative(3.74f, 3.74f)
                        close()
                        moveTo(4.44f, 22.33f)
                        quadTo(4.23f, 22.25f, 4.06f, 22.08f)
                        lineTo(1.94f, 19.97f)
                        quadTo(1.77f, 19.8f, 1.69f, 19.59f)
                        reflectiveQuadTo(1.61f, 19.16f)
                        reflectiveQuadTo(1.69f, 18.73f)
                        reflectiveQuadTo(1.94f, 18.35f)
                        lineTo(7.42f, 12.88f)
                        horizontalLineToRelative(2.2f)
                        lineToRelative(0.83f, -0.83f)
                        lineTo(6.23f, 7.83f)
                        horizontalLineTo(4.76f)
                        lineTo(1.56f, 4.64f)
                        lineToRelative(3.01f, -3f)
                        lineTo(7.76f, 4.82f)
                        verticalLineTo(6.3f)
                        lineToRelative(4.22f, 4.22f)
                        lineTo(14.9f, 7.59f)
                        lineTo(13.79f, 6.48f)
                        lineTo(15.19f, 5.08f)
                        horizontalLineTo(12.37f)
                        lineTo(11.59f, 4.3f)
                        lineTo(15.35f, 0.54f)
                        lineToRelative(0.78f, 0.78f)
                        verticalLineTo(4.14f)
                        lineToRelative(1.39f, -1.4f)
                        lineToRelative(3.72f, 3.72f)
                        quadToRelative(0.44f, 0.44f, 0.69f, 1.01f)
                        reflectiveQuadToRelative(0.24f, 1.19f)
                        quadToRelative(0f, 0.65f, -0.25f, 1.24f)
                        reflectiveQuadTo(21.2f, 10.96f)
                        lineTo(19f, 8.76f)
                        lineToRelative(-1.45f, 1.45f)
                        lineTo(16.47f, 9.12f)
                        lineToRelative(-5.31f, 5.31f)
                        verticalLineToRelative(2.18f)
                        lineTo(5.68f, 22.08f)
                        quadTo(5.51f, 22.25f, 5.31f, 22.33f)
                        reflectiveQuadTo(4.88f, 22.42f)
                        reflectiveQuadTo(4.44f, 22.33f)
                        close()
                        moveTo(4.88f, 19.83f)
                        lineTo(9.21f, 15.5f)
                        verticalLineTo(14.82f)
                        horizontalLineTo(8.53f)
                        lineTo(4.2f, 19.15f)
                        lineToRelative(0.68f, 0.68f)
                        close()
                        moveToRelative(0f, 0f)
                        lineTo(4.2f, 19.15f)
                        lineToRelative(0.35f, 0.33f)
                        lineToRelative(0.33f, 0.35f)
                        close()
                        moveTo(19.11f, 19.82f)
                        lineToRelative(0.68f, -0.68f)
                        lineToRelative(-0.68f, 0.68f)
                        close()
                    }
                }
                .build()
        return _handyman!!
    }

private var _handyman: ImageVector? = null
