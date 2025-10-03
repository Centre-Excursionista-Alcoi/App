package org.centrexcursionistalcoi.app.database

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.observable.makeObservable
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.storage.settings

@OptIn(ExperimentalSettingsApi::class)
object ProfileRepository {
    val profile = settings
        .makeObservable()
        .getStringOrNullFlow("profile")
        .map { data -> data?.let { json.decodeFromString(ProfileResponse.serializer(), it) } }

    fun update(profile: ProfileResponse) {
        settings.putString("profile", json.encodeToString(ProfileResponse.serializer(), profile))
    }

    fun clear() {
        settings.remove("profile")
    }
}
