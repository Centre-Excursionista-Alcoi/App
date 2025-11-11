package org.centrexcursionistalcoi.app.storage.fs

import io.github.vinceglb.filekit.utils.toKotlinxPath
import kotlinx.io.files.Path
import org.centrexcursionistalcoi.app.AppBase
import org.centrexcursionistalcoi.app.AppInitProvider

actual val SystemDataPath: Path
    get() = (AppInitProvider.context ?: AppBase.instance ?: error("Could not find any valid context")).filesDir.toKotlinxPath()
