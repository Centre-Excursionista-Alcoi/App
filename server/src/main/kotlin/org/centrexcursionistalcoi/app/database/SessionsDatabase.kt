package org.centrexcursionistalcoi.app.database

import io.github.crackthecodeabhi.kreds.args.SetOption
import io.github.crackthecodeabhi.kreds.commands.IScanResult
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.github.crackthecodeabhi.kreds.connection.newClient
import java.time.Instant
import java.util.UUID
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.serverJson
import org.slf4j.LoggerFactory

object SessionsDatabase {
    /**
     * The duration of a session in minutes.
     */
    private val sessionDuration = System.getenv("SESSION_DURATION")?.toLongOrNull() ?: (24 * 60)

    private val logger = LoggerFactory.getLogger(SessionsDatabase::class.java)

    @Volatile
    private var sessions = mapOf<String, Pair<String, Instant>>()
    private var client: KredsClient? = null

    fun initialize(endpoint: String?) {
        if (endpoint != null) {
            logger.info("Using Redis endpoint: $endpoint")
            logger.info("  Sessions will expire after $sessionDuration minutes")
            newClient(Endpoint.from(endpoint))
        } else {
            logger.info("No Redis endpoint provided, using in-memory storage")
        }
    }

    private suspend fun set(key: String, value: String, expiration: Instant): Boolean {
        return client?.let {
            it.set(
                key = key,
                value = value,
                setOption = SetOption.Builder()
                    .exatTimestamp(expiration.toEpochMilli().toULong())
                    .build()
            ) == "OK"
        } ?: run {
            sessions = sessions + (key to (value to expiration))
            true
        }
    }

    private suspend fun get(key: String): String? {
        client?.let {
            return it.get(key)
        } ?: run {
            return sessions[key]?.first
        }
    }

    /**
     * Returns all sessions for a user.
     * @param simpleEmail The simplified email of the user.
     */
    private suspend fun allForUser(simpleEmail: String): List<String> {
        client?.let { cl ->
            val keys = mutableListOf<String>()
            var result: IScanResult<String>
            while (cl.scan(0, "session:$simpleEmail:*").also { result = it }.cursor != 0L) {
                keys.addAll(result.elements)
            }
            val sessions = keys.mapNotNull { cl.get(it) }
            return sessions
        } ?: run {
            return sessions.filterKeys { it.startsWith("session:$simpleEmail:") }
                .map { it.value.first }
        }
    }

    private suspend fun del(key: String) {
        client?.del(key) ?: run {
            sessions = sessions - key
        }
    }

    private fun simplifyEmail(email: String): String =
        email.substringBefore('@').lowercase() + email.substringAfter('@').hashCode()

    suspend fun newSession(email: String, ipAddress: String): UserSession {
        val simpleEmail = simplifyEmail(email)
        val sessionId = UUID.randomUUID().toString()
        val sessionEnd = Instant.now().plusSeconds(sessionDuration * 60)
        val session = UserSession(sessionId, sessionEnd.toKotlinInstant(), email, ipAddress)
        val sessionJson = serverJson.encodeToString(UserSession.serializer(), session)
        val success = set(
            key = "session:$simpleEmail:$sessionId",
            value = sessionJson,
            expiration = sessionEnd
        )
        check(success) { "Failed to create session" }
        return session
    }

    suspend fun validateSession(session: UserSession): Boolean {
        val sessionId = session.sessionId
        val simpleEmail = simplifyEmail(session.email)

        val sessionJson = get("session:$simpleEmail:$sessionId") ?: return false
        val storedSession = serverJson.decodeFromString(UserSession.serializer(), sessionJson)
        if (storedSession != session) return false

        return Instant.now() <= storedSession.expiresAt.toJavaInstant()
    }

    suspend fun deleteSession(session: UserSession) {
        val sessionId = session.sessionId
        val simpleEmail = simplifyEmail(session.email)
        del("session:$simpleEmail:$sessionId")
    }

    suspend fun updateFCMToken(session: UserSession, token: String) {
        val sessionId = session.sessionId
        val simpleEmail = simplifyEmail(session.email)
        val sessionJson = get("session:$simpleEmail:$sessionId") ?: error("Session not found")
        val storedSession = serverJson.decodeFromString(UserSession.serializer(), sessionJson)
        val updatedSession = storedSession.copy(fcmToken = token)
        val updatedSessionJson = serverJson.encodeToString(UserSession.serializer(), updatedSession)
        set(
            key = "session:$simpleEmail:$sessionId",
            value = updatedSessionJson,
            expiration = storedSession.expiresAt.toJavaInstant()
        )
    }

    /**
     * Returns all FCM tokens for a user.
     */
    suspend fun getTokensForEmail(email: String): List<String> {
        val simpleEmail = simplifyEmail(email)
        return allForUser(simpleEmail).mapNotNull {
            serverJson.decodeFromString(UserSession.serializer(), it).fcmToken
        }
    }
}
