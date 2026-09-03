package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.database.room.entity.MemberEntity.Companion.toEntity
import org.koin.core.annotation.Singleton

@Singleton
class MembersRepository(db: AppDatabase) : Repository<Member, UInt> {
    private val dao = db.memberDao()

    override fun selectAllAsFlow() = dao
        .selectAllAsFlow()
        .map { list -> list.map { it.toMember() } }

    override suspend fun selectAll() = dao.selectAll().map { it.toMember() }

    override suspend fun get(id: UInt): Member? {
        return dao.get(id.toLong())?.toMember()
    }

    override fun getAsFlow(id: UInt): Flow<Member?> {
        return dao
            .getAsFlow(id.toLong())
            .map { it?.toMember() }
    }

    override suspend fun insert(item: Member) = dao.insert(
        item.toEntity()
    )

    override suspend fun update(item: Member) = dao.update(
        item.toEntity()
    )

    override suspend fun delete(id: UInt) {
        dao.deleteById(id.toLong())
    }
}
