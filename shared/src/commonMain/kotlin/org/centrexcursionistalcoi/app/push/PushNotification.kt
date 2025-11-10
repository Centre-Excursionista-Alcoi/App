package org.centrexcursionistalcoi.app.push

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.utils.toUuidOrNull

@Serializable
sealed interface PushNotification {
    companion object {
        fun fromData(data: Map<String, *>): PushNotification {
            val type = data["type"] ?: throw IllegalArgumentException("Missing type field in push notification data")

            val lendingId = (data["lendingId"] as? String?)?.toUuidOrNull()
            val userSub = data["userSub"] as? String?

            return when (type) {
                NewLendingRequest.TYPE -> {
                    lendingId ?: throw IllegalArgumentException("Missing or invalid lendingId field in NewLendingRequest push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in NewLendingRequest push notification data")
                    NewLendingRequest(lendingId, userSub)
                }
                NewMemoryUpload.TYPE -> {
                    lendingId ?: throw IllegalArgumentException("Missing or invalid lendingId field in NewMemoryUpload push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in NewMemoryUpload push notification data")
                    NewMemoryUpload(lendingId, userSub)
                }
                LendingConfirmed.TYPE -> {
                    lendingId ?: throw IllegalArgumentException("Missing or invalid lendingId field in LendingConfirmed push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in LendingConfirmed push notification data")
                    LendingConfirmed(lendingId, userSub)
                }
                LendingCancelled.TYPE -> {
                    lendingId ?: throw IllegalArgumentException("Missing or invalid lendingId field in LendingCancelled push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in LendingCancelled push notification data")
                    LendingCancelled(lendingId, userSub)
                }
                LendingTaken.TYPE -> {
                    lendingId ?: throw IllegalArgumentException("Missing or invalid lendingId field in LendingTaken push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in LendingTaken push notification data")
                    LendingTaken(lendingId, userSub)
                }
                LendingReturned.TYPE -> {
                    lendingId ?: throw IllegalArgumentException("Missing or invalid lendingId field in LendingReturned push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in LendingReturned push notification data")
                    LendingReturned(lendingId, userSub)
                }
                LendingPartiallyReturned.TYPE -> {
                    lendingId ?: throw IllegalArgumentException("Missing or invalid lendingId field in LendingPartiallyReturned push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in LendingPartiallyReturned push notification data")
                    LendingPartiallyReturned(lendingId, userSub)
                }
                else -> throw IllegalArgumentException("Unknown push notification type: $type")
            }
        }
    }

    val type: String

    fun toMap(): Map<String, String>

    @Serializable
    sealed interface LendingUpdated : PushNotification {
        /**
         * The ID of the lending that was updated/created.
         */
        val lendingId: Uuid

        /**
         * The userSub of the user related to the lending.
         */
        val userSub: String

        override fun toMap(): Map<String, String> = mapOf("lendingId" to lendingId.toString(), "userSub" to userSub)
    }

    @Serializable
    class NewLendingRequest(
        override val lendingId: Uuid,
        override val userSub: String,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "NewLendingRequest"
        }

        override val type: String = TYPE
    }

    @Serializable
    class NewMemoryUpload(
        override val lendingId: Uuid,
        override val userSub: String,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "NewMemoryUpload"
        }

        override val type: String = TYPE
    }

    @Serializable
    class LendingCancelled(
        override val lendingId: Uuid,
        override val userSub: String,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "LendingCancelled"
        }

        override val type: String = TYPE
    }

    @Serializable
    class LendingConfirmed(
        override val lendingId: Uuid,
        override val userSub: String,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "LendingConfirmed"
        }

        override val type: String = TYPE
    }

    @Serializable
    class LendingTaken(
        override val lendingId: Uuid,
        override val userSub: String,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "LendingTaken"
        }

        override val type: String = TYPE
    }

    @Serializable
    class LendingPartiallyReturned(
        override val lendingId: Uuid,
        override val userSub: String,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "LendingPartiallyReturned"
        }

        override val type: String = TYPE
    }

    @Serializable
    class LendingReturned(
        override val lendingId: Uuid,
        override val userSub: String,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "LendingReturned"
        }

        override val type: String = TYPE
    }
}
