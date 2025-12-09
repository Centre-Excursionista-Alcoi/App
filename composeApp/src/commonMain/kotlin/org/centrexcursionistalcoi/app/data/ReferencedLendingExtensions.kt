package org.centrexcursionistalcoi.app.data

import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType.Companion.getType

fun Lending.referenced(users: List<UserData>, inventoryItemTypes: List<ReferencedInventoryItemType>) = ReferencedLending(
    id = this.id,
    user = users.getUser(userSub),
    timestamp = this.timestamp,
    confirmed = this.confirmed,
    taken = this.taken,
    givenBy = this.givenBy?.let { givenBy -> users.getUser(givenBy) },
    givenAt = this.givenAt,
    returned = this.returned,
    receivedItems = receivedItems,
    memorySubmitted = this.memorySubmitted,
    memorySubmittedAt = this.memorySubmittedAt,
    memory = this.memory,
    memoryPdf = this.memoryPdf,
    memoryReviewed = this.memoryReviewed,
    from = this.from,
    to = this.to,
    notes = this.notes,
    items = this.items.map { item ->
        val type = inventoryItemTypes.getType(item.type)
        item.referenced(type)
    },
    referencedEntity = this,
)
