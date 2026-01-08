package org.centrexcursionistalcoi.app.storage.fs

import io.github.vinceglb.filekit.utils.toKotlinxPath
import kotlinx.io.files.Path
import org.centrexcursionistalcoi.app.android.AppBase
import org.centrexcursionistalcoi.app.android.ContextProvider

actual val SystemDataPath: Path
    get() = (ContextProvider.context ?: AppBase.instance ?: error("Could not find any valid context")).filesDir.toKotlinxPath()
