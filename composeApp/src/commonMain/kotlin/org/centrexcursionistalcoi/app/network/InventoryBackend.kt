package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.server.request.LendingRequest

object InventoryBackend {
    suspend fun listTypes() = Backend.get("/inventory/types", ListSerializer(ItemTypeD.serializer()))

    suspend fun create(itemType: ItemTypeD) = Backend.post(
        path = "/inventory/types",
        body = itemType,
        bodySerializer = ItemTypeD.serializer()
    )

    suspend fun update(itemType: ItemTypeD) = Backend.patch(
        path = "/inventory/types",
        body = itemType,
        bodySerializer = ItemTypeD.serializer()
    )

    suspend fun listItems() = Backend.get("/inventory/items", ListSerializer(ItemD.serializer()))

    suspend fun listItems(filterIds: Set<Int>) = Backend.get(
        "/inventory/items?filterItems=${filterIds.joinToString(",")}",
        ListSerializer(ItemD.serializer())
    )

    suspend fun create(item: ItemD) = Backend.post(
        path = "/inventory/items",
        body = item,
        bodySerializer = ItemD.serializer()
    )

    suspend fun update(item: ItemD) = Backend.patch(
        path = "/inventory/items",
        body = item,
        bodySerializer = ItemD.serializer()
    )

    suspend fun availability(from: Long, to: Long) = Backend.get(
        "/availability?from=${from}&to=${to}",
        ListSerializer(ItemD.serializer())
    )

    /**
     * Lists all the bookings made by the user.
     */
    suspend fun listBookings() = Backend.get("/lendings", ListSerializer(ItemLendingD.serializer()))

    /**
     * Lists all the bookings made by all the users ever.
     * Must be admin.
     */
    suspend fun allBookings() = Backend.get("/lendings?all=true", ListSerializer(ItemLendingD.serializer()))

    suspend fun book(from: Long, to: Long, itemIds: Set<Int>) = Backend.post(
        path = "/lending",
        body = LendingRequest(from, to, itemIds),
        bodySerializer = LendingRequest.serializer()
    )

    suspend fun confirm(bookingId: Int) = Backend.post(
        path = "/lending/$bookingId/confirm"
    )

    suspend fun markTaken(bookingId: Int) = Backend.post(
        path = "/lending/$bookingId/taken"
    )

    suspend fun markReturned(bookingId: Int) = Backend.post(
        path = "/lending/$bookingId/returned"
    )
}
