package org.centrexcursionistalcoi.app.database.entity

import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestUserCredentialRecordEntity {
    private fun newRecord(credentialId: String, owner: UserReferenceEntity) = Database {
        UserCredentialRecordEntity.new(credentialId) {
            user = owner
            attestedCredentialData = byteArrayOf(1, 2, 3)
            signCount = 0
        }
    }

    @Test
    fun `deleteIfOwnedBy deletes the owner's credential`() = runTest {
        Database.initForTests()
        val user = Database { FakeUser.provideEntity() }
        newRecord("owned-credential", user)

        assertTrue(Database { UserCredentialRecordEntity.deleteIfOwnedBy("owned-credential", FakeUser.SUB) })
        assertNull(Database { UserCredentialRecordEntity.findById("owned-credential") })
    }

    @Test
    fun `deleteIfOwnedBy leaves another user's credential untouched`() = runTest {
        Database.initForTests()
        Database { FakeUser.provideEntity() }
        val otherUser = Database { FakeUser2.provideEntity() }
        newRecord("other-users-credential", otherUser)

        assertFalse(Database { UserCredentialRecordEntity.deleteIfOwnedBy("other-users-credential", FakeUser.SUB) })
        assertNotNull(Database { UserCredentialRecordEntity.findById("other-users-credential") })
    }

    @Test
    fun `deleteIfOwnedBy ignores an unknown credential`() = runTest {
        Database.initForTests()
        Database { FakeUser.provideEntity() }

        assertFalse(Database { UserCredentialRecordEntity.deleteIfOwnedBy("unknown-credential", FakeUser.SUB) })
    }
}
