package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

/** The users holding each qualification, see [org.centrexcursionistalcoi.app.data.QualificationGrant]. */
object UserQualifications : Table("user_qualifications") {
    val qualification = reference("qualification_id", Qualifications, ReferenceOption.CASCADE, ReferenceOption.RESTRICT)
    val userSub = reference("sub", UserReferences, ReferenceOption.CASCADE, ReferenceOption.RESTRICT)

    /** The examiner that granted it. Kept (as `null`) if their account is later removed. */
    val grantedBy = optReference("granted_by", UserReferences, ReferenceOption.SET_NULL, ReferenceOption.RESTRICT)
    val grantedAt = timestamp("granted_at").defaultExpression(DatabaseNowExpression)

    /** `null` if the grant never expires. */
    val expiresAt = timestamp("expires_at").nullable()

    override val primaryKey = PrimaryKey(qualification, userSub, name = "PK_UserQualifications_qualification_userSub")
}
