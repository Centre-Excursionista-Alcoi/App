package ui.theme.fonts

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import dev.icerock.moko.resources.compose.fontFamilyResource
import resources.MR

@Suppress("unused")
object Jost {
    val Black: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.black)

    val BlackItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.blackItalic)

    val Bold: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.bold)

    val BoldItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.boldItalic)

    val ExtraBold: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.extraBold)

    val ExtraBoldItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.extraBoldItalic)

    val ExtraLight: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.extraLight)

    val ExtraLightItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.extraLightItalic)

    val Italic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.italic)

    val Light: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.light)

    val LightItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.lightItalic)

    val Medium: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.medium)

    val MediumItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.mediumItalic)

    val Regular: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.regular)

    val SemiBold: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.semiBold)

    val SemiBoldItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.semiBoldItalic)

    val Thin: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.thin)

    val ThinItalic: FontFamily
        @Composable
        get() = fontFamilyResource(MR.fonts.Jost.thinItalic)
}
