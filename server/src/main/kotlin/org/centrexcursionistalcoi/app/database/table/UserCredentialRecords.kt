package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.table.UserCredentialRecords.id
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/** What a [UserCredentialRecords] row is for. */
enum class CredentialKind {
    /**
     * An Android Restore Credential: created silently after every login, and redeemed to log in again on a new
     * device. Never shown to the user, and used without verifying them.
     */
    RESTORE_KEY,

    /** A passkey the user created to sign in: listed in their security settings, and always verifies them. */
    PASSKEY,
}

/**
 * A WebAuthn credential registered for a user: a restore key or a passkey (see [CredentialKind]). Both go through
 * the same ceremonies (see `security/Authentication.kt`), and differ only in [kind].
 */
object UserCredentialRecords : IdTable<String>("user_credential_records") {
    /** The Base64Url-encoded (no padding) WebAuthn credential ID. */
    override val id: Column<EntityID<String>> = text("credential_id").entityId()

    /** Alias for [id]. */
    val credentialId get() = id

    /** A user can register more than one credential (e.g. more than one device), so this is not unique. */
    val user = reference("user", UserReferences)

    /**
     * The serialized `AttestedCredentialData` (AAGUID + credential ID + COSE public key) from registration --
     * not just the raw public key, since verifying a later authentication needs the whole structure to
     * reconstruct an `Authenticator` (see `AttestedCredentialDataConverter` in `security/WebAuthn.kt`).
     */
    val attestedCredentialData = binary("attested_credential_data")

    /**
     * The authenticator's signature counter as of the last successful use, for clone/replay detection. Many
     * platform authenticators (including Android's, backing Restore Credentials) always report `0` -- webauthn4j
     * treats that as "this authenticator doesn't support counters" and skips the strictly-increasing check
     * rather than treating every use as a clone.
     */
    val signCount = long("sign_count")

    val kind = enumerationByName<CredentialKind>("kind", 32)

    /** A name for the user to tell their passkeys apart (e.g. the device it was created on). */
    val name = text("name").nullable()

    val createdAt = timestamp("created_at")

    /** When it was last used to log in, if ever. */
    val lastUsedAt = timestamp("last_used_at").nullable()
}
