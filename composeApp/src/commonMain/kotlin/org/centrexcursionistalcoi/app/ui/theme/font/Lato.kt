package org.centrexcursionistalcoi.app.ui.theme.font

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import cea_app.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font

val Lato @Composable get()= FontFamily(
    Font(Res.font.Lato_Regular, FontWeight.Normal, FontStyle.Normal),
    Font(Res.font.Lato_Italic, FontWeight.Normal, FontStyle.Italic),
    Font(Res.font.Lato_Black, FontWeight.Black, FontStyle.Normal),
    Font(Res.font.Lato_BlackItalic, FontWeight.Black, FontStyle.Italic),
    Font(Res.font.Lato_Bold, FontWeight.Bold, FontStyle.Normal),
    Font(Res.font.Lato_BoldItalic, FontWeight.Bold, FontStyle.Italic),
    Font(Res.font.Lato_Thin, FontWeight.Thin, FontStyle.Normal),
    Font(Res.font.Lato_ThinItalic, FontWeight.Thin, FontStyle.Italic),
    Font(Res.font.Lato_Light, FontWeight.Light, FontStyle.Normal),
    Font(Res.font.Lato_LightItalic, FontWeight.Light, FontStyle.Italic),
)
