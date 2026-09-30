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
val MaterialSymbols.Passkey: ImageVector
    get() {
        if (_passkey != null) {
            return _passkey!!
        }
        _passkey =
            ImageVector.Builder(
                name = "passkey",
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
                        moveTo(5f, 20f)
                        quadTo(4.18f, 20f, 3.59f, 19.41f)
                        reflectiveQuadTo(3f, 18f)
                        verticalLineTo(17.2f)
                        quadTo(3f, 16.35f, 3.44f, 15.64f)
                        quadTo(3.88f, 14.93f, 4.6f, 14.55f)
                        quadTo(6.15f, 13.77f, 7.75f, 13.39f)
                        reflectiveQuadTo(11f, 13f)
                        quadToRelative(0.35f, 0f, 0.7f, 0.01f)
                        reflectiveQuadToRelative(0.7f, 0.06f)
                        quadToRelative(0.28f, 0.03f, 0.44f, 0.21f)
                        reflectiveQuadTo(13f, 13.75f)
                        quadToRelative(0.05f, 1.17f, 0.58f, 2.21f)
                        reflectiveQuadToRelative(1.4f, 1.76f)
                        quadToRelative(0.17f, 0.13f, 0.27f, 0.31f)
                        reflectiveQuadToRelative(0.1f, 0.41f)
                        verticalLineTo(19f)
                        quadToRelative(0f, 0.43f, -0.29f, 0.71f)
                        quadTo(14.78f, 20f, 14.35f, 20f)
                        horizontalLineTo(5f)
                        close()
                        moveToRelative(6f, -8f)
                        quadTo(9.35f, 12f, 8.18f, 10.83f)
                        reflectiveQuadTo(7f, 8f)
                        reflectiveQuadTo(8.18f, 5.18f)
                        reflectiveQuadTo(11f, 4f)
                        reflectiveQuadToRelative(2.83f, 1.18f)
                        reflectiveQuadTo(15f, 8f)
                        reflectiveQuadToRelative(-1.17f, 2.82f)
                        reflectiveQuadTo(11f, 12f)
                        close()
                        moveToRelative(8.21f, 1.71f)
                        quadTo(19.5f, 13.43f, 19.5f, 13f)
                        reflectiveQuadTo(19.21f, 12.29f)
                        reflectiveQuadTo(18.5f, 12f)
                        reflectiveQuadToRelative(-0.71f, 0.29f)
                        reflectiveQuadTo(17.5f, 13f)
                        reflectiveQuadToRelative(0.29f, 0.71f)
                        reflectiveQuadTo(18.5f, 14f)
                        reflectiveQuadToRelative(0.71f, -0.29f)
                        close()
                        moveToRelative(-0.56f, 8.94f)
                        lineToRelative(-1f, -1f)
                        quadTo(17.6f, 21.6f, 17.5f, 21.3f)
                        verticalLineTo(16.85f)
                        quadTo(16.4f, 16.52f, 15.7f, 15.61f)
                        reflectiveQuadTo(15f, 13.5f)
                        quadToRelative(0f, -1.45f, 1.03f, -2.48f)
                        reflectiveQuadTo(18.5f, 10f)
                        reflectiveQuadToRelative(2.48f, 1.02f)
                        reflectiveQuadTo(22f, 13.5f)
                        quadToRelative(0f, 1.13f, -0.64f, 2f)
                        reflectiveQuadToRelative(-1.61f, 1.25f)
                        lineToRelative(0.9f, 0.9f)
                        quadTo(20.8f, 17.8f, 20.8f, 18f)
                        reflectiveQuadToRelative(-0.15f, 0.35f)
                        lineToRelative(-0.8f, 0.8f)
                        quadTo(19.7f, 19.3f, 19.7f, 19.5f)
                        reflectiveQuadToRelative(0.15f, 0.35f)
                        lineToRelative(0.8f, 0.8f)
                        quadTo(20.8f, 20.8f, 20.8f, 21f)
                        reflectiveQuadToRelative(-0.15f, 0.35f)
                        lineToRelative(-1.3f, 1.3f)
                        quadTo(19.2f, 22.8f, 19f, 22.8f)
                        reflectiveQuadTo(18.65f, 22.65f)
                        close()
                    }
                }
                .build()
        return _passkey!!
    }

private var _passkey: ImageVector? = null
