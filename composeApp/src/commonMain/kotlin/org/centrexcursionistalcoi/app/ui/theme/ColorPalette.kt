package org.centrexcursionistalcoi.app.ui.theme

import androidx.compose.ui.graphics.Color

object ColorPalette {
    val primary = Color(0xff1e4b35)
    val secondary = Color(0xffd95d39)
    val tertiary = Color(0xff5a7353)
    val neutral = Color(0xff6d726d)

    val onPrimary = Color(0xffffffff)
    val onSecondary = Color(0xffffffff)
    val onTertiary = Color(0xffffffff)
    val onNeutral = Color(0xffffffff)

    val containerNeutral = Color(0xffE5E9E3)
    val onContainerNeutral = Color(0xff414943)

    val success = Color(0xff023420)
    val warning = Color(0xffA73918)

    val containerWarning = Color(0xffFFF1ED)
    val containerWarningBorder = Color(0xffFFCABB)
    val onContainerWarning = onContainerNeutral

    object Style {
        val containerNeutral = ColorStyle(onContainerNeutral, ColorPalette.containerNeutral)
        val containerNeutralDisabled = ColorStyle(onContainerNeutral.copy(alpha = .7f), ColorPalette.containerNeutral)
        val containerWarning = ColorStyle(onContainerWarning, onContainerWarning, containerWarningBorder)
    }
}
