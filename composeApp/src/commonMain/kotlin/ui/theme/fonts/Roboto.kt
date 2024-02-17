package ui.theme.fonts

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.Roboto_Black
import app.composeapp.generated.resources.Roboto_BlackItalic
import app.composeapp.generated.resources.Roboto_Bold
import app.composeapp.generated.resources.Roboto_BoldItalic
import app.composeapp.generated.resources.Roboto_Italic
import app.composeapp.generated.resources.Roboto_Light
import app.composeapp.generated.resources.Roboto_LightItalic
import app.composeapp.generated.resources.Roboto_Medium
import app.composeapp.generated.resources.Roboto_MediumItalic
import app.composeapp.generated.resources.Roboto_Regular
import app.composeapp.generated.resources.Roboto_Thin
import app.composeapp.generated.resources.Roboto_ThinItalic
import org.jetbrains.compose.resources.Font

@Suppress("unused")
object Roboto {
    val Black: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_Black))

    val BlackItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_BlackItalic))

    val Bold: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_Bold))

    val BoldItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_BoldItalic))

    val Italic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_Italic))

    val Light: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_Light))

    val LightItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_LightItalic))

    val Medium: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_Medium))

    val MediumItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_MediumItalic))

    val Regular: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_Regular))

    val Thin: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_Thin))

    val ThinItalic: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.Roboto_ThinItalic))
}
