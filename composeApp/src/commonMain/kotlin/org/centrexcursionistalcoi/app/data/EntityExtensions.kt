package org.centrexcursionistalcoi.app.data

import io.github.aakira.napier.Napier
import io.ktor.client.request.forms.formData
import io.ktor.http.content.PartData
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.storage.fs.PlatformFileSystem
import org.centrexcursionistalcoi.app.utils.isZero

suspend fun <Id: Any> Entity<Id>.toFormData(): List<PartData> {
    val dataMap = toMap().mapValues { (_, value) ->
        when (value) {
            is FileReference -> {
                when (val id = id) {
                    is Long if id <= 0L -> {
                        // New item, file is in memory
                        InMemoryFileAllocator.delete(value.uuid)
                    }

                    is Uuid if id.isZero() -> {
                        // New item, file is in memory
                        InMemoryFileAllocator.delete(value.uuid)
                    }

                    else -> {
                        PlatformFileSystem.read(value.uuid.toString())
                    }
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
                is ByteArray -> append(key, value)
                else -> Napier.e { "Unsupported type: ${value::class.simpleName ?: "N/A"}" }
            }
        }
    }
}
