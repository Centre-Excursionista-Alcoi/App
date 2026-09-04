package org.centrexcursionistalcoi.app.database.relation

import androidx.room3.Embedded
import androidx.room3.Relation
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.database.room.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.room.entity.PostEntity

data class PostWithRelations(
    @Embedded val post: PostEntity,
    @Relation(parentColumns = ["department"], entityColumns = ["id"])
    val department: DepartmentEntity?,
)

fun PostWithRelations.toReferenced() = ReferencedPost(
    id = post.id,
    date = post.date,
    title = post.title,
    content = post.content,
    department = department?.toDepartment(),
    link = post.link,
    files = post.files.orEmpty(),
)
