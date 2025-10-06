@file:OptIn(ExperimentalWasmJsInterop::class)

package org.centrexcursionistalcoi.app.storage.fs

import io.github.aakira.napier.Napier
import io.ktor.util.toJsArray
import kotlin.js.Promise
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.toUByteArray
import org.w3c.files.Blob
import org.w3c.files.File
import org.w3c.files.FileReader

// Helper extension to access the StorageManager
val navigatorStorageManager: StorageManager = js("window.navigator.storage")

object OPFS {

    suspend fun root(): FileSystemDirectoryHandle {
        return navigatorStorageManager.getDirectory().await()
    }

    suspend fun clear() {
        val root = root()
        val entries = root.entries().toList()
        for (entry in entries) {
            val (key: JsString, value: FileSystemDirectoryHandle) = entry.toArray().let { it[0].unsafeCast<JsString>() to it[1].unsafeCast<FileSystemDirectoryHandle>() }
            Napier.d { "Removing entry $key" }
            root.removeEntry(key.toString(), createFSFHRemoveOptions(recursive = true)).await<JsAny>()
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
    ) {
        val fileHandle = getFileHandle(root, name, create = true)
        val writable = fileHandle.createWritable().await<FileSystemWritableFileStream>()
        writable.use {
            write(content.toJsString())
        }
    }

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
        writable.write(content.toJsArray())
        writable.close()
    }

    private suspend fun <Result: JsAny> readFile(
        root: FileSystemDirectoryHandle,
        name: String,
        readerOperation: FileReader.(Blob) -> Unit
    ): Result? {
        val fileHandle = try {
            getFileHandle(root, name, create = false)
        } catch (e: JsException) {
            Napier.e("Could not read file handle for $name", e)
            return null
        }
        val file = fileHandle.getFile().await<File>()
        val reader = FileReader()
        val promise = Promise({ resolve, reject ->
            reader.onload = {
                resolve(reader.result)
            }
            reader.onerror = {
                val error = reader.error?.toThrowableOrNull()
                Napier.e(error) { "Error reading file! ${reader.error}" }
                reject(reader.error!!)
            }
        })
        reader.readerOperation(file)
        return promise.await()
    }

    /**
     * Reads text content from a file.
     */
    suspend fun readTextFile(
        root: FileSystemDirectoryHandle,
        name: String
    ): String? {
        return readFile<JsString>(root, name) { readAsText(it) }?.toString()
    }

    @ExperimentalUnsignedTypes
    suspend fun readFile(
        root: FileSystemDirectoryHandle,
        name: String
    ): ByteArray? {
        val result = readFile<ArrayBuffer>(root, name) { readAsArrayBuffer(it) } ?: return null
        return Uint8Array(result).toUByteArray().toByteArray()
    }
}
