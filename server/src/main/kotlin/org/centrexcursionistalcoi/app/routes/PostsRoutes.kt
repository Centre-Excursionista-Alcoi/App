package org.centrexcursionistalcoi.app.routes

import io.ktor.server.routing.Route
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.jetbrains.exposed.v1.dao.with
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.table.PostFiles
import org.centrexcursionistalcoi.app.integration.Telegram
import org.centrexcursionistalcoi.app.request.CreatePostRequest
import org.centrexcursionistalcoi.app.request.UpdatePostRequest
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.exposed.v1.jdbc.insert

fun Route.postsRoutes() {
    provideEntityRoutes(
        resources = Api.Posts.resources,
        entityClass = PostEntity,
        syncKey = "posts",
        idTypeConverter = { it.toUuidOrNull() },
        listProvider = { session -> PostEntity.forSession(session).with(PostEntity::files) },
        visibleTo = { post, session -> post.isVisibleTo(session) },
        afterCreate = { postEntity ->
            Telegram.launch {
                val post = Database { postEntity.toData() }
                Telegram.sendPost(post)
            }
        },
        // No deleteReferencesCheck needed: PostFiles is the only table referencing Posts, and
        // PostFiles.post has onDelete = ReferenceOption.CASCADE (see PostFiles.kt), so the DB already
        // drops those join rows cleanly on delete -- there's no FK that could throw a raw
        // ExposedSQLException here. This previously compared Posts.department against the post's own id
        // (copy-pasted from a department-scoped check and never adapted -- see EventsRoutes.kt's
        // identical, but actually load-bearing, pattern), which could never match, so it always evaluated
        // to "no references exist" anyway -- removing it changes nothing observable, just the dead/wrong
        // logic behind it.
        updater = UpdatePostRequest.serializer(),
        createRequestSerializer = CreatePostRequest.serializer(),
        creator = { request ->
            val department = request.department?.let {
                Database { DepartmentEntity.findById(it) }
                    ?: throw IllegalArgumentException("Department with id $it does not exist")
            }

            val fileEntities = Database { request.files.map { FileEntity.newFrom(it) } }

            Database {
                PostEntity.new {
                    this.title = request.title
                    this.content = request.content
                    this.department = department
                    this.link = request.link
                }.also { postEntity ->
                    for (fileEntity in fileEntities) {
                        PostFiles.insert {
                            it[file] = fileEntity.id
                            it[post] = postEntity.id
                        }
                    }
                }
            }
        },
        writePermission = EntityWritePermission(
            role = DepartmentRole.CONTENT_MANAGER,
            departmentOfEntity = { it.department?.id?.value },
        ),
    )
}
