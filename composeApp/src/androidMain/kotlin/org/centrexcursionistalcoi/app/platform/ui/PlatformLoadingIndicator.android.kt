package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformLoadingIndicator(modifier: Modifier, large: Boolean) {
    CircularProgressIndicator(modifier)
}
