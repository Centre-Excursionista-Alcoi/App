package org.centrexcursionistalcoi.app.database.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.room.entity.MemberEntity

@Dao
interface MemberDao {
    @Insert
    suspend fun insert(member: MemberEntity)

    @Query("SELECT * FROM Members WHERE memberNumber = :memberNumber")
    suspend fun get(memberNumber: Long): MemberEntity?

    @Query("SELECT * FROM Members WHERE memberNumber = :memberNumber")
    fun getAsFlow(memberNumber: Long): Flow<MemberEntity?>

    @Query("SELECT * FROM Members")
    suspend fun selectAll(): List<MemberEntity>

    @Query("SELECT * FROM Members")
    fun selectAllAsFlow(): Flow<List<MemberEntity>>

    @Query("DELETE FROM Members WHERE memberNumber = :memberNumber")
    suspend fun deleteById(memberNumber: Long)

    @Update
    suspend fun update(member: MemberEntity)
}
