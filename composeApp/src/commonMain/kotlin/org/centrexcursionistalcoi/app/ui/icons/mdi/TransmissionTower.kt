package org.centrexcursionistalcoi.app.ui.icons.mdi

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MDI.TransmissionTower: ImageVector
    get() {
        if (_TransmissionTower != null) {
            return _TransmissionTower!!
        }
        _TransmissionTower = ImageVector.Builder(
            name = "TransmissionTower",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.28f, 5.45f)
                lineTo(6.5f, 4.55f)
                lineTo(7.76f, 2f)
                horizontalLineTo(16.23f)
                lineTo(17.5f, 4.55f)
                lineTo(15.72f, 5.44f)
                lineTo(15f, 4f)
                horizontalLineTo(9f)
                lineTo(8.28f, 5.45f)
                moveTo(18.62f, 8f)
                horizontalLineTo(14.09f)
                lineTo(13.3f, 5f)
                horizontalLineTo(10.7f)
                lineTo(9.91f, 8f)
                horizontalLineTo(5.38f)
                lineTo(4.1f, 10.55f)
                lineTo(5.89f, 11.44f)
                lineTo(6.62f, 10f)
                horizontalLineTo(17.38f)
                lineTo(18.1f, 11.45f)
                lineTo(19.89f, 10.56f)
                lineTo(18.62f, 8f)
                moveTo(17.77f, 22f)
                horizontalLineTo(15.7f)
                lineTo(15.46f, 21.1f)
                lineTo(12f, 15.9f)
                lineTo(8.53f, 21.1f)
                lineTo(8.3f, 22f)
                horizontalLineTo(6.23f)
                lineTo(9.12f, 11f)
                horizontalLineTo(11.19f)
                lineTo(10.83f, 12.35f)
                lineTo(12f, 14.1f)
                lineTo(13.16f, 12.35f)
                lineTo(12.81f, 11f)
                horizontalLineTo(14.88f)
                lineTo(17.77f, 22f)
                moveTo(11.4f, 15f)
                lineTo(10.5f, 13.65f)
                lineTo(9.32f, 18.13f)
                lineTo(11.4f, 15f)
                moveTo(14.68f, 18.12f)
                lineTo(13.5f, 13.64f)
                lineTo(12.6f, 15f)
                lineTo(14.68f, 18.12f)
                close()
            }
        }.build()

        return _TransmissionTower!!
    }

@Suppress("ObjectPropertyName")
private var _TransmissionTower: ImageVector? = null
