package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.room.entity.UserEntity.Companion.toEntity

class UsersRepository(db: AppDatabase) : Repository<UserData, String> {
    private val dao = db.userDao()

    override fun selectAllAsFlow() = dao
        .selectAllAsFlow()
        .map { list -> list.map { it.toUser() } }

    override suspend fun get(id: String): UserData? {
        return dao.get(id)?.toUser()
    }

    override fun getAsFlow(id: String): Flow<UserData?> {
        return dao
            .getAsFlow(id)
            .map { it?.toUser() }
    }

    override suspend fun selectAll() = dao.selectAll().map { it.toUser() }

    override suspend fun insert(item: UserData) = dao.insert(
        item.toEntity()
    )

    override suspend fun update(item: UserData) = dao.update(
        item.toEntity()
    )

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}
