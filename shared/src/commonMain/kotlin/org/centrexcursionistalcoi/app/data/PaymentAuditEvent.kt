package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

/** What an entry of the payments audit log (`PaymentAuditLog` on the server) records. */
@Serializable
enum class PaymentAuditEvent {
    /** A payment was started and recorded as pending. */
    CREATED,

    /** The provider refused to start a payment, or could not be reached. */
    CREATE_FAILED,

    /** The provider reported the state of a payment (a notification, or the answer to a refund/cancellation). */
    RESULT_RECEIVED,

    /** A transaction moved to another status. */
    STATUS_CHANGED,

    /** The provider reported something that doesn't move the transaction (repeated or late notification). */
    RESULT_IGNORED,

    /** The provider reported a payment no transaction has. */
    UNKNOWN_REFERENCE,

    /** A notification was rejected before being trusted (bad signature, malformed...). */
    NOTIFICATION_REJECTED,

    /** Someone asked to refund a transaction. */
    REFUND_REQUESTED,

    /** Someone asked to cancel a transaction. */
    CANCEL_REQUESTED,

    /** The provider refused a refund or cancellation, or could not be reached. */
    OPERATION_FAILED,

    /** Whatever reacts to a status change (a lending...) failed. The transaction itself is recorded. */
    LISTENER_FAILED,
}
