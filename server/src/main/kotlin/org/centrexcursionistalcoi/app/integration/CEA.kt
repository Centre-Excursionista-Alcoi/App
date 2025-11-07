package org.centrexcursionistalcoi.app.integration

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.csv.Csv
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.utils.generateRandomString
import org.jetbrains.exposed.v1.core.inList
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
    )

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
        val existingMembers = Database { UserReferenceEntity.find { UserReferences.nif inList members.map { it.nif } }.toList() }
        val nonExistingMembers = members.filter { member -> existingMembers.none { it.id.value == member.nif } }
        logger.debug("Found ${existingMembers.size} existing members, ${nonExistingMembers.size} new members.")
        logger.debug("Updating existing members...")
        Database {
            existingMembers.forEach { userReference ->
                val member = members.first { it.nif == userReference.id.value }
                userReference.nif = member.nif
                userReference.memberNumber = member.number.toUInt()
                userReference.fullName = member.fullName
                userReference.email = member.email
                userReference.isDisabled = member.status.equals("alta", ignoreCase = true).not()
            }
        }
        logger.debug("Inserting new members...")
        Database {
            nonExistingMembers.forEach { member ->
                val sub = generateRandomString(12)
                UserReferenceEntity.new(sub) {
                    nif = member.nif
                    memberNumber = member.number.toUInt()
                    fullName = member.fullName
                    email = member.email
                    isDisabled = member.status.equals("alta", ignoreCase = true).not()
                }
            }
        }
        logger.info("Synchronization complete.")
    }
}
