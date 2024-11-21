package org.centrexcursionistalcoi.app.network

import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.ItemBooking
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.server.request.LendingRequest

object InventoryBackend {
    suspend fun listTypes() = Backend.get("/inventory/types", ListSerializer(ItemTypeD.serializer()))
        .map(ItemType::deserialize)

    suspend fun create(itemType: ItemType) = Backend.post(
        path = "/inventory/types",
        body = itemType.serializable(),
        bodySerializer = ItemTypeD.serializer()
    )

    suspend fun update(itemType: ItemType) = Backend.patch(
        path = "/inventory/types",
        body = itemType.serializable(),
        bodySerializer = ItemTypeD.serializer()
    )

    suspend fun listItems() = Backend.get("/inventory/items", ListSerializer(ItemD.serializer()))
        .map(Item::deserialize)

    suspend fun listItems(filterIds: Set<Int>) = Backend.get(
        "/inventory/items?filterItems=${filterIds.joinToString(",")}",
        ListSerializer(ItemD.serializer())
    ).map(Item::deserialize)

    suspend fun create(item: Item) = Backend.post(
        path = "/inventory/items",
        body = item.serializable(),
        bodySerializer = ItemD.serializer()
    )

    suspend fun update(item: Item) = Backend.patch(
        path = "/inventory/items",
        body = item.serializable(),
        bodySerializer = ItemD.serializer()
    )

    suspend fun availability(from: LocalDate, to: LocalDate) = Backend.get(
        "/availability?from=${from}&to=${to}",
        ListSerializer(ItemD.serializer())
    ).map(Item::deserialize)

    /**
     * Lists all the bookings made by the user.
     */
    suspend fun listBookings() = Backend.get("/lendings", ListSerializer(ItemLendingD.serializer()))
        .map(ItemBooking::deserialize)

    /**
     * Lists all the bookings made by all the users ever.
     * Must be admin.
     */
    suspend fun allBookings() = Backend.get("/lendings?all=true", ListSerializer(ItemLendingD.serializer()))
        .map(ItemBooking::deserialize)

    suspend fun getBooking(id: Int) = Backend.get(
        path = "/lending/$id",
        deserializer = ItemLendingD.serializer()
    ).let(ItemBooking::deserialize)

    suspend fun book(from: LocalDate, to: LocalDate, itemIds: Set<Int>) = Backend.post(
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

    suspend fun cancelBooking(bookingId: Int) = Backend.delete(
        path = "/lending/$bookingId"
    )
}
