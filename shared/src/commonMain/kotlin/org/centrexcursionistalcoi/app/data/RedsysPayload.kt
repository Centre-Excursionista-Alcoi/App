package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RedsysPayload(
    @SerialName("Ds_Order")
    val order: String,

    @SerialName("Ds_Response")
    val response: String,

    @SerialName("Ds_Amount")
    val amount: String,

    @SerialName("Ds_Currency")
    val currency: String,

    @SerialName("Ds_MerchantCode")
    val merchantCode: String,

    @SerialName("Ds_Terminal")
    val terminal: String,

    @SerialName("Ds_TransactionType")
    val transactionType: String,

    @SerialName("Ds_Date")
    val date: String? = null,

    @SerialName("Ds_Hour")
    val hour: String? = null,

    @SerialName("Ds_AuthorisationCode")
    val authorisationCode: String? = null,

    @SerialName("Ds_SecurePayment")
    val securePayment: String? = null,

    @SerialName("Ds_Card_Number")
    val cardNumber: String? = null,

    @SerialName("Ds_Card_Country")
    val cardCountry: String? = null,

    @SerialName("Ds_Card_Brand")
    val cardBrand: String? = null,

    @SerialName("Ds_Card_Type")
    val cardType: String? = null,

    @SerialName("Ds_ConsumerLanguage")
    val consumerLanguage: String? = null,

    @SerialName("Ds_MerchantData")
    val merchantData: String? = null
)
