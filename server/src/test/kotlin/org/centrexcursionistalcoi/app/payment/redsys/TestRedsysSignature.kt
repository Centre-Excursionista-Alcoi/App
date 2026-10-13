package org.centrexcursionistalcoi.app.payment.redsys

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks the signature against the example in Redsys's redirect manual (v4.2, section 6). */
class TestRedsysSignature {
    private val key = RedsysConfig.SANDBOX_KEY
    private val order = "1234567890"

    // The JSON of the manual's example, which the manual shows Base64Url-encoded
    private val parameters = Base64.getUrlEncoder().withoutPadding().encodeToString(
        """{"DS_MERCHANT_AMOUNT":"999","DS_MERCHANT_ORDER":"1234567890","DS_MERCHANT_MERCHANTCODE":"999008881","DS_MERCHANT_CURRENCY":"978","DS_MERCHANT_TRANSACTIONTYPE":"0","DS_MERCHANT_TERMINAL":"1","DS_MERCHANT_MERCHANTURL":"http:\/\/www.prueba.com\/urlNotificacion.php","DS_MERCHANT_URLOK":"http:\/\/www.prueba.com\/urlOK.php","DS_MERCHANT_URLKO":"http:\/\/www.prueba.com\/urlKO.php"}""".toByteArray()
    )

    @Test
    fun `the key of an operation is derived as in the manual`() {
        assertEquals("RWt3/IPTzYRMXsQtkiGRKg==", Base64.getEncoder().encodeToString(RedsysSignature.deriveKey(key, order)))
    }

    @Test
    fun `parameters are encoded as in the manual`() {
        assertTrue(parameters.startsWith("eyJEU19NRVJDSEFOVF9BTU9VTlQiOiI5OTkiLCJEU19NRVJDSEFOVF9PUkRFUiI6IjEyMzQ1Njc4OTAi"))
    }

    @Test
    fun `the signature matches the manual`() {
        // The manual's text loses the hyphen after "Vjo02eSWq249IeZZp3R" (it prints 85 characters instead of 86)
        val printed = "Vjo02eSWq249IeZZp3RArFnGLhKY0OuzDDlx1BuVtZDC2yhczA7_11uZhsYzLZBCMFAz8u8uzGDX3AErHKmmw"
        val signature = RedsysSignature.sign(key, order, parameters)
        assertEquals(86, signature.length)
        assertEquals(printed, signature.replaceFirst("-", ""))
    }

    @Test
    fun `verify accepts both alphabets and padding, and rejects changes`() {
        val signature = RedsysSignature.sign(key, order, parameters)
        val standard = signature.replace('-', '+').replace('_', '/') + "=="

        assertTrue(RedsysSignature.verify(key, order, parameters, signature))
        assertTrue(RedsysSignature.verify(key, order, parameters, standard))
        assertFalse(RedsysSignature.verify(key, order, parameters + "x", signature))
        assertFalse(RedsysSignature.verify(key, "1234567891", parameters, signature))
        assertFalse(RedsysSignature.verify("another key 16 chars", order, parameters, signature))
        assertFalse(RedsysSignature.verify(key, order, parameters, ""))
    }
}
