package org.centrexcursionistalcoi.app.nav

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

abstract class SerializableNavType<T : Any>(
    private val serializer: KSerializer<T>,
    private val json: Json = org.centrexcursionistalcoi.app.json,
    isNullableAllowed: Boolean = true
) : NavType<T>(isNullableAllowed) {
    override fun put(bundle: SavedState, key: String, value: T) {
        val string = json.encodeToString(serializer, value)
        bundle.write {
            putString(key, string)
        }
    }

    override fun get(bundle: SavedState, key: String): T? {
        val string = bundle.read { getStringOrNull(key) } ?: return null
        return json.decodeFromString(serializer, string)
    }

    override fun parseValue(value: String): T = json.decodeFromString(serializer, value)
}
