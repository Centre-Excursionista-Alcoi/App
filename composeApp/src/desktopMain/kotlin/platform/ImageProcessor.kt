package platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual object ImageProcessor {
    /**
     * Process an image and return an [ImageBitmap] object.
     */
    actual fun processImage(image: ByteArray): ImageBitmap {
        Image.makeFromEncoded(image).use { skiaImage ->
            return skiaImage.toComposeImageBitmap()
        }
    }
}
