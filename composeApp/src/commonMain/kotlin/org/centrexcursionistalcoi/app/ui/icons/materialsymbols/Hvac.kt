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
val MaterialSymbols.Hvac: ImageVector
    get() {
        if (_hvac != null) {
            return _hvac!!
        }
        _hvac =
            ImageVector.Builder(
                name = "hvac",
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
                        moveTo(16.25f, 16.25f)
                        quadTo(18f, 14.5f, 18f, 12f)
                        reflectiveQuadTo(16.25f, 7.75f)
                        reflectiveQuadTo(12f, 6f)
                        reflectiveQuadTo(7.75f, 7.75f)
                        reflectiveQuadTo(6f, 12f)
                        reflectiveQuadToRelative(1.75f, 4.25f)
                        reflectiveQuadTo(12f, 18f)
                        reflectiveQuadToRelative(4.25f, -1.75f)
                        close()
                        moveTo(10.6f, 15.74f)
                        quadTo(9.93f, 15.48f, 9.38f, 15f)
                        horizontalLineToRelative(5.25f)
                        quadToRelative(-0.55f, 0.47f, -1.22f, 0.74f)
                        reflectiveQuadTo(12f, 16f)
                        quadToRelative(-0.72f, 0f, -1.4f, -0.26f)
                        close()
                        moveTo(8.55f, 14f)
                        quadTo(8.35f, 13.65f, 8.23f, 13.27f)
                        reflectiveQuadTo(8.05f, 12.5f)
                        horizontalLineToRelative(7.9f)
                        quadToRelative(-0.05f, 0.4f, -0.18f, 0.77f)
                        reflectiveQuadTo(15.45f, 14f)
                        horizontalLineTo(8.55f)
                        close()
                        moveTo(8.05f, 11.5f)
                        quadTo(8.1f, 11.1f, 8.23f, 10.73f)
                        reflectiveQuadTo(8.55f, 10f)
                        horizontalLineToRelative(6.9f)
                        quadToRelative(0.2f, 0.35f, 0.32f, 0.72f)
                        reflectiveQuadToRelative(0.18f, 0.78f)
                        horizontalLineTo(8.05f)
                        close()
                        moveTo(9.38f, 9f)
                        quadTo(9.93f, 8.52f, 10.6f, 8.26f)
                        reflectiveQuadTo(12f, 8f)
                        reflectiveQuadToRelative(1.4f, 0.26f)
                        reflectiveQuadTo(14.63f, 9f)
                        horizontalLineTo(9.38f)
                        close()
                        moveTo(5f, 21f)
                        quadTo(4.18f, 21f, 3.59f, 20.41f)
                        reflectiveQuadTo(3f, 19f)
                        verticalLineTo(5f)
                        quadTo(3f, 4.17f, 3.59f, 3.59f)
                        reflectiveQuadTo(5f, 3f)
                        horizontalLineTo(19f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        reflectiveQuadTo(21f, 5f)
                        verticalLineTo(19f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(19f, 21f)
                        horizontalLineTo(5f)
                        close()
                        moveTo(5f, 19f)
                        horizontalLineTo(19f)
                        verticalLineTo(5f)
                        horizontalLineTo(5f)
                        verticalLineTo(19f)
                        close()
                        moveTo(5f, 5f)
                        verticalLineTo(19f)
                        verticalLineTo(5f)
                        close()
                    }
                }
                .build()
        return _hvac!!
    }

private var _hvac: ImageVector? = null
