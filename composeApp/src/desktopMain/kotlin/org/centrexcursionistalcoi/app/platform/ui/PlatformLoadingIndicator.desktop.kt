package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gabrieldrn.carbon.loading.Loading

@Composable
actual fun PlatformLoadingIndicator(modifier: Modifier) {
    Loading(modifier = modifier)
}
