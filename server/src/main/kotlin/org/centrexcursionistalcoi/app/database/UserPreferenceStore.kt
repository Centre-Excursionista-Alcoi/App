package org.centrexcursionistalcoi.app.database

import org.centrexcursionistalcoi.app.database.table.UserPreferences
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Reads and writes the preferences of the users (see [UserPreferences]). A preference can be anything: by its name as
 * text, or with a [UserPreferenceKey] for the ones that have a type.
 */
object UserPreferenceStore {
    /** The value of the preference [name] of the user with [sub], or `null` if they don't have it. */
    context(_: JdbcTransaction)
    operator fun get(sub: String, name: String): String? = UserPreferences.selectAll()
        .where { (UserPreferences.userSub eq sub) and (UserPreferences.key eq name) }
        .firstOrNull()
        ?.get(UserPreferences.value)

    /** The value of [key] for the user with [sub], or `null` if they don't have it. */
    context(_: JdbcTransaction)
    operator fun <T : Any> get(sub: String, key: UserPreferenceKey<T>): T? = get(sub, key.name)?.let(key.decode)

    /** Every preference of the user with [sub]. */
    context(_: JdbcTransaction)
    fun all(sub: String): Map<String, String> = UserPreferences.selectAll()
        .where { UserPreferences.userSub eq sub }
        .associate { it[UserPreferences.key] to it[UserPreferences.value] }

    /** Sets the preference [name] of the user with [sub] to [value], replacing the one it had. */
    context(_: JdbcTransaction)
    operator fun set(sub: String, name: String, value: String) {
        val updated = UserPreferences.update({ (UserPreferences.userSub eq sub) and (UserPreferences.key eq name) }) {
            it[UserPreferences.value] = value
        }
        if (updated == 0) insert(sub, name, value)
    }

    /** Sets [key] for the user with [sub] to [value], replacing the one it had. */
    context(_: JdbcTransaction)
    operator fun <T : Any> set(sub: String, key: UserPreferenceKey<T>, value: T) = set(sub, key.name, key.encode(value))

    /**
     * Sets the preference [name] of the user with [sub] to [value], unless they already have it.
     * @return `true` if it was set.
     */
    context(_: JdbcTransaction)
    fun setIfMissing(sub: String, name: String, value: String): Boolean {
        if (get(sub, name) != null) return false
        insert(sub, name, value)
        return true
    }

    /**
     * Sets [key] for the user with [sub] to [value], unless they already have it.
     * @return `true` if it was set.
     */
    context(_: JdbcTransaction)
    fun <T : Any> setIfMissing(sub: String, key: UserPreferenceKey<T>, value: T): Boolean =
        setIfMissing(sub, key.name, key.encode(value))

    /** Removes the preference [name] of the user with [sub]. */
    context(_: JdbcTransaction)
    fun remove(sub: String, name: String) {
        UserPreferences.deleteWhere { (userSub eq sub) and (key eq name) }
    }

    context(_: JdbcTransaction)
    private fun insert(sub: String, name: String, value: String) {
        UserPreferences.insert {
            it[userSub] = sub
            it[key] = name
            it[UserPreferences.value] = value
        }
    }
}
