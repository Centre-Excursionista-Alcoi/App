package org.centrexcursionistalcoi.app.data

import io.github.aakira.napier.Napier
import io.ktor.client.request.forms.formData
import io.ktor.http.content.PartData
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.storage.fs.FileSystem

suspend fun <Id: Any> Entity<Id>.toFormData(): List<PartData> {
    val dataMap = toMap().mapValues { (_, value) ->
        when (value) {
            is FileReference -> {
                if (InMemoryFileAllocator.contains(value.uuid)) {
                    // New item, file is in memory
                    InMemoryFileAllocator.delete(value.uuid)
                } else {
                    // Existing item, read file from filesystem
                    FileSystem.read(value.uuid.toString())
                }
            }
            else -> value
        }
    }

    return formData {
        dataMap.forEach { (key, value) ->
            when (value) {
                null -> { /* ignore null values */ }
                is String -> append(key, value)
                is Uuid -> append(key, value.toString())
                is Number -> append(key, value)
                is Boolean -> append(key, value)
                // Encode ByteArray as Base64 string
                is ByteArray -> append(key, Base64.UrlSafe.encode(value))
                else -> Napier.e { "Unsupported type: ${value::class.simpleName ?: "N/A"}" }
            }
        }
    }
}
