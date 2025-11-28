package org.centrexcursionistalcoi.app.ui.reusable.form

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.isInputValid
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char
import org.centrexcursionistalcoi.app.ui.reusable.clickInteractionSource
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DateTimePickerFormField(
    value: LocalDateTime?,
    onValueChange: (LocalDateTime) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    noValueString: String = "----/--/-- --:--",
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
    formatter: DateTimeFormat<LocalDateTime> = LocalDateTime.Format {
        year()
        char('/')
        monthNumber()
        char('/')
        day()
        char(' ')

        hour()
        char(':')
        minute()
    },
) {
    var date by remember(value) { mutableStateOf(value?.date) }
    var time by remember(value) { mutableStateOf(value?.time) }

    var showingDateDialog by remember { mutableStateOf(false) }
    var showingTimeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            showingDateDialog = false
            showingTimeDialog = false
        }
    }

    fun onValueChange() {
        date?.let { date ->
            time?.let { time ->
                onValueChange(LocalDateTime(date, time))
            }
        }
    }

    if (showingDateDialog) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.toEpochDays()?.times(24 * 60 * 60 * 1000),
            selectableDates = selectableDates,
        )

        DatePickerDialog(
            onDismissRequest = { showingDateDialog = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        val selectedDate = LocalDate.fromEpochDays(state.selectedDateMillis!! / (24 * 60 * 60 * 1000))
                        date = selectedDate
                        onValueChange()
                        showingDateDialog = false
                        showingTimeDialog = true
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

    if (showingTimeDialog) {
        val state = rememberTimePickerState(
            initialHour = time?.hour ?: 0,
            initialMinute = time?.minute ?: 0,
        )

        TimePickerDialog(
            onDismissRequest = { showingTimeDialog = false },
            confirmButton = {
                TextButton(
                    enabled = state.isInputValid,
                    onClick = {
                        time = LocalTime(state.hour, state.minute)
                        onValueChange()
                        showingTimeDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.confirm))
                }
            },
            title = { Text(stringResource(Res.string.time_picker_title)) },
        ) {
            TimePicker(
                state = state,
            )
        }
    }

    OutlinedTextField(
        value = value?.let { formatter.format(it) } ?: noValueString,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        modifier = modifier,
        label = { Text(label) },
        interactionSource = clickInteractionSource {
            showingDateDialog = true
        },
    )
}
