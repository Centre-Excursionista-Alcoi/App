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
val MaterialSymbols.PendingActions: ImageVector
    get() {
        if (_pending_actions != null) {
            return _pending_actions!!
        }
        _pending_actions =
            ImageVector.Builder(
                name = "pending_actions",
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
                        moveTo(13.57f, 20.54f)
                        quadTo(12.11f, 19.08f, 12.11f, 17f)
                        reflectiveQuadToRelative(1.46f, -3.54f)
                        reflectiveQuadTo(17.11f, 12f)
                        reflectiveQuadToRelative(3.54f, 1.46f)
                        quadToRelative(1.46f, 1.46f, 1.46f, 3.54f)
                        reflectiveQuadToRelative(-1.46f, 3.54f)
                        reflectiveQuadTo(17.11f, 22f)
                        reflectiveQuadTo(13.57f, 20.54f)
                        close()
                        moveToRelative(4.09f, -3.76f)
                        verticalLineTo(14.49f)
                        quadToRelative(0f, -0.22f, -0.16f, -0.38f)
                        reflectiveQuadTo(17.11f, 13.95f)
                        reflectiveQuadToRelative(-0.38f, 0.16f)
                        reflectiveQuadTo(16.57f, 14.5f)
                        verticalLineToRelative(2.24f)
                        quadToRelative(0f, 0.23f, 0.08f, 0.44f)
                        reflectiveQuadToRelative(0.25f, 0.38f)
                        lineToRelative(1.5f, 1.5f)
                        quadToRelative(0.16f, 0.16f, 0.38f, 0.16f)
                        reflectiveQuadToRelative(0.38f, -0.17f)
                        reflectiveQuadToRelative(0.16f, -0.38f)
                        reflectiveQuadTo(19.16f, 18.29f)
                        lineTo(17.66f, 16.78f)
                        close()
                        moveTo(5.07f, 21.2f)
                        quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
                        reflectiveQuadTo(2.8f, 18.93f)
                        verticalLineTo(5.07f)
                        quadTo(2.8f, 4.13f, 3.47f, 3.47f)
                        reflectiveQuadTo(5.07f, 2.8f)
                        horizontalLineTo(9.02f)
                        quadTo(9.33f, 1.9f, 10.17f, 1.34f)
                        reflectiveQuadTo(12f, 0.77f)
                        quadToRelative(1.03f, 0f, 1.86f, 0.57f)
                        reflectiveQuadTo(15.01f, 2.8f)
                        horizontalLineToRelative(3.92f)
                        quadToRelative(0.94f, 0f, 1.61f, 0.67f)
                        reflectiveQuadTo(21.2f, 5.07f)
                        verticalLineTo(8.82f)
                        quadToRelative(0f, 0.48f, -0.33f, 0.81f)
                        reflectiveQuadTo(20.07f, 9.96f)
                        reflectiveQuadTo(19.26f, 9.63f)
                        reflectiveQuadTo(18.93f, 8.82f)
                        verticalLineTo(5.07f)
                        horizontalLineTo(16.97f)
                        verticalLineTo(7.03f)
                        quadToRelative(0f, 0.48f, -0.33f, 0.81f)
                        reflectiveQuadTo(15.83f, 8.17f)
                        horizontalLineTo(8.17f)
                        quadTo(7.69f, 8.17f, 7.36f, 7.84f)
                        reflectiveQuadTo(7.03f, 7.03f)
                        verticalLineTo(5.07f)
                        horizontalLineTo(5.07f)
                        verticalLineTo(18.93f)
                        horizontalLineTo(9.45f)
                        quadToRelative(0.48f, 0f, 0.81f, 0.33f)
                        reflectiveQuadToRelative(0.33f, 0.81f)
                        reflectiveQuadToRelative(-0.33f, 0.81f)
                        reflectiveQuadTo(9.45f, 21.2f)
                        horizontalLineTo(5.07f)
                        close()
                        moveTo(12.71f, 4.68f)
                        quadTo(13f, 4.39f, 13f, 3.96f)
                        reflectiveQuadTo(12.71f, 3.25f)
                        reflectiveQuadTo(12f, 2.96f)
                        reflectiveQuadTo(11.29f, 3.25f)
                        reflectiveQuadTo(11f, 3.96f)
                        reflectiveQuadToRelative(0.29f, 0.71f)
                        reflectiveQuadTo(12f, 4.96f)
                        reflectiveQuadTo(12.71f, 4.68f)
                        close()
                    }
                }
                .build()
        return _pending_actions!!
    }

private var _pending_actions: ImageVector? = null
