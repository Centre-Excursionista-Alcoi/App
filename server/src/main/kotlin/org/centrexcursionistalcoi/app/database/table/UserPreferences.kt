package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

/**
 * The preferences of the users: a row for each preference a user has, with the name of the preference and its value
 * as text. The names aren't fixed by the table: any can be stored (see `UserPreferenceStore`, and
 * `UserPreferenceKey` for the ones that have a type).
 */
object UserPreferences : Table("user_preferences") {
    val userSub = reference("userSub", UserReferences, onDelete = ReferenceOption.CASCADE)

    /** What the preference is, e.g. `language`. */
    val key = varchar("preferenceKey", 100)

    val value = text("preferenceValue")

    override val primaryKey = PrimaryKey(userSub, key, name = "PK_UserPreferences")
}
