package org.centrexcursionistalcoi.app.push

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import kotlin.uuid.Uuid

@Serializable
sealed interface PushNotification {
    companion object {
        /**
         * Creates a [PushNotification] from the given [data] map.
         * @throws IllegalArgumentException if the data is invalid or missing required fields.
         */
        fun fromData(data: Map<String, *>): PushNotification {
            val type = data["type"] ?: throw IllegalArgumentException("Missing type field in push notification data")

            val lendingId = (data["lendingId"] as? String?)?.toUuidOrNull()
            val userSub = data["userSub"] as? String?
            val postId = (data["postId"] as? String?)?.toUuidOrNull()
            val eventId = (data["eventId"] as? String?)?.toUuidOrNull()

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
                NewPost.TYPE -> {
                    postId ?: throw IllegalArgumentException("Missing or invalid postId field in NewPost push notification data")
                    NewPost(postId)
                }
                NewEvent.TYPE -> {
                    eventId ?: throw IllegalArgumentException("Missing or invalid eventId field in NewEvent push notification data")
                    NewEvent(eventId)
                }
                else -> throw IllegalArgumentException("Unknown push notification type: $type")
            }
        }
    }

    val type: String

    fun toMap(): Map<String, String> = mapOf("type" to type)

    sealed interface TargetedNotification : PushNotification {
        /**
         * The userSub of the user related to the notification.
         */
        val userSub: String

        override fun toMap(): Map<String, String> = super.toMap() + mapOf("userSub" to userSub)
    }

    @Serializable
    sealed interface LendingUpdated : TargetedNotification {
        /**
         * The ID of the lending that was updated/created.
         */
        val lendingId: Uuid

        override fun toMap(): Map<String, String> = super.toMap() + mapOf("lendingId" to lendingId.toString())
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

    @Serializable
    class NewPost(
        val postId: Uuid,
    ) : PushNotification {
        companion object {
            const val TYPE = "NewPost"
        }

        override val type: String = TYPE

        override fun toMap(): Map<String, String> = super.toMap() + mapOf("postId" to postId.toString())
    }

    @Serializable
    class DepartmentJoinRequestUpdated(
        val requestId: Uuid,
        val departmentId: Uuid,
        override val userSub: String,
        val isConfirmed: Boolean,
    ) : TargetedNotification {
        companion object {
            const val TYPE = "DepartmentJoinRequestUpdated"
        }

        override val type: String = TYPE

        override fun toMap(): Map<String, String> = super.toMap() + mapOf("requestId" to requestId.toString(), "departmentId" to departmentId.toString(), "isConfirmed" to isConfirmed.toString())
    }

    /**
     * Notifies a user that they have been kicked from a department.
     */
    @Serializable
    class DepartmentKicked(
        val requestId: Uuid,
        val departmentId: Uuid,
        override val userSub: String,
    ) : TargetedNotification {
        companion object {
            const val TYPE = "DepartmentKicked"
        }

        override val type: String = TYPE

        override fun toMap(): Map<String, String> = super.toMap() + mapOf("requestId" to requestId.toString(), "departmentId" to departmentId.toString())
    }

    @Serializable
    class NewEvent(
        val eventId: Uuid,
    ) : PushNotification {
        companion object {
            const val TYPE = "NewEvent"
        }

        override val type: String = TYPE

        override fun toMap(): Map<String, String> = super.toMap() + mapOf("eventId" to eventId.toString())
    }
}
