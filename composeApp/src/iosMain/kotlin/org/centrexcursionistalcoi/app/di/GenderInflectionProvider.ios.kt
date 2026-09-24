package org.centrexcursionistalcoi.app.di

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Singleton
import platform.Foundation.NSGrammaticalGenderFeminine
import platform.Foundation.NSGrammaticalGenderMasculine
import platform.Foundation.NSGrammaticalGenderNeuter
import platform.Foundation.NSMorphology
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTermOfAddress
import platform.Foundation.NSUserDefaults
import platform.Foundation.userMorphology

@Singleton
class IosGenderInflectionProvider : GenderInflectionProvider {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val prefKey = "app_grammatical_gender"

    override val observableGender: StateFlow<GenderInflection?>
        field = MutableStateFlow(getGenderInflection())

    override fun setGenderInflection(genderInflection: GenderInflection) {
        // iOS does not have an OS-level API to override grammatical gender per-app.
        // We must store this manually to mirror Android's behavior.
        val stringValue = when (genderInflection) {
            GenderInflection.Masculine -> "masculine"
            GenderInflection.Feminine -> "feminine"
            GenderInflection.Neutral -> "neutral"
        }
        defaults.setObject(stringValue, forKey = prefKey)

        observableGender.value = genderInflection
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun getGenderInflection(): GenderInflection? {
        // Check if the user set an app-specific override
        val override = defaults.stringForKey(prefKey)
        when (override) {
            "masculine" -> return GenderInflection.Masculine
            "feminine" -> return GenderInflection.Feminine
            "neutral" -> return GenderInflection.Neutral
        }

        // Otherwise, use the system-level grammatical gender if available
        val majorVersion = NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion }

        // iOS 18+: Use the newer NSTermOfAddress API
        if (majorVersion >= 18L) {
            val systemTerm = NSTermOfAddress.currentUser()

            // Objective-C equality translates cleanly to Kotlin `when` checks
            return when (systemTerm) {
                NSTermOfAddress.masculine() -> GenderInflection.Masculine
                NSTermOfAddress.feminine() -> GenderInflection.Feminine
                NSTermOfAddress.neutral() -> GenderInflection.Neutral
                else -> null
            }
        }

        // iOS 15–17: Fall back to the older NSMorphology API
        if (majorVersion >= 15L) {
            val morphology = NSMorphology.userMorphology

            return when (morphology.grammaticalGender) {
                NSGrammaticalGenderMasculine -> GenderInflection.Masculine
                NSGrammaticalGenderFeminine -> GenderInflection.Feminine
                NSGrammaticalGenderNeuter -> GenderInflection.Neutral
                else -> null
            }
        }

        return null
    }
}
