package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mohamedrejeb.calf.ui.datepicker.AdaptiveDatePicker
import com.mohamedrejeb.calf.ui.datepicker.rememberAdaptiveDatePickerState
import io.github.alexzhirkevich.cupertino.CupertinoAlertDialog
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTextField
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.utils.atEndOfDayInMilliseconds
import org.centrexcursionistalcoi.app.utils.toEpochMilliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCupertinoApi::class)
@Composable
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
        // TODO: Restrict selectable dates
        val state = rememberAdaptiveDatePickerState(
            initialSelectedDateMillis = value?.toEpochMilliseconds(),
            initialDisplayedMonthMillis = (value ?: initialDisplayedDate)?.atEndOfDayInMilliseconds()
        )

        LaunchedEffect(state.selectedDateMillis) {
            val timestamp = state.selectedDateMillis ?: return@LaunchedEffect
            onValueChanged(
                Instant.fromEpochMilliseconds(timestamp)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            )
        }

        CupertinoAlertDialog(
            onDismissRequest = { showingPicker = false },
            title = { CupertinoText(label) },
            message = {
                AdaptiveDatePicker(
                    state = state
                )
            },
            buttons = {
            }
        )
    }

    Column(modifier) {
        CupertinoText(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = CupertinoTheme.typography.caption1
        )

        CupertinoTextField(
            value = value?.let { format.format(it) } ?: "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
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
}
