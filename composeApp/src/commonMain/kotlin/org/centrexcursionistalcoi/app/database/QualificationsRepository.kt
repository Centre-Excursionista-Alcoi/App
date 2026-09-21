package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.entity.QualificationGrantEntity.Companion.toEntity
import org.koin.core.annotation.Singleton

/**
 * The qualification definitions and the logged-in user's own grants, kept locally so events can show what they
 * require (and whether the user qualifies) without a connection.
 *
 * Both lists are small and are always fetched whole from the server, so each is replaced as a whole ([replaceAll]
 * and [replaceMyGrants]) rather than diffed. Not a [Repository]: nothing here is looked up or edited one by one.
 */
@Singleton
class QualificationsRepository(db: AppDatabase) {
    private val qualificationDao = db.qualificationDao()
    private val grantDao = db.qualificationGrantDao()

    fun qualificationsAsFlow(): Flow<List<Qualification>> =
        qualificationDao.selectAllAsFlow().map { list -> list.map { it.toQualification() } }

    fun myGrantsAsFlow(): Flow<List<QualificationGrant>> =
        grantDao.selectAllAsFlow().map { list -> list.map { it.toGrant() } }

    suspend fun replaceAll(qualifications: List<Qualification>) {
        if (qualifications.isEmpty()) {
            qualificationDao.deleteAll()
        } else {
            qualificationDao.upsertAll(qualifications.map { it.toEntity() })
            qualificationDao.deleteAllExcept(qualifications.map { it.id })
        }
    }

    suspend fun replaceMyGrants(grants: List<QualificationGrant>) {
        if (grants.isEmpty()) {
            grantDao.deleteAll()
        } else {
            grantDao.upsertAll(grants.map { it.toEntity() })
            grantDao.deleteAllExcept(grants.map { it.qualificationId })
        }
    }

    suspend fun deleteAll() {
        qualificationDao.deleteAll()
        grantDao.deleteAll()
    }
}
