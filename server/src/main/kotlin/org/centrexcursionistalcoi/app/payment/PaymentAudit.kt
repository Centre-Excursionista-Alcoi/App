package org.centrexcursionistalcoi.app.payment

import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.centrexcursionistalcoi.app.data.PaymentAuditEntry
import org.centrexcursionistalcoi.app.data.PaymentAuditEvent
import org.centrexcursionistalcoi.app.data.PaymentPayload
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.PaymentAuditLog
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.serializer.PaymentPayloadSerializer
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory

/**
 * Writes and reads the payments audit log ([PaymentAuditLog]). Entries are only ever added.
 */
object PaymentAudit {
    private val logger = LoggerFactory.getLogger("PaymentAudit")

    /** Who caused something that isn't a user or a provider (the server itself). */
    const val ACTOR_SYSTEM = "system"

    /** The provider, reporting through its notification. */
    const val ACTOR_PROVIDER = "provider"

    fun userActor(sub: String) = "user:$sub"

    /**
     * Adds an entry. Joins the transaction it is called from, if any, so the entry of a change is saved or lost with
     * the change. Throws if it can't be written: use [tryRecord] where the audit mustn't break what is being audited.
     */
    fun record(
        event: PaymentAuditEvent,
        actor: String,
        transactionId: Uuid? = null,
        referenceKey: String? = null,
        provider: String? = null,
        previousStatus: PaymentStatus? = null,
        newStatus: PaymentStatus? = null,
        amountCents: Int? = null,
        detail: JsonObject? = null,
    ) {
        Database {
            PaymentAuditLog.insert {
                it[PaymentAuditLog.event] = event
                it[PaymentAuditLog.actor] = actor
                it[PaymentAuditLog.transactionId] = transactionId
                it[PaymentAuditLog.referenceKey] = referenceKey
                it[PaymentAuditLog.provider] = provider
                it[PaymentAuditLog.previousStatus] = previousStatus
                it[PaymentAuditLog.newStatus] = newStatus
                it[PaymentAuditLog.amountCents] = amountCents
                it[PaymentAuditLog.detail] = detail
            }
        }
    }

    /** Like [record], but a failure is only logged: for entries about something that has already happened or failed. */
    fun tryRecord(
        event: PaymentAuditEvent,
        actor: String,
        transactionId: Uuid? = null,
        referenceKey: String? = null,
        provider: String? = null,
        previousStatus: PaymentStatus? = null,
        newStatus: PaymentStatus? = null,
        amountCents: Int? = null,
        detail: JsonObject? = null,
    ) {
        try {
            record(event, actor, transactionId, referenceKey, provider, previousStatus, newStatus, amountCents, detail)
        } catch (e: Exception) {
            logger.error("Could not record the audit entry $event for transaction $transactionId ($referenceKey)", e)
        }
    }

    /** Everything recorded about [transactionId], oldest first. */
    fun forTransaction(transactionId: Uuid): List<PaymentAuditEntry> = forTransactions(listOf(transactionId))

    /** Everything recorded about any of [transactionIds], oldest first. */
    fun forTransactions(transactionIds: Collection<Uuid>): List<PaymentAuditEntry> = Database {
        PaymentAuditLog.selectAll()
            .where { PaymentAuditLog.transactionId inList transactionIds }
            .orderBy(PaymentAuditLog.createdAt to SortOrder.ASC)
            .map { it.toPaymentAuditEntry() }
    }

    /** Everything recorded about a provider [referenceKey], even if no transaction exists for it. Oldest first. */
    fun forReference(referenceKey: String): List<PaymentAuditEntry> = Database {
        PaymentAuditLog.selectAll()
            .where { PaymentAuditLog.referenceKey eq referenceKey }
            .orderBy(PaymentAuditLog.createdAt to SortOrder.ASC)
            .map { it.toPaymentAuditEntry() }
    }

    /** The latest [limit] entries of the whole log, oldest first. */
    fun latest(limit: Int): List<PaymentAuditEntry> = Database {
        PaymentAuditLog.selectAll()
            .orderBy(PaymentAuditLog.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toPaymentAuditEntry() }
            .reversed()
    }

    /** [detail] for what a provider reported: only its sanitized payload. */
    fun payloadDetail(payload: PaymentPayload): JsonObject = buildJsonObject {
        put("payload", json.encodeToJsonElement(PaymentPayloadSerializer, payload.sanitized()))
    }

    /** [detail] for something that failed. */
    fun errorDetail(error: Throwable, extra: Map<String, JsonElement> = emptyMap()): JsonObject = buildJsonObject {
        put("error", error::class.simpleName ?: "Exception")
        put("message", error.message ?: "")
        for ((key, value) in extra) put(key, value)
    }

    fun detailOf(vararg pairs: Pair<String, String?>): JsonObject = buildJsonObject {
        for ((key, value) in pairs) if (value != null) put(key, JsonPrimitive(value))
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toPaymentAuditEntry() = PaymentAuditEntry(
        id = this[PaymentAuditLog.id].value,
        createdAt = this[PaymentAuditLog.createdAt],
        transactionId = this[PaymentAuditLog.transactionId],
        referenceKey = this[PaymentAuditLog.referenceKey],
        provider = this[PaymentAuditLog.provider],
        event = this[PaymentAuditLog.event],
        actor = this[PaymentAuditLog.actor],
        previousStatus = this[PaymentAuditLog.previousStatus],
        newStatus = this[PaymentAuditLog.newStatus],
        amountCents = this[PaymentAuditLog.amountCents],
        detail = this[PaymentAuditLog.detail],
    )
}
