package org.centrexcursionistalcoi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import org.centrexcursionistalcoi.app.ui.theme.font.Lato

val AppTypography: Typography @Composable get() = MaterialTheme.typography.apply {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Lato),
        displayMedium = displayMedium.copy(fontFamily = Lato),
        displaySmall = displaySmall.copy(fontFamily = Lato),
        headlineLarge = headlineLarge.copy(fontFamily = Lato),
        headlineMedium = headlineMedium.copy(fontFamily = Lato),
        headlineSmall = headlineSmall.copy(fontFamily = Lato),
        titleLarge = titleLarge.copy(fontFamily = Lato),
        titleMedium = titleMedium.copy(fontFamily = Lato),
        titleSmall = titleSmall.copy(fontFamily = Lato),
        bodyLarge = bodyLarge.copy(fontFamily = Lato),
        bodyMedium = bodyMedium.copy(fontFamily = Lato),
        bodySmall = bodySmall.copy(fontFamily = Lato),
        labelLarge = labelLarge.copy(fontFamily = Lato),
        labelMedium = labelMedium.copy(fontFamily = Lato),
        labelSmall = labelSmall.copy(fontFamily = Lato),
    )
}
