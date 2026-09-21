package org.centrexcursionistalcoi.app.data

import io.ktor.http.content.PartData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.ReferencedEvent.Companion.referenced
import org.centrexcursionistalcoi.app.database.RoomConverters
import org.centrexcursionistalcoi.app.database.entity.EventEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.json

/** Event qualification requirements must survive every hop between the server's JSON and the local database. */
class TestEventQualificationRequirements {
    private val a = Uuid.random()
    private val b = Uuid.random()
    private val c = Uuid.random()

    private fun anEvent(requirements: List<List<Uuid>>) = Event(
        id = Uuid.random(),
        start = Instant.fromEpochMilliseconds(1_000_000),
        end = null,
        place = "Somewhere",
        title = "Climbing day",
        description = null,
        maxPeople = null,
        requiresConfirmation = false,
        requiresInsurance = false,
        department = Uuid.random(),
        image = null,
        userSubList = emptyList(),
        qualificationRequirements = requirements,
    )

    private fun List<PartData>.formItem(name: String): String? =
        filterIsInstance<PartData.FormItem>().firstOrNull { it.name == name }?.value

    // ---- Room ----

    @Test
    fun roomConverter_roundTrips() {
        val converters = RoomConverters()
        val groups = listOf(listOf(a), listOf(b, c))
        assertEquals(groups, converters.stringToUuidGroups(converters.uuidGroupsToString(groups)))
        assertEquals(emptyList(), converters.stringToUuidGroups(converters.uuidGroupsToString(emptyList())))
        assertNull(converters.uuidGroupsToString(null))
        assertNull(converters.stringToUuidGroups(null))
    }

    @Test
    fun toEntity_keepsRequirements() {
        val groups = listOf(listOf(a), listOf(b, c))
        assertEquals(groups, anEvent(groups).toEntity().qualificationRequirements)
    }

    @Test
    fun referenced_and_dereference_keepRequirements() {
        val groups = listOf(listOf(a), listOf(b, c))
        val event = anEvent(groups)
        assertEquals(groups, event.referenced(emptyList(), emptyList()).qualificationRequirements)
        assertEquals(event, event.referenced(emptyList(), emptyList()).dereference().copy(department = event.department))
    }

    // ---- Server JSON ----

    @Test
    fun event_decodesRequirements_andOldServersWithoutThem() {
        val groups = listOf(listOf(a), listOf(b, c))
        val encoded = json.encodeToString(Event.serializer(), anEvent(groups))
        assertEquals(groups, json.decodeFromString(Event.serializer(), encoded).qualificationRequirements)

        // A server that predates requirements doesn't send the field at all
        val withoutField = json.parseToJsonElement(encoded).let { element ->
            kotlinx.serialization.json.JsonObject(element.let { it as kotlinx.serialization.json.JsonObject } - "qualificationRequirements")
        }
        assertEquals(emptyList(), json.decodeFromString(Event.serializer(), withoutField.toString()).qualificationRequirements)
    }

    // ---- Form data (event creation) ----

    @Test
    fun toFormData_encodesNestedGroups_asJsonTheServerParses() {
        val groups = listOf(listOf(a), listOf(b, c))
        val value = anEvent(groups).toFormData().formItem("qualificationRequirements")
        assertIs<String>(value)
        // The server reads this field as an array of arrays of id strings
        val parsed = json.decodeFromString(ListSerializer(ListSerializer(String.serializer())), value)
        assertEquals(groups.map { group -> group.map { it.toString() } }, parsed)
    }

    @Test
    fun toFormData_emptyRequirements_areAnEmptyArray() {
        assertEquals("[]", anEvent(emptyList()).toFormData().formItem("qualificationRequirements"))
    }
}
