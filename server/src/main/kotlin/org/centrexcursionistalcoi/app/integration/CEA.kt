package org.centrexcursionistalcoi.app.integration

import java.io.File
import java.time.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.csv.Csv
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.ConfigEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.security.NIFValidation
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.utils.generateRandomString
import org.slf4j.LoggerFactory

object CEA {
    @Serializable
    data class Member(
        @SerialName("Núm. soci/a")
        val number: Int,
        @SerialName("Estat")
        val status: String,
        @SerialName("Nom i cognoms")
        val fullName: String,
        @SerialName("NIF/NIE")
        val nif: String,
        @SerialName("Correu electrònic")
        val email: String,
    ) {
        val isDisabled = status.lowercase() != "alta"
    }

    /**
     * Sync once a day.
     */
    private const val SYNC_EVERY_SECONDS = 24 * 60 * 60 // 1 day

    private val logger = LoggerFactory.getLogger(this::class.java)

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
        for (member in members) {
            val isNifValid = NIFValidation.validate(member.nif)
            if (!isNifValid) {
                logger.warn("Invalid NIF for member number=${member.number}, NIF=${member.nif}. Skipping.")
                continue
            }

            val existingEntity = Database { UserReferenceEntity.findByNif(member.nif) }
            if (existingEntity != null) {
                // Update existing member
                Database {
                    existingEntity.fullName = member.fullName
                    existingEntity.email = member.email
                    existingEntity.isDisabled = member.isDisabled
                }
                logger.debug("Updated member NIF=${member.nif}, number=${member.number}, status=${member.status}")
            } else {
                // Create new member
                val randomSub = generateRandomString(16)
                Database {
                    UserReferenceEntity.new(randomSub) {
                        nif = member.nif
                        memberNumber = member.number.toUInt()
                        fullName = member.fullName
                        email = member.email
                        groups = listOf("cea_member")
                        isDisabled = member.isDisabled
                    }
                }
                logger.debug("Created new member NIF=${member.nif}, number=${member.number}, status=${member.status}")
            }
        }
        logger.info("Synchronization complete.")
    }

    fun synchronizeIfNeeded(clock: Clock = Clock.systemDefaultZone()) {
        val dataFile = File("/cea_members.csv")
        if (!dataFile.exists()) {
            logger.warn("CEA members data file ($dataFile) not found, skipping synchronization.")
            return
        }

        val now = clock.instant()
        val lastSync = Database { ConfigEntity[ConfigEntity.LastCEASync] }
        if (lastSync == null || now.epochSecond - lastSync.epochSecond >= SYNC_EVERY_SECONDS) {
            logger.info("Starting CEA members synchronization...")
            val data = dataFile.readText()
            val members = parse(data)
            synchronizeWithDatabase(members)
            Database { ConfigEntity[ConfigEntity.LastCEASync] = now }
            logger.info("CEA members synchronization finished.")
        } else {
            logger.info("CEA members synchronization not needed. Last sync at $lastSync.")
        }
    }
}
