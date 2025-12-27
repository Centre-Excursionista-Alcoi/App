package org.centrexcursionistalcoi.app.ui.platform

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import org.centrexcursionistalcoi.app.android.MainActivity

@Composable
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
actual fun calculateWindowSizeClass(): WindowSizeClass {
    MainActivity.instance?.let {
        return calculateWindowSizeClass(it)
    }
    return WindowSizeClass.calculateFromSize(DpSize.Zero)
}
