package org.centrexcursionistalcoi.app.ui

import org.centrexcursionistalcoi.app.ui.data.GrammaticalGender

expect object Gender {
    /**
     * Returns the grammatical gender of the application as specified by the user or system settings.
     *
     * @throws UnsupportedOperationException if grammatical gender is not supported on the current platform or version.
     */
    fun getGrammaticalGender(): GrammaticalGender
}
