package org.centrexcursionistalcoi.app.di

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Singleton
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTermOfAddress
import platform.Foundation.NSUserDefaults

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
        // 1. Check if the user set an app-specific override
        val override = defaults.stringForKey(prefKey)
        when (override) {
            "masculine" -> return GenderInflection.Masculine
            "feminine" -> return GenderInflection.Feminine
            "neutral" -> return GenderInflection.Neutral
        }

        // 2. Fall back to the iOS 17+ system-wide Term of Address
        val majorVersion = NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion }

        if (majorVersion >= 17L) {
            val systemTerm = NSTermOfAddress.currentUser()

            // Objective-C equality translates cleanly to Kotlin `when` checks
            return when (systemTerm) {
                NSTermOfAddress.masculine() -> GenderInflection.Masculine
                NSTermOfAddress.feminine() -> GenderInflection.Feminine
                NSTermOfAddress.neutral() -> GenderInflection.Neutral
                else -> null
            }
        }

        return null
    }
}
