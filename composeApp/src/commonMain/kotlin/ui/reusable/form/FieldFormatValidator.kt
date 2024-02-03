package ui.reusable.form

import androidx.compose.runtime.Composable

/**
 * Used by some form fields to check whether its contents are valid or not.
 */
interface FieldFormatValidator {
    /**
     * Validates the string given.
     * @return `true` if the [value] is valid, `false` otherwise.
     */
    fun validate(value: String?): Boolean

    /**
     * The error that should be shown if the validation is not successful.
     */
    @Composable
    fun error(): String

    abstract class Regex : FieldFormatValidator {
        protected abstract val pattern: kotlin.text.Regex

        override fun validate(value: String?): Boolean = value != null && pattern.matches(value)
    }
}
