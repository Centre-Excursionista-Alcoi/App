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
val MaterialSymbols.HolidayVillage: ImageVector
    get() {
        if (_holiday_village != null) {
            return _holiday_village!!
        }
        _holiday_village =
            ImageVector.Builder(
                name = "holiday_village",
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
                        moveTo(1.72f, 17.87f)
                        verticalLineTo(10.85f)
                        quadToRelative(0f, -0.46f, 0.17f, -0.87f)
                        reflectiveQuadTo(2.38f, 9.25f)
                        lineTo(6.2f, 5.44f)
                        quadTo(6.54f, 5.1f, 6.96f, 4.93f)
                        reflectiveQuadTo(7.81f, 4.76f)
                        reflectiveQuadTo(8.67f, 4.93f)
                        reflectiveQuadTo(9.43f, 5.44f)
                        lineToRelative(3.82f, 3.81f)
                        quadToRelative(0.32f, 0.32f, 0.49f, 0.73f)
                        reflectiveQuadToRelative(0.17f, 0.87f)
                        verticalLineToRelative(7.02f)
                        quadToRelative(0f, 0.94f, -0.67f, 1.61f)
                        reflectiveQuadToRelative(-1.61f, 0.67f)
                        horizontalLineTo(4f)
                        quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
                        reflectiveQuadTo(1.72f, 17.87f)
                        close()
                        moveToRelative(2.19f, 0.09f)
                        horizontalLineTo(6.72f)
                        verticalLineTo(16.05f)
                        quadToRelative(0f, -0.46f, 0.32f, -0.78f)
                        reflectiveQuadTo(7.82f, 14.96f)
                        reflectiveQuadTo(8.6f, 15.27f)
                        reflectiveQuadToRelative(0.31f, 0.78f)
                        verticalLineToRelative(1.91f)
                        horizontalLineToRelative(2.81f)
                        verticalLineTo(10.81f)
                        lineTo(7.81f, 6.91f)
                        lineToRelative(-3.9f, 3.9f)
                        verticalLineToRelative(7.15f)
                        close()
                        moveToRelative(12f, 1.1f)
                        verticalLineTo(9.09f)
                        lineTo(12.38f, 5.55f)
                        quadTo(11.86f, 5.03f, 12.13f, 4.36f)
                        reflectiveQuadTo(13.14f, 3.7f)
                        quadToRelative(0.22f, 0f, 0.43f, 0.09f)
                        reflectiveQuadToRelative(0.35f, 0.23f)
                        lineToRelative(3.5f, 3.5f)
                        quadToRelative(0.32f, 0.32f, 0.49f, 0.73f)
                        reflectiveQuadToRelative(0.17f, 0.87f)
                        verticalLineToRelative(9.93f)
                        quadToRelative(0f, 0.46f, -0.31f, 0.78f)
                        reflectiveQuadTo(17f, 20.14f)
                        reflectiveQuadTo(16.22f, 19.83f)
                        reflectiveQuadTo(15.91f, 19.05f)
                        close()
                        moveToRelative(4.18f, -0.01f)
                        verticalLineTo(7.36f)
                        lineTo(18.28f, 5.55f)
                        quadTo(17.77f, 5.03f, 18.04f, 4.36f)
                        reflectiveQuadTo(19.05f, 3.7f)
                        quadToRelative(0.22f, 0f, 0.43f, 0.09f)
                        reflectiveQuadToRelative(0.35f, 0.23f)
                        lineTo(21.62f, 5.8f)
                        quadToRelative(0.32f, 0.31f, 0.49f, 0.73f)
                        reflectiveQuadTo(22.28f, 7.4f)
                        verticalLineTo(19.05f)
                        quadToRelative(0f, 0.46f, -0.31f, 0.78f)
                        reflectiveQuadToRelative(-0.78f, 0.31f)
                        reflectiveQuadTo(20.41f, 19.83f)
                        reflectiveQuadTo(20.09f, 19.05f)
                        close()
                        moveTo(3.91f, 17.96f)
                        horizontalLineTo(6.72f)
                        horizontalLineTo(8.91f)
                        horizontalLineToRelative(2.81f)
                        horizontalLineTo(7.82f)
                        horizontalLineTo(3.91f)
                        close()
                        moveTo(7.04f, 12.83f)
                        quadTo(6.72f, 12.51f, 6.72f, 12.05f)
                        reflectiveQuadTo(7.04f, 11.27f)
                        reflectiveQuadTo(7.82f, 10.96f)
                        reflectiveQuadTo(8.6f, 11.27f)
                        reflectiveQuadToRelative(0.31f, 0.78f)
                        reflectiveQuadTo(8.6f, 12.83f)
                        reflectiveQuadTo(7.82f, 13.14f)
                        reflectiveQuadTo(7.04f, 12.83f)
                        close()
                    }
                }
                .build()
        return _holiday_village!!
    }

private var _holiday_village: ImageVector? = null
