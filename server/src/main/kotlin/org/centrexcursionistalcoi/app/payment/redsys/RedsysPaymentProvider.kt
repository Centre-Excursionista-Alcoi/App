package org.centrexcursionistalcoi.app.payment.redsys

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.io.IOException
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
import org.centrexcursionistalcoi.app.payment.PaymentGatewayUnavailableException
import org.centrexcursionistalcoi.app.payment.PaymentProvider
import org.centrexcursionistalcoi.app.payment.PaymentProviderException
import org.centrexcursionistalcoi.app.payment.PaymentRequest
import org.centrexcursionistalcoi.app.payment.PaymentResult
import org.centrexcursionistalcoi.app.payment.PaymentSession
import org.centrexcursionistalcoi.app.payment.RawPaymentNotification
import org.centrexcursionistalcoi.app.payment.UnsupportedPaymentMethodException
import org.centrexcursionistalcoi.app.security.crypto.RandomValues
import org.slf4j.LoggerFactory

/**
 * Charges through Redsys's hosted payment page: the user is sent there (a signed form), pays with a card or Bizum
 * without anything about it touching the server, and Redsys reports the result to [notificationUrl].
 */
class RedsysPaymentProvider(
    private val config: RedsysConfig,
    private val notificationUrl: String = "${AppLinks.baseUrl}/payments/redsys/notification",
    private val httpClient: HttpClient = defaultHttpClient,
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

    override suspend fun handleNotification(notification: RawPaymentNotification): PaymentResult =
        resultOf(readSigned(notification.parameters))

    override suspend fun refund(reference: PaymentReference, amountCents: Int): PaymentResult {
        requireOwn(reference)
        val result = operate(reference as RedsysReference, TYPE_REFUND, amountCents)
        return result.takeIf { it.status == PaymentStatus.REFUNDED }
            ?: throw PaymentProviderException("Redsys denied the refund of ${reference.order}")
    }

    override suspend fun cancel(reference: PaymentReference, amountCents: Int): PaymentResult {
        requireOwn(reference)
        reference as RedsysReference
        val cancelled = try {
            operate(reference, TYPE_CANCEL, amountCents)
        } catch (e: PaymentGatewayUnavailableException) {
            throw e
        } catch (e: PaymentProviderException) {
            // Not every payment can be voided (Bizum can't): Redsys treats it as a refund, so ask for that
            logger.info("Redsys could not cancel ${reference.order} (${e.message}), refunding it instead")
            null
        }
        if (cancelled?.status == PaymentStatus.CANCELLED) return cancelled
        return refund(reference, amountCents)
    }

    /**
     * Checks that [parameters] come from Redsys (signature) and reads them.
     * @throws InvalidPaymentNotificationException if they can't be trusted.
     */
    private fun readSigned(parameters: Map<String, String>): RedsysPayload {
        val version = parameters["Ds_SignatureVersion"]
        val encoded = parameters["Ds_MerchantParameters"]
        val signature = parameters["Ds_Signature"]
        if (encoded == null || signature == null) throw InvalidPaymentNotificationException("Missing parameters")
        if (version != RedsysSignature.VERSION) throw InvalidPaymentNotificationException("Unsupported signature version $version")

        return try {
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
    }

    private fun resultOf(payload: RedsysPayload) = PaymentResult(
        reference = RedsysReference(payload.order),
        status = statusOf(payload),
        amountCents = payload.amount.toIntOrNull(),
        payload = payload.sanitized(),
    )

    /**
     * Asks Redsys, server to server, for an operation on a payment that already exists: no card data is involved.
     * Redsys can take up to 30 seconds to answer, so the client waits longer.
     * @throws PaymentGatewayUnavailableException if Redsys can't be reached.
     * @throws PaymentProviderException if Redsys answers with an error.
     */
    private suspend fun operate(reference: RedsysReference, type: String, amountCents: Int): PaymentResult {
        require(amountCents > 0) { "The amount must be positive" }
        val parameters = buildJsonObject {
            put("DS_MERCHANT_AMOUNT", amountCents.toString())
            put("DS_MERCHANT_ORDER", reference.order)
            put("DS_MERCHANT_MERCHANTCODE", config.merchantCode)
            put("DS_MERCHANT_TERMINAL", config.terminal)
            put("DS_MERCHANT_CURRENCY", CURRENCY_EUR)
            put("DS_MERCHANT_TRANSACTIONTYPE", type)
        }.toString()
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(parameters.toByteArray())
        val body = buildJsonObject {
            put("Ds_SignatureVersion", RedsysSignature.VERSION)
            put("Ds_MerchantParameters", encoded)
            put("Ds_Signature", RedsysSignature.sign(config.secretKey, reference.order, encoded))
        }.toString()

        val response = try {
            httpClient.post(config.restUrl) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        } catch (e: IOException) {
            throw PaymentGatewayUnavailableException("Redsys could not be reached", e)
        } catch (e: HttpRequestTimeoutException) {
            throw PaymentGatewayUnavailableException("Redsys did not answer in time", e)
        }
        if (response.status.value >= 500) throw PaymentGatewayUnavailableException("Redsys answered ${response.status}")

        val answer = try {
            json.parseToJsonElement(response.bodyAsText()).jsonObject
        } catch (e: SerializationException) {
            throw PaymentProviderException("Redsys answered something unreadable", e)
        } catch (e: IllegalArgumentException) {
            throw PaymentProviderException("Redsys answered something unreadable", e)
        }
        answer["errorCode"]?.jsonPrimitive?.contentOrNull?.let { throw PaymentProviderException("Redsys refused the operation: $it") }

        val payload = try {
            readSigned(answer.mapValues { it.value.jsonPrimitive.content })
        } catch (e: InvalidPaymentNotificationException) {
            throw PaymentProviderException("Redsys answered something untrustworthy: ${e.message}", e)
        }
        return resultOf(payload)
    }

    private fun JsonObject.ignoringCase(name: String): String? =
        entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.jsonPrimitive?.contentOrNull

    companion object {
        private const val ORDER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val CURRENCY_EUR = "978"
        private const val TYPE_PAYMENT = "0"
        private const val TYPE_REFUND = "3"
        private const val TYPE_CANCEL = "9"
        private val logger = LoggerFactory.getLogger("RedsysPaymentProvider")

        /** Redsys can take 30 seconds to answer an operation: wait longer, so it always does. */
        private val defaultHttpClient by lazy {
            HttpClient(Java) { install(HttpTimeout) { requestTimeoutMillis = 50_000 } }
        }
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
