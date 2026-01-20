package org.centrexcursionistalcoi.app.notifications.email.mailersend

import java.util.Base64
import kotlinx.serialization.Serializable

@Serializable
data class MailerSendAttachment(
    /**
     * Base64 encoded content of the attachment.
     *
     * Max size of 25MB. After decoding Base64.
     */
    val content: String,
    /**
     * Use inline to make it accessible for content. use attachment to normal attachments
     *
     * Must be one of: inline, attachment
     */
    val disposition: String,
    val filename: String,
    /**
     * Can be used in content as `<img src="cid:*"/>`. Must also set `attachments.*.disposition` as `inline`.
     */
    val id: String? = null,
) {
    init {
        require(id == null || disposition == "inline") {
            "id can only be set if disposition is inline"
        }
    }

    constructor(content: ByteArray, filename: String, disposition: String = "attachment", id: String? = null): this(
        content = Base64.getMimeEncoder().encodeToString(content),
        filename = filename,
        disposition = disposition,
        id = id,
    )

    fun decodeContent(): ByteArray {
        return Base64.getMimeDecoder().decode(content)
    }
}
