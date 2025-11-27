package org.centrexcursionistalcoi.app

/**
 * The current version of the application.
 */
val version: String
    get() = object {}.javaClass.`package`?.implementationVersion
        ?: System.getProperty("app.version")
        ?: System.getenv("APP_VERSION")
        ?: error("Version not found")

/**
 * The current version code of the application.
 */
val versionCode: Int
    get() = System.getProperty("app.versionCode")?.toInt()
        ?: System.getenv("APP_VERSION_CODE")?.toInt()
        ?: error("Version Code not found")
