package org.centrexcursionistalcoi.app.utils

enum class FileType {
    PNG,
    JPEG,
    PDF
}

// PNG: 89 50 4E 47 0D 0A 1A 0A
private val pngSignature = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
)

// JPEG: FF D8 FF (Common signature for JPEG files)
// Note: JPEG always starts with FF D8 (SOI), usually followed by another FF for a marker.
private val jpegSignature = byteArrayOf(
    0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()
)

// PDF: 25 50 44 46 2D (%PDF-)
private val pdfSignature = byteArrayOf(
    0x25, 0x50, 0x44, 0x46, 0x2D
)

// Helper function to check if the data starts with the signature
private fun matchSignature(data: ByteArray, signature: ByteArray): Boolean {
    if (data.size < signature.size) return false

    for (i in signature.indices) {
        if (data[i] != signature[i]) {
            return false
        }
    }
    return true
}

/**
 * Detects the file type based on the byte array signature.
 */
fun detectFileType(data: ByteArray): FileType? {
    return when {
        matchSignature(data, pngSignature) -> FileType.PNG
        matchSignature(data, jpegSignature) -> FileType.JPEG
        matchSignature(data, pdfSignature) -> FileType.PDF
        else -> null
    }
}
