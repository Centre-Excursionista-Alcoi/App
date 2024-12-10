package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.R
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun PlatformTheme(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp
    val isLandscape = screenWidth > screenHeight

    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(Res.drawable.CEA_Monochrome),
                contentDescription = stringResource(R.string.app_name),
                modifier = if (isLandscape) {
                    Modifier.fillMaxHeight(.8f)
                } else {
                    Modifier.fillMaxWidth(.8f)
                }
                    .align(Alignment.Center),
                alpha = .05f
            )
            content()
        }
    }
}
