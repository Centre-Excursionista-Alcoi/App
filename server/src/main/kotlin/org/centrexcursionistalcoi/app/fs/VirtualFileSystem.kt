package org.centrexcursionistalcoi.app.fs

import io.ktor.http.ContentType
import io.ktor.http.fileExtensions
import java.time.Instant
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.slf4j.LoggerFactory

object VirtualFileSystem {
    private val forcedExtensions = mapOf(
        ContentType.Image.JPEG to "jpg",
        ContentType.Image.PNG to "png",
    )

    private fun ContentType.extension(): String? = forcedExtensions[this] ?: this.fileExtensions().firstOrNull()

    data class Item(
        val path: String,          // full path like "foo/bar.txt" or "foo/subdir/"
        val name: String,          // name component
        val isDirectory: Boolean,
        val contentType: ContentType? = null, // for files
        val size: Long? = null,               // for files
        val lastModified: Instant? = null
    ) {
        val nameWithExtension: String
            get() {
                val extension = contentType?.extension() ?: return name
                return "$name.$extension"
            }
    }

    class ItemData(
        val contentType: ContentType,
        val size: Int,
        val data: ByteArray,
    )

    @VisibleForTesting
    internal class RootDir<Id : Any, E : Entity<Id>>(
        val name: String,
        val entityClass: EntityClass<Id, E>,
        val fallbackExtension: String? = null,
        val idConverter: (String) -> Id?,
        val customFileDisplayName: ((E) -> String)? = null,
        val accessor: (E) -> FileEntity?,
    ) {
        /**
         * Returns all entities of type [E] along with their associated [FileEntity]s.
         */
        fun all(): List<Pair<E, FileEntity>> {
            return Database {
                entityClass.all().mapNotNull { entity ->
                    val fileEntity = accessor(entity) ?: return@mapNotNull null
                    entity to fileEntity
                }
            }
        }

        /**
         * Finds a [FileEntity] by a string representation of the entity ID.
         *
         * @param idStr The string representation of the entity ID.
         *
         * @return The found [FileEntity], or `null` if not found or the ID string is malformed.
         */
        fun findByStringIdOrNull(idStr: String): FileEntity? = try {
            findByStringId(idStr)
        } catch (_: IllegalArgumentException) {
            logger.debug("Malformed id string ($idStr) cannot find entity by string id")
            null
        }

        /**
         * Finds a [FileEntity] by a string representation of the entity ID.
         *
         * Works by finding the entity using the converted ID, then accessing its [FileEntity] via the [accessor].
         *
         * @throws IllegalArgumentException if the ID string is malformed and cannot be converted.
         */
        fun findByStringId(idStr: String): FileEntity? {
            val id = idConverter(idStr) ?: throw IllegalArgumentException("Malformed id: $idStr")
            return findById(id)
        }

        /**
         * Finds a [FileEntity] by the entity ID.
         *
         * Works by finding the entity using the ID, then accessing its [FileEntity] via the [accessor].
         */
        fun findById(id: Id): FileEntity? = Database {
            val entity = entityClass.findById(id) ?: return@Database null
            return@Database accessor(entity)
        }

        fun fileDisplayName(entity: Entity<out Any>, fileEntity: FileEntity): String {
            @Suppress("UNCHECKED_CAST")
            customFileDisplayName?.invoke(entity as E)?.let {
                return it
            }

            val fileName = fileEntity.name ?: fileEntity.id.value.toString()
            val fileNameHasExtension = fileName.contains('.')
            return if (fileNameHasExtension) {
                fileName
            } else {
                val fileExtension = fileEntity.contentType
                    .extension()
                    ?.let { ".$it" }
                // If no extension from content type, use fallback extension
                    ?: fallbackExtension?.let { ".$it" }
                    // If no fallback extension, use empty string (no extension)
                    ?: ""
                "$fileName$fileExtension"
            }
        }
    }

    private val logger = LoggerFactory.getLogger(VirtualFileSystem::class.java)

    private val originalRootDirs: List<RootDir<out Any, out Entity<out Any>>> = listOf(
        RootDir("Departments", DepartmentEntity, idConverter = { it.toIntOrNull() }, customFileDisplayName = { it.displayName }) { it.image },
        RootDir("Inventory Item", InventoryItemTypeEntity, idConverter = { it.toUUIDOrNull() }, customFileDisplayName = { it.displayName }) { it.image },
        RootDir("Lending Memories", LendingEntity, idConverter = { it.toUUIDOrNull() }) { it.memoryDocument },
        RootDir("Insurances", UserInsuranceEntity, idConverter = { it.toUUIDOrNull() }) { it.document },
    )

    @VisibleForTesting
    internal var rootDirs: List<RootDir<out Any, out Entity<out Any>>> = originalRootDirs

    @TestOnly
    fun resetRootDirs() {
        rootDirs = originalRootDirs
    }

    fun list(path: String): List<Item>? {
        val parts = path.replace('+', ' ').trim('/').split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty()) {
            // root directory
            return rootDirs.map { rootDir ->
                val dirName = rootDir.name
                Item(
                    path = "/webdav/$dirName/",
                    name = dirName,
                    isDirectory = true,
                )
            }
        } else if (parts.size == 1) {
            // accessing a root dir
            val dirName = parts[0]
            val rootDir = rootDirs.find { it.name == dirName } ?: return null
            return Database {
                rootDir.all().map { (entity, fileEntity) ->
                    mapItem(rootDir, entity, fileEntity)
                }
            }
        } else {
            // There are no subdirectories
            logger.error("Invalid path for listing: $path")
            return null
        }
    }

    private fun mapItem(rootDir: RootDir<out Any, out Entity<out Any>>, entity: Entity<out Any>, fileEntity: FileEntity): Item {
        val bytes = fileEntity.data
        return Item(
            path = "/webdav/${rootDir.name}/${entity.id.value}",
            name = rootDir.fileDisplayName(entity, fileEntity),
            isDirectory = false,
            size = bytes.size.toLong(),
            contentType = fileEntity.contentType,
            lastModified = fileEntity.lastModified,
        )
    }

    /**
     * Reads a file from the virtual file system.
     *
     * @param path The path to the file in the format "/rootDir/fileId".
     * @return The [ItemData] containing file content and metadata, or `null` if not found.
     *
     * @throws IllegalArgumentException if the root directory is invalid.
     */
    fun read(path: String): ItemData? {
        logger.debug("Reading file at path: $path")
        val parts = path.replace('+', ' ').trim('/').split('/').filter { it.isNotEmpty() }
        // Expecting exactly two parts: [rootDir, fileId]
        if (parts.size != 2) {
            logger.error("Invalid path for reading file: $path")
            return null
        }
        val dirName = parts[0]

        // File ID may be one of:
        // - If of the entity (Int or UUID)
        // - The FileEntity ID (UUID)
        // - The display name of the file (String)
        val fileId = parts[1]

        val rootDir = rootDirs.find { it.name == dirName } ?: throw IllegalArgumentException("Invalid directory: $dirName")

        var fileEntity: FileEntity? = null

        // First, try finding by entity ID
        rootDir.findByStringIdOrNull(fileId)?.let {
            fileEntity = it
        }
        // If not found, try finding by FileEntity ID or display name
        if (fileEntity == null) {
            logger.warn("File not found by entity ID, trying FileEntity ID or display name")
            val fileUuid = fileId.toUUIDOrNull()
            if (fileUuid != null) {
                fileEntity = Database { FileEntity.findById(fileUuid) }
            } else {
                logger.warn("File ID is not a valid UUID: $fileId")
            }
        }
        // If still not found, try finding by display name
        if (fileEntity == null) {
            logger.warn("File not found by FileEntity ID, trying display name")
            fileEntity = Database {
                rootDir.all().firstOrNull { (e, fileEntity) ->
                    val displayName = rootDir.fileDisplayName(e, fileEntity)
                    val displayNameWithExtension = "$displayName.${fileEntity.contentType.extension()}"
                    displayName == fileId || displayNameWithExtension == fileId
                }?.second
            }
        }
        if (fileEntity == null) {
            logger.error("File not found: $fileId in directory: $dirName")
            return null
        }

        val data = Database { fileEntity.data }
        return ItemData(
            contentType = fileEntity.contentType,
            size = data.size,
            data = data,
        )
    }
}
