package org.centrexcursionistalcoi.app.storage

import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid

object InMemoryFileAllocator {
    private val files = mutableMapOf<Uuid, ByteArray>()

    fun put(bytes: ByteArray, uuid: Uuid? = null): Uuid {
        val uuid = uuid ?: Uuid.random()
        Napier.i { "Allocated a file of ${bytes.size} bytes at $uuid" }
        files[uuid] = bytes
        return uuid
    }

    fun get(uuid: Uuid): ByteArray? = files[uuid]

    fun contains(uuid: Uuid): Boolean = files.containsKey(uuid)

    /**
     * Deletes the file associated with the given UUID from the in-memory storage.
     * @return The removed ByteArray if it existed, or null if no file was associated with the UUID.
     */
    fun delete(uuid: Uuid) = files.remove(uuid)
}
