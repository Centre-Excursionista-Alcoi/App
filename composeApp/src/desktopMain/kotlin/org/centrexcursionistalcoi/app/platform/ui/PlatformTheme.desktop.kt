package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import com.gabrieldrn.carbon.CarbonDesignSystem

@Composable
actual fun PlatformTheme(content: @Composable () -> Unit) {
    CarbonDesignSystem {
        content()
    }
}
