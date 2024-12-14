package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toOffset
import ceaapp.composeapp.generated.resources.*
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.options.QrOptions
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.QrShapes
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.toImageBitmap
import kotlin.math.roundToInt
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.platform.ui.Action
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.PlatformScaffold
import org.centrexcursionistalcoi.app.route.Settings
import org.centrexcursionistalcoi.app.theme.barlow
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfilePage(
    user: UserD?,
    onLogout: () -> Unit
) {
    val navController = LocalNavController.current

    PlatformScaffold(
        actions = listOf(
            Action(
                Icons.Default.Settings,
                stringResource(Res.string.nav_settings)
            ) { navController.navigate(Settings) },
            Action(Icons.AutoMirrored.Default.Logout, stringResource(Res.string.logout), onClick = onLogout),
        )
    ) {
        AnimatedContent(
            targetState = user,
            modifier = Modifier.fillMaxHeight(.7f).align(Alignment.CenterHorizontally)
        ) { userData ->
            if (userData == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PlatformLoadingIndicator() }
            } else {
                ProfileCard(userData)
            }
        }
    }
}

private val originalSizeMm = Size(85.6f, 53.98f)

@Composable
fun ProfileCard(user: UserD, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    var cardSize by remember { mutableStateOf(DpSize.Unspecified) }

    fun calcFromHeight(originalMm: Double): Dp {
        // originalHeight -> originalMm
        // cardSize.height -> x
        val x = cardSize.height.value * originalMm / originalSizeMm.height
        return x.dp
    }

    fun Modifier.relativeHeight(originalMm: Double): Modifier = this.height(calcFromHeight(originalMm))

    Box(
        modifier = modifier
            .aspectRatio(originalSizeMm.width / originalSizeMm.height)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xffa9a587))
            .onSizeChanged { size ->
                cardSize = with(density) { DpSize(size.width.toDp(), size.height.toDp()) }
            }
    ) {
        // Background CEA Logo
        Image(
            painter = painterResource(Res.drawable.CEA_Monochrome),
            null,
            modifier = Modifier
                .relativeHeight(35.587)
                .aspectRatio(1f)
                .align(Alignment.Center),
            alpha = .3f,
            colorFilter = ColorFilter.tint(Color(0xffffffff))
        )

        val textMeasurer = rememberTextMeasurer()
        val barlow = barlow

        val pvGlyph = imageResource(Res.drawable.PV_Glyph)

        val qrCodePainter = QrCodePainter(
            data = user.vCard(),
            options = QrOptions(
                shapes = QrShapes(
                    darkPixel = QrPixelShape.roundCorners()
                )
            ),
        )
        val qrCode = qrCodePainter.toImageBitmap(512, 512)

        Canvas(Modifier.fillMaxSize()) {
            fun calcFromHeight(originalMm: Double): Float = (size.height * originalMm / originalSizeMm.height).toFloat()
            fun calcFromWidth(originalMm: Double): Float = (size.width * originalMm / originalSizeMm.width).toFloat()
            fun calcOffset(originalXMm: Double, originalYMm: Double): IntOffset {
                return IntOffset(
                    calcFromWidth(originalXMm).roundToInt(),
                    calcFromHeight(originalYMm).roundToInt()
                )
            }
            fun relativeFontSize(heightMm: Double): TextUnit = calcFromHeight(heightMm).sp
            fun textStyle(fontHeightMm: Double): TextStyle = TextStyle.Default.copy(
                fontFamily = barlow,
                fontWeight = FontWeight.SemiBold,
                fontSize = relativeFontSize(fontHeightMm),
                color = Color.White
            )

            // PV Glyph
            val backgroundHeight = calcFromHeight(15.0)
            drawImage(
                image = pvGlyph,
                dstOffset = calcOffset(0.0, 2.2),
                dstSize = IntSize(backgroundHeight.toInt(), backgroundHeight.toInt()),
                colorFilter = ColorFilter.tint(Color.White)
            )

            // User QR Code
            val qrHeight = calcFromHeight(15.0)
            drawImage(
                image = qrCode,
                dstOffset = calcOffset(67.0, 35.5),
                dstSize = IntSize(qrHeight.toInt(), qrHeight.toInt()),
                colorFilter = ColorFilter.tint(Color.White)
            )

            // CEA Text
            drawText(
                textMeasurer = textMeasurer,
                text = "Centre Excursionista\nd'Alcoi",
                style = textStyle(3.5),
                topLeft = calcOffset(13.0, 6.0).toOffset()
            )

            // User full name
            drawText(
                textMeasurer = textMeasurer,
                text = user.name + ' ' + user.familyName,
                style = textStyle(2.8),
                topLeft = calcOffset(6.929, 41.0).toOffset()
            )
            // User NIF
            drawText(
                textMeasurer = textMeasurer,
                text = user.nif,
                style = textStyle(2.8),
                topLeft = calcOffset(6.929, 45.0).toOffset()
            )
        }
    }
}
