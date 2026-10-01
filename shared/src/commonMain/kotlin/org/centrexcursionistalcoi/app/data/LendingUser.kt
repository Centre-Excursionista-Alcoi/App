package org.centrexcursionistalcoi.app.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class LendingUser(
    override val id: Uuid,
    val sub: String,

    val phoneNumber: String,

    val sports: List<Sports>,
): Entity<Uuid>
