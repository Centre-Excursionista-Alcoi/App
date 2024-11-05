package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import com.gabrieldrn.carbon.foundation.text.LocalCarbonTypography

@Composable
actual fun getPlatformTextStyles(): PlatformTextStyles {
    val typo = LocalCarbonTypography.current
    return PlatformTextStyles(
        titleLarge = typo.display01,
        titleRegular = typo.display02,
        heading = typo.heading01,
        body = typo.body01,
        label = typo.label01
    )
}
