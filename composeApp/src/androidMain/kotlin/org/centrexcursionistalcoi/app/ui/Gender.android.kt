package org.centrexcursionistalcoi.app.ui

import android.app.GrammaticalInflectionManager
import android.os.Build
import androidx.core.app.GrammaticalInflectionManagerCompat
import org.centrexcursionistalcoi.app.appContext
import org.centrexcursionistalcoi.app.ui.data.GrammaticalGender

actual object Gender {
    actual fun getGrammaticalGender(): GrammaticalGender {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            throw UnsupportedOperationException("GrammaticalInflectionManager is not supported on this Android version.")
        }

        val gim = appContext.getSystemService(GrammaticalInflectionManager::class.java)
        return when (gim.applicationGrammaticalGender) {
            GrammaticalInflectionManagerCompat.GRAMMATICAL_GENDER_NOT_SPECIFIED -> GrammaticalGender.NOT_SPECIFIED
            GrammaticalInflectionManagerCompat.GRAMMATICAL_GENDER_NEUTRAL -> GrammaticalGender.NEUTRAL
            GrammaticalInflectionManagerCompat.GRAMMATICAL_GENDER_FEMININE -> GrammaticalGender.FEMININE
            GrammaticalInflectionManagerCompat.GRAMMATICAL_GENDER_MASCULINE -> GrammaticalGender.MASCULINE
            else -> GrammaticalGender.NOT_SPECIFIED
        }
    }
}
