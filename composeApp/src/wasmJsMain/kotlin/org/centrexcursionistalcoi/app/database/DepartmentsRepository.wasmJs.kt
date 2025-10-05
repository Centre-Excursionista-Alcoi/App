package org.centrexcursionistalcoi.app.database

import org.centrexcursionistalcoi.app.data.Department

// Use Settings repository for WASM/JS temporally, since SQLDelight doesn't store the data properly yet
actual val DepartmentsRepository: Repository<Department, Long> = DepartmentsSettingsRepository
