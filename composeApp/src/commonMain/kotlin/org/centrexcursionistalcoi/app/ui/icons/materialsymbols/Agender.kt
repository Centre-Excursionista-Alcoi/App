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
val MaterialSymbols.Agender: ImageVector
    get() {
        if (_agender != null) {
            return _agender!!
        }
        _agender =
            ImageVector.Builder(
                name = "agender",
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
                        moveTo(12f, 21f)
                        quadTo(9.5f, 21f, 7.75f, 19.25f)
                        reflectiveQuadTo(6f, 15f)
                        quadTo(6f, 12.75f, 7.44f, 11.09f)
                        reflectiveQuadTo(11f, 9.07f)
                        verticalLineTo(4f)
                        quadTo(11f, 3.57f, 11.29f, 3.29f)
                        reflectiveQuadTo(12f, 3f)
                        reflectiveQuadToRelative(0.71f, 0.29f)
                        reflectiveQuadTo(13f, 4f)
                        verticalLineTo(9.07f)
                        quadToRelative(2.15f, 0.35f, 3.57f, 2.01f)
                        reflectiveQuadTo(18f, 15f)
                        quadToRelative(0f, 2.5f, -1.75f, 4.25f)
                        reflectiveQuadTo(12f, 21f)
                        close()
                        moveToRelative(0f, -2f)
                        quadToRelative(1.4f, 0f, 2.46f, -0.85f)
                        reflectiveQuadTo(15.88f, 16f)
                        horizontalLineTo(8.13f)
                        quadToRelative(0.35f, 1.3f, 1.41f, 2.15f)
                        reflectiveQuadTo(12f, 19f)
                        close()
                        moveTo(8.13f, 14f)
                        horizontalLineToRelative(7.75f)
                        quadTo(15.53f, 12.7f, 14.46f, 11.85f)
                        reflectiveQuadTo(12f, 11f)
                        reflectiveQuadTo(9.54f, 11.85f)
                        quadTo(8.48f, 12.7f, 8.13f, 14f)
                        close()
                    }
                }
                .build()
        return _agender!!
    }

private var _agender: ImageVector? = null
