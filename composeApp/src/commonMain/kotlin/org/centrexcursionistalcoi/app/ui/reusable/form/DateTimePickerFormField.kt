package org.centrexcursionistalcoi.app.ui.reusable.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char
import org.centrexcursionistalcoi.app.ui.reusable.clickInteractionSource
import org.jetbrains.compose.resources.stringResource

private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

/** Matches Material3's own `DatePickerModalTokens.ContainerWidth` -- wide enough for the calendar grid. */
private val PICKER_WIDTH = 360.dp

/**
 * Fixed so the dialog doesn't resize as the pager swipes between the (taller) date grid and the (shorter) time
 * dial -- generous enough for [DatePicker]'s "picker" display mode; [TimePicker] just centers within the extra
 * space. Each page still scrolls internally as a safety net in case a given platform/theme renders taller.
 */
private val PICKER_HEIGHT = 460.dp

@Composable
fun DateTimePickerFormField(
    value: LocalDateTime?,
    onValueChange: (LocalDateTime) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    noValueString: String = "----/--/-- --:--",
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
    /**
     * When `false`, the dialog shows a checkbox to opt out of picking a time at all. Since [value] is a
     * [LocalDateTime] and always carries a time component, "no time" is represented by convention as midnight
     * (`LocalTime(0, 0)`) -- both on the way out (via [onValueChange]) and on the way back in (an [value] whose
     * time is exactly midnight starts the dialog with the checkbox unchecked). A deliberately-picked midnight is
     * therefore indistinguishable from "no time"; callers that need to tell those apart shouldn't use `false`
     * here.
     */
    requireTime: Boolean = true,
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
    var showingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) showingDialog = false
    }

    if (showingDialog) {
        DateTimePickerDialog(
            title = label,
            initialValue = value,
            selectableDates = selectableDates,
            requireTime = requireTime,
            onConfirm = {
                onValueChange(it)
                showingDialog = false
            },
            onDismissRequest = { showingDialog = false },
        )
    }

    OutlinedTextField(
        value = value?.let { formatter.format(it) } ?: noValueString,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        modifier = modifier,
        label = { Text(label) },
        interactionSource = clickInteractionSource {
            showingDialog = true
        },
    )
}

/**
 * A single dialog holding both the date and time pickers as [HorizontalPager] pages switched via a
 * [SecondaryTabRow], instead of two separate Material3 dialogs shown one after another -- picking a date used to
 * close one dialog and immediately open another, which reads as a stutter rather than a smooth flow. Both
 * pickers keep their own state for as long as the dialog is open and are only combined into a single
 * [LocalDateTime] once, when [onConfirm] fires, so there's no intermediate value to keep in sync between them.
 *
 * [SecondaryTabRow] (not [PrimaryTabRow]) matters here: Primary's background is opaque, meant for page-level
 * navigation directly on a scaffold -- nested inside a dialog's already-elevated surface it shows as a visibly
 * mismatched rectangle. Secondary is designed to blend into whatever surface it's nested in.
 */
@Composable
private fun DateTimePickerDialog(
    title: String,
    initialValue: LocalDateTime?,
    selectableDates: SelectableDates,
    requireTime: Boolean,
    onConfirm: (LocalDateTime) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialValue?.date?.toEpochDays()?.times(MILLIS_PER_DAY),
        selectableDates = selectableDates,
    )
    val timePickerState = rememberTimePickerState(
        initialHour = initialValue?.hour ?: 0,
        initialMinute = initialValue?.minute ?: 0,
    )
    // Always true when requireTime -- the checkbox (and the possibility of unchecking it) only exists otherwise.
    var includeTime by remember { mutableStateOf(requireTime || initialValue?.time != LocalTime(0, 0)) }

    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    AlertDialog(
        // Workaround for the same Material3 AlertDialog width constraint DatePickerDialog itself works around:
        // without this, the platform default width clips DatePicker's calendar grid.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            // horizontalAlignment centers the (narrower, fixed-width) tab row and pager as a block, in case the
            // dialog itself ends up wider than PICKER_WIDTH (e.g. to fit the title or button row).
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.requiredWidth(PICKER_WIDTH),
                    // Forced transparent rather than relying on the default matching AlertDialog's own tonal
                    // surface color -- SecondaryTabRow's default container color doesn't reliably match the
                    // dialog's elevated background, and shows as a mismatched rectangle behind the tabs.
                    containerColor = Color.Transparent,
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text(stringResource(Res.string.date_time_picker_date_tab)) },
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        // Not selectable while unchecked -- there's nothing to show on that page. The checkbox
                        // itself stays independently clickable (its own toggleable modifier, separate from the
                        // tab's), so it's still the way back in.
                        enabled = includeTime,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!requireTime) {
                                    Checkbox(
                                        checked = includeTime,
                                        onCheckedChange = { checked ->
                                            includeTime = checked
                                            // Checking it opens the time page to pick one; unchecking it has
                                            // nothing left to show there, so fall back to the date page.
                                            scope.launch { pagerState.animateScrollToPage(if (checked) 1 else 0) }
                                        },
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Text(stringResource(Res.string.date_time_picker_time_tab))
                            }
                        },
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    // Locked while unchecked too, so a swipe can't reach the empty time page either.
                    userScrollEnabled = includeTime,
                    modifier = Modifier.requiredWidth(PICKER_WIDTH).height(PICKER_HEIGHT),
                ) { page ->
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        when (page) {
                            // title = null: DatePicker's own "Select date" headline would otherwise duplicate
                            // the AlertDialog's title above it.
                            0 -> DatePicker(state = datePickerState, title = null)
                            else -> if (includeTime) {
                                TimePicker(state = timePickerState, layoutType = TimePickerLayoutType.Vertical)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = datePickerState.selectedDateMillis != null,
                onClick = {
                    val date = LocalDate.fromEpochDays(datePickerState.selectedDateMillis!! / MILLIS_PER_DAY)
                    val time = if (includeTime) LocalTime(timePickerState.hour, timePickerState.minute) else LocalTime(0, 0)
                    onConfirm(LocalDateTime(date, time))
                },
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
