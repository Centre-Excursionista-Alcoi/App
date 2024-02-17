package screenmodel

import backend.wrapper.SupabaseWrapper
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.ExperimentalSettingsApi
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import storage.SettingsKeys.CONFIRMATION_ID
import storage.SettingsKeys.TEMP_USER_EMAIL
import storage.SettingsKeys.TEMP_USER_PASSWORD
import storage.settings

class ConfirmationScreenModel : ScreenModel {
    @OptIn(ExperimentalSettingsApi::class)
    fun verify() {
        screenModelScope.launch(Dispatchers.Default) {
            val email = settings.getStringOrNull(TEMP_USER_EMAIL)
            val password = settings.getStringOrNull(TEMP_USER_PASSWORD)

            if (email == null || password == null) {
                println("There isn't any email or password stored in memory, cannot verify email.")
                settings.remove(TEMP_USER_EMAIL)
                settings.remove(TEMP_USER_PASSWORD)
                settings.remove(CONFIRMATION_ID)
                return@launch
            }

            SupabaseWrapper.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }
    }
}
