package org.centrexcursionistalcoi.app.ui.theme.font

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import cea_app.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font

val Oswald @Composable get()= FontFamily(
    Font(Res.font.Oswald_Regular, FontWeight.Normal, FontStyle.Normal),
    Font(Res.font.Oswald_Bold, FontWeight.Bold, FontStyle.Normal),
    Font(Res.font.Oswald_SemiBold, FontWeight.SemiBold, FontStyle.Normal),
    Font(Res.font.Oswald_Light, FontWeight.Light, FontStyle.Normal),
    Font(Res.font.Oswald_ExtraLight, FontWeight.ExtraLight, FontStyle.Normal),
)
