package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.Table

object PostFiles : Table("post_files") {
    val post = reference("post", Posts)
    val file = reference("file", Files)

    override val primaryKey = PrimaryKey(post, file, name = "PK_PostFiles_post_file")
}
