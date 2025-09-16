package org.centrexcursionistalcoi.app.storage

import org.publicvalue.multiplatform.oidc.tokenstore.SettingsStore

class WasmSettingsStore : SettingsStore {
    override suspend fun get(key: String): String? {
        return authSettings.getStringOrNull(key)
    }

    override suspend fun put(key: String, value: String) {
        authSettings.putString(key, value)
    }

    override suspend fun remove(key: String) {
        authSettings.remove(key)
    }

    override suspend fun clear() {
        authSettings.clear()
    }
}
