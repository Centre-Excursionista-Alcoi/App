package org.centrexcursionistalcoi.app.route

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Reservation(
    val from: String?,
    val to: String?,
    /** Ids of the selected items separated by , */
    val selectedItems: String?,
    /** The id of the selected space, if any */
    val selectedSpaceId: Int?,
    val lendingId: Int?,
    val spaceBookingId: Int?
) : Route {
    constructor(
        from: LocalDate,
        to: LocalDate,
        /** Ids of the selected items */
        selectedItems: Set<Int>,
        /** The id of the selected space, if any */
        selectedSpaceId: Int?
    ) : this(from.toString(), to.toString(), selectedItems.joinToString(","), selectedSpaceId, null, null)

    constructor(lendingId: Int?, spaceBookingId: Int?) : this(null, null, null, null, lendingId, spaceBookingId)

    init {
        require(isCorrect()) { "Reservation is not correct" }
    }

    /**
     * When a reservation is a draft, [lendingId] is null, but [from], [to] and [selectedItems] are not.
     * [selectedSpaceId] may be null if there is no space selected.
     *
     * @return true if the reservation is a draft, false otherwise.
     */
    fun isDraft(): Boolean = lendingId == null && spaceBookingId == null

    /**
     * A reservation is correct if it is a draft and [from], [to] and [selectedItems] are not null,
     * or if it is not a draft and [lendingId] or [spaceBookingId] is not null.
     */
    fun isCorrect(): Boolean {
        return if (isDraft()) {
            from != null && to != null && selectedItems != null
        } else {
            lendingId != null || spaceBookingId != null
        }
    }

    fun fromDate(): LocalDate? = from?.let(LocalDate::parse)
    fun toDate(): LocalDate? = to?.let(LocalDate::parse)

    fun selectedItemsSet(): Set<Int> = selectedItems
        ?.takeIf { it.isNotBlank() }
        ?.split(",")
        .orEmpty()
        .map(String::toInt)
        .toSet()
}
