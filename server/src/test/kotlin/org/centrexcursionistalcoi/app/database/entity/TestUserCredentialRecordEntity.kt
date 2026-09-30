package org.centrexcursionistalcoi.app.database.entity

import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.CredentialKind
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import kotlin.test.AfterTest
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestUserCredentialRecordEntity {
    // The in-memory database outlives each test: leftover users would clash with other tests' (same NIF...).
    @AfterTest
    fun tearDown() {
        Database.clear()
    }

    private fun newRecord(
        credentialId: String,
        owner: UserReferenceEntity,
        kind: CredentialKind = CredentialKind.RESTORE_KEY,
    ) = Database {
        UserCredentialRecordEntity.new(credentialId) {
            user = owner
            attestedCredentialData = byteArrayOf(1, 2, 3)
            signCount = 0
            this.kind = kind
            createdAt = Clock.System.now()
        }
    }

    @Test
    fun `deleteIfOwnedBy deletes the owner's credential`() = runTest {
        Database.initForTests()
        val user = Database { FakeUser.provideEntity() }
        newRecord("owned-credential", user)

        assertTrue(Database { UserCredentialRecordEntity.deleteIfOwnedBy("owned-credential", FakeUser.SUB, CredentialKind.RESTORE_KEY) })
        assertNull(Database { UserCredentialRecordEntity.findById("owned-credential") })
    }

    @Test
    fun `deleteIfOwnedBy leaves another user's credential untouched`() = runTest {
        Database.initForTests()
        Database { FakeUser.provideEntity() }
        val otherUser = Database { FakeUser2.provideEntity() }
        newRecord("other-users-credential", otherUser)

        assertFalse(Database { UserCredentialRecordEntity.deleteIfOwnedBy("other-users-credential", FakeUser.SUB, CredentialKind.RESTORE_KEY) })
        assertNotNull(Database { UserCredentialRecordEntity.findById("other-users-credential") })
    }

    @Test
    fun `deleteIfOwnedBy ignores an unknown credential`() = runTest {
        Database.initForTests()
        Database { FakeUser.provideEntity() }

        assertFalse(Database { UserCredentialRecordEntity.deleteIfOwnedBy("unknown-credential", FakeUser.SUB, CredentialKind.RESTORE_KEY) })
    }

    @Test
    fun `deleteIfOwnedBy leaves a credential of another kind untouched`() = runTest {
        Database.initForTests()
        val user = Database { FakeUser.provideEntity() }
        newRecord("passkey", user, CredentialKind.PASSKEY)

        assertFalse(Database { UserCredentialRecordEntity.deleteIfOwnedBy("passkey", FakeUser.SUB, CredentialKind.RESTORE_KEY) })
        assertNotNull(Database { UserCredentialRecordEntity.findById("passkey") })
    }
}
