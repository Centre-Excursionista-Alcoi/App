package org.centrexcursionistalcoi.app.test

import java.time.Instant
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.mockTime
import org.centrexcursionistalcoi.app.resetTimeFunctions
import org.centrexcursionistalcoi.app.test.TestCase.Companion.withEntity
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.exists
import org.junit.jupiter.api.DynamicTest
import org.jetbrains.exposed.v1.dao.Entity as ExposedEntity

data class TestCase<EID: Any, EE: ExposedEntity<EID>>(
    val name: String,
    val block: (TestCaseContext<EID, EE>.() -> Unit)? = null,
    val skip: Boolean = false,
    val before: (() -> Unit)? = null,
    val after: (() -> Unit)? = null,
    val entityProvider: (JdbcTransaction.() -> EE)? = null,
    val auxiliaryEntitiesProvider: (JdbcTransaction.() -> Unit)? = null,
) {
    companion object {
        /**
         * Provide an entity instance to be used in the test case.
         *
         * This creates a new [TestCase] with the provided entity provider.
         * @param provider A function that returns an instance of the entity.
         * @return A new [TestCase] with the entity provider set.
         */
        infix fun <_EID: Any, _EE: ExposedEntity<_EID>> String.withEntity(provider: JdbcTransaction.() -> _EE): TestCase<_EID, _EE> {
            return TestCase(this, entityProvider = provider)
        }

        infix fun String.withEntities(provider: JdbcTransaction.() -> Unit): TestCase<Unit, ExposedEntity<Unit>> {
            return TestCase(this, auxiliaryEntitiesProvider = provider)
        }

        infix fun String.runs(block: TestCaseContext<Unit, ExposedEntity<Unit>>.() -> Unit): TestCase<Unit, ExposedEntity<Unit>> {
            return TestCase(this, block)
        }
    }

    /**
     * @see Companion.withEntity
     */
    infix fun <_EID: Any, _EE: ExposedEntity<_EID>> withEntity(entityProvider: JdbcTransaction.() -> _EE): TestCase<_EID, _EE> {
        @Suppress("UNCHECKED_CAST")
        return TestCase(
            name,
            block as (TestCaseContext<_EID, _EE>.() -> Unit)?,
            skip,
            before,
            after,
            entityProvider,
            auxiliaryEntitiesProvider
        )
    }

    infix fun withEntities(provider: JdbcTransaction.() -> Unit): TestCase<EID, EE> {
        return this.copy(auxiliaryEntitiesProvider = provider)
    }

    infix fun skipIf(condition: Boolean): TestCase<EID, EE> {
        return this.copy(skip = condition)
    }

    infix fun before(block: () -> Unit): TestCase<EID, EE> {
        return this.copy(before = block)
    }

    infix fun after(block: () -> Unit): TestCase<EID, EE> {
        return this.copy(after = block)
    }

    infix fun runs(block: TestCaseContext<EID, EE>.() -> Unit): TestCase<EID, EE> {
        return this.copy(block = block)
    }

    fun createDynamicTest(at: Instant?): DynamicTest? {
        if (skip) return null
        requireNotNull(block) { "No block has been provided." }
        return DynamicTest.dynamicTest(name) {
            var context: TestCaseContext<EID, EE>? = null
            try {
                at?.let(::mockTime)
                before?.invoke()
                if (!Database.isInitialized()) Database.init(TEST_URL)
                auxiliaryEntitiesProvider?.let { Database { it() } }
                val entity = entityProvider?.let { Database { it() } }
                context = TestCaseContext(entity)
                block(context)
            } finally {
                context?.entity?.let { entity ->
                    Database {
                        if (!entity.id.table.exists()) return@Database
                        println("Cleaning up entity ${entity::class.simpleName} with ID ${entity.id.value}")
                        entity.delete()
                    }
                }
                Database.clear()
                after?.invoke()
                resetTimeFunctions()
            }
        }
    }
}
