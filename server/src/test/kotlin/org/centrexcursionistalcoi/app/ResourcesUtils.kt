package org.centrexcursionistalcoi.app

object ResourcesUtils {
    fun bytesFromResource(path: String) = this::class.java.getResourceAsStream(path)!!.readBytes()
}
