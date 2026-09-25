package org.centrexcursionistalcoi.app.security

import com.webauthn4j.data.client.Origin
import org.centrexcursionistalcoi.app.routes.WellKnownConfigProvider
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestWebAuthnOrigins {
    @AfterTest
    fun tearDown() {
        WellKnownConfigProvider.override(WellKnownConfigProvider.SHA256_CERT_FINGERPRINTS_VARIABLE, null)
    }

    @Test
    fun test_androidOrigins_toleratesWhitespaceAndEmptyEntries() {
        WellKnownConfigProvider.override(WellKnownConfigProvider.SHA256_CERT_FINGERPRINTS_VARIABLE, "AA:BB, CC:DD ,")

        assertEquals(
            setOf(
                // 0xAABB and 0xCCDD, Base64Url without padding
                Origin.create("android:apk-key-hash:qrs"),
                Origin.create("android:apk-key-hash:zN0"),
            ),
            webAuthnAndroidOrigins,
        )
    }
}
