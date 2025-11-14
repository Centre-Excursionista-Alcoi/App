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
import org.centrexcursionistalcoi.app.now
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
        fun findByNif(nif: String): UserReferenceEntity? = find { UserReferences.nif eq nif }.limit(1).firstOrNull()
    }

    var sub by UserReferences.sub

    var lastUpdate by UserReferences.lastUpdate

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

    context(_: JdbcTransaction)
    fun toData(lendingUser: LendingUser?, insurances: List<UserInsurance>?, departments: List<DepartmentMemberInfo>?) = UserData(
        sub = sub.value,
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
                        document = certificateEntity
                        femecvLicense = license
                    }
                }
            }
        }
        Database { lastUpdate = now() }
    }

    context(_: JdbcTransaction)
    fun addFCMRegistrationToken(token: String, deviceId: String? = null) = FCMRegistrationTokenEntity.new(token) {
        this.user = this@UserReferenceEntity
        this.deviceId = deviceId
    }
}
