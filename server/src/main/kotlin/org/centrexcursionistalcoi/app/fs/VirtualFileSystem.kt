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
    data class Item(
        val path: String,          // full path like "foo/bar.txt" or "foo/subdir/"
        val name: String,          // name component
        val isDirectory: Boolean,
        val size: Long? = null,    // for files
        val lastModified: Instant? = null
    )

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
        fun all(): List<Pair<E, FileEntity>> {
            return Database {
                entityClass.all().mapNotNull { entity ->
                    val fileEntity = accessor(entity) ?: return@mapNotNull null
                    entity to fileEntity
                }
            }
        }

        fun findByStringId(idStr: String): FileEntity? {
            val id = idConverter(idStr) ?: throw IllegalArgumentException("Malformed id: $idStr")
            return findById(id)
        }

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
                val fileExtension = fileEntity.type
                    ?.let(ContentType::parse)
                    // Ignore application/octet-stream as it is too generic
                    ?.takeUnless { it == ContentType.Application.OctetStream }
                    ?.fileExtensions()
                    ?.firstOrNull()
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
            lastModified = fileEntity.lastModified,
        )
    }

    fun read(path: String): ItemData? {
        logger.debug("Reading file at path: $path")
        val parts = path.replace('+', ' ').trim('/').split('/').filter { it.isNotEmpty() }
        // Expecting exactly two parts: [rootDir, fileId]
        if (parts.size != 2) return null
        val dirName = parts[0]
        val fileId = parts[1]

        val rootDir = rootDirs.find { it.name == dirName } ?: throw IllegalArgumentException("Invalid directory: $dirName")
        val fileEntity = rootDir.findByStringId(fileId) ?: return null
        val data = Database { fileEntity.data }
        return ItemData(
            contentType = fileEntity.type?.let(ContentType::parse) ?: ContentType.Application.OctetStream,
            size = data.size,
            data = data,
        )
    }
}
