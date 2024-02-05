package ui.theme.fonts

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import app.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Suppress("unused")
object Roboto {
    val Black: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_black))

    val BlackItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_blackitalic))

    val Bold: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_bold))

    val BoldItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_bolditalic))

    val Italic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_italic))

    val Light: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_light))

    val LightItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_lightitalic))

    val Medium: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_medium))

    val MediumItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_mediumitalic))

    val Regular: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_regular))

    val Thin: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_thin))

    val ThinItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.roboto_thinitalic))
}
