package org.centrexcursionistalcoi.app.data

import io.github.aakira.napier.Napier
import io.ktor.client.request.forms.formData
import io.ktor.http.content.PartData
import org.centrexcursionistalcoi.app.storage.fs.PlatformFileSystem

suspend fun <Id: Any> Entity<Id>.toFormData(): List<PartData> {
    val dataMap = toMap().mapValues { (_, value) ->
        when (value) {
            is FileReference -> {
                val id = id
                if (id is Long && id <= 0L) {
                    // New item, file is in temp/
                    PlatformFileSystem.read("temp/${value.uuid}")
                } else {
                    PlatformFileSystem.read(value.uuid.toString())
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
                is Number -> append(key, value)
                is Boolean -> append(key, value)
                is ByteArray -> append(key, value)
                else -> Napier.e { "Unsupported type: ${value::class.simpleName ?: "N/A"}" }
            }
        }
    }
}
