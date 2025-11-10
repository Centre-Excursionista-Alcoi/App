package org.centrexcursionistalcoi.app.utils

class FileBytesWrapper(
    val bytes: ByteArray
) {
    companion object {
        fun ByteArray.wrapFile(): FileBytesWrapper = FileBytesWrapper(this)
    }
}
