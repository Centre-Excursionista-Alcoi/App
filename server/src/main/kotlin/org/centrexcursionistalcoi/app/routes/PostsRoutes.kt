package org.centrexcursionistalcoi.app.routes

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.routing.Route
import java.util.UUID
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.PostFiles
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.integration.Telegram
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.FileRequestData.Companion.toFileRequestData
import org.centrexcursionistalcoi.app.request.UpdatePostRequest
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Route.postsRoutes() {
    provideEntityRoutes(
        base = "posts",
        entityClass = PostEntity,
        idTypeConverter = { UUID.fromString(it) },
        listProvider = { session ->
            if (session == null) {
                // Not logged in, only show public posts (without department)
                PostEntity.find { Posts.department eq null }
            } else {
                // Logged in, show public posts, and posts for the user's department
                val userDepartments = transaction {
                    DepartmentMembers.selectAll()
                        .where { (DepartmentMembers.userSub eq session.sub) and (DepartmentMembers.confirmed eq true) }
                        .toList()
                        .map { it[DepartmentMembers.departmentId] }
                }
                PostEntity.find {
                    (Posts.department eq null) or (Posts.department inList userDepartments)
                }
            }
        },
        creator = { formParameters ->
            var title: String? = null
            var content: String? = null
            var departmentId: UUID? = null
            var link: String? = null
            val files: MutableList<FileRequestData> = mutableListOf()

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        when (partData.name) {
                            "title" -> title = partData.value
                            "content" -> content = partData.value
                            "department" -> departmentId = partData.value.toUUIDOrNull()
                            "link" -> link = partData.value
                            "files" -> {
                                val fileList: List<FileWithContext> = json.decodeFromString(
                                    ListSerializer(FileWithContext.serializer()),
                                    partData.value
                                )
                                files += fileList.map { it.toFileRequestData() }
                            }
                            else -> if (partData.name?.startsWith("file_") == true) {
                                val reference = FileRequestData()
                                reference.populate(partData)
                                files += reference
                            }
                        }
                    }
                    is PartData.FileItem -> {
                        val reference = FileRequestData()
                        reference.populate(partData)
                        files += reference
                    }
                    else -> { /* nothing */ }
                }
            }

            title ?: throw NullPointerException("Missing title")
            content ?: throw NullPointerException("Missing content")

            // Check that the department exists if departmentId is provided
            val department = departmentId?.let {
                Database { DepartmentEntity.findById(it) }  ?: throw IllegalArgumentException("Department with id $it does not exist")
            }

            val fileEntities = Database { files.map { it.newEntity() } }

            Database {
                PostEntity.new {
                    this.title = title
                    this.content = content
                    this.department = department
                    this.link = link
                }.also { postEntity ->
                    for (fileEntity in fileEntities) {
                        PostFiles.insert {
                            it[file] = fileEntity.id
                            it[post] = postEntity.id
                        }
                    }
                }
            }.also { postEntity ->
                Telegram.launch {
                    val post = Database { postEntity.toData() }
                    Telegram.sendPost(post)
                }
            }
        },
        deleteReferencesCheck = { department ->
            // departments are referenced in posts, make sure no posts reference the department before deleting
            PostEntity.find { Posts.department eq department.id }.empty()
        },
        updater = UpdatePostRequest.serializer(),
    )
}
