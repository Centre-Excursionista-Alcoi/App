package org.centrexcursionistalcoi.app.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java

actual fun createHttpClientEngine(): HttpClientEngine {
    return Java.create()
}
