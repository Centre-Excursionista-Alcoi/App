package platform

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual object ImageProcessor {
    /**
     * Process an image and return an [ImageBitmap] object.
     */
    actual fun processImage(image: ByteArray): ImageBitmap {
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
        return bitmap.asImageBitmap()
    }
}
