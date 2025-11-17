package org.centrexcursionistalcoi.app.network

import io.ktor.client.engine.darwin.DarwinClientEngineConfig

actual val customizeDarwinConfig: DarwinClientEngineConfig.() -> Unit = {
    // nothing
}
