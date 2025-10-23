package org.centrexcursionistalcoi.app.ui.theme.font

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import cea_app.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font

val Barlow @Composable get()= FontFamily(
    Font(Res.font.Barlow_Regular, FontWeight.Normal, FontStyle.Normal),
    Font(Res.font.Barlow_Italic, FontWeight.Normal, FontStyle.Italic),
    Font(Res.font.Barlow_Black, FontWeight.Black, FontStyle.Normal),
    Font(Res.font.Barlow_BlackItalic, FontWeight.Black, FontStyle.Italic),
    Font(Res.font.Barlow_ExtraBold, FontWeight.ExtraBold, FontStyle.Normal),
    Font(Res.font.Barlow_ExtraBoldItalic, FontWeight.ExtraBold, FontStyle.Italic),
    Font(Res.font.Barlow_Bold, FontWeight.Bold, FontStyle.Normal),
    Font(Res.font.Barlow_BoldItalic, FontWeight.Bold, FontStyle.Italic),
    Font(Res.font.Barlow_SemiBold, FontWeight.SemiBold, FontStyle.Normal),
    Font(Res.font.Barlow_SemiBoldItalic, FontWeight.SemiBold, FontStyle.Italic),
    Font(Res.font.Barlow_Thin, FontWeight.Thin, FontStyle.Normal),
    Font(Res.font.Barlow_ThinItalic, FontWeight.Thin, FontStyle.Italic),
    Font(Res.font.Barlow_Light, FontWeight.Light, FontStyle.Normal),
    Font(Res.font.Barlow_LightItalic, FontWeight.Light, FontStyle.Italic),
    Font(Res.font.Barlow_ExtraLight, FontWeight.ExtraLight, FontStyle.Normal),
    Font(Res.font.Barlow_ExtraLightItalic, FontWeight.ExtraLight, FontStyle.Italic),
)
