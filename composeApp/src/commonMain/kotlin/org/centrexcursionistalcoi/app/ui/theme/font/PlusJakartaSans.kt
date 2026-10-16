package org.centrexcursionistalcoi.app.ui.theme.font

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import cea_app.composeapp.generated.resources.PlusJakartaSans_Bold
import cea_app.composeapp.generated.resources.PlusJakartaSans_BoldItalic
import cea_app.composeapp.generated.resources.PlusJakartaSans_ExtraBold
import cea_app.composeapp.generated.resources.PlusJakartaSans_ExtraBoldItalic
import cea_app.composeapp.generated.resources.PlusJakartaSans_ExtraLight
import cea_app.composeapp.generated.resources.PlusJakartaSans_ExtraLightItalic
import cea_app.composeapp.generated.resources.PlusJakartaSans_Italic
import cea_app.composeapp.generated.resources.PlusJakartaSans_Light
import cea_app.composeapp.generated.resources.PlusJakartaSans_LightItalic
import cea_app.composeapp.generated.resources.PlusJakartaSans_Medium
import cea_app.composeapp.generated.resources.PlusJakartaSans_MediumItalic
import cea_app.composeapp.generated.resources.PlusJakartaSans_Regular
import cea_app.composeapp.generated.resources.PlusJakartaSans_SemiBold
import cea_app.composeapp.generated.resources.PlusJakartaSans_SemiBoldItalic
import cea_app.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font

val PlusJakartaSans @Composable get() = FontFamily(
    Font(Res.font.PlusJakartaSans_Regular, FontWeight.Normal, FontStyle.Normal),
    Font(Res.font.PlusJakartaSans_Bold, FontWeight.Bold, FontStyle.Normal),
    Font(Res.font.PlusJakartaSans_BoldItalic, FontWeight.Bold, FontStyle.Italic),
    Font(Res.font.PlusJakartaSans_ExtraBold, FontWeight.ExtraBold, FontStyle.Normal),
    Font(Res.font.PlusJakartaSans_ExtraBoldItalic, FontWeight.ExtraBold, FontStyle.Italic),
    Font(Res.font.PlusJakartaSans_ExtraLight, FontWeight.ExtraLight, FontStyle.Normal),
    Font(Res.font.PlusJakartaSans_ExtraLightItalic, FontWeight.ExtraLight, FontStyle.Italic),
    Font(Res.font.PlusJakartaSans_Italic, FontWeight.Normal, FontStyle.Italic),
    Font(Res.font.PlusJakartaSans_Light, FontWeight.Light, FontStyle.Normal),
    Font(Res.font.PlusJakartaSans_LightItalic, FontWeight.Light, FontStyle.Italic),
    Font(Res.font.PlusJakartaSans_Medium, FontWeight.Medium, FontStyle.Normal),
    Font(Res.font.PlusJakartaSans_MediumItalic, FontWeight.Medium, FontStyle.Italic),
    Font(Res.font.PlusJakartaSans_SemiBold, FontWeight.SemiBold, FontStyle.Normal),
    Font(Res.font.PlusJakartaSans_SemiBoldItalic, FontWeight.SemiBold, FontStyle.Italic),
)
