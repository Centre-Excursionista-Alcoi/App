package org.centrexcursionistalcoi.app.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.ItemBooking
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking

@Dao
interface BookingsDao {
    @Insert
    suspend fun insertItemBooking(booking: ItemBooking)

    @Update
    suspend fun updateItemBooking(booking: ItemBooking)

    @Delete
    suspend fun deleteItemBooking(booking: ItemBooking)

    @Query("SELECT * FROM ItemBooking")
    suspend fun getAllItemBookings(): List<ItemBooking>

    @Query("SELECT * FROM ItemBooking")
    fun getAllItemBookingsAsFlow(): Flow<List<ItemBooking>>

    @Query("SELECT * FROM ItemBooking WHERE id = :id")
    suspend fun getItemBookingWithId(id: Int): ItemBooking?


    @Insert
    suspend fun insertSpaceBooking(booking: SpaceBooking)

    @Update
    suspend fun updateSpaceBooking(booking: SpaceBooking)

    @Delete
    suspend fun deleteSpaceBooking(booking: SpaceBooking)

    @Query("SELECT * FROM SpaceBooking")
    suspend fun getAllSpaceBookings(): List<SpaceBooking>

    @Query("SELECT * FROM SpaceBooking")
    fun getAllSpaceBookingsAsFlow(): Flow<List<SpaceBooking>>

    @Query("SELECT * FROM SpaceBooking WHERE id = :id")
    suspend fun getSpaceBookingWithId(id: Int): SpaceBooking?
}
