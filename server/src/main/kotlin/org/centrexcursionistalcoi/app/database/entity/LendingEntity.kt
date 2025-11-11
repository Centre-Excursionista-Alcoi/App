package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.database.table.Lendings
import org.centrexcursionistalcoi.app.database.table.ReceivedItems
import org.centrexcursionistalcoi.app.push.PushNotification
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass

class LendingEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<LendingEntity>(Lendings)

    var userSub by UserReferenceEntity referencedOn Lendings.userSub
    var timestamp by Lendings.timestamp
    var confirmed by Lendings.confirmed

    var taken by Lendings.taken
    var givenBy by Lendings.givenBy
    var givenAt by Lendings.givenAt

    var returned by Lendings.returned
    val receivedItems by ReceivedItemEntity referrersOn ReceivedItems.lending

    var memorySubmitted by Lendings.memorySubmitted
    var memorySubmittedAt by Lendings.memorySubmittedAt
    var memoryDocument by FileEntity optionalReferencedOn Lendings.memoryDocument
    var memoryPlainText by Lendings.memoryPlainText
    var memoryReviewed by Lendings.memoryReviewed

    var from by Lendings.from
    var to by Lendings.to
    var notes by Lendings.notes

    val items by InventoryItemEntity via LendingItems

    /**
     * Create a new lending request push notification for this lending.
     */
    fun newNotification(): PushNotification.NewLendingRequest = Database {
        PushNotification.NewLendingRequest(
            lendingId = this@LendingEntity.id.value.toKotlinUuid(),
            userSub = this@LendingEntity.userSub.sub.value,
        )
    }

    fun confirmedNotification(): PushNotification.LendingConfirmed = Database {
        PushNotification.LendingConfirmed(
            lendingId = this@LendingEntity.id.value.toKotlinUuid(),
            userSub = this@LendingEntity.userSub.sub.value,
        )
    }

    fun cancelledNotification(): PushNotification.LendingCancelled = Database {
        PushNotification.LendingCancelled(
            lendingId = this@LendingEntity.id.value.toKotlinUuid(),
            userSub = this@LendingEntity.userSub.sub.value,
        )
    }

    fun takenNotification(isSelf: Boolean): PushNotification.LendingTaken = Database {
        PushNotification.LendingTaken(
            lendingId = this@LendingEntity.id.value.toKotlinUuid(),
            userSub = this@LendingEntity.userSub.sub.value,
            isSelf = isSelf,
        )
    }

    fun returnedNotification(isSelf: Boolean): PushNotification.LendingReturned = Database {
        PushNotification.LendingReturned(
            lendingId = this@LendingEntity.id.value.toKotlinUuid(),
            userSub = this@LendingEntity.userSub.sub.value,
            isSelf = isSelf,
        )
    }

    fun partialReturnNotification(isSelf: Boolean): PushNotification.LendingPartiallyReturned = Database {
        PushNotification.LendingPartiallyReturned(
            lendingId = this@LendingEntity.id.value.toKotlinUuid(),
            userSub = this@LendingEntity.userSub.sub.value,
            isSelf = isSelf,
        )
    }

    fun memoryAddedNotification(): PushNotification.NewMemoryUpload = Database {
        PushNotification.NewMemoryUpload(
            lendingId = this@LendingEntity.id.value.toKotlinUuid(),
            userSub = this@LendingEntity.userSub.sub.value,
        )
    }
}
