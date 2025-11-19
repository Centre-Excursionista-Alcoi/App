package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration from version 1 to version 2:
 * - Post's ID type has changed from Int to UUID.
 * - DepartmentMembers's ID type has changed from Int to UUID.
 * - Departments's ID type has changed from Int to UUID.
 * To migrate:
 * 1. Rename existing ID columns to temporary names.
 * 2. Create new ID columns with UUID type.
 * 3. Random UUIDs have to be generated for existing records.
 * 4. Update foreign key references in related tables to point to the new UUID IDs:
 *   - Posts reference Departments. (ignored - see note below)
 *   - DepartmentMembers reference Departments.
 * 5. Drop the temporary ID columns.
 *
 * WE ONLY DO THE DEPARTMENTS MIGRATION, SINCE AT THIS MOMENT THERE'S NO IMPORTANT DATA IN THE DATABASE, AND WE CAN JUST DROP THE OTHER TABLES.
 *
 * See [commit](https://github.com/Centre-Excursionista-Alcoi/App/commit/5d0112229a1530b096d2270b2dc7bcedc8d63b90).
 */
object V2 : DatabaseMigration {
    override val from: Int = 1
    override val to: Int = 2

    context(tr: JdbcTransaction)
    override fun migrate() {
        // Departments migration:
        // - Table is called "departments"
        // - There's a pkey constraint created with:
        //   ALTER TABLE ONLY public.departments ADD CONSTRAINT departments_pkey PRIMARY KEY (id);
        tr.exec(
            """
                ALTER TABLE public.departments RENAME COLUMN id TO old_id;
                
                ALTER TABLE public.departments ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();
                
                ALTER TABLE public.departments DROP CONSTRAINT departments_pkey;
                
                ALTER TABLE public.departments ADD CONSTRAINT departments_pkey PRIMARY KEY (id);
            """.trimIndent()
        )
    }
}
