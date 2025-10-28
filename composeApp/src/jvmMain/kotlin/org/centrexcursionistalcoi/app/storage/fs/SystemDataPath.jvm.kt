package org.centrexcursionistalcoi.app.storage.fs

import io.github.vinceglb.filekit.utils.div
import io.github.vinceglb.filekit.utils.toPath
import kotlinx.io.files.Path

actual val SystemDataPath: Path
    get() {
        val home = System.getProperty("user.home").toPath()
        return home / ".centrexcursionistalcoi"
    }
