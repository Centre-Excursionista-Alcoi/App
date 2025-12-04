package org.centrexcursionistalcoi.app.ui.icons.materialsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MaterialSymbols.SupervisorAccount: ImageVector
    get() {
        if (_SupervisorAccount != null) {
            return _SupervisorAccount!!
        }
        _SupervisorAccount = ImageVector.Builder(
            name = "SupervisorAccount",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(680f, 600f)
                quadToRelative(-42f, 0f, -71f, -29f)
                reflectiveQuadToRelative(-29f, -71f)
                quadToRelative(0f, -42f, 29f, -71f)
                reflectiveQuadToRelative(71f, -29f)
                quadToRelative(42f, 0f, 71f, 29f)
                reflectiveQuadToRelative(29f, 71f)
                quadToRelative(0f, 42f, -29f, 71f)
                reflectiveQuadToRelative(-71f, 29f)
                close()
                moveTo(480f, 760f)
                verticalLineToRelative(-16f)
                quadToRelative(0f, -24f, 12.5f, -44.5f)
                reflectiveQuadTo(528f, 670f)
                quadToRelative(36f, -15f, 74.5f, -22.5f)
                reflectiveQuadTo(680f, 640f)
                quadToRelative(39f, 0f, 77.5f, 7.5f)
                reflectiveQuadTo(832f, 670f)
                quadToRelative(23f, 9f, 35.5f, 29.5f)
                reflectiveQuadTo(880f, 744f)
                verticalLineToRelative(16f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(840f, 800f)
                lineTo(520f, 800f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(480f, 760f)
                close()
                moveTo(400f, 480f)
                quadToRelative(-66f, 0f, -113f, -47f)
                reflectiveQuadToRelative(-47f, -113f)
                quadToRelative(0f, -66f, 47f, -113f)
                reflectiveQuadToRelative(113f, -47f)
                quadToRelative(66f, 0f, 113f, 47f)
                reflectiveQuadToRelative(47f, 113f)
                quadToRelative(0f, 66f, -47f, 113f)
                reflectiveQuadToRelative(-113f, 47f)
                close()
                moveTo(400f, 320f)
                close()
                moveTo(80f, 688f)
                quadToRelative(0f, -34f, 17f, -62.5f)
                reflectiveQuadToRelative(47f, -43.5f)
                quadToRelative(60f, -30f, 124.5f, -46f)
                reflectiveQuadTo(400f, 520f)
                quadToRelative(35f, 0f, 70f, 6f)
                reflectiveQuadToRelative(70f, 14f)
                lineToRelative(-34f, 34f)
                lineToRelative(-34f, 34f)
                quadToRelative(-18f, -5f, -36f, -6.5f)
                reflectiveQuadToRelative(-36f, -1.5f)
                quadToRelative(-58f, 0f, -113.5f, 14f)
                reflectiveQuadTo(180f, 654f)
                quadToRelative(-10f, 5f, -15f, 14f)
                reflectiveQuadToRelative(-5f, 20f)
                verticalLineToRelative(32f)
                horizontalLineToRelative(240f)
                verticalLineToRelative(39f)
                quadToRelative(0f, 13f, 5f, 23.5f)
                reflectiveQuadToRelative(14f, 17.5f)
                lineTo(160f, 800f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(80f, 720f)
                verticalLineToRelative(-32f)
                close()
                moveTo(400f, 720f)
                close()
                moveTo(400f, 400f)
                quadToRelative(33f, 0f, 56.5f, -23.5f)
                reflectiveQuadTo(480f, 320f)
                quadToRelative(0f, -33f, -23.5f, -56.5f)
                reflectiveQuadTo(400f, 240f)
                quadToRelative(-33f, 0f, -56.5f, 23.5f)
                reflectiveQuadTo(320f, 320f)
                quadToRelative(0f, 33f, 23.5f, 56.5f)
                reflectiveQuadTo(400f, 400f)
                close()
            }
        }.build()

        return _SupervisorAccount!!
    }

@Suppress("ObjectPropertyName")
private var _SupervisorAccount: ImageVector? = null
