package org.centrexcursionistalcoi.app.database

import java.time.Instant
import java.time.format.DateTimeFormatter
import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.javatime.JavaInstantColumnType
import org.slf4j.LoggerFactory

object DatabaseNowExpression : Function<Instant>(JavaInstantColumnType()) {
    private val logger = LoggerFactory.getLogger(DatabaseNowExpression::class.java)
    private val fixedTime = ThreadLocal<Instant?>()

    fun mockTime(instant: Instant) {
        fixedTime.set(instant)
    }

    fun reset() {
        fixedTime.remove()
    }

    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        val mock = fixedTime.get()

        if (mock != null) {
            // If mocked, output the timestamp as a SQL string literal.
            // use ISO-8601 strings (e.g. '2023-11-27T10:00:00Z')
            logger.info("Using mocked CURRENT_TIMESTAMP: $mock")
            append("'")
            append(DateTimeFormatter.ISO_INSTANT.format(mock))
            append("'")
        } else {
            append("CURRENT_TIMESTAMP")
        }
    }
}
