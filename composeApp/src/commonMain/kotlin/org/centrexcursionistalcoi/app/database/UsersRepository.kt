package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.data.Users
import org.centrexcursionistalcoi.app.storage.databaseInstance

expect val UsersRepository : Repository<UserData, String>

object UsersSettingsRepository : SettingsRepository<UserData, String>("users", UserData.serializer())

object UsersDatabaseRepository : DatabaseRepository<UserData, String>() {
    override val queries by lazy { databaseInstance.usersQueries }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher) = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)
        .map { list -> list.map { it.toUser() } }

    override suspend fun selectAll(): List<UserData> = queries.selectAll().awaitAsList()
        .map { it.toUser() }

    override suspend fun insert(item: UserData) = queries.insert(
        sub = item.sub,
        username = item.username,
        email = item.email,
        groups = item.groups,
        departments = item.departments,
        lendingUser = item.lendingUser,
        insurances = item.insurances,
    )

    override suspend fun update(item: UserData) = queries.update(
        sub = item.sub,
        username = item.username,
        email = item.email,
        groups = item.groups,
        departments = item.departments,
        lendingUser = item.lendingUser,
        insurances = item.insurances,
    )

    override suspend fun delete(id: String) {
        queries.deleteById(id)
    }

    private fun Users.toUser() = UserData(
        sub = sub,
        username = username,
        email = email,
        groups = groups,
        departments = departments,
        lendingUser = lendingUser,
        insurances = insurances,
    )
}
