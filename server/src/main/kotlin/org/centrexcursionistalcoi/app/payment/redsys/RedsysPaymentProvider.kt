package org.centrexcursionistalcoi.app.payment.redsys

import java.util.Base64
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.data.PaymentMethod
import org.centrexcursionistalcoi.app.data.PaymentReference
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.RedsysPayload
import org.centrexcursionistalcoi.app.data.RedsysReference
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.payment.InvalidPaymentNotificationException
import org.centrexcursionistalcoi.app.payment.PaymentProvider
import org.centrexcursionistalcoi.app.payment.PaymentProviderException
import org.centrexcursionistalcoi.app.payment.PaymentRequest
import org.centrexcursionistalcoi.app.payment.PaymentResult
import org.centrexcursionistalcoi.app.payment.PaymentSession
import org.centrexcursionistalcoi.app.payment.RawPaymentNotification
import org.centrexcursionistalcoi.app.payment.UnsupportedPaymentMethodException
import org.centrexcursionistalcoi.app.security.crypto.RandomValues

/**
 * Charges through Redsys's hosted payment page: the user is sent there (a signed form), pays with a card or Bizum
 * without anything about it touching the server, and Redsys reports the result to [notificationUrl].
 */
class RedsysPaymentProvider(
    private val config: RedsysConfig,
    private val notificationUrl: String = "${AppLinks.baseUrl}/payments/redsys/notification",
) : PaymentProvider {
    override val id: String get() = RedsysReference.PROVIDER

    override val methods: Set<PaymentMethod> = buildSet {
        add(PaymentMethod.CARD)
        if (config.bizum) add(PaymentMethod.BIZUM)
    }

    override suspend fun createPayment(request: PaymentRequest): PaymentSession {
        if (request.method != PaymentMethod.ANY && request.method !in methods) throw UnsupportedPaymentMethodException(request.method)

        // 4 digits and 8 alphanumerics: valid for every Redsys operation, which differ on how loose an order may be
        val order = RandomValues.digits(4) + RandomValues.string(8, ORDER_ALPHABET)
        val parameters = buildJsonObject {
            put("DS_MERCHANT_AMOUNT", request.amountCents.toString())
            put("DS_MERCHANT_ORDER", order)
            put("DS_MERCHANT_MERCHANTCODE", config.merchantCode)
            put("DS_MERCHANT_TERMINAL", config.terminal)
            put("DS_MERCHANT_CURRENCY", CURRENCY_EUR)
            put("DS_MERCHANT_TRANSACTIONTYPE", TYPE_PAYMENT)
            put("DS_MERCHANT_MERCHANTURL", notificationUrl)
            put("DS_MERCHANT_URLOK", request.successUrl)
            put("DS_MERCHANT_URLKO", request.failureUrl)
            put("DS_MERCHANT_PRODUCTDESCRIPTION", request.description.take(MAX_DESCRIPTION))
            when (request.method) {
                PaymentMethod.CARD -> put("DS_MERCHANT_PAYMETHODS", "C")
                PaymentMethod.BIZUM -> {
                    put("DS_MERCHANT_PAYMETHODS", "z")
                    request.payerPhone?.let { put("DS_MERCHANT_BIZUM_MOBILENUMBER", it) }
                }
                PaymentMethod.ANY -> Unit
            }
            languageCode(request.language)?.let { put("DS_MERCHANT_CONSUMERLANGUAGE", it) }
            request.metadata?.let { put("DS_MERCHANT_MERCHANTDATA", it) }
        }.toString()
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(parameters.toByteArray())

        return PaymentSession.Form(
            reference = RedsysReference(order),
            url = config.paymentUrl,
            fields = mapOf(
                "Ds_SignatureVersion" to RedsysSignature.VERSION,
                "Ds_MerchantParameters" to encoded,
                "Ds_Signature" to RedsysSignature.sign(config.secretKey, order, encoded),
            ),
        )
    }

    override suspend fun handleNotification(notification: RawPaymentNotification): PaymentResult {
        val parameters = notification.parameters
        val version = parameters["Ds_SignatureVersion"]
        val encoded = parameters["Ds_MerchantParameters"]
        val signature = parameters["Ds_Signature"]
        if (encoded == null || signature == null) throw InvalidPaymentNotificationException("Missing parameters")
        if (version != RedsysSignature.VERSION) throw InvalidPaymentNotificationException("Unsupported signature version $version")

        val payload = try {
            val text = String(Base64.getUrlDecoder().decode(encoded.replace('+', '-').replace('/', '_')))
            val obj = json.parseToJsonElement(text).jsonObject
            // The signature depends on the order, so it's read before anything is trusted
            val order = obj.ignoringCase("Ds_Order") ?: throw InvalidPaymentNotificationException("Missing order")
            if (!RedsysSignature.verify(config.secretKey, order, encoded, signature)) {
                throw InvalidPaymentNotificationException("Invalid signature")
            }
            json.decodeFromString(RedsysPayload.serializer(), text)
        } catch (e: InvalidPaymentNotificationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw InvalidPaymentNotificationException("Malformed notification: ${e.message}")
        } catch (e: SerializationException) {
            throw InvalidPaymentNotificationException("Malformed notification: ${e.message}")
        }

        return PaymentResult(
            reference = RedsysReference(payload.order),
            status = statusOf(payload),
            amountCents = payload.amount.toIntOrNull(),
            payload = payload.sanitized(),
        )
    }

    // Done in the next step, with the REST client
    override suspend fun refund(reference: PaymentReference, amountCents: Int?): PaymentResult {
        requireOwn(reference)
        throw PaymentProviderException("Refunds are not implemented yet")
    }

    override suspend fun cancel(reference: PaymentReference): PaymentResult {
        requireOwn(reference)
        throw PaymentProviderException("Cancellations are not implemented yet")
    }

    private fun JsonObject.ignoringCase(name: String): String? =
        entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.jsonPrimitive?.contentOrNull

    companion object {
        private const val ORDER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val CURRENCY_EUR = "978"
        private const val TYPE_PAYMENT = "0"
        private const val MAX_DESCRIPTION = 125

        /** Redsys's `Ds_Merchant_ConsumerLanguage` for the languages of the app, `null` to let Redsys choose. */
        internal fun languageCode(language: String?): String? = when (language?.substringBefore('-')?.lowercase()) {
            "es" -> "001"
            "en" -> "002"
            "ca" -> "003"
            else -> null
        }

        /**
         * What the result of an operation means. `Ds_Response` 0 to 99 is an authorized payment, 900 an authorized
         * refund (Bizum also processes cancellations as refunds), 400 an authorized cancellation. Anything else failed.
         */
        internal fun statusOf(payload: RedsysPayload): PaymentStatus {
            val response = payload.response.toIntOrNull() ?: return PaymentStatus.FAILED
            return when (payload.transactionType) {
                "3", "45" -> if (response == 900) PaymentStatus.REFUNDED else PaymentStatus.FAILED
                "9" -> if (response == 400) PaymentStatus.CANCELLED else PaymentStatus.FAILED
                else -> if (response in 0..99) PaymentStatus.COMPLETED else PaymentStatus.FAILED
            }
        }
    }
}
