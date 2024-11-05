package org.centrexcursionistalcoi.app.fs

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div

/**
 * Returns the path to the directory where the application data is stored.
 */
fun dataDir(): Path = System.getenv("APPDATA")?.let {
    Path(it) / "CEA"
} ?: (Path(System.getProperty("user.home")) / ".cea")
