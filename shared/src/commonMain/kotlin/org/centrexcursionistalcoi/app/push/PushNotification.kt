package org.centrexcursionistalcoi.app.push

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import kotlin.reflect.KClass
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
            val eventId = (data["eventId"] as? String?)?.toUuidOrNull()
            val requestId = (data["requestId"] as? String?)?.toUuidOrNull()
            val departmentId = (data["departmentId"] as? String?)?.toUuidOrNull()
            val isConfirmed = (data["isConfirmed"] as? String?)?.toBoolean()
            val entityClass = data["entityClass"] as? String?
            val entityId = data["entityId"] as? String?
            val isCreate = (data["isCreate"] as? String?)?.toBoolean()

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
                DepartmentJoinRequestUpdated.TYPE -> {
                    requestId ?: throw IllegalArgumentException("Missing or invalid requestId field in DepartmentJoinRequestUpdated push notification data")
                    departmentId ?: throw IllegalArgumentException("Missing or invalid departmentId field in DepartmentJoinRequestUpdated push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in DepartmentJoinRequestUpdated push notification data")
                    isConfirmed ?: throw IllegalArgumentException("Missing or invalid isConfirmed field in DepartmentJoinRequestUpdated push notification data")
                    DepartmentJoinRequestUpdated(requestId, departmentId, userSub, isConfirmed)
                }
                DepartmentKicked.TYPE -> {
                    requestId ?: throw IllegalArgumentException("Missing or invalid requestId field in DepartmentJoinRequestUpdated push notification data")
                    departmentId ?: throw IllegalArgumentException("Missing or invalid departmentId field in DepartmentJoinRequestUpdated push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in DepartmentJoinRequestUpdated push notification data")
                    DepartmentKicked(requestId, departmentId, userSub)
                }
                EventAssistanceUpdated.TYPE -> {
                    eventId ?: throw IllegalArgumentException("Missing or invalid eventId field in EventAssistanceUpdated push notification data")
                    userSub ?: throw IllegalArgumentException("Missing or invalid userSub field in EventAssistanceUpdated push notification data")
                    isConfirmed ?: throw IllegalArgumentException("Missing or invalid isConfirmed field in EventAssistanceUpdated push notification data")
                    EventAssistanceUpdated(eventId, userSub, isConfirmed)
                }

                EntityUpdated.TYPE -> {
                    entityClass ?: throw IllegalArgumentException("Missing or invalid entityClass field in EntityUpdated push notification data")
                    entityId ?: throw IllegalArgumentException("Missing or invalid entityId field in EntityUpdated push notification data")
                    isCreate ?: throw IllegalArgumentException("Missing or invalid isCreate field in EntityUpdated push notification data")
                    EntityUpdated(entityClass, entityId, isCreate)
                }
                EntityDeleted.TYPE -> {
                    entityClass ?: throw IllegalArgumentException("Missing or invalid entityClass field in EntityDeleted push notification data")
                    entityId ?: throw IllegalArgumentException("Missing or invalid entityId field in EntityDeleted push notification data")
                    EntityDeleted(entityClass, entityId)
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
    sealed interface EventUpdated : PushNotification {
        /**
         * The ID of the event that was updated/created.
         */
        val eventId: Uuid

        override fun toMap(): Map<String, String> = super.toMap() + mapOf("eventId" to eventId.toString())
    }

    @Serializable
    class EventAssistanceUpdated(
        override val eventId: Uuid,
        override val userSub: String,
        val isConfirmed: Boolean,
    ) : EventUpdated, TargetedNotification {
        companion object {
            const val TYPE = "EventAssistanceUpdated"
        }

        override val type: String = TYPE

        // super.toMap cannot be used because it conflicts between EventUpdated and TargetedNotification
        override fun toMap(): Map<String, String> = mapOf(
            "type" to type,
            "userSub" to userSub,
            "eventId" to eventId.toString(),
            "isConfirmed" to isConfirmed.toString(),
        )
    }

    @Serializable
    open class EntityUpdated(
        val entityClass: String,
        val entityId: String,
        val isCreate: Boolean,
    ): PushNotification {
        companion object {
            const val TYPE = "EntityUpdated"
        }

        constructor(entityClass: KClass<*>, entityId: String, isCreate: Boolean): this(entityClass.simpleName!!, entityId, isCreate)

        val entityUuid: Uuid? get() = entityId.toUuidOrNull()

        override val type: String = TYPE

        override fun toMap(): Map<String, String> = super.toMap() + mapOf(
            "entityClass" to entityClass,
            "entityId" to entityId,
            "isCreate" to isCreate.toString(),
        )
    }

    @Serializable
    class EntityDeleted(
        val entityClass: String,
        val entityId: String,
    ): PushNotification {
        companion object {
            const val TYPE = "EntityDeleted"
        }

        constructor(entityClass: KClass<*>, entityId: String): this(entityClass.simpleName!!, entityId)

        val entityUuid: Uuid? get() = entityId.toUuidOrNull()

        override val type: String = TYPE

        override fun toMap(): Map<String, String> = super.toMap() + mapOf("entityClass" to entityClass, "entityId" to entityId)
    }
}
