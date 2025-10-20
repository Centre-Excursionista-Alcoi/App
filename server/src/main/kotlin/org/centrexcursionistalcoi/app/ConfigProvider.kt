package org.centrexcursionistalcoi.app

import org.jetbrains.annotations.VisibleForTesting

abstract class ConfigProvider {
    private val override = mutableMapOf<String, String>()

    @VisibleForTesting
    fun override(name: String, value: String?) {
        if (value == null) {
            override.remove(name)
        } else {
            override[name] = value
        }
    }

    protected fun getenv(name: String): String? = override[name] ?: System.getenv(name)?.ifBlank { null }
}
