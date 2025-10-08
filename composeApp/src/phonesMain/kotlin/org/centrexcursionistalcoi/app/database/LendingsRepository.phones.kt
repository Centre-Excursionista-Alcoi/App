package org.centrexcursionistalcoi.app.database

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Lending

actual val LendingsRepository: Repository<Lending, Uuid> = LendingsDatabaseRepository
