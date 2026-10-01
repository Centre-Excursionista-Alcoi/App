package org.centrexcursionistalcoi.app.verification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.AppLinks

class TestDocumentVerification {
    @Test
    fun test_newCode_isRandom_andValid() {
        val codes = List(1000) { DocumentVerification.newCode() }

        assertEquals(codes.size, codes.toSet().size, "Codes repeat")
        for (code in codes) {
            assertEquals(12, code.length)
            // Its own normal form: no characters that could be misread
            assertEquals(code, DocumentVerification.normalize(code))
        }
    }

    @Test
    fun test_normalize_acceptsHowPeopleTypeIt() {
        assertEquals("ABCDEFGHJKMN", DocumentVerification.normalize("ABCD-EFGH-JKMN"))
        assertEquals("ABCDEFGHJKMN", DocumentVerification.normalize(" abcd efgh jkmn "))
        // Letters Crockford's Base32 doesn't use, mistaken for the digits they look like
        assertEquals("011100000000", DocumentVerification.normalize("0IlL-oO00-0000"))
    }

    @Test
    fun test_normalize_rejectsWhatCantBeACode() {
        assertNull(DocumentVerification.normalize(""))
        assertNull(DocumentVerification.normalize("ABCD-EFGH-JKM"))
        assertNull(DocumentVerification.normalize("ABCD-EFGH-JKMNP"))
        assertNull(DocumentVerification.normalize("ABCD-EFGH-JKMU"))
        assertNull(DocumentVerification.normalize("<script>alert"))
    }

    @Test
    fun test_url_andFormat() {
        assertEquals("ABCD-EFGH-JKMN", DocumentVerification.format("ABCDEFGHJKMN"))
        assertEquals("${AppLinks.baseUrl}/verify?code=ABCD-EFGH-JKMN", DocumentVerification.url("ABCDEFGHJKMN"))
        assertTrue(DocumentVerification.url("ABCDEFGHJKMN").startsWith("https://"))
    }
}
