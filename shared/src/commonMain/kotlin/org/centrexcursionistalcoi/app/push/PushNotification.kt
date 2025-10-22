package org.centrexcursionistalcoi.app.push

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.utils.toUuidOrNull

@Serializable
sealed interface PushNotification {
    companion object {
        fun fromData(data: Map<String, String>): PushNotification {
            val type = data["type"] ?: throw IllegalArgumentException("Missing type field in push notification data")

            return when (type) {
                NewLendingRequest.TYPE -> {
                    val lendingId = data["lendingId"]?.toUuidOrNull()
                        ?: throw IllegalArgumentException("Missing or invalid lendingId field in NewLendingRequest push notification data")
                    NewLendingRequest(lendingId)
                }
                NewMemoryUpload.TYPE -> {
                    val lendingId = data["lendingId"]?.toUuidOrNull()
                        ?: throw IllegalArgumentException("Missing or invalid lendingId field in NewMemoryUpload push notification data")
                    NewMemoryUpload(lendingId)
                }
                else -> throw IllegalArgumentException("Unknown push notification type: $type")
            }
        }
    }

    val type: String

    fun toMap(): Map<String, String>

    @Serializable
    class NewLendingRequest(
        val lendingId: Uuid
    ) : PushNotification {
        companion object {
            const val TYPE = "NewLendingRequest"
        }

        override val type: String = TYPE

        override fun toMap(): Map<String, String> {
            return mapOf(
                "lendingId" to lendingId.toString()
            )
        }
    }

    @Serializable
    class NewMemoryUpload(
        val lendingId: Uuid
    ) : PushNotification {
        companion object {
            const val TYPE = "NewMemoryUpload"
        }

        override val type: String = TYPE

        override fun toMap(): Map<String, String> {
            return mapOf(
                "lendingId" to lendingId.toString()
            )
        }
    }
}
