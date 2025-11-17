package org.centrexcursionistalcoi.app

/**
 * The current version of the application, as specified in the JAR manifest.
 */
val version: String
    get() = object {}.javaClass.`package`?.implementationVersion
        ?: System.getProperty("app.version")
        ?: System.getenv("APP_VERSION")
        ?: error("Version not found")
