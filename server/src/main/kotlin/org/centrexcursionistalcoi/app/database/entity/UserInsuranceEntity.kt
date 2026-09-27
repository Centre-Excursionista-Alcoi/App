package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import kotlinx.datetime.toKotlinLocalDate
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.database.table.UserInsuranceDocuments
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select

class UserInsuranceEntity(id: EntityID<UUID>): UUIDEntity(id), EntityDataConverter<UserInsurance, Uuid> {
    companion object : UUIDEntityClass<UserInsuranceEntity>(UserInsurances)

    var userSub by UserReferenceEntity referencedOn UserInsurances.userSub
    var insuranceCompany by UserInsurances.insuranceCompany
    var policyNumber by UserInsurances.policyNumber
    var validFrom by UserInsurances.validFrom
    var validTo by UserInsurances.validTo

    var femecvLicense by UserInsurances.femecvLicense

    /**
     * The ids of this insurance's documents, in upload order. Read straight from [UserInsuranceDocuments], without
     * loading each [FileEntity].
     */
    context(_: JdbcTransaction)
    fun documentIds(): List<UUID> = UserInsuranceDocuments
        .select(UserInsuranceDocuments.file)
        .where { UserInsuranceDocuments.insurance eq id }
        .orderBy(UserInsuranceDocuments.position to SortOrder.ASC)
        .map { it[UserInsuranceDocuments.file].value }

    /** Attaches [files] to this insurance, after any documents it already has. */
    context(_: JdbcTransaction)
    fun addDocuments(files: List<FileEntity>) {
        val maxPosition = UserInsuranceDocuments.position.max()
        var position = UserInsuranceDocuments
            .select(maxPosition)
            .where { UserInsuranceDocuments.insurance eq id }
            .firstOrNull()
            ?.get(maxPosition)
            ?.plus(1)
            ?: 0
        for (file in files) {
            UserInsuranceDocumentEntity.new {
                insurance = this@UserInsuranceEntity
                this.file = file
                this.position = position++
            }
        }
    }

    context(_: JdbcTransaction)
    override fun toData(): UserInsurance {
        val documents = documentIds().map { it.toKotlinUuid() }
        return UserInsurance(
            id = id.value.toKotlinUuid(),
            userSub = userSub.id.value,
            insuranceCompany = insuranceCompany,
            policyNumber = policyNumber,
            validFrom = validFrom.toKotlinLocalDate(),
            validTo = validTo.toKotlinLocalDate(),
            documentId = documents.firstOrNull(),
            documents = documents,
            femecvLicense = femecvLicense,
        )
    }

    override fun delete() {
        val documents = UserInsuranceDocumentEntity.find { UserInsuranceDocuments.insurance eq id }.map { it.file }
        super.delete() // user_insurance_documents cascades
        FileEntity.deleteOwnedFiles(documents)
    }
}
