package org.centrexcursionistalcoi.app.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.Space

@Dao
interface SpacesDao {
    @Insert
    suspend fun insertSpace(space: Space)

    @Update
    suspend fun updateSpace(space: Space)

    @Delete
    suspend fun deleteSpace(space: Space)

    @Query("SELECT * FROM Space")
    suspend fun getAllSpaces(): List<Space>

    @Query("SELECT * FROM Space")
    fun getAllSpacesAsFlow(): Flow<List<Space>>

    @Query("SELECT * FROM Space WHERE id = :id")
    suspend fun getSpaceById(id: Int): Space?
}
