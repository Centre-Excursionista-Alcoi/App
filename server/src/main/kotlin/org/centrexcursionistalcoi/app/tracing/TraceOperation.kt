package org.centrexcursionistalcoi.app.tracing

/**
 * The operations the server's Sentry spans record, the `op` Sentry groups them by. Add new ones here rather than
 * reusing one that doesn't fit, and prefer Sentry's conventional names
 * (https://develop.sentry.dev/sdk/telemetry/traces/span-operations/) when one exists, since Sentry builds some of its
 * insights on them.
 */
enum class TraceOperation(val value: String) {
    /** A request the server handles, see `configureSentryTracing()`. */
    HTTP_SERVER("http.server"),

    /** A request the server makes to another service, see [SentryHttpClientTracing]. */
    HTTP_CLIENT("http.client"),

    /** Downloading the body of a [HTTP_CLIENT] request's response. */
    HTTP_CLIENT_BODY("http.client.body"),

    /** A SQL statement, see `SentryStatementInterceptor`. */
    DB_SQL_QUERY("db.sql.query"),

    /** A section of the answer of `GET /sync`, see `SyncRoutes`. */
    SYNC_SECTION("sync.section"),

    /** A run of a background task, see `PeriodicWorker`. */
    TASK("task"),
}
