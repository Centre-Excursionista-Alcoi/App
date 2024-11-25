package org.centrexcursionistalcoi.app.composition

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable

@Composable
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
actual fun calculateWindowSizeClass(): WindowSizeClass {
    return androidx.compose.material3.windowsizeclass.calculateWindowSizeClass()
}
