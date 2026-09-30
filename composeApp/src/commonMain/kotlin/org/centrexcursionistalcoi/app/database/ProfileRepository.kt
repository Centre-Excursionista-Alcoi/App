package org.centrexcursionistalcoi.app.database

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.koin.core.annotation.Singleton

@Singleton
class ProfileRepository(
    private val settings: SettingsStore
) {
    private val profileKey = stringPreferencesKey("profile")

    val profile: Flow<ProfileResponse?> = settings.getFlow(profileKey, ProfileResponse.serializer())

    suspend fun getProfile(): ProfileResponse? {
        return settings.get(profileKey, ProfileResponse.serializer())
    }

    suspend fun isLoggedIn(): Boolean = getProfile() != null

    suspend fun update(profile: ProfileResponse) {
        settings.set(profileKey, ProfileResponse.serializer(), profile)
    }

    suspend fun clear() {
        settings.remove(profileKey)
    }
}
