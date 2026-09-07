package org.centrexcursionistalcoi.app.serializer

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TestZonedDateTimeSerializer {
    @Test
    fun testSerialization() {
        val zonedDateTime = ZonedDateTime(
            timeZone = TimeZone.of("Europe/Madrid"),
            date = LocalDate(2024, 6, 15),
            time = LocalTime(14, 30)
        )

        val serialized = Json.encodeToString(ZonedDateTimeSerializer, zonedDateTime)
        val deserialized = Json.decodeFromString(ZonedDateTimeSerializer, serialized)

        assertEquals(zonedDateTime, deserialized)
    }
}
