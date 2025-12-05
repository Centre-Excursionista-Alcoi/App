package org.centrexcursionistalcoi.app.integration

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.csv.Csv
import org.centrexcursionistalcoi.app.PeriodicWorker
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.ConfigEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.exception.HttpResponseException
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.security.EmailValidation
import org.centrexcursionistalcoi.app.security.NIFValidation
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.utils.generateRandomString
import org.jetbrains.exposed.v1.core.notInList
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.time.Duration.Companion.days

object CEA : PeriodicWorker(period = 1.days) {
    @Serializable
    data class Member(
        @SerialName("Núm. soci/a")
        val number: Int? = null,
        @SerialName("Estat")
        val status: String? = null,
        @SerialName("Nom i cognoms")
        val fullName: String? = null,
        @SerialName("NIF/NIE")
        val nif: String? = null,
        @SerialName("Correu electrònic")
        val email: String? = null,
    ) {
        /**
         * Whether the member is disabled (not "alta").
         */
        val isDisabled = !NIFValidation.validate(nif) || !EmailValidation.validate(email) || status?.trim()?.equals("alta", true)?.not() ?: true

        val disabledReason = if (isDisabled) {
            if (!NIFValidation.validate(nif)) {
                "invalid_nif"
            } else if (!EmailValidation.validate(email)) {
                "invalid_email"
            } else if (status.equals("baixa", true)) {
                "status_baixa"
            } else if (status.equals("pendent", true)) {
                "status_pendent"
            } else {
                "status_unknown"
            }
        } else {
            null
        }
    }

    fun List<Member>.filterInvalid() = filter { member ->
        if (member.number == null) {
            logger.warn("Member has no number. Skipping.")
            return@filter false
        }
        if (member.fullName == null) {
            logger.warn("Member #${member.number} has no full name. Skipping.")
            return@filter false
        }
        true
    }

    /**
     * Sync once a day.
     */
    private const val SYNC_EVERY_SECONDS = 24 * 60 * 60 // 1 day

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Parses the CEA members data from a CSV string.
     * @param data The CSV data as a string.
     * @return A list of [Member] objects.
     * @throws SerializationException if the data cannot be parsed.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun parse(data: String): List<Member> {
        return Csv {
            hasHeaderRecord = true
            ignoreUnknownColumns = true
        }.decodeFromString(Member.serializer().list(), data)
    }

    /**
     * Cleans up the members' NIFs by correcting invalid letters.
     * @param members The list of members to clean up.
     * @return A new list of members with corrected NIFs.
     */
    fun cleanupMembersNIF(members: List<Member>) = members.map { member ->
        if (!NIFValidation.validate(member.nif)) {
            val letter = NIFValidation.calculateLetter(member.nif)
            if (letter != null) {
                val cleanedNif = member.nif!!.dropLast(1) + letter
                logger.info("Corrected NIF for member #${member.number}: ${member.nif} -> $cleanedNif")
                member.copy(nif = cleanedNif)
            } else {
                member
            }
        } else {
            member
        }
    }

    fun synchronizeWithDatabase(members: List<Member>) {
        logger.info("Synchronizing ${members.size} members with database...")
        logger.debug("Fetching all existing members...")
        val userSubList = mutableListOf<String>()
        for (member in members.filterInvalid()) {
            member.number!!
            member.fullName!!

            val existingEntity = if (member.email != null) Database { UserReferenceEntity.findByEmail(member.email) } else null
            if (existingEntity != null) {
                // Update existing member
                Database {
                    existingEntity.fullName = member.fullName.trim('_')
                    existingEntity.email = member.email
                    existingEntity.isDisabled = member.isDisabled
                    existingEntity.disableReason = member.disabledReason
                }
                userSubList.add(existingEntity.sub.value)
                logger.debug("Updated member NIF=${member.nif}, number=${member.number}, status=${member.status}")
            } else {
                // Create new member
                val randomSub = generateRandomString(16)
                Database {
                    UserReferenceEntity.new(randomSub) {
                        nif = member.nif
                        memberNumber = member.number.toUInt()
                        fullName = member.fullName.trim('_')
                        email = member.email
                        groups = listOf("cea_member")

                        isDisabled = member.isDisabled
                        disableReason = member.disabledReason
                    }
                }
                userSubList.add(randomSub)
                logger.debug("Created new member NIF=${member.nif}, number=${member.number}, status=${member.status}")
            }
        }
        logger.debug("Disabling not found users...")
        // Disable users not in the CEA members list
        val membersList = Database { UserReferenceEntity.find { UserReferences.sub notInList userSubList }.toList() }
        for (entity in membersList) {
            if (entity.nif == "87654321X") {
                // Skip test user
                continue
            }
            Database {
                entity.isDisabled = true
                entity.disableReason = "not_in_cea_members"
            }
            logger.trace("Disabled member email=${entity.email}, sub=${entity.sub.value}")
        }
        logger.info("Synchronization complete.")
    }

    /**
     * Synchronizes the CEA members data with the database if needed.
     * @suspend
     * @throws HttpResponseException if the download fails.
     * @throws SerializationException if the data cannot be parsed.
     */
    suspend fun synchronizeIfNeeded() {
        val now = now()
        val lastSync = Database { ConfigEntity[ConfigEntity.LastCEASync] }
        if (lastSync == null || now.epochSecond - lastSync.epochSecond >= SYNC_EVERY_SECONDS) {
            run()
        } else {
            logger.info("CEA members synchronization not needed. Last sync at $lastSync.")
        }
    }

    private fun generateNonce(length: Int = 10): String {
        val charset = ('a'..'f') + ('0'..'9')
        return List(length) { charset.random() }.joinToString("")
    }

    /**
     * Downloads the CEA members data CSV from the CEA website.
     * @suspend
     * @return The CSV data as a string.
     * @throws HttpResponseException if the login or data export fails.
     * @throws IllegalStateException if the `CEA_USERNAME` or `CEA_PASSWORD` environment variables are not set.
     */
    suspend fun download(): String {
        val username = System.getenv("CEA_USERNAME")
            ?: throw IllegalStateException("CEA_USERNAME environment variable is not set.")
        val password = System.getenv("CEA_PASSWORD")
            ?: throw IllegalStateException("CEA_PASSWORD environment variable is not set.")

        val cookiesStorage = AcceptAllCookiesStorage()
        val client = HttpClient {
            defaultRequest {
                url("https://centrexcursionistalcoi.org")
            }
            install(ContentNegotiation)
            install(HttpCookies) {
                storage = cookiesStorage
            }
            install(Logging) {
                level = LogLevel.HEADERS
                logger = object : Logger {
                    override fun log(message: String) {
                        CEA.logger.debug(message)
                    }
                }
            }
        }

        logger.debug("Logging in to CEA website...")
        val loginResponse = client.submitForm(
            url = "/wp-login.php",
            formParameters = parameters {
                append("log", username)
                append("pwd", password)
                append("rememberme", "forever")
                append("wp-submit", "Entra")
                append("redirect_to", "https://centrexcursionistalcoi.org/wp-admin/testcookie=1")
            },
        )
        if (loginResponse.status == HttpStatusCode.Found) {
            logger.debug("Logged in to CEA website successfully.")
        } else {
            logger.error("Failed to log in to CEA website.\n\tStatus: ${loginResponse.status}\n\tBody: ${loginResponse.bodyAsText()}")
            throw HttpResponseException(
                "Failed to log in to CEA website.",
                loginResponse.status.value,
                loginResponse.bodyAsText(),
            )
        }

        val ceaPageResponse = client.get("/wp-admin/admin.php?page=cea-members&tab-filter-status=0")
        val exportUrl = if (ceaPageResponse.status.isSuccess()) {
            val bodyText = ceaPageResponse.bodyAsText()
            val urlRegex = "https://centrexcursionistalcoi\\.org/wp-admin/admin\\.php\\?page=cea-members&tab-filter-status=0&action=export&nonce=[0-9a-f]{10}".toRegex()
            val matchResult = urlRegex.find(bodyText)
            if (matchResult != null) {
                val exportUrl = matchResult.value
                logger.debug("Found export URL: $exportUrl")
                exportUrl
            } else {
                logger.error("Failed to find export URL in CEA members page.")
                throw IllegalArgumentException("Failed to find export URL in CEA members page.")
            }
        } else {
            logger.error("Failed to access CEA members page. Status: ${ceaPageResponse.status}")
            throw HttpResponseException(
                "Failed to access CEA members page.",
                loginResponse.status.value,
                loginResponse.bodyAsText(),
            )
        }

        /*val nonce = generateNonce()
        logger.debug("Exporting CEA members data (nonce=$nonce)...")
        val exportResponse = client.get("/wp-admin/admin.php?page=cea-members&tab-filter-status=0&action=export&nonce=$nonce")
        if (exportResponse.status == HttpStatusCode.OK) {
            logger.debug("Downloaded CEA members data successfully.")
            return exportResponse.bodyAsText()
        } else {
            logger.error("Failed to export CEA members data. Status: ${exportResponse.status}")
            throw HttpResponseException(
                "Failed to export data from the CEA website.",
                loginResponse.status.value,
                loginResponse.bodyAsText(),
            )
        }*/

        logger.debug("Exporting CEA members data from $exportUrl ...")
        val exportResponse = client.get(Url(exportUrl))
        if (exportResponse.status == HttpStatusCode.OK) {
            logger.debug("Downloaded CEA members data successfully.")
            return exportResponse.bodyAsText()
        } else {
            logger.error("Failed to export CEA members data. Status: ${exportResponse.status}")
            throw HttpResponseException(
                "Failed to export data from the CEA website.",
                exportResponse.status.value,
                exportResponse.bodyAsText(),
            )
        }
    }

    override suspend fun run() {
        try {
            logger.info("Starting CEA members synchronization...")
            val data = download()

            logger.info("Cleaning up data...")
            val cleanedData = data.split("\n")
                .map { line -> line.trim() }
                // Remove empty lines
                .filter { line -> line.isNotEmpty() }
                .joinToString("\n")

            logger.info("Storing downloaded file...")
            File("/cea_members.csv").writeText(cleanedData)

            logger.info("CEA members data downloaded. Parsing...")
            var members = parse(cleanedData)

            logger.info("Cleaning up members' NIFs...")
            members = cleanupMembersNIF(members)

            logger.info("CEA members data parsed. Synchronizing with database...")
            synchronizeWithDatabase(members)

            logger.info("Updating last synchronization time...")
            Database { ConfigEntity[ConfigEntity.LastCEASync] = now() }

            logger.info("CEA members synchronization finished.")
        } catch (e: HttpResponseException) {
            logger.error("Failed to synchronize CEA members: ${e.statusCode} ${e.message}")
        } catch (e: SerializationException) {
            logger.error("Failed to parse CEA members data: ${e.message}")
        } catch (e: Exception) {
            logger.error("Unexpected error during CEA members synchronization", e)
        }
    }
}
