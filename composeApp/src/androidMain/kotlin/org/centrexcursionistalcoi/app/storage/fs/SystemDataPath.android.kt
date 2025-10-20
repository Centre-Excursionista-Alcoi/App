package org.centrexcursionistalcoi.app.storage.fs

import io.github.vinceglb.filekit.utils.toKotlinxPath
import kotlinx.io.files.Path
import org.centrexcursionistalcoi.app.MainActivity

actual val SystemDataPath: Path
    get() = MainActivity.instance!!.filesDir.toKotlinxPath()
