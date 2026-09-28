package org.centrexcursionistalcoi.app.database.entity

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.database.table.UserInsuranceDocuments
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass

class UserInsuranceDocumentEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<UserInsuranceDocumentEntity>(UserInsuranceDocuments)

    var insurance by UserInsuranceEntity referencedOn UserInsuranceDocuments.insurance
    var file by FileEntity referencedOn UserInsuranceDocuments.file
    var position by UserInsuranceDocuments.position
}
