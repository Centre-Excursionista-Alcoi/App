package org.centrexcursionistalcoi.app.network

import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_MEMBERS_SYNC
import org.koin.core.annotation.Singleton

@Singleton
class MembersRemoteRepository(
    repository: MembersRepository
) : SymmetricRemoteRepository<UInt, Member>(
    "/members",
    SETTINGS_LAST_MEMBERS_SYNC,
    Member.serializer(),
    repository,
)
