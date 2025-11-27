package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
class ServerInfo(
    val version: Version,

    /**
     * Timestamp (in milliseconds) of the last successful synchronization with the CEA database.
     */
    val lastCEASync: Long,
) {
    @Serializable
    data class Version(
        /**
         * Application version.
         */
        val version: String,

        /**
         * Version of the database schema.
         */
        val databaseVersion: Int,

        /**
         * Version code (internal use).
         */
        val code: Int,
    )
}
