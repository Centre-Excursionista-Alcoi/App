package org.centrexcursionistalcoi.app.database

import org.centrexcursionistalcoi.app.data.UserData

actual val UsersRepository: Repository<UserData, String> = UsersDatabaseRepository
