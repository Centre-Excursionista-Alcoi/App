package ui.reusable.form

import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.compose.stringResource
import resources.MR

object FieldFormatValidators {
    data object Email : FieldFormatValidator.Regex() {
        override val pattern: Regex = "[\\w.]{1,255}@[\\w.]{1,255}".toRegex()

        @Composable
        override fun error(): String = stringResource(MR.strings.validation_email)
    }

    data object Password : FieldFormatValidator.Regex() {
        override val pattern: Regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}\$".toRegex()

        @Composable
        override fun error(): String = stringResource(MR.strings.validation_password)
    }
}
