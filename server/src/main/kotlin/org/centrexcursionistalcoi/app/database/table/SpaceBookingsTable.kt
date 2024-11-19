package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.common.BookingTable

object SpaceBookingsTable : BookingTable() {
    // If the space requires a key, this field will be filled once the user takes it
    val key = reference("key_id", SpaceKeysTable).nullable()

    // Payment information
    val paid = bool("paid").default(false)
    val paymentReference = varchar("payment_reference", 255).nullable()
    val paymentDocument = binary("payment_document").nullable()

    val space = reference("space_id", SpacesTable)
}
