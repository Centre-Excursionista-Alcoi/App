package ui.theme.fonts

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import app.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Suppress("unused")
object Jost {
    val Black: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_Black))

    val BlackItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_BlackItalic))

    val Bold: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_Bold))

    val BoldItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_BoldItalic))

    val ExtraBold: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_ExtraBold))

    val ExtraBoldItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_ExtraBoldItalic))

    val ExtraLight: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_ExtraLight))

    val ExtraLightItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_ExtraLightItalic))

    val Italic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_Italic))

    val Light: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_Light))

    val LightItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_LightItalic))

    val Medium: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_Medium))

    val MediumItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_MediumItalic))

    val Regular: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_Regular))

    val SemiBold: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_SemiBold))

    val SemiBoldItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_SemiBoldItalic))

    val Thin: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_Thin))

    val ThinItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Jost_ThinItalic))
}
