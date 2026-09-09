package org.centrexcursionistalcoi.app.auth

import com.diamondedge.logging.logging
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.network.getHttpClient
import org.centrexcursionistalcoi.app.push.FCMTokenManager
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
import org.centrexcursionistalcoi.app.storage.settings
import org.koin.core.annotation.Singleton

@Singleton
class AuthBackend(
    private val lendingsRepository: LendingsRepository,
    private val inventoryItemsRepository: InventoryItemsRepository,
    private val inventoryItemTypesRepository: InventoryItemTypesRepository,
    private val eventsRepository: EventsRepository,
    private val postsRepository: PostsRepository,
    private val membersRepository: MembersRepository,
    private val usersRepository: UsersRepository,
    private val departmentsRepository: DepartmentsRepository,
    private val memoriesRepository: MemoriesRepository,
    private val credentialsStore: CredentialsStore,
) {
    private val log = logging()
    
    suspend fun register(email: String, password: String) {
        val response = getHttpClient().submitForm(
            url = "/register",
            formParameters = parameters {
                append("email", email)
                append("password", password)
            }
        )
        if (response.status.isSuccess()) {
            log.d { "Registration successful." }
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    suspend fun login(email: String, password: String) {
        // Clear storage before logging in. This clears the cookies
        settings.clear()

        val response = getHttpClient().submitForm(
            url = "/login",
            formParameters = parameters {
                append("email", email)
                append("password", password)
            }
        )
        if (response.status.isSuccess()) {
            log.d { "Login successful." }
            credentialsStore.save(email, password)
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    /**
     * Tries to silently re-authenticate using the credentials saved from the last successful [login] (see
     * [CredentialsStore] -- Android only for now). Used when a session expires unexpectedly, so the user isn't
     * bounced back to the login screen for what's often just an expired cookie.
     * @return `true` if re-authentication succeeded (a fresh session is now active); `false` if there were no
     * saved credentials, or they were rejected -- in which case they're cleared, and the caller should fall
     * back to a normal [logout].
     */
    suspend fun tryAutoRelogin(): Boolean {
        val saved = credentialsStore.get() ?: return false
        return try {
            login(saved.email, saved.password)
            log.d { "Automatic re-login succeeded." }
            true
        } catch (e: Exception) {
            log.w(e) { "Automatic re-login failed." }
            credentialsStore.clear()
            false
        }
    }

    @Suppress("KNOWN_EXCEPTION") // suppress because order is correct, and there won't be missing references
    suspend fun logout() {
        val response = getHttpClient().get("/logout")
        if (response.status.isSuccess()) {
            log.d { "Logged out. Removing all data..." }
            // order is important due to foreign key constraints: children before their parents
            // (Memories has FKs to both Lendings and Departments, see MemoryEntity)
            memoriesRepository.deleteAll()
            lendingsRepository.deleteAll()
            inventoryItemsRepository.deleteAll()
            inventoryItemTypesRepository.deleteAll()
            eventsRepository.deleteAll()
            postsRepository.deleteAll()
            membersRepository.deleteAll()
            usersRepository.deleteAll()
            departmentsRepository.deleteAll()
            log.d { "Removing all files..." }
            FileSystem.deleteAll().also { log.v { "$it files were deleted." } }
            log.d { "Revoking FCM token..." }
            FCMTokenManager.revoke()
            log.d { "Removing all settings..." }
            settings.clear()
            credentialsStore.clear()
        } else {
            val error = response.bodyAsError()
            log.d { "Logout failed (${response.status}): $error" }
            throw error.toThrowable()
        }
    }

    suspend fun forgotPassword(email: String) {
        val response = getHttpClient().submitForm(
            url = "/lost_password",
            formParameters = parameters {
                append("email", email)
            }
        )
        if (response.status.isSuccess()) {
            log.d { "Forgot password request successful." }
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    suspend fun deleteAccount() {
        val response = getHttpClient().post("/delete_account")
        if (response.status.isSuccess()) {
            log.w { "Account delete request successful." }
            log.w { "Account deleted from server. Removing all data..." }
            // order is important due to foreign key constraints: children before their parents
            // (Memories has FKs to both Lendings and Departments, see MemoryEntity)
            memoriesRepository.deleteAll()
            lendingsRepository.deleteAll()
            inventoryItemsRepository.deleteAll()
            inventoryItemTypesRepository.deleteAll()
            eventsRepository.deleteAll()
            postsRepository.deleteAll()
            membersRepository.deleteAll()
            usersRepository.deleteAll()
            departmentsRepository.deleteAll()
            log.w { "Removing all files..." }
            FileSystem.deleteAll().also { log.v { "$it files were deleted." } }
            log.w { "Revoking FCM token..." }
            FCMTokenManager.revoke()
            log.w { "Removing all settings..." }
            settings.clear()
            credentialsStore.clear()
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }
}
