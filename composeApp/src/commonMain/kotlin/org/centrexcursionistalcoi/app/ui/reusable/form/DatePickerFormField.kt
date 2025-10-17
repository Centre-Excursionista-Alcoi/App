package org.centrexcursionistalcoi.app.ui.reusable.form

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DateTimeFormat
import org.centrexcursionistalcoi.app.ui.reusable.clickInteractionSource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DatePickerFormField(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    noValueString: String = "----/--/--",
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
    formatter: DateTimeFormat<LocalDate> = LocalDate.Formats.ISO,
) {
    var showingPicker by remember { mutableStateOf(false) }
    if (showingPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.toEpochDays()?.times(24 * 60 * 60 * 1000),
            selectableDates = selectableDates,
        )

        DatePickerDialog(
            onDismissRequest = { showingPicker = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        val selectedDate = LocalDate.fromEpochDays(state.selectedDateMillis!! / (24 * 60 * 60 * 1000))
                        onValueChange(selectedDate)
                        showingPicker = false
                    }
                ) {
                    Text(stringResource(Res.string.confirm))
                }
            }
        ) {
            DatePicker(
                state = state
            )
        }
    }

    OutlinedTextField(
        value = value?.let { formatter.format(it) } ?: noValueString,
        onValueChange = {},
        readOnly = true,
        modifier = modifier,
        label = { Text(label) },
        interactionSource = clickInteractionSource { showingPicker = true },
    )
}
