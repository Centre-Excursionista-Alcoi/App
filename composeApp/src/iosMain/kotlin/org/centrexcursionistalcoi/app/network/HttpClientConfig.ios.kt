package org.centrexcursionistalcoi.app.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig

expect val customizeDarwinConfig: DarwinClientEngineConfig.() -> Unit

actual fun createHttpClientEngine(): HttpClientEngine {
    return Darwin.create {
        customizeDarwinConfig()
    }
}
