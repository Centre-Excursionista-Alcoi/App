package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
class ServerInfo(
    /**
     * Application version.
     */
    val version: String,

    /**
     * Version of the database schema.
     */
    val databaseVersion: Int,

    /**
     * Timestamp (in milliseconds) of the last successful synchronization with the CEA database.
     */
    val lastCEASync: Long,
)
