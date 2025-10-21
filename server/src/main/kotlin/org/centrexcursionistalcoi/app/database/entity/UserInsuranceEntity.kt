package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import kotlinx.datetime.toKotlinLocalDate
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class UserInsuranceEntity(id: EntityID<UUID>): UUIDEntity(id), EntityDataConverter<UserInsurance, Uuid> {
    companion object : UUIDEntityClass<UserInsuranceEntity>(UserInsurances)

    var userSub by UserInsurances.userSub
    var insuranceCompany by UserInsurances.insuranceCompany
    var policyNumber by UserInsurances.policyNumber
    var validFrom by UserInsurances.validFrom
    var validTo by UserInsurances.validTo
    var document by UserInsurances.document

    var femecvLicense by UserInsurances.femecvLicense

    context(_: JdbcTransaction)
    override fun toData(): UserInsurance = UserInsurance(
        id = id.value.toKotlinUuid(),
        userSub = userSub.value,
        insuranceCompany = insuranceCompany,
        policyNumber = policyNumber,
        validFrom = validFrom.toKotlinLocalDate(),
        validTo = validTo.toKotlinLocalDate(),
        documentId = document?.value?.toKotlinUuid(),
        femecvLicense = femecvLicense,
    )
}
