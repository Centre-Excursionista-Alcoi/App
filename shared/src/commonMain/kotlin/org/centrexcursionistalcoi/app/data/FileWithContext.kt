package org.centrexcursionistalcoi.app.data

import io.ktor.http.ContentType
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import org.centrexcursionistalcoi.app.serializer.ContentTypeSerializer

@Serializable
data class FileWithContext(
    @Serializable(Base64Serializer::class) val bytes: ByteArray,
    val name: String? = null,
    @Serializable(ContentTypeSerializer::class) val contentType: ContentType? = null,
    val lastModified: Instant? = null,
    val id: Uuid? = null,
) {
    companion object {
        fun FileWithContext?.isNullOrEmpty(): Boolean {
            return this == null || this.bytes.isEmpty()
        }

        fun ByteArray.wrapFile(
            name: String? = null,
            contentType: ContentType? = null,
            lastModified: Instant? = null,
            id: Uuid? = null,
        ): FileWithContext {
            return FileWithContext(
                bytes = this,
                name = name,
                contentType = contentType,
                lastModified = lastModified,
                id = id,
            )
        }
    }

    fun isEmpty(): Boolean {
        return bytes.isEmpty()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FileWithContext

        if (name != other.name) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + (lastModified?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }
}
