package ui.reusable.form

import androidx.compose.runtime.Composable
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.validation_email
import app.composeapp.generated.resources.validation_password
import org.jetbrains.compose.resources.stringResource

object FieldFormatValidators {
    data object Email : FieldFormatValidator.Regex() {
        override val pattern: Regex = "[\\w.]{1,255}@[\\w.]{1,255}".toRegex()

        @Composable
        override fun error(): String = stringResource(Res.string.validation_email)
    }

    data object Password : FieldFormatValidator.Regex() {
        override val pattern: Regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}\$".toRegex()

        @Composable
        override fun error(): String = stringResource(Res.string.validation_password)
    }
}
