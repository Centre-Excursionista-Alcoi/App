package org.centrexcursionistalcoi.app.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

actual fun createHttpClientEngine(): HttpClientEngine {
    return Android.create()
}
