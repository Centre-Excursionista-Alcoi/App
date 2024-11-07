package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gabrieldrn.carbon.loading.Loading
import com.gabrieldrn.carbon.loading.SmallLoading

@Composable
actual fun PlatformLoadingIndicator(modifier: Modifier, large: Boolean) {
    if (large) {
        Loading(modifier = modifier)
    } else {
        SmallLoading(modifier = modifier)
    }
}
