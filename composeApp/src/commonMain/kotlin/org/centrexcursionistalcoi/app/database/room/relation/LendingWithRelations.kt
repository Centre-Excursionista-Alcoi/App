package org.centrexcursionistalcoi.app.database.room.relation

import androidx.room3.Embedded
import androidx.room3.Junction
import androidx.room3.Relation
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.room.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.room.entity.LendingItemEntity
import org.centrexcursionistalcoi.app.database.room.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.room.entity.ReceivedItemEntity
import org.centrexcursionistalcoi.app.database.room.entity.UserEntity
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException

data class LendingWithRelations(
    @Embedded val lending: LendingEntity,
    // Nullable for the same reason as InventoryItemWithRelations.type: Room can't express a required to-one
    // relation, only "zero or more" -- the mapper treats a null `user` as a genuine data-integrity error.
    @Relation(parentColumns = ["userSub"], entityColumns = ["sub"])
    val user: UserEntity?,
    @Relation(parentColumns = ["givenBy"], entityColumns = ["sub"])
    val givenByUser: UserEntity?,
    @Relation(
        entity = InventoryItemEntity::class,
        parentColumns = ["id"],
        entityColumns = ["id"],
        associateBy = Junction(LendingItemEntity::class, parentColumns = ["lendingId"], entityColumns = ["itemId"]),
    )
    val items: List<InventoryItemWithRelations>,
    @Relation(parentColumns = ["id"], entityColumns = ["lending"])
    val receivedItems: List<ReceivedItemEntity>,
    // The reverse of MemoryEntity.lending: at most one memory currently links back to a given lending.
    @Relation(entity = MemoryEntity::class, parentColumns = ["id"], entityColumns = ["lending"])
    val memory: MemoryWithRelations?,
)

fun LendingWithRelations.toReferenced(): ReferencedLending {
    val referencedUser = user?.toUser() ?: throw MissingCrossReferenceException("User", lending.userSub)
    val referencedItems = items.map { it.toReferenced() }
    return ReferencedLending(
        id = lending.id,
        user = referencedUser,
        timestamp = lending.timestamp,
        confirmed = lending.confirmed,
        taken = lending.taken,
        givenBy = givenByUser?.toUser(),
        givenAt = lending.givenAt,
        returned = lending.returned,
        receivedItems = receivedItems.map { it.toReceivedItem() },
        memorySubmitted = lending.memorySubmitted,
        memorySubmittedAt = lending.memorySubmittedAt,
        memory = memory?.toReferenced(),
        memoryReviewed = lending.memoryReviewed,
        from = lending.fromDate,
        to = lending.toDate,
        notes = lending.notes,
        items = referencedItems,
    )
}
