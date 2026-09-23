package org.centrexcursionistalcoi.app.platform

import com.mohamedrejeb.calf.io.KmpFile
import com.mohamedrejeb.calf.share.ShareContent
import io.ktor.http.ContentType
import org.koin.core.annotation.Singleton

/**
 * Resolves a path (relative to [org.centrexcursionistalcoi.app.di.PathsProvider.systemDataPath]) into a [KmpFile],
 * for Calf's `ShareLauncher`/`ShareContent.File` -- the one piece of platform-specific plumbing Calf's sharing
 * API doesn't provide itself, since [KmpFile] has no cross-platform "from a path" constructor.
 *
 * On Android this must go through the app's own `FileProvider` (see
 * [org.centrexcursionistalcoi.app.storage.fs.FilePermissionsUtil]), not Calf's own `File.toKmpFile()`: that
 * extension builds the `KmpFile` from `Uri.fromFile(file)`, a plain `file://` URI. `ShareLauncher` puts whatever
 * URI a `KmpFile` wraps straight into the share `Intent` with no `FileProvider` step of its own, and a `file://`
 * URI there throws `FileUriExposedException` on any real device (targetSdk >= 24) -- the same class of bug as #673.
 */
@Singleton
expect class PlatformKmpFileLogic {
    fun kmpFile(path: String, contentType: ContentType): KmpFile
}

/**
 * Creates a new [ShareContent.File] instance from the given [KmpFile] and [ContentType].
 */
fun KmpFile.toShareContent(contentType: ContentType): ShareContent = ShareContent.File(this, contentType.toString())
