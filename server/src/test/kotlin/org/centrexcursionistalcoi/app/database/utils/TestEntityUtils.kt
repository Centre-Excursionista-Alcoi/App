package org.centrexcursionistalcoi.app.database.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.Department
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.Departments.displayName

class TestEntityUtils {
    @Test
    fun test_entity_serializer() = runTest {
        Database.init("r2dbc:h2:mem:///test;DB_CLOSE_DELAY=-1;")

        val department = Database {
            Department.insert {
                it[displayName] = "test"
            }
        }

        val serializer = Department.serializer()
        assertEquals("org.centrexcursionistalcoi.app.database.entity.Department", serializer.descriptor.serialName)

        val json = Json.encodeToString(serializer, department)
        val element = Json.decodeFromString(JsonElement.serializer(), json).jsonObject
        assertEquals(
            department.id.value,
            element.getValue("id").jsonPrimitive.int
        )
        assertEquals(
            department.displayName,
            element.getValue("displayName").jsonPrimitive.content
        )
    }
}
