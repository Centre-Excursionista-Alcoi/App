package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ceaapp.composeapp.generated.resources.*
import com.gabrieldrn.carbon.CarbonDesignSystem
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun PlatformTheme(content: @Composable () -> Unit) {
    CarbonDesignSystem {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(Res.drawable.CEA_Monochrome),
                contentDescription = "",
                modifier = Modifier.fillMaxHeight(.4f).align(Alignment.Center),
                alpha = .05f
            )
            content()
        }
    }
}
