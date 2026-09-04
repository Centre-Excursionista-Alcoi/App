package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Insert
    suspend fun insert(member: org.centrexcursionistalcoi.app.database.entity.MemberEntity)

    @Query("SELECT * FROM Members WHERE memberNumber = :memberNumber")
    suspend fun get(memberNumber: Long): org.centrexcursionistalcoi.app.database.entity.MemberEntity?

    @Query("SELECT * FROM Members WHERE memberNumber = :memberNumber")
    fun getAsFlow(memberNumber: Long): Flow<org.centrexcursionistalcoi.app.database.entity.MemberEntity?>

    @Query("SELECT * FROM Members")
    suspend fun selectAll(): List<org.centrexcursionistalcoi.app.database.entity.MemberEntity>

    @Query("SELECT * FROM Members")
    fun selectAllAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.entity.MemberEntity>>

    @Query("DELETE FROM Members WHERE memberNumber = :memberNumber")
    suspend fun deleteById(memberNumber: Long)

    @Update
    suspend fun update(member: org.centrexcursionistalcoi.app.database.entity.MemberEntity)
}
