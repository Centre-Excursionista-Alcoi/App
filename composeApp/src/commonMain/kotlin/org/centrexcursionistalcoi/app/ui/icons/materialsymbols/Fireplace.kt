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
val MaterialSymbols.Fireplace: ImageVector
    get() {
        if (_fireplace != null) {
            return _fireplace!!
        }
        _fireplace =
            ImageVector.Builder(
                name = "fireplace",
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
                        moveTo(10.6f, 16.95f)
                        quadToRelative(0.32f, 0.28f, 0.69f, 0.39f)
                        reflectiveQuadTo(12f, 17.45f)
                        quadToRelative(0.73f, 0f, 1.31f, -0.46f)
                        quadTo(13.9f, 16.52f, 14f, 15.65f)
                        quadToRelative(0.13f, -1.18f, -0.72f, -1.74f)
                        reflectiveQuadTo(12f, 12.45f)
                        quadTo(11.88f, 12.8f, 11.88f, 13.1f)
                        reflectiveQuadToRelative(0.08f, 0.65f)
                        quadToRelative(0.07f, 0.42f, 0.17f, 0.8f)
                        reflectiveQuadToRelative(0.03f, 0.8f)
                        quadTo(12.03f, 15.8f, 11.6f, 16.27f)
                        reflectiveQuadToRelative(-1f, 0.68f)
                        close()
                        moveTo(2f, 20f)
                        verticalLineTo(4f)
                        quadTo(2f, 3.17f, 2.59f, 2.59f)
                        reflectiveQuadTo(4f, 2f)
                        horizontalLineTo(20f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        reflectiveQuadTo(22f, 4f)
                        verticalLineTo(20f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(20f, 22f)
                        horizontalLineTo(4f)
                        quadTo(3.18f, 22f, 2.59f, 21.41f)
                        reflectiveQuadTo(2f, 20f)
                        close()
                        moveTo(12f, 18f)
                        quadToRelative(1.25f, 0f, 2.13f, -0.88f)
                        reflectiveQuadTo(15f, 15f)
                        quadToRelative(0f, -0.6f, -0.25f, -1f)
                        reflectiveQuadToRelative(-0.7f, -0.75f)
                        quadTo(13.1f, 12.58f, 12.46f, 11.84f)
                        reflectiveQuadTo(11.45f, 10.35f)
                        quadToRelative(-1.1f, 0.88f, -1.78f, 1.99f)
                        reflectiveQuadTo(9f, 14.95f)
                        quadToRelative(0f, 0.88f, 0.9f, 1.96f)
                        reflectiveQuadTo(12f, 18f)
                        close()
                        moveTo(4f, 20f)
                        horizontalLineTo(6f)
                        verticalLineTo(19f)
                        quadTo(6f, 18.58f, 6.29f, 18.29f)
                        reflectiveQuadTo(7f, 18f)
                        horizontalLineTo(8.25f)
                        quadTo(7.68f, 17.27f, 7.34f, 16.48f)
                        reflectiveQuadTo(7f, 14.95f)
                        quadTo(7f, 12.33f, 8.2f, 10.74f)
                        reflectiveQuadTo(11.58f, 7.85f)
                        quadTo(11.83f, 7.7f, 12.08f, 7.7f)
                        reflectiveQuadToRelative(0.47f, 0.13f)
                        reflectiveQuadToRelative(0.38f, 0.35f)
                        reflectiveQuadToRelative(0.18f, 0.5f)
                        quadToRelative(0.05f, 0.85f, 0.72f, 1.65f)
                        reflectiveQuadToRelative(1.32f, 1.28f)
                        quadToRelative(0.83f, 0.6f, 1.34f, 1.41f)
                        reflectiveQuadTo(17f, 15f)
                        quadToRelative(0f, 0.88f, -0.27f, 1.61f)
                        reflectiveQuadTo(16f, 18f)
                        horizontalLineToRelative(1f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(18f, 19f)
                        verticalLineToRelative(1f)
                        horizontalLineToRelative(2f)
                        verticalLineTo(4f)
                        horizontalLineTo(4f)
                        verticalLineTo(20f)
                        close()
                        moveToRelative(8f, -2f)
                        close()
                    }
                }
                .build()
        return _fireplace!!
    }

private var _fireplace: ImageVector? = null
