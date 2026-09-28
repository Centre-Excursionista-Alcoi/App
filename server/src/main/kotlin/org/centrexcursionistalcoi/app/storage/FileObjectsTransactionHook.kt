package org.centrexcursionistalcoi.app.storage

import io.sentry.Sentry
import org.jetbrains.exposed.v1.core.Key
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementInterceptor
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.slf4j.LoggerFactory

/**
 * Keeps the objects in [FileStorage] consistent with the rows of the transaction it's registered in:
 * - objects uploaded for rows created in the transaction are deleted if it rolls back.
 * - objects of rows deleted (or replaced) in the transaction are only deleted once it has committed, so a rollback
 *   never leaves a row pointing at a deleted object.
 *
 * Get it with [of]: there's one per transaction.
 */
class FileObjectsTransactionHook private constructor(
    private val storage: () -> FileStorage,
) : StatementInterceptor {
    companion object {
        private val logger = LoggerFactory.getLogger(FileObjectsTransactionHook::class.java)

        private val KEY = Key<FileObjectsTransactionHook>()

        /**
         * Gets the hook of [transaction], registering one if it doesn't have it yet.
         */
        fun of(transaction: JdbcTransaction): FileObjectsTransactionHook =
            transaction.getUserData(KEY) ?: FileObjectsTransactionHook { FileStorageProvider.current }.also {
                transaction.putUserData(KEY, it)
                transaction.registerInterceptor(it)
            }
    }

    private val uploaded = mutableListOf<String>()
    private val toDelete = mutableListOf<String>()

    /**
     * Deletes the object at [key] if the transaction rolls back.
     */
    fun deleteOnRollback(key: String) {
        uploaded += key
    }

    /**
     * Deletes the object at [key] once the transaction commits.
     */
    fun deleteAfterCommit(key: String) {
        toDelete += key
    }

    // Transactions clear their user data on commit. Keep the hook, since it stays registered as an interceptor.
    override fun keepUserDataInTransactionStoreOnCommit(userData: Map<Key<*>, Any?>): Map<Key<*>, Any?> =
        mapOf(KEY to this)

    // Neither callback may throw: that would make Exposed roll back a transaction that has already committed.
    override fun afterCommit(transaction: Transaction) {
        val keys = toDelete.toList()
        toDelete.clear()
        uploaded.clear()
        keys.forEach(::safeDelete)
    }

    override fun afterRollback(transaction: Transaction) {
        val keys = uploaded.toList()
        uploaded.clear()
        toDelete.clear()
        keys.forEach(::safeDelete)
    }

    private fun safeDelete(key: String) {
        try {
            storage().delete(key)
        } catch (e: Exception) {
            logger.error("Could not delete object $key. It will stay orphaned in the storage.", e)
            Sentry.captureException(e)
        }
    }
}
