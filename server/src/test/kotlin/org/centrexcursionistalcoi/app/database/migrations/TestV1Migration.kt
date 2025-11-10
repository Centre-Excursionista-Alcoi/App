package org.centrexcursionistalcoi.app.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase

class TestV1Migration : PostgresTestBase() {
    @Suppress("DEPRECATION")
    @Test
    fun test() {
        // Original table
        Database.exec(
            """
                CREATE TABLE public.inventory_item_types (
                    id uuid NOT NULL,
                    "displayName" text NOT NULL,
                    description text,
                    category text,
                    image uuid
                );
            """.trimIndent()
        )
        // insert test data
        Database.exec(
            """
                INSERT INTO public.inventory_item_types (id, "displayName", description, category, image)
                VALUES ('123e4567-e89b-12d3-a456-426614174000', 'Item 1', 'Description 1', 'Category 1', NULL);
            """.trimIndent()
        )

        // Run migration
        Database {
            V1.migrate()
        }

        // Verify migration
        val rs = Database.execQuery("SELECT id, categories FROM public.inventory_item_types WHERE id='123e4567-e89b-12d3-a456-426614174000';")
        assertTrue { rs.next() }
        val categories = rs.getArray("categories")
        val array = categories.array as? Array<*> ?: error("Categories is not an array")
        assertEquals(1, array.size)
        assertEquals("Category 1", array[0])
    }
}
