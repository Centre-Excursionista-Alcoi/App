package ui.reusable.form

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import utils.toInstant
import utils.toLocalDate

private const val SecondsInADay = 24 * 60 * 60L

/**
 * Displays an outlined text field intended to be used in forms.
 * It forced to have just one line, and some utilities are provided.
 *
 * @param value The current value of the field, if null, the field will be empty.
 * @param onValueChange Will be called whenever the user types something in the field.
 * @param label The text to display on top of the field.
 * @param modifier If any, modifiers to apply to the field.
 * @param enabled If `true` the field is intractable, `false` disables the field. Default: `true`
 * @param supportingText If any, the text that will be displayed under the field for giving more information about the
 * field to the user.
 * Won't be displayed if [error] is not null.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FormDatePicker(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null
) {
    val valueString = value?.let {
        "${it.year}/${it.monthNumber}/${it.dayOfMonth}"
    } ?: ""

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = value
            ?.toInstant()
            ?.toEpochMilliseconds()
    )

    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog)
        DatePickerDialog(
            onDismissRequest = { showingDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis!!
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val date = instant.toLocalDate()
                        onValueChange(date)
                        showingDialog = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Confirm")
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }

    OutlinedTextField(
        value = valueString,
        onValueChange = { },
        modifier = modifier,
        label = { Text(label) },
        enabled = enabled,
        readOnly = true,
        singleLine = true,
        maxLines = 1,
        trailingIcon = (@Composable {
            IconButton(
                onClick = { showingDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null
                )
            }
        }),
        supportingText = supportingText?.let {
            { Text(it) }
        },
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            // works like onClick
                            showingDialog = true
                        }
                    }
                }
            }
    )
}
