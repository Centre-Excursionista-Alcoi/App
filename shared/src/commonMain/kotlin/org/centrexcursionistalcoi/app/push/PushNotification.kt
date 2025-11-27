package org.centrexcursionistalcoi.app.push

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.utils.toUuidOrNull

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
            val isSelf = (data["isSelf"] as? String?)?.toBoolean()
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
                    isSelf ?: throw IllegalArgumentException("Missing or invalid isSelf field in LendingConfirmed push notification data")
                    LendingConfirmed(lendingId, userSub, isSelf)
                }
                LendingCancelled.TYPE -> {
                    lendingId ?: throw IllegalArgumentException("Missing or invalid lendingId field in LendingCancelled push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in LendingCancelled push notification data")
                    isSelf ?: throw IllegalArgumentException("Missing or invalid isSelf field in LendingCancelled push notification data")
                    LendingCancelled(lendingId, userSub, isSelf)
                }
                LendingTaken.TYPE -> {
                    lendingId ?: throw IllegalArgumentException("Missing or invalid lendingId field in LendingTaken push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in LendingTaken push notification data")
                    isSelf ?: throw IllegalArgumentException("Missing or invalid isSelf field in LendingTaken push notification data")
                    LendingTaken(lendingId, userSub, isSelf)
                }
                LendingReturned.TYPE -> {
                    lendingId ?: throw IllegalArgumentException("Missing or invalid lendingId field in LendingReturned push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in LendingReturned push notification data")
                    isSelf ?: throw IllegalArgumentException("Missing or invalid isSelf field in LendingReturned push notification data")
                    LendingReturned(lendingId, userSub, isSelf)
                }
                LendingPartiallyReturned.TYPE -> {
                    lendingId ?: throw IllegalArgumentException("Missing or invalid lendingId field in LendingPartiallyReturned push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in LendingPartiallyReturned push notification data")
                    isSelf ?: throw IllegalArgumentException("Missing or invalid isSelf field in LendingPartiallyReturned push notification data")
                    LendingPartiallyReturned(lendingId, userSub, isSelf)
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

    fun toMap(): Map<String, String>

    sealed interface TargetedNotification : PushNotification {
        /**
         * The userSub of the user related to the notification.
         */
        val userSub: String

        /**
         * Alias for checking whether [userSub] corresponds to the current user.
         *
         * Determines whether the notification is about the current user or someone else (received by an admin, for example).
         */
        val isSelf: Boolean

        override fun toMap(): Map<String, String> = mapOf("userSub" to userSub, "isSelf" to isSelf.toString())

        fun notSelf(): TargetedNotification
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

        // For new lending requests, we always consider it as not self, since users don't receive notifications about new lendings
        override val isSelf: Boolean = false

        override fun notSelf() = this // never self
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

        // For memory uploads, we always consider it as not self, since users don't receive notifications about their own uploads
        override val isSelf: Boolean = false

        override fun notSelf() = this // never self
    }

    @Serializable
    class LendingCancelled(
        override val lendingId: Uuid,
        override val userSub: String,
        override val isSelf: Boolean,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "LendingCancelled"
        }

        override val type: String = TYPE

        override fun notSelf() = LendingCancelled(lendingId, userSub, isSelf = false)
    }

    @Serializable
    class LendingConfirmed(
        override val lendingId: Uuid,
        override val userSub: String,
        override val isSelf: Boolean,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "LendingConfirmed"
        }

        override val type: String = TYPE

        override fun notSelf() = LendingConfirmed(lendingId, userSub, isSelf = false)
    }

    @Serializable
    class LendingTaken(
        override val lendingId: Uuid,
        override val userSub: String,
        override val isSelf: Boolean,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "LendingTaken"
        }

        override val type: String = TYPE

        override fun notSelf() = LendingTaken(lendingId, userSub, isSelf = false)
    }

    @Serializable
    class LendingPartiallyReturned(
        override val lendingId: Uuid,
        override val userSub: String,
        override val isSelf: Boolean,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "LendingPartiallyReturned"
        }

        override val type: String = TYPE

        override fun notSelf() = LendingPartiallyReturned(lendingId, userSub, isSelf = false)
    }

    @Serializable
    class LendingReturned(
        override val lendingId: Uuid,
        override val userSub: String,
        override val isSelf: Boolean,
    ) : LendingUpdated {
        companion object {
            const val TYPE = "LendingReturned"
        }

        override val type: String = TYPE

        override fun notSelf() = LendingReturned(lendingId, userSub, isSelf = false)
    }

    @Serializable
    class NewPost(
        val postId: Uuid,
    ) : PushNotification {
        companion object {
            const val TYPE = "NewPost"
        }

        override val type: String = TYPE

        override fun toMap(): Map<String, String> = mapOf("postId" to postId.toString())
    }

    @Serializable
    class DepartmentJoinRequestUpdated(
        val requestId: Uuid,
        val departmentId: Uuid,
        override val userSub: String,
        override val isSelf: Boolean,
        val isConfirmed: Boolean,
    ) : TargetedNotification {
        companion object {
            const val TYPE = "DepartmentJoinRequestUpdated"
        }

        override val type: String = TYPE

        override fun toMap(): Map<String, String> = super.toMap() + mapOf("requestId" to requestId.toString(), "departmentId" to departmentId.toString(), "isConfirmed" to isConfirmed.toString())

        override fun notSelf() = DepartmentJoinRequestUpdated(requestId, departmentId, userSub, isSelf = false, isConfirmed)
    }

    @Serializable
    class NewEvent(
        val eventId: Uuid,
    ) : PushNotification {
        companion object {
            const val TYPE = "NewEvent"
        }

        override val type: String = TYPE

        override fun toMap(): Map<String, String> = mapOf("eventId" to eventId.toString())
    }
}
