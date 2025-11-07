package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.data.Users
import org.centrexcursionistalcoi.app.storage.databaseInstance

object UsersRepository : DatabaseRepository<UserData, String>() {
    override val queries by lazy { databaseInstance.usersQueries }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher) = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)
        .map { list -> list.map { it.toUser() } }

    override suspend fun get(id: String): UserData? {
        return queries.get(id).awaitAsList().firstOrNull()?.toUser()
    }

    override fun getAsFlow(id: String, dispatcher: CoroutineDispatcher): Flow<UserData?> {
        return queries
            .get(id)
            .asFlow()
            .mapToList(dispatcher)
            .map { it.firstOrNull()?.toUser() }
    }

    override suspend fun selectAll(): List<UserData> = queries.selectAll().awaitAsList()
        .map { it.toUser() }

    override suspend fun insert(item: UserData) = queries.insert(
        sub = item.sub,
        fullName = item.fullName,
        email = item.email,
        groups = item.groups,
        departments = item.departments,
        lendingUser = item.lendingUser,
        insurances = item.insurances,
    )

    override suspend fun update(item: UserData) = queries.update(
        sub = item.sub,
        fullName = item.fullName,
        email = item.email,
        groups = item.groups,
        departments = item.departments,
        lendingUser = item.lendingUser,
        insurances = item.insurances,
    )

    override suspend fun delete(id: String) {
        queries.deleteById(id)
    }

    fun Users.toUser() = UserData(
        sub = sub,
        fullName = fullName,
        email = email,
        groups = groups,
        departments = departments,
        lendingUser = lendingUser,
        insurances = insurances,
    )
}
