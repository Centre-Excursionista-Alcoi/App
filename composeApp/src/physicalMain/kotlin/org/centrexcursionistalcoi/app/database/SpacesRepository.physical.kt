package org.centrexcursionistalcoi.app.database

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Space

actual val SpacesRepository: Repository<Space, Uuid> = SpacesDatabaseRepository
