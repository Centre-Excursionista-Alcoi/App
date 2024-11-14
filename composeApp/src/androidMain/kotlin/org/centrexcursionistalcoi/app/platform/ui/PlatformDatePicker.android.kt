package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ceaapp.composeapp.generated.resources.Res
import ceaapp.composeapp.generated.resources.ok
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.toLocalDateTime
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
    max: LocalDate?
) {
    var showingPicker by remember { mutableStateOf(false) }
    if (enabled && showingPicker) {
        val state = rememberDatePickerState(
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
