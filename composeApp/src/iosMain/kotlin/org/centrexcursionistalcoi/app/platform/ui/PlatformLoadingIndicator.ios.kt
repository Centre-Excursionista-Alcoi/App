package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi

@Composable
@OptIn(ExperimentalCupertinoApi::class)
actual fun PlatformLoadingIndicator(modifier: Modifier, large: Boolean) {
    CupertinoActivityIndicator(modifier)
}
