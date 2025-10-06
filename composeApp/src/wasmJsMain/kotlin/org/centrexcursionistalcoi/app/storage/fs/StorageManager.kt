@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("unused")

package org.centrexcursionistalcoi.app.storage.fs

import kotlin.js.Promise
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.w3c.files.File

external interface FSFHGetOptions {
    var create: Boolean?
}

fun createFSFHGetOptions(create: Boolean? = null): FSFHGetOptions =
    js("({create: create})")

external interface FSFHRemoveOptions {
    var recursive: Boolean?
}

fun createFSFHRemoveOptions(recursive: Boolean? = null): FSFHRemoveOptions =
    js("({recursive: recursive})")

external interface FSSAHOptions {
    var at: Int?
}

fun createFSSAHOptions(at: Int? = null): FSSAHOptions =
    js("({at: at})")

external interface WritableOptions {
    /**
     * A [Boolean]. Default `false`.
     * When set to `true` if the file exists, the existing file is first copied to the temporary file.
     * Otherwise the temporary file starts out empty.
     */
    val keepExistingData: Boolean?

    /**
     * A string specifying the locking mode for the writable file stream. The default value is `"siloed"`. Possible values are:
     * - `"exclusive"`: Only one [FileSystemWritableFileStream] writer can be opened. Attempting to open subsequent writers before the first
     * writer is closed results in a `NoModificationAllowedError` exception being thrown.
     * - `"siloed"`: Multiple [FileSystemWritableFileStream] writers can be opened at the same time, each with its own swap file, for example
     * when using the same app in multiple tabs. The last writer opened has its data written, as the data gets flushed when each writer is closed.
     */
    val mode: String?
}

fun createWritableOptions(keepExistingData: Boolean? = null, mode: String? = null): WritableOptions =
    js("({keepExistingData: keepExistingData, mode: mode})")

external interface WritableStreamDefaultWriter : JsAny {
    /**
     * The closed read-only property of the [WritableStreamDefaultWriter] interface returns a [Promise] that fulfills if the stream becomes closed,
     * or rejects if the stream errors or the writer's lock is released.
     */
    val closed: Promise<JsAny>

    /**
     * Returns the desired size required to fill the stream's internal queue.
     */
    val desiredSize: Long?

    /**
     * Returns a [Promise] that resolves when the desired size of the stream's internal queue transitions from non-positive to positive, signaling
     * that it is no longer applying backpressure.
     */
    val ready: Promise<JsAny>

    /**
     * Aborts the stream, signaling that the producer can no longer successfully write to the stream and it is to be immediately moved to an error
     * state, with any queued writes discarded.
     */
    fun abort(reason: JsAny = definedExternally): Promise<JsAny>

    /**
     * Closes the associated writable stream.
     */
    fun close(): Promise<JsAny>

    /**
     * Releases the writer's lock on the corresponding stream. After the lock is released, the writer is no longer active. If the associated stream
     * is errored when the lock is released, the writer will appear errored in the same way from now on; otherwise, the writer will appear closed.
     */
    fun releaseLock()

    /**
     * Writes a passed chunk of data to a [WritableStream] and its underlying sink, then returns a Promise that resolves to indicate the success
     * or failure of the write operation.
     * @return A [Promise], which fulfills with the undefined upon a successful write, or rejects if the write fails or stream becomes errored
     * before the writing process is initiated.
     */
    fun write(chunk: JsAny): Promise<JsAny>
}

external interface WritableStream : JsAny {
    /**
     * A boolean indicating whether the [WritableStream] is locked to a writer.
     */
    val locked: Boolean

    /**
     * Aborts the stream, signaling that the producer can no longer successfully write to the stream and it is to be immediately moved to an error
     * state, with any queued writes discarded.
     */
    fun abort(reason: JsAny = definedExternally): Promise<JsAny>

    /**
     * Closes the stream.
     */
    fun close(): Promise<JsAny>

    /**
     * Returns a new instance of [WritableStreamDefaultWriter] and locks the stream to that instance. While the stream is locked, no other writer
     * can be acquired until this one is released.
     */
    fun getWriter(): WritableStreamDefaultWriter
}

suspend fun <T> WritableStream.use(block: suspend WritableStreamDefaultWriter.() -> T): T {
    val writer = this.getWriter()
    return try {
        block(writer)
    } finally {
        writer.ready.await<JsAny>()
        writer.releaseLock()
        writer.close()
        close()
    }
}

external interface FileSystemWritableFileStream : WritableStream {
    /**
     * The write() method of the FileSystemWritableFileStream interface writes content into the file the method is called on,
     * at the current file cursor offset.
     *
     * No changes are written to the actual file on disk until the stream has been closed. Changes are typically written to a
     * temporary file instead. This method can also be used to seek to a byte point within the stream and truncate to modify
     * the total bytes the file contains.
     *
     * @param data Can be one of the following:
     * - The file data to write, in the form of an [ArrayBuffer], `TypedArray`, [org.khronos.webgl.DataView], [org.w3c.files.Blob], or [JsString].
     * - An object containing the following properties:
     *   - `type`: A string that is one of "write", "seek", or "truncate".
     *   - `data`: The file data to write. Can be an ArrayBuffer, TypedArray, DataView, Blob, or string. This property is required if type is set
     *   to "write".
     *   - `position`: The byte position the current file cursor should move to if type "seek" is used. Can also be set if type is "write", in which
     *   case the write will start at the specified position.
     *   - `size`: A number representing the number of bytes the stream should contain. This property is required if type is set to "truncate".
     */
    fun write(data: JsAny): Promise<JsAny>

    // Returns undefined
    fun seek(position: Int = definedExternally): Promise<JsAny>

    // Returns undefined
    fun truncate(size: Int = definedExternally): Promise<JsAny>
}

external interface FileSystemSyncAccessHandle : JsAny {
    /**
     * Closes an open synchronous file handle, disabling any further operations on it and releasing the exclusive lock previously put on the file
     * associated with the file handle.
     */
    fun close()

    /**
     * Persists any changes made to the file associated with the handle via the [write] method to disk.
     */
    fun flush()

    /**
     * Returns the size of the file associated with the handle in bytes.
     */
    fun getSize(): Int

    /**
     * Reads the content of the file associated with the handle into a specified buffer, optionally at a given offset.
     */
    fun read(buffer: ArrayBuffer, options: FSSAHOptions = definedExternally): Int

    /**
     * Resizes the file associated with the handle to a specified number of bytes.
     */
    fun truncate(size: Int)

    /**
     * Writes the content of a specified buffer to the file associated with the handle, optionally at a given offset.
     */
    fun write(buffer: ArrayBuffer, options: FSSAHOptions = definedExternally): Int
}

external interface FileSystemHandle: JsAny {
    /**
     * Returns the type of entry. This is `'file'` if the associated entry is a file or `'directory'`.
     */
    val kind: JsString

    /**
     * Returns the name of the associated entry.
     */
    val name: JsString

    /**
     * Compares two handles to see if the associated entries (either a file or directory) match.
     * @param fileSystemHandle The [FileSystemHandle] to match against the handle on which the method is invoked.
     */
    fun isSameEntry(fileSystemHandle: FileSystemHandle): Promise<JsBoolean>
}

external interface FileSystemFileHandle: FileSystemHandle {
    /**
     * Returns a [Promise] which resolves to a [File] object representing the state on disk of the entry represented by the handle.
     */
    fun getFile(): Promise<File>

    /**
     * Returns a [Promise] which resolves to a [FileSystemSyncAccessHandle] object that can be used to synchronously read from and write to a file.
     * The synchronous nature of this method brings performance advantages, but it is only usable inside dedicated `Web Workers`.
     */
    fun createSyncAccessHandle(): Promise<FileSystemSyncAccessHandle>

    /**
     * Returns a [Promise] which resolves to a newly created [FileSystemWritableFileStream] object that can be used to write to a file.
     */
    fun createWritable(options: WritableOptions = definedExternally): Promise<FileSystemWritableFileStream>
}

external interface FileSystemDirectoryHandle : FileSystemHandle {
    /**
     * Returns a [Promise] fulfilled with a [FileSystemDirectoryHandle] for a subdirectory with the specified name within the directory handle on
     * which the method is called.
     */
    fun getDirectoryHandle(name: String, options: FSFHGetOptions = definedExternally): Promise<FileSystemDirectoryHandle>

    /**
     * Returns a [Promise] fulfilled with a [FileSystemFileHandle] for a file with the specified name, within the directory the method is called.
     */
    fun getFileHandle(name: String, options: FSFHGetOptions = definedExternally): Promise<FileSystemFileHandle>

    /**
     * Attempts to asynchronously remove an entry if the directory handle contains a file or directory called the name specified.
     */
    fun removeEntry(name: String, options: FSFHRemoveOptions = definedExternally): Promise<JsAny>

    /**
     * Returns a [Promise] fulfilled with an [Array] of directory names from the parent handle to the specified child entry, with the name of
     * the child entry as the last array item.
     * @param possibleDescendant The [FileSystemHandle] from which to return the relative path.
     */
    fun resolve(possibleDescendant: FileSystemFileHandle): Promise<JsArray<JsString>?>

    fun entries(): JsArray<JsArray<JsAny>>

    /**
     * Returns a new async iterator containing the keys for each item in [FileSystemDirectoryHandle].
     */
    fun keys(): JsArray<JsString>

    /**
     * Returns a new async iterator containing the values for each index in the [FileSystemDirectoryHandle] object.
     */
    fun values(): JsArray<FileSystemDirectoryHandle>
}

external interface StorageManagerEstimate: JsAny {
    var quota: Double
    var usage: Double
}

external interface StorageManager {
    fun estimate(): Promise<StorageManagerEstimate>
    fun getDirectory(): Promise<FileSystemDirectoryHandle>
    fun persist(): Promise<JsBoolean>
    fun persisted(): Promise<JsBoolean>
}
