package org.centrexcursionistalcoi.app.nav

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.utils.toUuid

object UuidNavType : NavType<Uuid>(isNullableAllowed = false) {
    override fun put(bundle: SavedState, key: String, value: Uuid) {
        bundle.write {
            putString(key, value.toString())
        }
    }

    override fun get(bundle: SavedState, key: String): Uuid? {
        val value = bundle.read { getStringOrNull(key) } ?: return null
        return value.toUuid()
    }

    override fun parseValue(value: String): Uuid = value.toUuid()
}
