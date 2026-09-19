package org.centrexcursionistalcoi.app.di

import android.content.Context
import io.github.vinceglb.filekit.utils.toKotlinxPath
import kotlinx.io.files.Path

class AndroidPathsProvider(private val context: Context) : PathsProvider {
    override val systemDataPath: Path
        get() = context.filesDir.toKotlinxPath()
}
