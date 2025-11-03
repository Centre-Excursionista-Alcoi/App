package org.centrexcursionistalcoi.app.database

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.ReferencedLending

actual val LendingsRepository: Repository<ReferencedLending, Uuid> = LendingsDatabaseRepository
