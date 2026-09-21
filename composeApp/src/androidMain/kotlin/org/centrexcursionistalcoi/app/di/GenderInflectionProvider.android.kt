package org.centrexcursionistalcoi.app.di

import android.app.GrammaticalInflectionManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import org.koin.core.annotation.Singleton

@Singleton
class AndroidGenderInflectionProvider(context: Context) : GenderInflectionProvider {
    private val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(GrammaticalInflectionManager::class.java)
    } else {
        null
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun GenderInflection.toGrammaticalGender(): Int = when (this) {
        GenderInflection.Masculine -> Configuration.GRAMMATICAL_GENDER_MASCULINE
        GenderInflection.Feminine -> Configuration.GRAMMATICAL_GENDER_FEMININE
        GenderInflection.Neutral -> Configuration.GRAMMATICAL_GENDER_NEUTRAL
    }

    override fun setGenderInflection(genderInflection: GenderInflection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            manager?.setRequestedApplicationGrammaticalGender(genderInflection.toGrammaticalGender())
        }
    }

    override fun getGenderInflection(): GenderInflection? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return when (manager?.applicationGrammaticalGender) {
                Configuration.GRAMMATICAL_GENDER_MASCULINE -> GenderInflection.Masculine
                Configuration.GRAMMATICAL_GENDER_FEMININE -> GenderInflection.Feminine
                Configuration.GRAMMATICAL_GENDER_NEUTRAL -> GenderInflection.Neutral
                else -> null
            }
        }
        return null
    }
}
