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
val MaterialSymbols.Hotel: ImageVector
    get() {
        if (_hotel != null) {
            return _hotel!!
        }
        _hotel =
            ImageVector.Builder(
                name = "hotel",
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
                        moveTo(2f, 19f)
                        quadTo(1.58f, 19f, 1.29f, 18.71f)
                        quadTo(1f, 18.43f, 1f, 18f)
                        verticalLineTo(5f)
                        quadTo(1f, 4.57f, 1.29f, 4.29f)
                        reflectiveQuadTo(2f, 4f)
                        quadTo(2.43f, 4f, 2.71f, 4.29f)
                        reflectiveQuadTo(3f, 5f)
                        verticalLineToRelative(9f)
                        horizontalLineToRelative(8f)
                        verticalLineTo(8f)
                        quadTo(11f, 7.18f, 11.59f, 6.59f)
                        reflectiveQuadTo(13f, 6f)
                        horizontalLineToRelative(6f)
                        quadToRelative(1.65f, 0f, 2.83f, 1.18f)
                        reflectiveQuadTo(23f, 10f)
                        verticalLineToRelative(8f)
                        quadToRelative(0f, 0.43f, -0.29f, 0.71f)
                        reflectiveQuadTo(22f, 19f)
                        reflectiveQuadTo(21.29f, 18.71f)
                        quadTo(21f, 18.43f, 21f, 18f)
                        verticalLineTo(16f)
                        horizontalLineTo(3f)
                        verticalLineToRelative(2f)
                        quadToRelative(0f, 0.43f, -0.29f, 0.71f)
                        reflectiveQuadTo(2f, 19f)
                        close()
                        moveTo(4.88f, 12.13f)
                        quadTo(4f, 11.25f, 4f, 10f)
                        reflectiveQuadTo(4.88f, 7.88f)
                        reflectiveQuadTo(7f, 7f)
                        reflectiveQuadTo(9.13f, 7.88f)
                        reflectiveQuadTo(10f, 10f)
                        reflectiveQuadTo(9.13f, 12.13f)
                        reflectiveQuadTo(7f, 13f)
                        reflectiveQuadTo(4.88f, 12.13f)
                        close()
                        moveTo(13f, 14f)
                        horizontalLineToRelative(8f)
                        verticalLineTo(10f)
                        quadTo(21f, 9.17f, 20.41f, 8.59f)
                        reflectiveQuadTo(19f, 8f)
                        horizontalLineTo(13f)
                        verticalLineToRelative(6f)
                        close()
                        moveTo(7.71f, 10.71f)
                        quadTo(8f, 10.43f, 8f, 10f)
                        quadTo(8f, 9.57f, 7.71f, 9.29f)
                        reflectiveQuadTo(7f, 9f)
                        quadTo(6.58f, 9f, 6.29f, 9.29f)
                        reflectiveQuadTo(6f, 10f)
                        reflectiveQuadToRelative(0.29f, 0.71f)
                        reflectiveQuadTo(7f, 11f)
                        reflectiveQuadTo(7.71f, 10.71f)
                        close()
                        moveTo(7f, 10f)
                        close()
                        moveTo(13f, 8f)
                        verticalLineToRelative(6f)
                        verticalLineTo(8f)
                        close()
                    }
                }
                .build()
        return _hotel!!
    }

private var _hotel: ImageVector? = null
