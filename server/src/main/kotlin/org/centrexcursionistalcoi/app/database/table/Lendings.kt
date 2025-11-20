package org.centrexcursionistalcoi.app.database.table

import java.util.UUID
import kotlinx.serialization.SerializationStrategy
import org.centrexcursionistalcoi.app.data.LendingMemory
import org.centrexcursionistalcoi.app.data.ReceivedItem
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.utils.CustomTableSerializer
import org.centrexcursionistalcoi.app.database.utils.ViaLink
import org.centrexcursionistalcoi.app.database.utils.list
import org.centrexcursionistalcoi.app.database.utils.serializer
import org.centrexcursionistalcoi.app.json
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.json.json

object Lendings : UUIDTable("Lendings"), ViaLink<UUID, LendingEntity, UUID, InventoryItemEntity>, CustomTableSerializer<UUID, LendingEntity> {
    val userSub = reference("userSub", UserReferences, onDelete = ReferenceOption.CASCADE)
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
    val from = date("from")
    val to = date("to")

    val confirmed = bool("confirmed").default(false)

    val taken = bool("taken").default(false)
    val givenBy = reference("givenBy", UserReferences).nullable()
    val givenAt = timestamp("givenAt").nullable()

    val returned = bool("returned").default(false)

    val memorySubmitted = bool("memorySubmitted").default(false)
    val memorySubmittedAt = timestamp("memorySubmittedAt").nullable()
    val memory = json("memory", json, LendingMemory.serializer()).nullable()
    val memoryReviewed = bool("memoryReviewed").default(false)

    val notes = text("notes").nullable()

    init {
        check("from_is_before_to") { from lessEq to }
    }


    override val linkName: String = "items"

    override fun linkSerializer(): Pair<SerializationStrategy<InventoryItemEntity>, Boolean> =
        (InventoryItemEntity.serializer() to /* nullable */ false)

    override fun links(entity: LendingEntity): SizedIterable<InventoryItemEntity> = entity.items


    override fun columnSerializers(): Map<String, SerializationStrategy<*>> = mapOf(
        "receivedItems" to ReceivedItem.serializer().list()
    )

    context(_: JdbcTransaction)
    override fun extraColumns(entity: LendingEntity): Map<String, Any?> = mapOf(
        "receivedItems" to entity.receivedItems.map { it.toReceivedItem() }
    )
}
