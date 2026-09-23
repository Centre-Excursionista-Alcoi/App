package org.centrexcursionistalcoi.app.di

import android.app.GrammaticalInflectionManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Singleton

@Singleton
class AndroidGenderInflectionProvider(context: Context) : GenderInflectionProvider {
    private val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(GrammaticalInflectionManager::class.java)
    } else {
        null
    }

    private val sharedPreferences = context.getSharedPreferences("gender_inflection_prefs", Context.MODE_PRIVATE)
    private val prefKey = "grammatical_gender"

    override val observableGender: StateFlow<GenderInflection?>
        field = MutableStateFlow(getGenderInflection())

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun GenderInflection.toGrammaticalGender(): Int = when (this) {
        GenderInflection.Masculine -> Configuration.GRAMMATICAL_GENDER_MASCULINE
        GenderInflection.Feminine -> Configuration.GRAMMATICAL_GENDER_FEMININE
        GenderInflection.Neutral -> Configuration.GRAMMATICAL_GENDER_NEUTRAL
    }

    override fun setGenderInflection(genderInflection: GenderInflection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            manager?.setRequestedApplicationGrammaticalGender(genderInflection.toGrammaticalGender())
        } else {
            // Store the preference in SharedPreferences for older Android versions
            sharedPreferences.edit { putString(prefKey, genderInflection.name) }
        }
        observableGender.value = genderInflection
    }

    override fun getGenderInflection(): GenderInflection? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return when (manager?.applicationGrammaticalGender) {
                Configuration.GRAMMATICAL_GENDER_MASCULINE -> GenderInflection.Masculine
                Configuration.GRAMMATICAL_GENDER_FEMININE -> GenderInflection.Feminine
                Configuration.GRAMMATICAL_GENDER_NEUTRAL -> GenderInflection.Neutral
                else -> null
            }
        } else {
            // Retrieve the preference from SharedPreferences for older Android versions
            val genderName = sharedPreferences.getString(prefKey, null)
            return when (genderName) {
                GenderInflection.Masculine.name -> GenderInflection.Masculine
                GenderInflection.Feminine.name -> GenderInflection.Feminine
                GenderInflection.Neutral.name -> GenderInflection.Neutral
                else -> null
            }
        }
    }
}
