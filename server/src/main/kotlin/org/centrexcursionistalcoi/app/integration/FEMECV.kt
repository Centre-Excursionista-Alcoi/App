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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.integration.femecv.FEMECVException
import org.centrexcursionistalcoi.app.integration.femecv.LicenseData
import org.centrexcursionistalcoi.app.storage.InMemoryStoreMap
import org.centrexcursionistalcoi.app.storage.RedisStoreMap
import org.jetbrains.annotations.VisibleForTesting
import org.slf4j.LoggerFactory

object FEMECV {
    private val logger = LoggerFactory.getLogger(FEMECV::class.java)

    private val cookiesStoreMap = RedisStoreMap.fromEnvOrNull() ?: InMemoryStoreMap()
    private val cookiesStorage = cookiesStoreMap.asCookiesStorage()

    /**
     * Number of days after which the cached licenses should be refreshed.
     */
    const val REFRESH_EVERY_DAYS = 7L

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
            defaultRequest {
                url("https://femecv.playoffinformatica.com")
            }
        }
    }

    suspend fun login(username: String, password: String, client: HttpClient = newClient()) {
        logger.debug("Logging in to FEMECV as $username...")
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
            logger.debug("Logged in to FEMECV as $username")
        } else {
            val body = response.bodyAsText()
            val document = Ksoup.parse(body)
            val errorBox = document.selectFirst(".errorBox")
            val errorMessage = errorBox?.text()

            logger.error("Could not log in to FEMECV as $username: $errorMessage")
            throw FEMECVException(errorMessage)
        }
    }

    suspend fun getLicense(client: HttpClient, id: Int): LicenseData {
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

            if (validFrom == null || validTo == null) {
                val modalityNameRegex = "FEMECV (20\\d{2})".toRegex()
                val modalityNameMatches = modalityNameRegex.find(modalityName.orEmpty())
                val modalityYear = modalityNameMatches?.groups?.get(1)?.value?.toIntOrNull()
                if (modalityYear != null) {
                    validFrom = LocalDate(modalityYear, 1, 1)
                    validTo = LocalDate(modalityYear, 12, 31)
                }
            }

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
            )
        } else {
            throw FEMECVException("Failed to retrieve license #$id, status code: ${response.status}")
        }
    }

    suspend fun getLicenses(username: String, password: String): List<LicenseData> {
        val client = newClient()
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
            logger.info("Retrieved ${licenseIds.size} licenses for user $username")
            // For demonstration purposes, we just log the URLs
            return licenseIds.map { id -> getLicense(client, id) }
        } else {
            throw FEMECVException("Failed to retrieve licenses, status code: ${response.status}")
        }
    }
}
