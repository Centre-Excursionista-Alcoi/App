package org.centrexcursionistalcoi.app.test

import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.itemHref
import org.centrexcursionistalcoi.app.routes.EntityResources
import org.jetbrains.exposed.v1.dao.Entity as ExposedEntity

class TestCaseContext<EID: Any, EE: ExposedEntity<EID>>(
    val entity: EE?
): ApplicationTestBase() {
    /**
     * The path of the entity among these resources. For example:
     * ```kotlin
     * Api.Departments.resources.withEntityId() // "/departments/1"
     * ```
     */
    fun EntityResources<*, *>.withEntityId(): String {
        requireNotNull(entity) { "Entity is null" }
        return itemHref(entity.id.value)
    }
}
