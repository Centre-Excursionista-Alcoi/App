package org.centrexcursionistalcoi.app

import kotlinx.serialization.json.Json

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    explicitNulls = false
    classDiscriminator = "type"
    encodeDefaults = true
}
