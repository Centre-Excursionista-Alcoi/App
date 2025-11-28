package org.centrexcursionistalcoi.app.data

import com.diamondedge.logging.logging
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.headers
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
import kotlin.time.Instant

private val log = logging()

@OptIn(InternalSerializationApi::class)
fun <Id: Any> Entity<Id>.toFormData(): List<PartData> {
    return formData {
        toMap().forEach { (key, value) ->
            when (value) {
                null -> { /* ignore null values */ }
                is String -> append(key, value)
                is Uuid -> append(key, value.toString())
                is Number -> append(key, value)
                is Boolean -> append(key, value)
                is ByteArray -> append(key, value)
                is Instant -> append(key, value.toEpochMilliseconds())
                is List<*> -> {
                    if (value.isNotEmpty()) {
                        @Suppress("UNCHECKED_CAST")
                        val jsonArray = when (val item = value.first()) {
                            is String -> json.encodeToString(ListSerializer(String.serializer()), value as List<String>)
                            is Int -> json.encodeToString(ListSerializer(Int.serializer()), value as List<Int>)
                            is Long -> json.encodeToString(ListSerializer(Long.serializer()), value as List<Long>)
                            is Float -> json.encodeToString(ListSerializer(Float.serializer()), value as List<Float>)
                            is Double -> json.encodeToString(ListSerializer(Double.serializer()), value as List<Double>)
                            is Boolean -> json.encodeToString(ListSerializer(Boolean.serializer()), value as List<Boolean>)
                            is FileWithContext -> json.encodeToString(ListSerializer(FileWithContext.serializer()), value as List<FileWithContext>)
                            else -> {
                                error("Unsupported list item type at $key: ${item?.let { it::class.simpleName } ?: "N/A"}")
                            }
                        }
                        append(key, jsonArray)
                    } else {
                        append(key, "[]")
                    }
                }
                is FileReference -> {
                    var contentType: ContentType? = null
                    val data = if (InMemoryFileAllocator.contains(value.uuid)) {
                        // New item, file is in memory
                        InMemoryFileAllocator.delete(value.uuid).also {
                            contentType = it?.contentType
                        }?.bytes
                    } else {
                        // Existing item, read file from filesystem
                        FileSystem.read(value.uuid.toString())
                    }
                    if (data == null) {
                        log.e { "FileReference data is null for key: $key, uuid: ${value.uuid}" }
                        return@forEach
                    }
                    append(
                        key = key,
                        value = Base64.UrlSafe.encode(data),
                        headers {
                            contentType?.let { append(HttpHeaders.ContentType, it.toString()) }
                        },
                    )
                }
                else -> error("Unsupported type at $key: ${value::class.simpleName ?: "N/A"}")
            }
        }
    }
}
