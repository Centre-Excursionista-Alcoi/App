@file:UseSerializers(InstantSerializer::class)

package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Serializable
data class Post(
    val id: Uuid,
    val date: Instant,
    val title: String,
    val content: String,
    val imageFile: String?,
    val onlyForMembers: Boolean,
    val departmentId: Long,
)
