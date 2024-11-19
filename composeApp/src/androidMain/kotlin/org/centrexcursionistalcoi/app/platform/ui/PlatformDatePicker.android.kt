package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ceaapp.composeapp.generated.resources.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.utils.atEndOfDayInMilliseconds
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
actual fun PlatformDatePicker(
    value: LocalDate?,
    onValueChanged: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    format: DateTimeFormat<LocalDate>,
    min: LocalDate?,
    max: LocalDate?,
    initialDisplayedDate: LocalDate?
) {
    var showingPicker by remember { mutableStateOf(false) }
    if (enabled && showingPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.atEndOfDayInMilliseconds(),
            initialDisplayedMonthMillis = (value ?: initialDisplayedDate)?.atEndOfDayInMilliseconds(),
            selectableDates = if (min == null && max == null) {
                DatePickerDefaults.AllDates
            } else {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val date = Instant.fromEpochMilliseconds(utcTimeMillis)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date

                        return when {
                            min != null && date < min -> false
                            max != null && date > max -> false
                            else -> true
                        }
                    }

                    override fun isSelectableYear(year: Int): Boolean {
                        return true
                    }
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showingPicker = false },
            confirmButton = {
                PlatformButton(
                    text = stringResource(Res.string.ok),
                ) {
                    state.selectedDateMillis
                        ?.let { Instant.fromEpochMilliseconds(it) }
                        ?.toLocalDateTime(TimeZone.currentSystemDefault())
                        ?.date
                        ?.let(onValueChanged)

                    showingPicker = false
                }
            }
        ) {
            DatePicker(
                state = state
            )
        }
    }

    OutlinedTextField(
        value = value?.let { format.format(it) } ?: "",
        onValueChange = {},
        modifier = modifier,
        label = { Text(label) },
        enabled = enabled,
        readOnly = true,
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            // on click, show date picker
                            showingPicker = true
                        }
                    }
                }
            }
    )
}
