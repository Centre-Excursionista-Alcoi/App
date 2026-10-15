package org.centrexcursionistalcoi.app.utils

/**
 * Checks the optional specs and location of a space, throwing an [IllegalArgumentException] if any makes no sense.
 * [latitude] and [longitude] go together: both are set, or none.
 */
fun validateSpaceDetails(seats: Int?, beds: Int?, latitude: Double?, longitude: Double?, altitude: Double?) {
    require(seats == null || seats >= 0) { "seats cannot be negative" }
    require(beds == null || beds >= 0) { "beds cannot be negative" }
    require((latitude == null) == (longitude == null)) { "latitude and longitude must be set together" }
    require(latitude == null || latitude in -90.0..90.0) { "latitude must be between -90 and 90" }
    require(longitude == null || longitude in -180.0..180.0) { "longitude must be between -180 and 180" }
    require(altitude == null || altitude in -500.0..9000.0) { "altitude must be between -500 and 9000 metres" }
}
