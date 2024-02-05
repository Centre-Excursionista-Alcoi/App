package ui.reusable.form

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle

/**
 * Displays an outlined text field intended to be used in forms.
 * It forced to have just one line, and some utilities are provided.
 *
 * @param value The current value of the field, if null, the field will be empty.
 * @param onValueChange Will be called whenever the user types something in the field.
 * @param label The text to display on top of the field.
 * @param modifier If any, modifiers to apply to the field.
 * @param enabled If `true` the field is intractable, `false` disables the field. Default: `true`
 * @param error If not `null`, this text will be displayed in red under the field.
 * @param capitalization Can be provided to specify the capitalization options for the keyboard. Defaults to none.
 * @param supportingText If any, the text that will be displayed under the field for giving more information about the
 * field to the user.
 * Won't be displayed if [error] is not null.
 * @param isRequired If `true`, a red tick (`*`) will be displayed at the end of [label].
 * @param validator If not null, will be used for validating the current text. Will show
 */
@Composable
fun FormMultilineField(
    value: TextFieldValue?,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    error: String? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isRequired: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    validator: FieldFormatValidator? = null
) {
    var selection: TextRange? = null

    val validationError = validator
        // Only validate if text has been introduced
        ?.takeIf { value != null }
        ?.takeUnless { it.validate(value?.text) }
        ?.error()

    OutlinedTextField(
        value = value ?: TextFieldValue(),
        onValueChange = {
            selection = it.selection
            onValueChange(it)
        },
        modifier = Modifier
            .onKeyEvent { ev ->
                when {
                    ev.isCtrlPressed && ev.key == Key.Backspace && ev.type == KeyEventType.KeyUp -> {
                        selection
                            ?.takeIf { value != null }
                            ?.takeIf { it.length <= 0 }
                            ?.takeIf { it.start > 0 }
                            ?.let { range ->
                                val textToRemove = value!!.text.substring(0, range.start)
                                onValueChange(
                                    value.copy(
                                        text = value.text.replace(textToRemove, "")
                                    )
                                )
                                true
                            } ?: false
                    }

                    ev.isCtrlPressed && ev.key == Key.Delete && ev.type == KeyEventType.KeyUp -> {
                        selection
                            ?.takeIf { value != null }
                            ?.takeIf { it.length <= 0 }
                            ?.let { range ->
                                val textToRemove = value!!.text.substring(range.start)
                                onValueChange(
                                    value.copy(
                                        text = value.text.replace(textToRemove, "")
                                    )
                                )
                                true
                            } ?: false
                    }

                    else -> false
                }
            }
            .then(modifier),
        label = {
            Text(
                text = buildAnnotatedString {
                    append(label)
                    if (isRequired) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.error)) {
                            append(" *")
                        }
                    }
                }
            )
        },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = false,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization
        ),
        isError = error != null || validationError != null,
        supportingText = {
            AnimatedContent(
                targetState = error to validationError,
                transitionSpec = {
                    slideInVertically { -it } togetherWith slideOutVertically { -it }
                }
            ) { (err, validation) ->
                err?.let { Text(it) }
                    ?: validation?.let { Text(it) }
                    ?: supportingText?.let { Text(it) }
            }
        }
    )
}
