package org.centrexcursionistalcoi.app.data.manufacturer

import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.petzl
import kotlinx.datetime.Month
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.DrawableResource

@Serializable
data class PetzlItemDetails(
    val year: Int,
    val month: Month,
    val identifier: String,
): ManufacturerItemDetails {
    companion object {
        val SERIAL_REGEX = Regex("""^(\d{2})([A-L])(\d{10})$""")

        /*
        This code specifically targets the 13-character alphanumerical format (Format B) which you encountered.
        Older Petzl gear (pre-2016) used an 11-digit numeric format (YY DDD X NNNN) where the month was not explicitly encoded,
        but rather the Day of the Year (DDD). Since your data class requires a Month object, this parser safely returns null for those older,
        incompatible serials.
         */

        /**
         * Parses a 13-digit Petzl serial number (Format B, post-2016).
         * Example: "24I0596894448" -> Year: 2024, Month: SEPTEMBER, ID: 0596894448
         *
         * @return PetzlItemDetails if valid, null otherwise.
         */
        fun parse(serialNumber: String): PetzlItemDetails? {
            // Validate format using Regex
            val matchResult = SERIAL_REGEX.find(serialNumber) ?: return null

            val (yearStr, monthCharStr, uniqueId) = matchResult.destructured

            // Parse Year: Petzl 13-digit codes started ~2016, so we assume 20xx century.
            val year = 2000 + yearStr.toInt()

            // Parse Month: Petzl uses A=Jan, B=Feb, ..., L=Dec
            val monthChar = monthCharStr[0]
            val monthIndex = monthChar - 'A' // 0 for A, 1 for B...
            val month = Month(monthIndex + 1)

            return PetzlItemDetails(
                year = year,
                month = month,
                identifier = uniqueId
            )
        }
    }

    @Transient
    override val logo: DrawableResource = Res.drawable.petzl

    @Transient
    override val name: String = "Petzl"
}
