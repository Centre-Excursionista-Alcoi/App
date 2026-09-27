package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import org.centrexcursionistalcoi.app.database.table.UserInsuranceDocuments
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass

class UserInsuranceDocumentEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserInsuranceDocumentEntity>(UserInsuranceDocuments)

    var insurance by UserInsuranceEntity referencedOn UserInsuranceDocuments.insurance
    var file by FileEntity referencedOn UserInsuranceDocuments.file
    var position by UserInsuranceDocuments.position
}
