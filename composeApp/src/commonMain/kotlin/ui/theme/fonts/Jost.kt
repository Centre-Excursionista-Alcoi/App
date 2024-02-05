package ui.theme.fonts

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import app.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Suppress("unused")
object Jost {
    val Black: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_black))

    val BlackItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_blackitalic))

    val Bold: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_bold))

    val BoldItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_bolditalic))

    val ExtraBold: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_extrabold))

    val ExtraBoldItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_extrabolditalic))

    val ExtraLight: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_extralight))

    val ExtraLightItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_extralightitalic))

    val Italic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_italic))

    val Light: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_light))

    val LightItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_lightitalic))

    val Medium: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_medium))

    val MediumItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_mediumitalic))

    val Regular: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_regular))

    val SemiBold: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_semibold))

    val SemiBoldItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_semibolditalic))

    val Thin: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_thin))

    val ThinItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.jost_thinitalic))
}
