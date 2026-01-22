package org.centrexcursionistalcoi.app.database.table

import java.util.UUID
import kotlinx.serialization.SerializationStrategy
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.utils.ViaLink
import org.centrexcursionistalcoi.app.database.utils.serializer
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.SizedIterable

object Posts : UUIDTable("posts"), ViaLink<UUID, PostEntity, UUID, FileEntity> {
    val date = timestamp("date").defaultExpression(DatabaseNowExpression)
    val lastUpdate = timestamp("lastUpdate").defaultExpression(CurrentTimestamp)

    val title = varchar("title", 255)
    val content = text("content")
    val department = optReference("department", Departments)


    val link = varchar("link", 512).nullable()

    override val linkName: String = "files"

    override fun linkSerializer(): Pair<SerializationStrategy<FileEntity>, Boolean> {
        return FileEntity.serializer() to /* nullable */ false
    }

    override fun links(entity: PostEntity): SizedIterable<FileEntity> {
        return entity.files
    }
}
