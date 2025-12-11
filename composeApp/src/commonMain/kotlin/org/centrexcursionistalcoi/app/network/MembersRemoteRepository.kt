package org.centrexcursionistalcoi.app.network

import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_MEMBERS_SYNC

object MembersRemoteRepository : SymmetricRemoteRepository<UInt, Member>(
    "/members",
    SETTINGS_LAST_MEMBERS_SYNC,
    Member.serializer(),
    MembersRepository,
)
