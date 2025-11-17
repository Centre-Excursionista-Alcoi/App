package org.centrexcursionistalcoi.app.routes

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.routing.Route
import java.util.UUID
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.table.Posts
import org.jetbrains.exposed.v1.core.eq

fun Route.postsRoutes() {
    provideEntityRoutes(
        base = "posts",
        entityClass = PostEntity,
        idTypeConverter = { UUID.fromString(it) },
        listProvider = { session ->
            if (session == null) {
                // Not logged in, only show public posts
                PostEntity.find { Posts.onlyForMembers eq false }
            } else {
                // Logged in, show all posts
                PostEntity.all()
            }
        },
        creator = { formParameters ->
            var title: String? = null
            var content: String? = null
            var onlyForMembers = false
            var departmentId: Int? = null

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        when (partData.name) {
                            "title" -> title = partData.value
                            "content" -> content = partData.value
                            "onlyForMembers" -> onlyForMembers = partData.value.toBoolean()
                            "department" -> departmentId = partData.value.toInt()
                        }
                    }
                    else -> { /* nothing */ }
                }
            }

            title ?: throw NullPointerException("Missing title")
            content ?: throw NullPointerException("Missing content")
            departmentId ?: throw NullPointerException("Missing department")

            // Check that the department exists
            val department = Database { DepartmentEntity.findById(departmentId) }
            department ?: throw IllegalArgumentException("Department with id $departmentId does not exist")

            Database {
                PostEntity.new {
                    this.title = title
                    this.content = content
                    this.onlyForMembers = onlyForMembers
                    this.department = department
                }
            }
        }
    )
}
