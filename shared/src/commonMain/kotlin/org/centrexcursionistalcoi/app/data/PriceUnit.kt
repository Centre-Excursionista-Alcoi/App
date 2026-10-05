package org.centrexcursionistalcoi.app.data

/**
 * What a [CategoryPrice] is charged for.
 */
enum class PriceUnit {
    /** Charged once per night spent. A stay without nights (day use) is free. */
    PER_NIGHT,

    /** Charged once per calendar day touched by the stay (nights + 1). */
    PER_DAY,
}
