package org.centrexcursionistalcoi.app.database.table

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.json
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

/**
 * A lending of a space. Stays are measured in nights: the nights spent are `[checkIn, checkOut)`, so one ending the
 * day another one starts doesn't collide with it. A stay with `checkIn == checkOut` is day use, with no nights.
 */
object SpaceLendings : UuidTable("space_lendings") {
    // ALL COLUMN NAMES MUST MATCH THE FIELD NAMES

    // The user who created the lending. It's null if the user is deleted, but the lending remains.
    val userSub = optReference("userSub", UserReferences, onDelete = ReferenceOption.SET_NULL)
    val space = reference("space", Spaces, onDelete = ReferenceOption.CASCADE)

    val timestamp = timestamp("timestamp").defaultExpression(DatabaseNowExpression)
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)
    val checkIn = date("checkIn")
    val checkOut = date("checkOut")

    // People of each category
    val attendees = jsonb("attendees", json, MapSerializer(Category.serializer(), Int.serializer()))
    val acceptedConditionsAt = timestamp("acceptedConditionsAt").nullable()
    val cancelled = bool("cancelled").default(false)
    val notes = text("notes").nullable()

    // Step 2: a manager hands the keys over. The lending cannot be modified after that.
    val pickedUpAt = timestamp("pickedUpAt").nullable()
    val pickedUpBy = optReference("pickedUpBy", UserReferences, onDelete = ReferenceOption.SET_NULL)

    // Step 3: a manager takes the keys back, and the lending gets paid.
    val returnedAt = timestamp("returnedAt").nullable()
    val returnedBy = optReference("returnedBy", UserReferences, onDelete = ReferenceOption.SET_NULL)

    val totalPrice = double("totalPrice").default(0.0)
    val paymentStatus = enumerationByName<PaymentStatus>("paymentStatus", 20).default(PaymentStatus.PENDING)

    // Report written after the stay: notes, and issues found
    val reportNotes = text("reportNotes").nullable()
    val reportIssues = text("reportIssues").nullable()
    val reportSubmittedAt = timestamp("reportSubmittedAt").nullable()

    init {
        check("space_lendings_check_in_is_before_check_out") { checkIn lessEq checkOut }
    }
}
