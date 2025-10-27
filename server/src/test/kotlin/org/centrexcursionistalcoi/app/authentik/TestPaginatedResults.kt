package org.centrexcursionistalcoi.app.authentik

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.json

class TestPaginatedResults {
    @Test
    fun test_serialization() {
        val string = ResourcesUtils.bytesFromResource("/authentik/paginated-results.json").decodeToString()
        // Check that serialization is successful (no errors are thrown)
        val paginatedResults = json.decodeFromString(AuthentikPaginatedResults.serializer(AuthentikUser.serializer()), string)
        paginatedResults.pagination.apply {
            assertEquals(0, next)
            assertEquals(0, previous)
            assertEquals(1, count)
            assertEquals(1, current)
            assertEquals(1, totalPages)
            assertEquals(1, startIndex)
            assertEquals(1, endIndex)
        }
        assertEquals(1, paginatedResults.results.size)
    }
}
