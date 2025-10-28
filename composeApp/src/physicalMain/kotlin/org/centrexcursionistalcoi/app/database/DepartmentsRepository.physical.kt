package org.centrexcursionistalcoi.app.database

import org.centrexcursionistalcoi.app.data.Department

actual val DepartmentsRepository: Repository<Department, Int> = DepartmentsDatabaseRepository
