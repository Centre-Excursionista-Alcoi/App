package org.centrexcursionistalcoi.app.storage.fs

import com.mohamedrejeb.calf.io.KmpFile
import com.mohamedrejeb.calf.share.ShareContent
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.ContentType
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlin.jvm.JvmInline
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.centrexcursionistalcoi.app.di.globalDispatcherProvider
import org.centrexcursionistalcoi.app.di.globalPathsProvider
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.utils.copyTo

/**
 * A file the app has (or will have) cached locally under [globalPathsProvider]'s `systemDataPath`, identified
 * by its path relative to it -- e.g. `"documents/ReferencedMemory/<uuid>.pdf"`.
 *
 * Flows through the whole download -> cache -> open/share/drag pipeline in place of raw path `String`s and
 * ad-hoc `java.io.File`/`NSURL`/`Uri` construction. Not [io.github.vinceglb.filekit.PlatformFile]: that's an
 * arbitrary file the user picked to upload, not something cached under the app's own directory. Bulk,
 * directory-wide operations (wiping a whole category on logout) live in [FileSystem] instead.
 */
@JvmInline
value class AppFile(val relativePath: String)

/** The one place `systemDataPath / relativePath` resolution happens -- every platform actual builds on this. */
val AppFile.absolutePath: Path get() = globalPathsProvider.systemDataPath / relativePath

// SystemFileSystem's sink/source are blocking kotlinx-io calls, not coroutine-suspending ones -- withContext
// here is what actually keeps them off the caller's dispatcher (e.g. a ViewModel's main dispatcher), not the
// `suspend` modifier by itself. write()'s suspension would be "real" even without this (channel.copyTo
// genuinely suspends when channel is a network response body), but the disk-write side of it is just as
// blocking as read()'s, so both get the same treatment.
suspend fun AppFile.write(channel: ByteReadChannel, progress: ProgressNotifier? = null) = withContext(globalDispatcherProvider.io) {
    val path = this@write.absolutePath
    path.parent?.let { SystemFileSystem.createDirectories(it) }
    SystemFileSystem.sink(path).use { sink ->
        sink.asByteWriteChannel().use {
            if (progress != null) channel.copyTo(this, progress)
            else channel.copyTo(this)
        }
    }
}

suspend fun AppFile.read(progress: ProgressNotifier? = null): ByteArray = withContext(globalDispatcherProvider.io) {
    SystemFileSystem.source(this@read.absolutePath).use { source ->
        source.buffered().readByteArray()
    }
}

fun AppFile.exists(): Boolean = SystemFileSystem.exists(this.absolutePath)

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
