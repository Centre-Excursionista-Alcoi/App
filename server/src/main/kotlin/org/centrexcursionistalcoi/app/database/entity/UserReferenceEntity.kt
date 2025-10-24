package org.centrexcursionistalcoi.app.database.entity

import io.ktor.http.ContentType
import kotlinx.datetime.toJavaLocalDate
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.integration.FEMECV
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class UserReferenceEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserReferenceEntity>(UserReferences) {
        context(_: JdbcTransaction)
        fun getOrProvide(session: UserSession): UserReferenceEntity = findById(session.sub)?.apply {
            // Update existing user
            this.username = session.username
            this.email = session.email
            this.groups = session.groups
        } ?: new(session.sub) {
            this.username = session.username
            this.email = session.email
            this.groups = session.groups
        }
    }

    var sub by UserReferences.sub
    var username by UserReferences.username
    var email by UserReferences.email
    var groups by UserReferences.groups

    var femecvUsername by UserReferences.femecvUsername
    var femecvPassword by UserReferences.femecvPassword
    var femecvLastSync by UserReferences.femecvLastSync

    context(_: JdbcTransaction)
    fun toData(lendingUser: LendingUser?, insurances: List<UserInsurance>?, departments: List<DepartmentMemberInfo>?) = UserData(
        sub = sub.value,
        username = username,
        email = email,
        groups = groups,
        lendingUser = lendingUser,
        insurances = insurances.orEmpty(),
        departments = departments.orEmpty(),
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
                        data = certificate
                        type = ContentType.Application.Pdf.toString()
                        name = "certificado.pdf"
                    }
                }

                Database {
                    UserInsuranceEntity.new {
                        userSub = sub
                        insuranceCompany = "FEMECV"
                        policyNumber = license.code
                        validFrom = license.validFrom.toJavaLocalDate()
                        validTo = license.validTo.toJavaLocalDate()
                        document = certificateEntity.id
                        femecvLicense = license
                    }
                }
            }
        }
    }

    context(_: JdbcTransaction)
    fun addFCMRegistrationToken(token: String, deviceId: String? = null) = FCMRegistrationTokenEntity.new(token) {
        this.user = this@UserReferenceEntity
        this.deviceId = deviceId
    }
}
