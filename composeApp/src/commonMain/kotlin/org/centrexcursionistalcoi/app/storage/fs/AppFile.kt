package org.centrexcursionistalcoi.app.storage.fs

import com.mohamedrejeb.calf.io.KmpFile
import com.mohamedrejeb.calf.share.ShareContent
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.ContentType
import io.ktor.utils.io.ByteReadChannel
import kotlin.jvm.JvmInline
import kotlinx.io.files.Path
import org.centrexcursionistalcoi.app.di.globalPathsProvider
import org.centrexcursionistalcoi.app.process.ProgressNotifier

/**
 * A file the app has (or will have) cached locally under [globalPathsProvider]'s `systemDataPath`, identified
 * by its path relative to it -- e.g. `"documents/ReferencedMemory/<uuid>.pdf"`.
 *
 * This is the single type that should flow through the whole download -> cache -> open/share/drag pipeline,
 * instead of a mix of raw path `String`s, [kotlinx.io.files.Path], [com.mohamedrejeb.calf.io.KmpFile], and
 * ad-hoc `java.io.File`/`NSURL`/`Uri` construction repeated at each platform call site. See
 * [org.centrexcursionistalcoi.app.data.DocumentFileContainer.fetchDocumentFilePath]/
 * [org.centrexcursionistalcoi.app.data.ImageFileContainer.fetchImageFilePath] for how one gets produced
 * (downloading it first if it isn't cached yet), and `AppFile.android.kt`/`AppFile.ios.kt`/`AppFile.jvm.kt` for
 * the per-platform native handles `PlatformOpenFileLogic`/`PlatformDragAndDrop`/[toKmpFile] need.
 *
 * Deliberately unrelated to [io.github.vinceglb.filekit.PlatformFile]: that one represents an arbitrary file the
 * user just picked from their device to upload, not something the app cached under its own directory -- a
 * different concern with its own already-consistent abstraction.
 */
@JvmInline
value class AppFile(val relativePath: String)

/** The one place `systemDataPath / relativePath` resolution happens -- every platform actual builds on this. */
val AppFile.absolutePath: Path get() = globalPathsProvider.systemDataPath / relativePath

suspend fun AppFile.read(progress: ProgressNotifier? = null): ByteArray = FileSystem.read(relativePath, progress)

suspend fun AppFile.write(channel: ByteReadChannel, progress: ProgressNotifier? = null) = FileSystem.write(relativePath, channel, progress)

fun AppFile.exists(): Boolean = FileSystem.exists(relativePath)

/**
 * Resolves this file into a [KmpFile], for Calf's `ShareLauncher`/`ShareContent.File` -- the one piece of
 * platform-specific plumbing Calf's sharing API doesn't provide itself, since [KmpFile] has no cross-platform
 * "from a path" constructor.
 *
 * On Android this must go through the app's own `FileProvider` (see [FilePermissionsUtil]/[contentUri]), not
 * Calf's own `File.toKmpFile()`: that extension builds the `KmpFile` from `Uri.fromFile(file)`, a plain
 * `file://` URI. `ShareLauncher` puts whatever URI a `KmpFile` wraps straight into the share `Intent` with no
 * `FileProvider` step of its own, and a `file://` URI there throws `FileUriExposedException` on any real device
 * (targetSdk >= 24).
 */
expect fun AppFile.toKmpFile(contentType: ContentType): KmpFile

/** Wraps this [KmpFile] into a [ShareContent.File] for Calf's `ShareLauncher`. */
fun KmpFile.toShareContent(contentType: ContentType): ShareContent = ShareContent.File(this, contentType.toString())
