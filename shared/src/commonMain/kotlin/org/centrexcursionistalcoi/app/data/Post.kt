@file:UseSerializers(InstantSerializer::class)

package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

@Serializable
data class Post(
    override val id: Uuid,
    val date: Instant,
    val title: String,
    val content: String,
    val department: Uuid?,
    val link: String?,
    val files: List<FileWithContext>,
): Entity<Uuid>, ImageFileListContainer {
    override val images: List<Uuid> get() = files.mapNotNull { it.id }
}
