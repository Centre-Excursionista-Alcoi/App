package org.centrexcursionistalcoi.app.test

import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.jetbrains.exposed.v1.dao.Entity as ExposedEntity

class TestCaseContext<EID: Any, EE: ExposedEntity<EID>>(
    val entity: EE?
): ApplicationTestBase() {
    /**
     * Append the entity ID to the given path. For example:
     * ```kotlin
     * "/departments".withEntityId() // "/departments/1"
     * ```
     * @receiver The base path to which the entity ID will be appended.
     * @return The base path with the entity ID appended.
     */
    fun String.withEntityId(): String {
        requireNotNull(entity) { "Entity is null" }
        return this + "/${entity.id.value}"
    }
}
