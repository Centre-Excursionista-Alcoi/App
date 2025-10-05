package org.centrexcursionistalcoi.app.database.entity

import kotlinx.datetime.toKotlinLocalDate
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.util.UUID
import kotlin.uuid.toKotlinUuid

class UserInsuranceEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<UserInsuranceEntity>(UserInsurances)

    var userSub by UserInsurances.userSub
    var insuranceCompany by UserInsurances.insuranceCompany
    var policyNumber by UserInsurances.policyNumber
    var validFrom by UserInsurances.validFrom
    var validTo by UserInsurances.validTo
    var document by UserInsurances.document

    context(_: JdbcTransaction)
    fun toData(): UserInsurance = UserInsurance(
        id = id.value.toKotlinUuid(),
        userSub = userSub,
        insuranceCompany = insuranceCompany,
        policyNumber = policyNumber,
        validFrom = validFrom.toKotlinLocalDate(),
        validTo = validTo.toKotlinLocalDate(),
        documentId = document?.value?.toKotlinUuid()
    )
}
