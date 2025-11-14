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
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.slf4j.LoggerFactory

object VirtualFileSystem {
    data class Item(
        val path: String,          // full path like "foo/bar.txt" or "foo/subdir/"
        val name: String,          // name component
        val isDirectory: Boolean,
        val size: Long? = null,    // for files
        val lastModified: Instant? = null
    )

    private class RootDir<Id : Any, E : Entity<Id>>(
        val entityClass: EntityClass<Id, E>,
        val fallbackExtension: String? = null,
        val idConverter: (String) -> Id?,
        val accessor: (E) -> FileEntity?,
    ) {
        fun all(): List<FileEntity> {
            return Database { entityClass.all().mapNotNull { entity -> accessor(entity) } }
        }

        context(_: JdbcTransaction)
        fun findByStringId(idStr: String): FileEntity? {
            val id = idConverter(idStr) ?: throw IllegalArgumentException("Invalid ID: $idStr")
            return findById(id)
        }

        context(_: JdbcTransaction)
        fun findById(id: Id): FileEntity? {
            val entity = entityClass.findById(id) ?: return null
            return accessor(entity)
        }
    }

    private val logger = LoggerFactory.getLogger(VirtualFileSystem::class.java)

    private val rootDirs: Map<String, RootDir<*, *>> = mapOf(
        "Departments" to RootDir(DepartmentEntity, idConverter = { it.toIntOrNull() }) { it.image },
        "Inventory Item" to RootDir(InventoryItemTypeEntity, idConverter = { it.toUUIDOrNull() }) { it.image },
        "Lending Memories" to RootDir(LendingEntity, idConverter = { it.toUUIDOrNull() }) { it.memoryDocument },
        "Insurances" to RootDir(UserInsuranceEntity, idConverter = { it.toUUIDOrNull() }) { it.document },
    )

    fun list(path: String): List<Item>? {
        val parts = path.replace('+', ' ').trim('/').split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty()) {
            // root directory
            return rootDirs.map { (dirName) ->
                Item(
                    path = "/webdav/$dirName/",
                    name = dirName,
                    isDirectory = true,
                )
            }
        } else if (parts.size == 1) {
            // accessing a root dir
            val dirName = parts[0]
            val rootDir = rootDirs[dirName] ?: return null
            return Database {
                rootDir.all().map { fileEntity ->
                    val fileName = fileEntity.name ?: fileEntity.id.value.toString()
                    val fileNameHasExtension = fileName.contains('.')
                    val fileNameWithExtension = if (fileNameHasExtension) fileName else {
                        val fileExtension = fileEntity.type
                            ?.let(ContentType::parse)
                            // Ignore application/octet-stream as it is too generic
                            ?.takeUnless { it == ContentType.Application.OctetStream }
                            ?.fileExtensions()
                            ?.firstOrNull()
                            ?.let { ".$it" }
                        // If no extension from content type, use fallback extension from root dir
                            ?: rootDir.fallbackExtension
                                ?.let { ".$it" }
                            // If no fallback extension, use empty string (no extension)
                            ?: ""
                        "$fileName$fileExtension"
                    }
                    val bytes = fileEntity.data
                    Item(
                        path = "/webdav/$dirName/${fileEntity.id.value}",
                        name = fileNameWithExtension,
                        isDirectory = false,
                        size = bytes.size.toLong(),
                        lastModified = fileEntity.lastModified,
                    )
                }
            }
        } else {
            // There are no subdirectories
            return null
        }
    }

    fun read(path: String): ByteArray? {
        logger.debug("Reading file at path: $path")
        val parts = path.replace('+', ' ').trim('/').split('/').filter { it.isNotEmpty() }
        // Expecting exactly two parts: [rootDir, fileId]
        if (parts.size != 2) return null
        val dirName = parts[0]
        val fileId = parts[1]

        val rootDir = rootDirs[dirName] ?: throw IllegalArgumentException("Invalid directory: $dirName")
        return Database {
            val fileEntity = rootDir.findByStringId(fileId) ?: return@Database null
            fileEntity.data
        }
    }
}
