package org.centrexcursionistalcoi.app.storage

import java.io.Closeable

interface StoreMap: Closeable {
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String): String?

    suspend fun clear()
}
