package org.centrexcursionistalcoi.app.payment.redsys

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.data.RedsysReference
import org.centrexcursionistalcoi.app.payment.PaymentProviders
import org.centrexcursionistalcoi.app.payment.configurePayments

class TestRedsysConfig {
    private fun env(vararg pairs: Pair<String, String>): (String) -> String? = mapOf(*pairs)::get
    private val full = arrayOf(
        "REDSYS_MERCHANT_CODE" to "999008881", "REDSYS_TERMINAL" to "1", "REDSYS_SECRET_KEY" to RedsysConfig.SANDBOX_KEY,
    )

    @Test
    fun `nothing set means not configured`() {
        assertNull(RedsysConfig.fromEnvironment(env()))
        assertNull(RedsysConfig.fromEnvironment(env("REDSYS_TERMINAL" to "  ")))
    }

    @Test
    fun `defaults are the test environment without bizum`() {
        val config = RedsysConfig.fromEnvironment(env(*full))!!
        assertFalse(config.production)
        assertFalse(config.bizum)
        assertEquals("https://sis-t.redsys.es:25443/sis/realizarPago", config.paymentUrl)
    }

    @Test
    fun `production and bizum are opt in`() {
        val config = RedsysConfig.fromEnvironment(
            env(*full, "REDSYS_ENVIRONMENT" to "Production", "REDSYS_BIZUM" to "TRUE", "REDSYS_SECRET_KEY" to "a-real-key-of-32-characters-long")
        )!!
        assertTrue(config.production)
        assertTrue(config.bizum)
        assertEquals("https://sis.redsys.es/sis/realizarPago", config.paymentUrl)
        assertEquals("https://sis.redsys.es/sis/rest/trataPeticionREST", config.restUrl)
    }

    @Test
    fun `partial or wrong configuration is refused`() {
        assertFailsWith<IllegalArgumentException> { RedsysConfig.fromEnvironment(env("REDSYS_MERCHANT_CODE" to "1")) }
        assertFailsWith<IllegalArgumentException> { RedsysConfig.fromEnvironment(env(*full, "REDSYS_ENVIRONMENT" to "staging")) }
        assertFailsWith<IllegalArgumentException> { RedsysConfig.fromEnvironment(env(*full, "REDSYS_SECRET_KEY" to "short")) }
        // The sandbox key is public
        assertFailsWith<IllegalArgumentException> { RedsysConfig.fromEnvironment(env(*full, "REDSYS_ENVIRONMENT" to "production")) }
    }

    @Test
    fun `a wrong configuration never stops the server, it only leaves payments out`() {
        PaymentProviders.clear()
        configurePayments(env("REDSYS_MERCHANT_CODE" to "1"))
        assertNull(PaymentProviders[RedsysReference.PROVIDER])

        configurePayments(env(*full))
        assertEquals(RedsysReference.PROVIDER, PaymentProviders[RedsysReference.PROVIDER]?.id)
        PaymentProviders.clear()
    }
}
