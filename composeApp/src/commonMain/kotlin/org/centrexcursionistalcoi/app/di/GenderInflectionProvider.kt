package org.centrexcursionistalcoi.app.di

import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent

/**
 * Reads/writes the user's grammatical gender preference through the platform, where the platform supports it.
 *
 * Optional: only platforms with an implementation register one (currently just `AndroidGenderInflectionProvider`,
 * `@Singleton`-annotated and bound to this interface by `CoreScanModule`'s `@ComponentScan`). Nothing is bound on
 * other platforms, so never `get()`/`inject()` this type -- use [globalGenderInflectionProvider], which returns
 * `null` when there's none.
 */
interface GenderInflectionProvider {
    val observableGender: StateFlow<GenderInflection?>

    fun getGenderInflection(): GenderInflection?

    fun setGenderInflection(genderInflection: GenderInflection)
}

enum class GenderInflection {
    Masculine,
    Feminine,
    Neutral
}

private object GenderInflectionProviderHolder : KoinComponent {
    fun provider(): GenderInflectionProvider? = getKoin().getOrNull<GenderInflectionProvider>()
}

/**
 * The platform's [GenderInflectionProvider], or `null` if the current platform doesn't provide one. Resolved on
 * every access rather than cached, so it follows the Koin instance currently running.
 */
val globalGenderInflectionProvider: GenderInflectionProvider? get() = GenderInflectionProviderHolder.provider()
