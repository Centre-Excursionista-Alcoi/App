package org.centrexcursionistalcoi.app.ui.resources

import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.login_title_fem
import cea_app.composeapp.generated.resources.login_title_masc
import cea_app.composeapp.generated.resources.login_title_neut
import cea_app.composeapp.generated.resources.welcome_fem
import cea_app.composeapp.generated.resources.welcome_masc
import cea_app.composeapp.generated.resources.welcome_neut
import org.centrexcursionistalcoi.app.di.GenderInflection
import org.centrexcursionistalcoi.app.di.globalGenderInflectionProvider
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource as cmpStringResource

data class GenderedStringResource(
    val masculine: StringResource,
    val feminine: StringResource,
    val neutral: StringResource
) {
    @Composable
    fun stringResource(vararg formatArgs: Any): String {
        val gip = globalGenderInflectionProvider
        return when (gip?.getGenderInflection()) {
            GenderInflection.Masculine -> cmpStringResource(masculine, *formatArgs)
            GenderInflection.Feminine -> cmpStringResource(feminine, *formatArgs)
            GenderInflection.Neutral -> cmpStringResource(neutral, *formatArgs)
            null -> cmpStringResource(neutral)
        }
    }

    companion object {
        val Welcome @Composable get() = GenderedStringResource(
            masculine = Res.string.welcome_masc,
            feminine = Res.string.welcome_fem,
            neutral = Res.string.welcome_neut
        )

        val LoginTitle @Composable get() = GenderedStringResource(
            masculine = Res.string.login_title_masc,
            feminine = Res.string.login_title_fem,
            neutral = Res.string.login_title_neut
        )
    }
}
