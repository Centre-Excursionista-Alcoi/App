package platform

import androidx.compose.ui.graphics.ImageBitmap

expect object ImageProcessor {
    /**
     * Process an image and return an [ImageBitmap] object.
     */
    fun processImage(image: ByteArray): ImageBitmap
}
