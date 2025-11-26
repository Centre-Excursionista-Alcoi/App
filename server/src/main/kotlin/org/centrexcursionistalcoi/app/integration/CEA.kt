package org.centrexcursionistalcoi.app.integration

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlin.time.Duration.Companion.days
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
import org.centrexcursionistalcoi.app.security.NIFValidation
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.utils.generateRandomString
import org.jetbrains.exposed.v1.core.notInList
import org.slf4j.LoggerFactory

object CEA : PeriodicWorker(period = 1.days) {
    @Serializable
    data class Member(
        @SerialName("Núm. soci/a")
        val number: Int?,
        @SerialName("Estat")
        val status: String?,
        @SerialName("Nom i cognoms")
        val fullName: String?,
        @SerialName("NIF/NIE")
        val nif: String?,
        @SerialName("Correu electrònic")
        val email: String?,
    ) {
        /**
         * Whether the member is disabled (not "alta").
         */
        val isDisabled = status?.trim()?.equals("alta", true)?.not() ?: true

        val disabledReason = if (isDisabled) {
            if (status.equals("baixa", true)) {
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

    fun synchronizeWithDatabase(members: List<Member>) {
        logger.info("Synchronizing ${members.size} members with database...")
        logger.debug("Fetching all existing members...")
        val userSubList = mutableListOf<String>()
        for (member in members) {
            if (member.number == null) {
                logger.warn("Member has no number. Skipping.")
                continue
            }
            if (member.nif == null) {
                logger.warn("Member #${member.number} has no NIF. Skipping.")
                continue
            }
            if (member.fullName == null) {
                logger.warn("Member #${member.number} has no full name. Skipping.")
                continue
            }

            val isNifValid = NIFValidation.validate(member.nif)
            if (!isNifValid) {
                logger.warn("Invalid NIF for member number=${member.number}, NIF=${member.nif}. Skipping.")
                continue
            }

            val existingEntity = Database { UserReferenceEntity.findByNif(member.nif) }
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
        val memberNifs = Database { UserReferenceEntity.find { UserReferences.sub notInList userSubList } }
        for (entity in memberNifs) {
            Database {
                entity.isDisabled = true
                entity.disableReason = "not_in_cea_members"
            }
            logger.trace("Disabled member NIF=${entity.nif}, sub=${entity.sub.value}")
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

        val client = HttpClient {
            defaultRequest {
                url("https://centrexcursionistalcoi.org")
            }
            install(ContentNegotiation)
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
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

        val nonce = generateNonce()
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

            logger.info("CEA members data downloaded. Parsing...")
            val members = parse(cleanedData)

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
