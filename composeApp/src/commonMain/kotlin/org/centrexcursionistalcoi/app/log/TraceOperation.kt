package org.centrexcursionistalcoi.app.log

/**
 * The operations a [TraceSpan] can record, the `op` Sentry groups spans by. Add new ones here rather than reusing one
 * that doesn't fit, and prefer Sentry's conventional names (https://develop.sentry.dev/sdk/telemetry/traces/span-operations/)
 * when one exists, since Sentry builds some of its insights on them.
 */
enum class TraceOperation(val value: String) {
    /** A full synchronization of all data, see `SyncAllDataBackgroundJob`. */
    SYNC("sync"),

    /** Synchronizing a single entity type with the server. */
    SYNC_ENTITY("sync.entity"),

    /** Comparing the local and remote entities of a sync. */
    SYNC_PROCESS("sync.process"),

    /** Running a sync again after it failed, from scratch. */
    SYNC_RETRY("sync.retry"),

    /** A request to the server, including downloading its body. */
    HTTP_CLIENT("http.client"),

    /** Decoding a server response. */
    SERIALIZE("serialize"),

    /** Reading from the local database. */
    DB_READ("db.read"),

    /** Writing to the local database. */
    DB_WRITE("db.write"),
}
