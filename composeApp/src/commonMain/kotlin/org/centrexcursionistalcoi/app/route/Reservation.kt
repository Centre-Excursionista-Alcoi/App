package org.centrexcursionistalcoi.app.route

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Reservation(
    /** LocalDate in epoch days */
    val from: Int,
    /** LocalDate in epoch days */
    val to: Int,
    /** Ids of the selected items separated by , */
    val selectedItems: String,
    /** The id of the selected space, if any */
    val selectedSpaceId: Int?
) : Route {
    constructor(from: LocalDate, to: LocalDate, selectedItems: Set<Int>, selectedSpaceId: Int?) : this(
        from.toEpochDays(),
        to.toEpochDays(),
        selectedItems.joinToString(","),
        selectedSpaceId
    )

    fun fromDate(): LocalDate = LocalDate.fromEpochDays(from)
    fun toDate(): LocalDate = LocalDate.fromEpochDays(to)
    fun selectedItemsSet(): Set<Int> = selectedItems
        .takeIf { it.isNotBlank() }
        ?.split(",")
        .orEmpty()
        .map(String::toInt)
        .toSet()
}
