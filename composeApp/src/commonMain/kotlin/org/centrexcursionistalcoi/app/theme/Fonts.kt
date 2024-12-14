package org.centrexcursionistalcoi.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import ceaapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font

val barlow @Composable get() = FontFamily(
    Font(Res.font.Barlow_Black, weight = FontWeight.Black, style = FontStyle.Normal),
    Font(Res.font.Barlow_BlackItalic, weight = FontWeight.Black, style = FontStyle.Italic),

    Font(Res.font.Barlow_ExtraBold, weight = FontWeight.ExtraBold, style = FontStyle.Normal),
    Font(Res.font.Barlow_ExtraBoldItalic, weight = FontWeight.ExtraBold, style = FontStyle.Italic),

    Font(Res.font.Barlow_Bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(Res.font.Barlow_BoldItalic, weight = FontWeight.Bold, style = FontStyle.Italic),

    Font(Res.font.Barlow_SemiBold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(Res.font.Barlow_SemiBoldItalic, weight = FontWeight.SemiBold, style = FontStyle.Italic),

    Font(Res.font.Barlow_Medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(Res.font.Barlow_MediumItalic, weight = FontWeight.Medium, style = FontStyle.Italic),

    Font(Res.font.Barlow_Regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(Res.font.Barlow_Italic, weight = FontWeight.Normal, style = FontStyle.Italic),

    Font(Res.font.Barlow_Light, weight = FontWeight.Light, style = FontStyle.Normal),
    Font(Res.font.Barlow_LightItalic, weight = FontWeight.Light, style = FontStyle.Italic),

    Font(Res.font.Barlow_ExtraLight, weight = FontWeight.ExtraLight, style = FontStyle.Normal),
    Font(Res.font.Barlow_ExtraLightItalic, weight = FontWeight.ExtraLight, style = FontStyle.Italic),

    Font(Res.font.Barlow_Thin, weight = FontWeight.Thin, style = FontStyle.Normal),
    Font(Res.font.Barlow_ThinItalic, weight = FontWeight.Thin, style = FontStyle.Italic),
)
