package ui.theme.fonts

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import dev.icerock.moko.resources.compose.fontFamilyResource
import resources.MR

@Suppress("unused")
object Roboto {
    val Black: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.black)

    val BlackItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.blackItalic)

    val Bold: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.bold)

    val BoldItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.boldItalic)

    val Italic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.italic)

    val Light: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.light)

    val LightItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.lightItalic)

    val Medium: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.medium)

    val MediumItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.mediumItalic)

    val Regular: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.regular)

    val Thin: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.thin)

    val ThinItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Roboto.thinItalic)
}
