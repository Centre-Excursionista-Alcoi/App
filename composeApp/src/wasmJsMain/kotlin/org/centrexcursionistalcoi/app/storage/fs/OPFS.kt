@file:OptIn(ExperimentalWasmJsInterop::class)

package org.centrexcursionistalcoi.app.storage.fs

import io.github.aakira.napier.Napier
import io.ktor.util.toJsArray
import kotlin.js.Promise
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.w3c.files.File
import org.w3c.files.FileReader

// Helper extension to access the StorageManager
val navigatorStorageManager: StorageManager = js("window.navigator.storage")

@OptIn(ExperimentalUnsignedTypes::class)
object OPFS {

    suspend fun root(): FileSystemDirectoryHandle {
        return navigatorStorageManager.getDirectory().await()
    }

    suspend fun clear() {
        val root = root()
        val entries = root.keys()
        var entry: AsyncIteratorEntry<JsString> = entries.next().await()
        while (!entry.done) {
            Napier.d("Removing entry (done=${entry.done}): ${entry.value}")
            root.removeEntry(entry.value.toString(), createFSFHRemoveOptions(true)).await<JsAny>()
            entry = entries.next().await()
        }
    }

    /**
     * Returns storage quota and usage info (in bytes).
     */
    suspend fun getStorageEstimate(): StorageManagerEstimate {
        return navigatorStorageManager.estimate().await()
    }

    /**
     * Creates or retrieves a file in OPFS.
     */
    suspend fun getFileHandle(
        root: FileSystemDirectoryHandle,
        name: String,
        create: Boolean = true
    ): FileSystemFileHandle {
        return root.getFileHandle(name, createFSFHGetOptions(create)).await()
    }

    /**
     * Writes text data to a file in OPFS.
     */
    suspend fun writeTextFile(
        root: FileSystemDirectoryHandle,
        name: String,
        content: String
    ) = writeFile(root, name, content.encodeToByteArray())

    /**
     * Writes text data to a file in OPFS.
     */
    suspend fun writeFile(
        root: FileSystemDirectoryHandle,
        name: String,
        content: ByteArray
    ) {
        val fileHandle = getFileHandle(root, name, create = true)
        val writable = fileHandle.createWritable().await<FileSystemWritableFileStream>()
        writable.write(content.toJsArray()).await<JsAny>()
        writable.close().await<JsAny>()
    }

    /**
     * Reads text content from a file.
     */
    suspend fun readTextFile(
        root: FileSystemDirectoryHandle,
        name: String
    ): String? = readFile(root, name)?.decodeToString()

    suspend fun readFile(
        root: FileSystemDirectoryHandle,
        name: String
    ): ByteArray? {
        val fileHandle = try {
            getFileHandle(root, name, create = false)
        } catch (e: JsException) {
            Napier.e("Could not read file handle for $name", e)
            return null
        }
        val file = fileHandle.getFile().await<File>()
        val reader = FileReader()
        val promise = Promise { resolve, reject ->
            reader.onload = {
                resolve(reader.result)
            }
            reader.onerror = {
                val error = reader.error?.toThrowableOrNull()
                Napier.e(error) { "Error reading file! ${reader.error}" }
                reject(reader.error!!)
            }
        }
        reader.readAsArrayBuffer(file)
        val result = promise.await<ArrayBuffer>()
        Napier.v { "Read ${result.byteLength} bytes from ${root.name}/$name" }
        return Int8Array(result).toByteArray()
    }

    suspend fun exists(
        root: FileSystemDirectoryHandle,
        name: String
    ): Boolean {
        val entries = root.keys()
        var entry: AsyncIteratorEntry<JsString> = entries.next().await()
        while (!entry.done) {
            if (entry.value == name.toJsString()) {
                return true
            }
            entry = entries.next().await()
        }
        return false
    }
}
