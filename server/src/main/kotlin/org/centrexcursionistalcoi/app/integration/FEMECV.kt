package org.centrexcursionistalcoi.app.integration

import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.PeriodicWorker
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.ConfigEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.integration.femecv.FEMECVException
import org.centrexcursionistalcoi.app.integration.femecv.FEMECVInvalidCredentialsException
import org.centrexcursionistalcoi.app.integration.femecv.LicenseData
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.storage.RedisStoreMap
import org.centrexcursionistalcoi.app.tracing.SentryHttpClientTracing
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.neq
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.days

object FEMECV : PeriodicWorker(
    period = 1.days,
) {
    private val logger = LoggerFactory.getLogger(FEMECV::class.java)

    private val cookiesStoreMap by lazy { RedisStoreMap.fromEnv }
    private val cookiesStorage by lazy { cookiesStoreMap.asCookiesStorage() }

    /**
     * Number of days after which the cached licenses should be refreshed.
     */
    const val REFRESH_EVERY_DAYS = 7L

    /**
     * Number of days between the reminders sent to a user whose saved credentials FEMECV rejects.
     */
    const val CREDENTIALS_REMINDER_EVERY_DAYS = 7L

    /** The text of FEMECV's login error that is about the username or the password (in Spanish or Catalan). */
    private val credentialsErrorRegex = Regex("usuari|contrase|password", RegexOption.IGNORE_CASE)

    /** Whether the error FEMECV answered a login with says the username or the password are wrong. */
    @VisibleForTesting
    internal fun isInvalidCredentialsMessage(message: String?): Boolean =
        message != null && credentialsErrorRegex.containsMatchIn(message)

    @VisibleForTesting
    var engine: HttpClientEngine = Java.create()

    @VisibleForTesting
    fun resetEngine() {
        engine = Java.create()
    }

    private fun newClient(cookiesStorage: CookiesStorage = FEMECV.cookiesStorage): HttpClient {
        return HttpClient(engine) {
            // We want to handle redirects manually to detect login success/failure
            followRedirects = false

            install(ContentNegotiation)
            install(HttpCookies) {
                storage = cookiesStorage
            }
            install(SentryHttpClientTracing)
            defaultRequest {
                url("https://femecv.playoffinformatica.com")
            }
        }
    }

    suspend fun login(username: String, password: String, client: HttpClient = newClient()) {
        logger.debug("Logging in to FEMECV...")
        val response = client.submitForm(
            url = "/FormLogin.php",
            formParameters = parameters {
                append("accio", "login")
                append("nomUsu_OBL", username)
                append("pasUsu_OBL", password)
            }
        )
        if (response.status == HttpStatusCode.Found) {
            // Login successful, session has been stored in cookies
            logger.debug("Logged in to FEMECV")
        } else {
            val body = response.bodyAsText()
            val document = Ksoup.parse(body)
            val errorBox = document.selectFirst(".errorBox")
            val errorMessage = errorBox?.text()

            if (isInvalidCredentialsMessage(errorMessage)) {
                // Expected when someone changes their password: not an error of ours
                logger.warn("FEMECV rejected the credentials: $errorMessage")
                throw FEMECVInvalidCredentialsException(errorMessage)
            }
            logger.error("Could not log in to FEMECV: $errorMessage")
            throw FEMECVException(errorMessage)
        }
    }

    suspend fun downloadLicenseCertificate(client: HttpClient, id: Int): ByteArray {
        val response = client.get("/print/printAutoritzacioLlicencia.php?accio=accio_imprimir_autoritzacio&idLlicencia=$id")
        return if (response.status.isSuccess()) {
            response.bodyAsBytes()
        } else {
            throw FEMECVException("Failed to retrieve license #$id (status=${response.status}): ${response.bodyAsText()}")
        }
    }

    suspend fun getLicense(client: HttpClient, id: Int): Pair<LicenseData, ByteArray> {
        val response = client.get("/FormLlicencia.php?accio=edit&idLlicencia=$id")
        val body = response.bodyAsText()
        if (response.status.isSuccess()) {
            val document = Ksoup.parse(body)
            val code = document.selectFirst("#codiLlicencia")?.value()
            val modalityIdElement = document.selectFirst("#idmodalitat")
            val modalityId = modalityIdElement?.value()?.toIntOrNull()
            val modalityName = modalityIdElement?.parent()?.selectFirst(".form-control-static")?.text()
            val club = document.selectFirst("#club")?.value()
            val categoryId = document.selectFirst("#idcategoria")?.value()
            val subCategoryId = document.selectFirst("#idsubcategoria")?.value()?.toIntOrNull()

            var validFrom = document.selectFirst(".tempsCompromisInici")?.text()?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
            var validTo = document.selectFirst(".tempsCompromisFi")?.text()?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }

            var isFullYear = false
            if (validFrom == null || validTo == null) {
                val modalityNameRegex = "FEMECV (20\\d{2})".toRegex()
                val modalityNameMatches = modalityNameRegex.find(modalityName.orEmpty())
                val modalityYear = modalityNameMatches?.groups?.get(1)?.value?.toIntOrNull()
                if (modalityYear != null) {
                    validFrom = LocalDate(modalityYear, 1, 1)
                    validTo = LocalDate(modalityYear, 12, 31)
                    isFullYear = true
                }
            }

            val certificate = downloadLicenseCertificate(client, id)

            return LicenseData(
                code = code ?: throw FEMECVException("License code not found for license #$id"),
                id = id,
                club = club ?: throw FEMECVException("Club not found for license #$id"),
                modalityId = modalityId ?: throw FEMECVException("Modality ID not found for license #$id"),
                modalityName = modalityName,
                validFrom = validFrom ?: throw FEMECVException("Valid from date not found for license #$id"),
                validTo = validTo ?: throw FEMECVException("Valid to date not found for license #$id"),
                categoryId = categoryId?.toIntOrNull() ?: throw FEMECVException("Category ID not found for license #$id"),
                subCategoryId = subCategoryId ?: throw FEMECVException("Subcategory ID not found for license #$id"),
                imageUrl = if (isFullYear) {
                    "/profile/femecvSync/image/${validFrom.year}"
                } else {
                    null
                },
            ) to certificate
        } else {
            throw FEMECVException("Failed to retrieve license #$id, status code: ${response.status}")
        }
    }

    suspend fun getLicenses(username: String, password: String): List<Pair<LicenseData, ByteArray>> = newClient().use { client ->
        login(username, password, client)

        val response = client.get("/PanellControlUsuariFederat.php?accio=edit")
        val body = response.bodyAsText()
        if (response.status.isSuccess()) {
            val document = Ksoup.parse(body)
            // document.body.querySelector('.container').querySelector('.row').children[1].querySelectorAll('.card')[1]
            val licensesCard = document.selectFirst(".container .row .card:nth-of-type(2)")
            // licensesCard.querySelector('.card-body').querySelectorAll('div')
            val licensesRows = licensesCard?.select(".card-body div")
            val licenseIds = licensesRows?.mapNotNull { row ->
                // licensesRows[0].querySelector('a').href
                val href = row.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                href.substringAfter("?")
                    .split('&')
                    .find { it.startsWith("idLlicencia") }
                    ?.substringAfter('=')
                    ?.toIntOrNull()
            }?.toSet().orEmpty()
            logger.info("Retrieved ${licenseIds.size} licenses")
            // For demonstration purposes, we just log the URLs
            return licenseIds.map { id -> getLicense(client, id) }
        } else {
            throw FEMECVException("Failed to retrieve licenses, status code: ${response.status}")
        }
    }

    /**
     * Tells [user] to enter their FEMECV credentials again, unless they were told in the last
     * [CREDENTIALS_REMINDER_EVERY_DAYS] days.
     * @return `true` if the notification was sent.
     */
    suspend fun remindInvalidCredentialsIfDue(user: UserReferenceEntity): Boolean {
        val due = Database {
            val last = user.femecvCredentialsReminderAt
            (last == null || now() - last >= CREDENTIALS_REMINDER_EVERY_DAYS.days).also {
                // Saved before sending: if sending fails, trying again every day would be worse than missing one
                if (it) user.femecvCredentialsReminderAt = now()
            }
        }
        if (!due) return false
        logger.info("Reminding user ${user.sub.value} that their FEMECV credentials are wrong")
        Push.sendPushNotification(user, PushNotification.FemecvCredentialsInvalid(user.sub.value), includeAdmins = false)
        return true
    }

    override suspend fun run() {
        syncUsers()

        logger.info("Updating last synchronization time...")
        Database { ConfigEntity[ConfigEntity.LastFEMECVSync] = now() }
    }

    /** Refreshes the licenses of every user with FEMECV credentials, or reminds those whose credentials are wrong. */
    @VisibleForTesting
    internal suspend fun syncUsers() {
        logger.info("Starting periodic FEMECV sync...")
        val users = Database {
            UserReferenceEntity.find {
                (UserReferences.femecvUsername neq null) and (UserReferences.femecvPassword neq null)
            }.toList()
        }

        logger.debug("Found ${users.count()} users with FEMECV credentials.")
        for (user in users) {
            try {
                if (Database { user.femecvCredentialsInvalidSince } != null) {
                    // Not tried again until the user changes them: FEMECV could lock the account
                    logger.debug("Skipping user ${user.sub.value}: FEMECV rejected their credentials")
                    remindInvalidCredentialsIfDue(user)
                    continue
                }
                user.refreshFEMECVData()
            } catch (e: FEMECVInvalidCredentialsException) {
                // Already saved and told to the user by refreshFEMECVData
                logger.warn("FEMECV rejected the credentials of user ${user.sub.value}")
            } catch (e: FEMECVException) {
                logger.error("Error syncing FEMECV data for user ${user.id.value} (${user.sub}): ${e.message}")
            } catch (e: Exception) {
                logger.error("Unexpected error syncing FEMECV data for user ${user.id.value} (${user.sub}): ${e.message}", e)
            }
        }
    }
}
