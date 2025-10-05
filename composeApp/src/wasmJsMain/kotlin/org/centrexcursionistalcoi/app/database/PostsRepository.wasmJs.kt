package org.centrexcursionistalcoi.app.database

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Post

actual val PostsRepository: Repository<Post, Uuid> = PostsSettingsRepository
