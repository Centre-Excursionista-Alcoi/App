package org.centrexcursionistalcoi.app.di

import io.github.vinceglb.filekit.utils.div
import io.github.vinceglb.filekit.utils.toPath
import kotlinx.io.files.Path
import org.koin.core.annotation.Singleton

@Singleton
class JvmPathsProvider : PathsProvider {
    override val systemDataPath: Path
        get() {
            val home = System.getProperty("user.home").toPath()
            return home / ".centrexcursionistalcoi"
        }
}
