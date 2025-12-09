package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.database.data.Members
import org.centrexcursionistalcoi.app.storage.databaseInstance

object MembersRepository : DatabaseRepository<Member, UInt>() {
    override val queries by lazy { databaseInstance.membersQueries }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher) = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)
        .map { list -> list.map { it.toMember() } }

    override suspend fun get(id: UInt): Member? {
        return queries.get(id.toLong()).awaitAsList().firstOrNull()?.toMember()
    }

    override fun getAsFlow(id: UInt, dispatcher: CoroutineDispatcher): Flow<Member?> {
        return queries
            .get(id.toLong())
            .asFlow()
            .mapToList(dispatcher)
            .map { it.firstOrNull()?.toMember() }
    }

    override suspend fun selectAll(): List<Member> = queries.selectAll().awaitAsList()
        .map { it.toMember() }

    override suspend fun insert(item: Member) = queries.insert(
        memberNumber = item.memberNumber.toLong(),
        status = item.status,
        fullName = item.fullName,
        nif = item.nif,
        email = item.email,
    )

    override suspend fun update(item: Member) = queries.update(
        status = item.status,
        fullName = item.fullName,
        nif = item.nif,
        email = item.email,
        memberNumber = item.memberNumber.toLong(),
    )

    override suspend fun delete(id: UInt) {
        queries.deleteById(id.toLong())
    }

    fun Members.toMember() = Member(
        memberNumber = memberNumber.toUInt(),
        status = status,
        fullName = fullName,
        nif = nif,
        email = email,
    )
}
