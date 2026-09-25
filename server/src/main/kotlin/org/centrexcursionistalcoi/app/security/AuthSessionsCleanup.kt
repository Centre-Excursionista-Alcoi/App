package org.centrexcursionistalcoi.app.security

import org.centrexcursionistalcoi.app.PeriodicWorker
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.AuthSessions
import org.centrexcursionistalcoi.app.now
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

/**
 * Deletes sessions (and, cascading, their refresh tokens) that can no longer be used. Expired and revoked sessions
 * are kept for [retention] first, to investigate abuse after the fact.
 */
object AuthSessionsCleanup : PeriodicWorker(period = 6.hours) {
    private val logger = LoggerFactory.getLogger(AuthSessionsCleanup::class.java)

    private val retention = 30.days

    override suspend fun run() {
        val threshold = now() - retention.toJavaDuration()
        val deleted = Database {
            AuthSessions.deleteWhere {
                (AuthSessions.expiresAt less threshold) or
                    (AuthSessions.absoluteExpiresAt less threshold) or
                    (AuthSessions.revokedAt less threshold)
            }
        }
        logger.info("Deleted $deleted expired or revoked sessions.")
    }
}
