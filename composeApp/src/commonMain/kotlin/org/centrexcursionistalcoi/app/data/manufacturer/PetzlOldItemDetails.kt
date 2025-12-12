package org.centrexcursionistalcoi.app.data.manufacturer

import androidx.compose.runtime.Composable
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

/**
 * Pre-2016 Petzl Serial Number format.
 */
@Serializable
data class PetzlOldItemDetails(
    val year: Int,
    val dayOfYear: Int,
    val month: Month, // Calculated automatically
    val inspectorCode: String,
    val increment: String,
    val fullSerial: String
) : BasePetzlItemDetails() {
    companion object {
        // Regex for old 11-digit format: YY DDD XX NNNN (or similar variations)
        // Group 1: YY (Year)
        // Group 2: DDD (Day of Year)
        // Group 3: The rest (Inspector code + Increment) - usually 6 chars total
        val SERIAL_REGEX = Regex("""^(\d{2})(\d{3})([A-Za-z0-9]+)$""")

        /**
         * Parses a pre-2016 Petzl serial number (11 digits).
         * Format: YY DDD XX NNNN
         * Example: "15041AB1234" -> Year: 2015, Day: 041, Month: FEB
         */
        fun parse(serialNumber: String): PetzlOldItemDetails? {
            if (serialNumber.length != 11) return null

            val matchResult = SERIAL_REGEX.find(serialNumber) ?: return null
            val (yearStr, dayStr, suffix) = matchResult.destructured

            return try {
                // 1. Parse Year (Assume 2000-2099)
                val year = 2000 + yearStr.toInt()

                // 2. Parse Day of Year
                val dayOfYear = dayStr.toInt()

                // 3. Calculate Month using KMP logic
                // Start at Jan 1st and add (dayOfYear - 1) days
                val startOfYear = LocalDate(year, Month.JANUARY, 1)
                val calculatedDate = startOfYear.plus(dayOfYear - 1, DateTimeUnit.DAY)
                val month = calculatedDate.month

                // 4. Split suffix
                val inspector = suffix.take(2)
                val increment = suffix.drop(2)

                PetzlOldItemDetails(
                    year = year,
                    dayOfYear = dayOfYear,
                    month = month,
                    inspectorCode = inspector,
                    increment = increment,
                    fullSerial = serialNumber
                )
            } catch (_: Exception) {
                // Handles invalid numbers or impossible dates (e.g. day 400)
                null
            }
        }
    }

    @Composable
    override fun DataShowcase() {
        // TODO
    }
}
