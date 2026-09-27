package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

/**
 * The documents attached to an insurance: some companies hand out more than one (e.g. the policy and a card).
 *
 * A [UUIDTable] rather than a composite key like [PostFiles], so that each document can be addressed on its own
 * (the WebDAV `Insurances` directory lists one entry per document).
 */
object UserInsuranceDocuments : UUIDTable("user_insurance_documents") {
    val insurance = reference("insurance", UserInsurances, onDelete = ReferenceOption.CASCADE)
    val file = reference("file", Files, onDelete = ReferenceOption.RESTRICT)

    /** The order the documents were uploaded in, starting at 0. */
    val position = integer("position")

    init {
        uniqueIndex(insurance, file)
    }
}
