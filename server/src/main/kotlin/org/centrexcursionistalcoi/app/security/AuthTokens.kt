package org.centrexcursionistalcoi.app.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.AuthRefreshTokens
import org.centrexcursionistalcoi.app.database.table.AuthSessionMethod
import org.centrexcursionistalcoi.app.database.table.AuthSessionRevocationReason
import org.centrexcursionistalcoi.app.database.table.AuthSessions
import org.centrexcursionistalcoi.app.now
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/** Where a request came from, recorded on the sessions it starts. */
data class ClientInfo(val ipAddress: String?, val userAgent: String?) {
    companion object {
        fun from(call: ApplicationCall) = ClientInfo(
            ipAddress = call.request.origin.remoteHost,
            userAgent = call.request.headers[HttpHeaders.UserAgent],
        )
    }
}

sealed interface RefreshResult {
    data class Success(val tokens: TokenResponse, val email: String) : RefreshResult

    /** Unknown, expired or revoked token, or a disabled user. */
    data object Invalid : RefreshResult

    /** An already-rotated token was presented again, and its whole session has been revoked. */
    data class Reused(val email: String) : RefreshResult
}

/**
 * Token-based authentication: short-lived signed access tokens, and long-lived, rotating, server-side refresh
 * tokens (see [AuthSessions] and [AuthRefreshTokens]).
 *
 * - Access tokens are ES256 JWTs carrying only the user (`sub`) and the session (`sid`). Nothing else is trusted
 *   from them: every request re-checks that the session is still active and re-reads the user (see
 *   [resolveAccessToken]), so revocations, disabled users and group changes apply immediately.
 * - Refresh tokens are 256-bit random strings, stored hashed. Each one can only be used once: [refresh] issues a
 *   new one and marks the old one as used. Using a token twice revokes its whole session (see
 *   [AuthSessionRevocationReason.REFRESH_TOKEN_REUSE]), except within [refreshTokenReuseGracePeriod], to tolerate
 *   a client that never got the response to its previous refresh.
 */
object AuthTokens {
    private val logger = LoggerFactory.getLogger(AuthTokens::class.java)

    val accessTokenLifetime: Duration = 10.minutes
    val refreshTokenIdleLifetime: Duration = 30.days
    val refreshTokenAbsoluteLifetime: Duration = 90.days
    val refreshTokenReuseGracePeriod: Duration = 30.seconds

    private const val ISSUER = "centrexcursionistalcoi-server"
    private const val AUDIENCE = "centrexcursionistalcoi-app"
    private const val CLAIM_SESSION_ID = "sid"
    private const val REFRESH_TOKEN_PREFIX = "cea_rt_"

    private const val PRIVATE_KEY_FILE = "jwt-es256.key"
    private const val PUBLIC_KEY_FILE = "jwt-es256.pub"

    private val secureRandom = SecureRandom()

    private var keyPair: KeyPair? = null

    private val keys: KeyPair
        get() = keyPair ?: synchronized(this) {
            keyPair ?: generateKeyPair().also {
                // Only reached without init(), i.e. in tests: tokens won't survive a restart.
                logger.warn("Access token signing key not initialized, using an ephemeral key.")
                keyPair = it
            }
        }

    private val algorithm: Algorithm by lazy {
        Algorithm.ECDSA256(keys.public as ECPublicKey, keys.private as ECPrivateKey)
    }

    /** Identifies the signing key, so a rotated key is told apart from a forged signature in logs. */
    private val keyId: String by lazy {
        base64Url(sha256(keys.public.encoded)).take(16)
    }

    /** Honors [now], so that tests can mock the time. */
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now()
    }

    private val verifier: JWTVerifier by lazy {
        (JWT.require(algorithm)
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaimPresence(CLAIM_SESSION_ID)
            .acceptLeeway(5) as JWTVerifier.BaseVerification)
            .build(clock)
    }

    /**
     * Loads the access token signing key from [keysDir], generating it on first run. Replacing the key invalidates
     * every access token, but not the refresh tokens, so clients just refresh.
     */
    fun init(keysDir: File) {
        val privateFile = File(keysDir, PRIVATE_KEY_FILE)
        val publicFile = File(keysDir, PUBLIC_KEY_FILE)
        keyPair = if (privateFile.exists() && publicFile.exists()) {
            val keyFactory = KeyFactory.getInstance("EC")
            KeyPair(
                keyFactory.generatePublic(X509EncodedKeySpec(publicFile.readBytes())),
                keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateFile.readBytes())),
            )
        } else {
            logger.info("Access token signing key not found, generating a new one...")
            keysDir.mkdirs()
            generateKeyPair().also {
                privateFile.writeOwnerOnly(it.private.encoded)
                publicFile.writeOwnerOnly(it.public.encoded)
            }
        }
    }

    private fun File.writeOwnerOnly(bytes: ByteArray) {
        writeBytes(bytes)
        setReadable(false, false)
        setWritable(false, false)
        setReadable(true, true)
        setWritable(true, true)
    }

    private fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1"), secureRandom) }
        .generateKeyPair()

    /**
     * Starts a new session for [user], returning its first token pair.
     */
    context(_: JdbcTransaction)
    fun startSession(user: UserReferenceEntity, method: AuthSessionMethod, client: ClientInfo): TokenResponse {
        val now = now()
        val userSub = user.sub.value
        val expiresAt = now + refreshTokenIdleLifetime.toJavaDuration()
        val sessionId = AuthSessions.insertAndGetId {
            it[AuthSessions.user] = userSub
            it[this.method] = method
            it[createdAt] = now
            it[lastUsedAt] = now
            it[this.expiresAt] = expiresAt
            it[absoluteExpiresAt] = now + refreshTokenAbsoluteLifetime.toJavaDuration()
            it[ipAddress] = client.ipAddress?.take(64)
            it[userAgent] = client.userAgent
        }.value
        val refreshToken = insertRefreshToken(sessionId, now)
        return tokenResponse(userSub, user.email, sessionId, refreshToken, expiresAt, now)
    }

    /**
     * Exchanges [refreshToken] for a new token pair, invalidating it.
     */
    context(_: JdbcTransaction)
    fun refresh(refreshToken: String): RefreshResult {
        val now = now()
        val hash = hashRefreshToken(refreshToken)
        // Locked, so that two concurrent refreshes with the same token can't both succeed.
        val row = AuthRefreshTokens.selectAll()
            .where { AuthRefreshTokens.id eq hash }
            .forUpdate()
            .singleOrNull() ?: return RefreshResult.Invalid
        val sessionId = row[AuthRefreshTokens.session].value
        val session = AuthSessions.selectAll().where { AuthSessions.id eq sessionId }.single()
        val user = UserReferenceEntity.findById(session[AuthSessions.user].value) ?: return RefreshResult.Invalid
        if (!session.isActive(now) || user.isDisabled) return RefreshResult.Invalid

        val usedAt = row[AuthRefreshTokens.usedAt]
        if (usedAt != null) {
            // Already rotated. Only acceptable if it's the token right before the current one, whose successor was
            // never used: the client most likely never got the previous response.
            val successorHash = row[AuthRefreshTokens.replacedBy]
            val successorUnused = successorHash != null && AuthRefreshTokens.selectAll()
                .where { (AuthRefreshTokens.id eq successorHash) and AuthRefreshTokens.usedAt.isNull() }
                .any()
            val withinGracePeriod = usedAt + refreshTokenReuseGracePeriod.toJavaDuration() >= now
            if (!successorUnused || !withinGracePeriod) {
                revokeSession(sessionId, AuthSessionRevocationReason.REFRESH_TOKEN_REUSE)
                logger.warn("Refresh token reuse detected for user ${user.sub.value}, session $sessionId revoked.")
                return RefreshResult.Reused(user.email)
            }
            AuthRefreshTokens.deleteWhere { AuthRefreshTokens.id eq successorHash }
        }

        val newRefreshToken = insertRefreshToken(sessionId, now)
        AuthRefreshTokens.update({ AuthRefreshTokens.id eq hash }) {
            // Kept at the first use, so the grace period can't be extended by retrying.
            it[this.usedAt] = usedAt ?: now
            it[replacedBy] = hashRefreshToken(newRefreshToken)
        }
        val expiresAt = minOf(now + refreshTokenIdleLifetime.toJavaDuration(), session[AuthSessions.absoluteExpiresAt])
        AuthSessions.update({ AuthSessions.id eq sessionId }) {
            it[lastUsedAt] = now
            it[this.expiresAt] = expiresAt
        }
        val tokens = tokenResponse(user.sub.value, user.email, sessionId, newRefreshToken, expiresAt, now)
        return RefreshResult.Success(tokens, user.email)
    }

    /**
     * Revokes the session [refreshToken] belongs to, whether it's the current token or an already rotated one.
     * @return whether a session was found.
     */
    context(_: JdbcTransaction)
    fun revokeSessionOf(refreshToken: String, reason: AuthSessionRevocationReason): Boolean {
        val sessionId = AuthRefreshTokens.selectAll()
            .where { AuthRefreshTokens.id eq hashRefreshToken(refreshToken) }
            .singleOrNull()
            ?.get(AuthRefreshTokens.session)?.value ?: return false
        revokeSession(sessionId, reason)
        return true
    }

    context(_: JdbcTransaction)
    fun revokeSession(sessionId: UUID, reason: AuthSessionRevocationReason) {
        AuthSessions.update({ (AuthSessions.id eq sessionId) and AuthSessions.revokedAt.isNull() }) {
            it[revokedAt] = now()
            it[revocationReason] = reason
        }
    }

    /** Revokes every session of [userSub], e.g. once their password is reset. */
    context(_: JdbcTransaction)
    fun revokeAllSessions(userSub: String, reason: AuthSessionRevocationReason) {
        AuthSessions.update({ (AuthSessions.user eq userSub) and AuthSessions.revokedAt.isNull() }) {
            it[revokedAt] = now()
            it[revocationReason] = reason
        }
    }

    /**
     * Verifies [accessToken], and loads its user, as long as its session is still active and the user isn't
     * disabled.
     */
    fun resolveAccessToken(accessToken: String): AccessTokenSession? {
        val jwt = try {
            verifier.verify(accessToken)
        } catch (_: JWTVerificationException) {
            return null
        }
        val sub = jwt.subject ?: return null
        val sessionId = jwt.getClaim(CLAIM_SESSION_ID).asString()
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: return null
        return Database {
            val session = AuthSessions.selectAll().where { AuthSessions.id eq sessionId }.singleOrNull()
                ?: return@Database null
            if (session[AuthSessions.user].value != sub || !session.isActive(now())) return@Database null
            val user = UserReferenceEntity.findById(sub)?.takeUnless { it.isDisabled } ?: return@Database null
            AccessTokenSession(sessionId, UserSession.fromReference(user))
        }
    }

    data class AccessTokenSession(val sessionId: UUID, val userSession: UserSession)

    private fun ResultRow.isActive(now: Instant): Boolean =
        this[AuthSessions.revokedAt] == null &&
            this[AuthSessions.expiresAt] > now &&
            this[AuthSessions.absoluteExpiresAt] > now

    context(_: JdbcTransaction)
    private fun insertRefreshToken(sessionId: UUID, now: Instant): String {
        val bytes = ByteArray(32).also(secureRandom::nextBytes)
        val token = REFRESH_TOKEN_PREFIX + base64Url(bytes)
        AuthRefreshTokens.insert {
            it[id] = hashRefreshToken(token)
            it[session] = sessionId
            it[createdAt] = now
        }
        return token
    }

    private fun tokenResponse(
        userSub: String,
        email: String,
        sessionId: UUID,
        refreshToken: String,
        refreshTokenExpiresAt: Instant,
        now: Instant,
    ) = TokenResponse(
        accessToken = JWT.create()
            .withKeyId(keyId)
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(userSub)
            .withClaim(CLAIM_SESSION_ID, sessionId.toString())
            .withJWTId(UUID.randomUUID().toString())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now + accessTokenLifetime.toJavaDuration()))
            .sign(algorithm),
        accessTokenExpiresIn = accessTokenLifetime.inWholeSeconds,
        refreshToken = refreshToken,
        refreshTokenExpiresIn = java.time.Duration.between(now, refreshTokenExpiresAt).seconds,
        accountEmail = email,
    )

    private fun hashRefreshToken(token: String): String = sha256(token.toByteArray()).toHexString()

    private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
