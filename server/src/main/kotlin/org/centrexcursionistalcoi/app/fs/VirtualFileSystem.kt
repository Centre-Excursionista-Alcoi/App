package org.centrexcursionistalcoi.app.fs

import io.ktor.http.ContentType
import io.ktor.http.fileExtensions
import java.time.Instant
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object VirtualFileSystem {
    data class Item(
        val path: String,          // full path like "foo/bar.txt" or "foo/subdir/"
        val name: String,          // name component
        val isDirectory: Boolean,
        val size: Long? = null,    // for files
        val lastModified: Instant? = null
    )

    private class RootDir<Id: Any, E: Entity<Id>>(
        val entityClass: EntityClass<Id, E>,
        val accessor: (E) -> FileEntity?,
    )

    private val rootDirs = mapOf(
        "Departments" to RootDir(DepartmentEntity) { it.image }
    )

    fun list(path: String): List<Item>? {
        val parts = path.trim('/').split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty()) {
            // root directory
            return rootDirs.map { (dirName) ->
                Item(
                    path = "$dirName/",
                    name = dirName,
                    isDirectory = true,
                )
            }
        } else if (parts.size == 1) {
            // accessing a root dir
            val dirName = parts[0]
            val rootDir = rootDirs[dirName] ?: return null
            return Database {
                rootDir.entityClass.all().mapNotNull { entity ->
                    val fileEntity = rootDir.accessor(entity) ?: return@mapNotNull null
                    val fileName = fileEntity.name ?: fileEntity.id.value.toString()
                    val fileExtensions = fileEntity.type?.let(ContentType::parse)?.fileExtensions()
                    val fileExtension = fileExtensions?.firstOrNull()
                    val fileNameWithExtension = "$fileName${fileExtension?.let { ".$it" } ?: ""}"
                    val bytes = fileEntity.data
                    Item(
                        path = "$dirName/$fileNameWithExtension",
                        name = "$fileName$fileNameWithExtension",
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
        val parts = path.trim('/').split('/').filter { it.isNotEmpty() }
        if (parts.size != 2) return null
        val dirName = parts[0]
        val fileName = parts[1]
        val rootDir = rootDirs[dirName] ?: throw IllegalArgumentException("Invalid directory: $dirName")
        return Database {
            val entity = rootDir.entityClass.all().firstOrNull { entity ->
                val fileEntity = rootDir.accessor(entity) ?: return@firstOrNull false
                val storedFileName = fileEntity.name ?: fileEntity.id.value.toString()
                val fileExtensions = fileEntity.type?.let(ContentType::parse)?.fileExtensions()
                val fileExtension = fileExtensions?.firstOrNull()
                val storedFileNameWithExtension = "$storedFileName${fileExtension?.let { ".$it" } ?: ""}"
                storedFileNameWithExtension == fileName
            }
            if (entity == null) return@Database null

            val fileEntity = rootDir.accessor(entity) ?: return@Database null
            fileEntity.data
        }
    }
}
