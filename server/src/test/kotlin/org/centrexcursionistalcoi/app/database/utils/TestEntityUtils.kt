package org.centrexcursionistalcoi.app.database.utils

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.serialization.getString
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.junit.jupiter.api.assertNull
import kotlin.test.Test
import kotlin.test.assertEquals

class TestEntityUtils {
    @Test
    fun test_encodeListToString() = runTest {
        Database.initForTests()

        val department1 = Database {
            DepartmentEntity.new {
                displayName = "department1"
            }
        }
        val department2 = Database {
            DepartmentEntity.new {
                displayName = "department2"
            }
        }
        val departments = listOf(department1, department2)

        val encoded = json.encodeEntityListToString(departments)
        val array = json.decodeFromString(JsonArray.serializer(), encoded)
        assertEquals(2, array.size)
        assertEquals("department1", array[0].jsonObject.getString("displayName"))
        assertEquals("department2", array[1].jsonObject.getString("displayName"))
    }

    @Test
    fun test_entity_serializer() = runTest {
        Database.initForTests()

        val department = Database {
            DepartmentEntity.new {
                displayName = "test"
            }
        }

        val serializer = DepartmentEntity.serializer()
        assertEquals("org.centrexcursionistalcoi.app.database.entity.DepartmentEntity", serializer.descriptor.serialName)

        val json = Json.encodeToString(serializer, department)
        val element = Json.decodeFromString(JsonElement.serializer(), json).jsonObject
        assertEquals(
            department.id.value,
            element.getValue("id").jsonPrimitive.content.toUUIDOrNull()
        )
        assertEquals(
            department.displayName,
            element.getValue("displayName").jsonPrimitive.content
        )
        assertNull(
            element["imageFile"]
        )
    }
}
