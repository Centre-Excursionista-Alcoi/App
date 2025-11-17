package org.centrexcursionistalcoi.app.network

import io.ktor.client.engine.darwin.DarwinClientEngineConfig

actual val customizeDarwinConfig: DarwinClientEngineConfig.() -> Unit = {
    configureRequest {
        // Avoid simulator network throttling
        setAllowsConstrainedNetworkAccess(true)
        setAllowsExpensiveNetworkAccess(true)

        // Required when calling local dev servers on emulator
        setAllowsCellularAccess(true)
    }
}
