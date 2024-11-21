package org.centrexcursionistalcoi.app.database.entity

import kotlinx.datetime.Instant
import org.centrexcursionistalcoi.app.data.DatabaseData
import org.centrexcursionistalcoi.app.data.Serializable
import org.centrexcursionistalcoi.app.data.Validator

interface DatabaseEntity<SType: DatabaseData>: Validator, Serializable<SType> {
    val id: Int
    val createdAt: Instant
}
