package org.centrexcursionistalcoi.app.database.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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
        val json = Json.encodeToString(Department.serializer(Departments), department)
        assertEquals("{\"display_name\":\"test\"}", json)
    }
}
