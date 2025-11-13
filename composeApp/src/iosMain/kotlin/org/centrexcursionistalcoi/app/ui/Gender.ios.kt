package org.centrexcursionistalcoi.app.ui

import org.centrexcursionistalcoi.app.ui.data.GrammaticalGender

actual object Gender {
    actual fun getGrammaticalGender(): GrammaticalGender {
        throw UnsupportedOperationException("Grammatical gender is not supported on iOS.")
    }
}
