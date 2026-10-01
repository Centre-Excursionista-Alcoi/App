package org.centrexcursionistalcoi.app.database

import io.sentry.ISpan
import io.sentry.Sentry
import io.sentry.SpanStatus
import org.jetbrains.exposed.v1.core.Key
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.GlobalStatementInterceptor
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Records every SQL statement as a span of the current Sentry span (the request's transaction, see
 * `configureSentryTracing()`), so a slow request shows which queries it spent its time on. Statements run outside any
 * transaction (startup, background tasks) aren't recorded.
 *
 * Loaded by Exposed for every transaction through `META-INF/services`. The span only carries the SQL with its
 * parameter placeholders, never their values.
 */
class SentryStatementInterceptor : GlobalStatementInterceptor {
    private val spanKey = Key<ISpan>()

    override fun beforeExecution(transaction: Transaction, context: StatementContext) {
        val parent = Sentry.getSpan() ?: return
        val span = parent.startChild("db.sql.query", context.sql(transaction))
        (transaction as? JdbcTransaction)?.db?.vendor?.let { span.setData("db.system", it) }
        transaction.putUserData(spanKey, span)
    }

    override fun afterExecution(
        transaction: Transaction,
        contexts: List<StatementContext>,
        executedStatement: PreparedStatementApi,
    ) {
        transaction.removeUserData(spanKey)?.let { (it as ISpan).finish(SpanStatus.OK) }
    }

    // A failed statement never reaches afterExecution, and is followed by a rollback
    override fun beforeRollback(transaction: Transaction) {
        transaction.removeUserData(spanKey)?.let { (it as ISpan).finish(SpanStatus.INTERNAL_ERROR) }
    }
}
