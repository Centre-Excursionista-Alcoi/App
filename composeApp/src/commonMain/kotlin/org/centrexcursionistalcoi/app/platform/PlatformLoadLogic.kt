package org.centrexcursionistalcoi.app.platform

expect object PlatformLoadLogic {
    fun isReady(): Boolean

    /**
     * Will get called in the Loading Screen to perform platform-specific loading logic,
     * like initializing a database, loading secrets, etc.
     */
    suspend fun load()
}
