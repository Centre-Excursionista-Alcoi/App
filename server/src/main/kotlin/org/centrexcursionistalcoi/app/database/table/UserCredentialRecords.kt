package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.table.UserCredentialRecords.id
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * A WebAuthn credential registered for a user -- currently only ever created as a Restore Credential (see
 * `security/Authentication.kt`'s `webAuthnRoutes`), but the table itself is credential-purpose-agnostic: a
 * user-facing passkey (not yet implemented) would be a row here too, indistinguishable at this layer.
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
}
