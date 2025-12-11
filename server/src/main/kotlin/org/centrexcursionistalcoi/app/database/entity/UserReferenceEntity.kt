package org.centrexcursionistalcoi.app.database.entity

import io.ktor.http.*
import kotlinx.datetime.toJavaLocalDate
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.integration.FEMECV
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.upperCase
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.slf4j.LoggerFactory

class UserReferenceEntity(id: EntityID<String>) : Entity<String>(id), LastUpdateEntity {
    companion object : EntityClass<String, UserReferenceEntity>(UserReferences) {
        private val logger = LoggerFactory.getLogger(UserReferenceEntity::class.java)

        /**
         * Finds a user by their nif, case-insensitively.
         */
        context(_: JdbcTransaction)
        fun findByNif(nif: String): UserReferenceEntity? =
            find { UserReferences.nif.upperCase() eq nif.uppercase() }.limit(1).firstOrNull()

        /**
         * Finds a user by their email address, case-insensitively.
         */
        context(_: JdbcTransaction)
        fun findByEmail(email: String): UserReferenceEntity? =
            find { UserReferences.email.upperCase() eq email.uppercase() }.limit(1).firstOrNull()

        /**
         * Finds a user by either their email or nif, case-insensitively.
         * Returns null if both [email] and [nif] are null.
         */
        context(_: JdbcTransaction)
        fun findByEmailOrNif(email: String?, nif: String?): UserReferenceEntity? {
            if (email == null && nif == null) return null
            email?.let { findByEmail(it) }?.let { return it }
            nif?.let { findByNif(it) }?.let { return it }
            return null
        }
    }

    var sub by UserReferences.sub

    override var lastUpdate by UserReferences.lastUpdate

    var nif by UserReferences.nif
    var memberNumber by UserReferences.memberNumber
    var fullName by UserReferences.fullName
    var email by UserReferences.email
    var groups by UserReferences.groups

    var isDisabled by UserReferences.isDisabled

    var password by UserReferences.password

    var femecvUsername by UserReferences.femecvUsername
    var femecvPassword by UserReferences.femecvPassword
    var femecvLastSync by UserReferences.femecvLastSync

    fun isAdmin() = groups.contains(ADMIN_GROUP_NAME)

    context(_: JdbcTransaction)
    fun toData(lendingUser: LendingUser?, insurances: List<UserInsurance>?, departments: List<DepartmentMemberInfo>?) = UserData(
        sub = sub.value,
        memberNumber = memberNumber,
        fullName = fullName,
        email = email,
        groups = groups,
        lendingUser = lendingUser,
        insurances = insurances.orEmpty(),
        departments = departments.orEmpty(),
        isDisabled = isDisabled,
    )

    suspend fun refreshFEMECVData() {
        val username = femecvUsername
        val password = femecvPassword
        if (username == null || password == null) return
        val licenses = FEMECV.getLicenses(username, password)
        val existingLicenseEntities = Database { UserInsuranceEntity.find { (UserInsurances.userSub eq sub.value) and (UserInsurances.femecvLicense neq null) }.toList() }
        for ((license, certificate) in licenses) {
            val matchingEntity = existingLicenseEntities.find { it.femecvLicense?.id == license.id }
            if (matchingEntity != null) {
                // License already exists, ignore
            } else {
                val certificateEntity = Database {
                    FileEntity.new {
                        bytes = certificate
                        contentType = ContentType.Application.Pdf
                        name = "certificado.pdf"
                    }
                }

                Database {
                    UserInsuranceEntity.new {
                        userSub = this@UserReferenceEntity
                        insuranceCompany = "FEMECV"
                        policyNumber = license.code
                        validFrom = license.validFrom.toJavaLocalDate()
                        validTo = license.validTo.toJavaLocalDate()
                        document = certificateEntity
                        femecvLicense = license
                    }
                }
            }
        }
        updated()
    }

    context(_: JdbcTransaction)
    fun addFCMRegistrationToken(token: String, deviceId: String? = null) = FCMRegistrationTokenEntity.new(token) {
        this.user = this@UserReferenceEntity
        this.deviceId = deviceId
    }

    /**
     * Notifies that this entity has been updated by storing the current timestamp in Redis, and updating the lastUpdate field.
     */
    override suspend fun updated() {
        val now = now()
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now }
        logger.debug("Updating lastUpdate for UserReferenceEntity#{}: {}", id, now)
    }
}
