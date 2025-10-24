package org.centrexcursionistalcoi.app.sync

sealed class SyncResult(
    val outputData: Map<String, String>,
) {
    class Success(outputData: Map<String, String> = emptyMap()) : SyncResult(outputData)
    class Retry(outputData: Map<String, String> = emptyMap()) : SyncResult(outputData)
    class Failure(outputData: Map<String, String> = emptyMap()) : SyncResult(outputData) {
        constructor(message: String): this(mapOf("message" to message))
    }
}
