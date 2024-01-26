package screenmodel

import backend.data.UserData
import backend.supabase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.set
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import storage.SettingsKeys
import storage.settings
import utils.toInstant

class AuthScreenModel : ScreenModel {
    val isLoading = MutableStateFlow(false)

    val errors = MutableStateFlow<List<Throwable>>(emptyList())

    fun dismissError(index: Int) {
        errors.tryEmit(
            errors.value.toMutableList().apply { removeAt(index) }
        )
    }

    suspend fun notifyError(throwable: Throwable) {
        errors.emit(
            errors.value.toMutableList().apply { add(throwable) }
        )
    }

    fun login(email: String, password: String) {
        screenModelScope.launch(Dispatchers.IO) {
            try {
                isLoading.emit(true)

                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            } catch (e: RestException) {
                notifyError(e)
            } catch (e: HttpRequestTimeoutException) {
                notifyError(e)
            } catch (e: HttpRequestException) {
                notifyError(e)
            } finally {
                isLoading.emit(false)
            }
        }
    }

    @OptIn(ExperimentalSettingsApi::class)
    fun register(email: String, password: String, fullName: String, birthday: LocalDate) {
        screenModelScope.launch(Dispatchers.IO) {
            try {
                isLoading.emit(true)

                val data = UserData(fullName, birthday.toInstant())

                val result = supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    this.data = Json.encodeToJsonElement(data).jsonObject
                }
                if (result == null) {
                    notifyError(
                        IllegalStateException("Could not sign up.")
                    )
                } else {
                    settings[SettingsKeys.CONFIRMATION_ID] = result.id
                }
            } catch (e: RestException) {
                notifyError(e)
            } catch (e: HttpRequestTimeoutException) {
                notifyError(e)
            } catch (e: HttpRequestException) {
                notifyError(e)
            } finally {
                isLoading.emit(false)
            }
        }
    }
}
