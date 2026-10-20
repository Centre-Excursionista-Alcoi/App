package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.calendar_next_month
import cea_app.composeapp.generated.resources.calendar_previous_month
import cea_app.composeapp.generated.resources.close
import cea_app.composeapp.generated.resources.days_of_week_short_friday
import cea_app.composeapp.generated.resources.days_of_week_short_monday
import cea_app.composeapp.generated.resources.days_of_week_short_saturday
import cea_app.composeapp.generated.resources.days_of_week_short_sunday
import cea_app.composeapp.generated.resources.days_of_week_short_thursday
import cea_app.composeapp.generated.resources.days_of_week_short_tuesday
import cea_app.composeapp.generated.resources.days_of_week_short_wednesday
import cea_app.composeapp.generated.resources.lending_details_complete
import cea_app.composeapp.generated.resources.lending_details_confirmation_pending_title
import cea_app.composeapp.generated.resources.lending_details_memory_pending_title
import cea_app.composeapp.generated.resources.lending_details_pickup_pending_title
import cea_app.composeapp.generated.resources.lending_details_return_pending_title
import cea_app.composeapp.generated.resources.lendings_kind_all
import cea_app.composeapp.generated.resources.lendings_kind_items
import cea_app.composeapp.generated.resources.lendings_kind_space
import cea_app.composeapp.generated.resources.lendings_kind_spaces
import cea_app.composeapp.generated.resources.management_active_lendings
import cea_app.composeapp.generated.resources.management_complete_lendings
import cea_app.composeapp.generated.resources.management_lending_give
import cea_app.composeapp.generated.resources.management_lending_receive
import cea_app.composeapp.generated.resources.months_april
import cea_app.composeapp.generated.resources.months_august
import cea_app.composeapp.generated.resources.months_december
import cea_app.composeapp.generated.resources.months_february
import cea_app.composeapp.generated.resources.months_january
import cea_app.composeapp.generated.resources.months_july
import cea_app.composeapp.generated.resources.months_june
import cea_app.composeapp.generated.resources.months_march
import cea_app.composeapp.generated.resources.months_may
import cea_app.composeapp.generated.resources.months_november
import cea_app.composeapp.generated.resources.months_october
import cea_app.composeapp.generated.resources.months_september
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusMonths
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.ui.data.LendingKind
import org.centrexcursionistalcoi.app.ui.data.LendingKindFilter
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ChevronLeft
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ChevronRight
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Close
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Home
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Inventory2
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.page.main.LendingItem
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.ui.reusable.buttons.DeleteButton
import org.centrexcursionistalcoi.app.ui.reusable.buttons.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.screen.admin.lendingManagementScreenContent
import org.centrexcursionistalcoi.app.ui.screen.spaces.formatted
import org.centrexcursionistalcoi.app.ui.screen.spaces.label
import org.centrexcursionistalcoi.app.ui.screen.spaces.stage
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.centrexcursionistalcoi.app.viewmodel.management.LendingsManagementViewModel
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Every lending the viewer manages, of equipment ([showItems]) and of spaces ([showSpaces]), in a calendar and a list.
 * Equipment lendings open in a panel (or over the list); space lendings open their own screen.
 */
@Composable
fun LendingsListView(
    snackbarHostState: SnackbarHostState,
    showItems: Boolean,
    showSpaces: Boolean,
    onGiveRequested: (ReferencedLending) -> Unit,
    onReceiveRequested: (ReferencedLending) -> Unit,
    onSpaceLendingClick: (Uuid) -> Unit,
    model: LendingsManagementViewModel = koinViewModel()
) {
    val toggleCompletedLendings by model.toggleCompletedLendings.collectAsState()
    val lendings by model.lendings.collectAsState()
    val spaceLendings by model.spaceLendings.collectAsState()
    val spaces by model.spaces.collectAsState()
    val users by model.users.collectAsState()
    val kindFilter by model.kindFilter.collectAsState()

    LendingsListView(
        snackbarHostState = snackbarHostState,
        lendings = lendings?.takeIf { showItems },
        spaceLendings = spaceLendings?.takeIf { showSpaces },
        spaceNames = remember(spaces) { spaces.orEmpty().associate { it.id to it.name } },
        users = users.orEmpty(),
        kindFilter = when {
            // Only one kind to show: nothing to choose from
            !showSpaces -> LendingKindFilter.ITEMS
            !showItems -> LendingKindFilter.SPACES
            else -> kindFilter
        },
        showKindFilter = showItems && showSpaces,
        onKindFilterChange = model::setKindFilter,
        toggleCompletedLendings = toggleCompletedLendings,
        onToggleCompletedLendings = model::toggleCompletedLendings,
        onConfirmLendingRequest = { model.confirmLending(it) },
        onSkipMemoryRequest = { model.skipLendingMemory(it) },
        onGiveRequested = onGiveRequested,
        onReceiveRequested = onReceiveRequested,
        onDeleteRequested = { lending, reason -> model.delete(lending, reason) },
        onSpaceLendingClick = onSpaceLendingClick,
    )
}

@Composable
private fun LendingsListView(
    snackbarHostState: SnackbarHostState,
    lendings: List<ReferencedLending>?,
    spaceLendings: List<SpaceLending>?,
    spaceNames: Map<Uuid, String>,
    users: List<UserData>,
    kindFilter: LendingKindFilter,
    showKindFilter: Boolean,
    onKindFilterChange: (LendingKindFilter) -> Unit,
    toggleCompletedLendings: Boolean,
    onToggleCompletedLendings: (Boolean) -> Unit,
    onConfirmLendingRequest: (ReferencedLending) -> Job,
    onSkipMemoryRequest: (ReferencedLending) -> Job,
    onGiveRequested: (ReferencedLending) -> Unit,
    onReceiveRequested: (ReferencedLending) -> Unit,
    onDeleteRequested: (ReferencedLending, reason: String?) -> Job,
    onSpaceLendingClick: (Uuid) -> Unit,
) {
    val windowSizeClass = calculateWindowSizeClass()

    var selectedLending by remember { mutableStateOf<ReferencedLending?>(null) }
    LaunchedEffect(lendings) {
        if (selectedLending != null) {
            // Try to re-select the lending after lendings list update
            selectedLending = lendings?.find { it.id == selectedLending!!.id }
        }
    }

    val entries = remember(lendings, spaceLendings, spaceNames, kindFilter) {
        mergedLendingEntries(lendings, spaceLendings, spaceNames, kindFilter)
    }
    val calendarEntries = remember(lendings, spaceLendings, kindFilter) {
        calendarEntries(lendings, spaceLendings, kindFilter)
    }
    val onEntryClick: (LendingListEntry) -> Unit = { entry ->
        when (entry) {
            is LendingListEntry.Item -> selectedLending = entry.lending
            is LendingListEntry.Space -> onSpaceLendingClick(entry.id)
        }
    }


    if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded && windowSizeClass.heightSizeClass >= WindowHeightSizeClass.Medium) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.width(400.dp)) {
                if (showKindFilter) LendingKindFilterChips(kindFilter, onKindFilterChange)
                LendingsCalendar(
                    entries = calendarEntries,
                    tooltip = { entry -> lendingTooltip(entry, lendings, spaceLendings, spaceNames, users) },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { entry ->
                        when (entry.kind) {
                            LendingKind.ITEMS -> selectedLending = lendings?.find { it.id == entry.id }
                            LendingKind.SPACE -> onSpaceLendingClick(entry.id)
                        }
                    },
                )
                LendingsLazyColumn(
                    entries = entries,
                    toggleCompletedLendings = toggleCompletedLendings,
                    onToggleCompletedLendings = onToggleCompletedLendings,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp),
                    selectedLendingId = selectedLending?.id,
                    onClick = onEntryClick,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    selectedLending
                ) { lending ->
                    lending ?: return@AnimatedContent
                    LazyColumn {
                        lendingManagementScreenContent(
                            lending = lending,
                            snackbarHostState = snackbarHostState,
                            onConfirmRequest = { onConfirmLendingRequest(lending) },
                            onSkipMemoryRequest = { onSkipMemoryRequest(lending) },
                            users = users,
                            extraContent = {
                                LendingManagementScreenContent_ExtraContent(
                                    lending = lending,
                                    onGiveRequested = onGiveRequested,
                                    onReceiveRequested = onReceiveRequested,
                                    onDeleteRequested = onDeleteRequested,
                                )
                            },
                        )
                    }
                }
            }
        }
    } else {
        selectedLending?.let { lending ->
            // a lending is selected
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    TooltipIconButton(
                        imageVector = MaterialSymbols.Close,
                        tooltip = stringResource(Res.string.close),
                    ) { selectedLending = null }
                }

                lendingManagementScreenContent(
                    lending = lending,
                    users = users,
                    snackbarHostState = snackbarHostState,
                    onConfirmRequest = { onConfirmLendingRequest(lending) },
                    onSkipMemoryRequest = { onSkipMemoryRequest(lending) },
                    extraContent = {
                        LendingManagementScreenContent_ExtraContent(
                            lending = lending,
                            onGiveRequested = onGiveRequested,
                            onReceiveRequested = onReceiveRequested,
                            onDeleteRequested = onDeleteRequested,
                        )
                    },
                )
            }
        } ?: run {
            // no lending is selected
            Column(modifier = Modifier.fillMaxSize()) {
                if (showKindFilter) LendingKindFilterChips(kindFilter, onKindFilterChange)
                LendingsLazyColumn(
                    entries = entries,
                    toggleCompletedLendings = toggleCompletedLendings,
                    onToggleCompletedLendings = onToggleCompletedLendings,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp),
                    selectedLendingId = selectedLending?.id,
                    onClick = onEntryClick,
                )
            }
        }
    }
}

@Composable
private fun LendingManagementScreenContent_ExtraContent(
    lending: ReferencedLending,
    onGiveRequested: (ReferencedLending) -> Unit,
    onReceiveRequested: (ReferencedLending) -> Unit,
    onDeleteRequested: (ReferencedLending, reason: String?) -> Job,
) {
    when (lending.status()) {
        Lending.Status.CONFIRMED -> {
            ElevatedButton(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClick = { onGiveRequested(lending) }
            ) {
                Text(stringResource(Res.string.management_lending_give))
            }
        }
        Lending.Status.TAKEN -> {
            ElevatedButton(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClick = { onReceiveRequested(lending) }
            ) {
                Text(stringResource(Res.string.management_lending_receive))
            }
        }
        else -> {
            // nothing
        }
    }

    if (lending.status().isPending()) {
        // Only allow to delete lendings that are not yet completed
        DeleteButton(
            item = lending,
            showReasonField = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) { onDeleteRequested(lending, reason) }
    }
}

@Composable
private fun LendingKindFilterChips(
    filter: LendingKindFilter,
    onFilterChange: (LendingKindFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (option in LendingKindFilter.entries) {
            FilterChip(
                selected = filter == option,
                onClick = { onFilterChange(option) },
                label = {
                    Text(
                        stringResource(
                            when (option) {
                                LendingKindFilter.ALL -> Res.string.lendings_kind_all
                                LendingKindFilter.ITEMS -> Res.string.lendings_kind_items
                                LendingKindFilter.SPACES -> Res.string.lendings_kind_spaces
                            }
                        )
                    )
                },
            )
        }
    }
}

/** The icon and name of the kind of a lending, shown over each row of the list so that they can be told apart. */
@Composable
private fun LendingKindBadge(kind: LendingKind, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = when (kind) {
                LendingKind.ITEMS -> MaterialSymbols.Inventory2
                LendingKind.SPACE -> MaterialSymbols.Home
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(
                when (kind) {
                    LendingKind.ITEMS -> Res.string.lendings_kind_items
                    LendingKind.SPACE -> Res.string.lendings_kind_space
                }
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LendingRow(
    entry: LendingListEntry,
    isSelected: Boolean,
    onClick: (LendingListEntry) -> Unit,
) {
    Column {
        LendingKindBadge(entry.kind)
        when (entry) {
            is LendingListEntry.Item -> LendingItem(entry.lending, isActive = isSelected) { onClick(entry) }
            is LendingListEntry.Space -> SpaceLendingRow(entry) { onClick(entry) }
        }
    }
}

@Composable
private fun SpaceLendingRow(entry: LendingListEntry.Space, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        ListItem(
            headlineContent = { Text(entry.spaceName) },
            supportingContent = {
                Text("${entry.lending.checkIn.formatted()} → ${entry.lending.checkOut.formatted()} · ${entry.lending.stage.label()}")
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun LendingsLazyColumn(
    entries: List<LendingListEntry>,
    selectedLendingId: Uuid?,
    toggleCompletedLendings: Boolean,
    onToggleCompletedLendings: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (LendingListEntry) -> Unit,
) {
    val completedLendingsIndicatorRotation by animateFloatAsState(
        targetValue = if (toggleCompletedLendings) 0f else 90f
    )

    LazyColumn(modifier = modifier) {
        val activeLendings = entries.filter { it.isActive }
        if (activeLendings.isNotEmpty()) {
            item("active_lendings") {
                Text(
                    text = stringResource(Res.string.management_active_lendings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(activeLendings, key = { "${it.kind}_${it.id}" }) { entry ->
                LendingRow(entry, isSelected = selectedLendingId == entry.id, onClick = onClick)
            }

            item("active_lendings_divider") {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
        }

        val completedLendings = entries.filterNot { it.isActive }
        if (completedLendings.isNotEmpty()) {
            item("completed_lendings_header") {
                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable { onToggleCompletedLendings(!toggleCompletedLendings) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.management_complete_lendings),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = MaterialSymbols.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.rotate(completedLendingsIndicatorRotation),
                    )
                }
            }
            if (!toggleCompletedLendings) {
                items(completedLendings, key = { "${it.kind}_${it.id}" }) { entry ->
                    LendingRow(entry, isSelected = selectedLendingId == entry.id, onClick = onClick)
                }
            }
        }
    }
}

data class LendingToDraw(
    /**
     * The dates on which to draw this lending.
     */
    val dates: List<LocalDate>,

    /**
     * The lending to draw.
     */
    val entry: CalendarEntry,

    /**
     * The vertical index at which to draw this lending on the given date.
     *
     * This is used for stacking multiple lendings on the same day.
     *
     * Ensures there are no overlaps. Takes into consideration all the other events in the list, so that they are always drawn starting from 0 and increasing.
     */
    val yIndex: Int,
)

internal fun computeLendingsDrawings(entries: List<CalendarEntry>): List<LendingToDraw> {
    val list = mutableListOf<LendingToDraw>()
    for (entry in entries) {
        val dates = entry.dates
        // Find the lowest available yIndex for this lending
        var yIndex = 0
        while (true) {
            val conflict = list.any { existing ->
                existing.yIndex == yIndex && existing.dates.any { it in dates }
            }
            if (conflict) {
                yIndex++
            } else {
                break
            }
        }
        list.add(LendingToDraw(dates = dates, entry = entry, yIndex = yIndex))
    }
    return list.toList()
}

/**
 * A month calendar showing each lending as a bar across the days it covers, like the library's "Example 3": a small day
 * number on the top and a plain, full-width bar per lending below it. A lending keeps the same row on every day, and
 * its bar runs without gaps from its first day to its last.
 */
@Composable
fun LendingsCalendar(
    entries: List<CalendarEntry>,
    tooltip: @Composable (CalendarEntry) -> String,
    modifier: Modifier = Modifier,
    lineHeight: Dp = 5.dp,
    linePadding: Dp = 2.dp,
    onClick: (CalendarEntry) -> Unit,
) {
    val computedLendingsToDraw = remember(entries) { computeLendingsDrawings(entries) }

    val today = remember { LocalDate.now() }
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) } // Adjust as needed
    val endMonth = remember { currentMonth.plusMonths(100) } // Adjust as needed
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() } // Available from the library

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek,
        outDateStyle = OutDateStyle.EndOfGrid,
    )
    val scope = rememberCoroutineScope()
    val visibleMonth = state.firstVisibleMonth.yearMonth

    val maxNumberOfLendingsPerDay = remember(computedLendingsToDraw) {
        computedLendingsToDraw.maxOfOrNull { it.yIndex + 1 } ?: 0
    }

    Column(modifier = modifier) {
        CalendarTitle(
            month = visibleMonth,
            goToPrevious = { scope.launch { state.animateScrollToMonth(visibleMonth.minusMonths(1)) } },
            goToNext = { scope.launch { state.animateScrollToMonth(visibleMonth.plusMonths(1)) } },
        )

        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                Day(
                    day = day,
                    isToday = day.date == today,
                    lendingsToDraw = computedLendingsToDraw.filter { (dates) -> day.date in dates },
                    maxNumberOfLendingsPerDay = maxNumberOfLendingsPerDay,
                    lineHeight = lineHeight,
                    linePadding = linePadding,
                    firstDayOfWeek = firstDayOfWeek,
                    tooltip = tooltip,
                    onClick = onClick,
                )
            },
            monthHeader = {
                MonthHeader(
                    daysOfWeek = remember(firstDayOfWeek) { daysOfWeek(firstDayOfWeek) },
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            },
        )
    }
}

@Composable
private fun CalendarTitle(
    month: YearMonth,
    goToPrevious: () -> Unit,
    goToNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = goToPrevious) {
            Icon(MaterialSymbols.ChevronLeft, contentDescription = stringResource(Res.string.calendar_previous_month))
        }
        Text(
            text = "${month.month.displayName()} ${month.year}",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = goToNext) {
            Icon(MaterialSymbols.ChevronRight, contentDescription = stringResource(Res.string.calendar_next_month))
        }
    }
}

@Composable
private fun Month.displayName(): String = when (this) {
    Month.JANUARY -> stringResource(Res.string.months_january)
    Month.FEBRUARY -> stringResource(Res.string.months_february)
    Month.MARCH -> stringResource(Res.string.months_march)
    Month.APRIL -> stringResource(Res.string.months_april)
    Month.MAY -> stringResource(Res.string.months_may)
    Month.JUNE -> stringResource(Res.string.months_june)
    Month.JULY -> stringResource(Res.string.months_july)
    Month.AUGUST -> stringResource(Res.string.months_august)
    Month.SEPTEMBER -> stringResource(Res.string.months_september)
    Month.OCTOBER -> stringResource(Res.string.months_october)
    Month.NOVEMBER -> stringResource(Res.string.months_november)
    Month.DECEMBER -> stringResource(Res.string.months_december)
}

@Composable
fun MonthHeader(daysOfWeek: List<DayOfWeek>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        for (day in daysOfWeek) {
            val dayRes = when (day) {
                DayOfWeek.MONDAY -> Res.string.days_of_week_short_monday
                DayOfWeek.TUESDAY -> Res.string.days_of_week_short_tuesday
                DayOfWeek.WEDNESDAY -> Res.string.days_of_week_short_wednesday
                DayOfWeek.THURSDAY -> Res.string.days_of_week_short_thursday
                DayOfWeek.FRIDAY -> Res.string.days_of_week_short_friday
                DayOfWeek.SATURDAY -> Res.string.days_of_week_short_saturday
                DayOfWeek.SUNDAY -> Res.string.days_of_week_short_sunday
            }

            Text(
                text = stringResource(dayRes),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    isToday: Boolean,
    /**
     * A filtered list of all the lendings to draw on this day.
     */
    lendingsToDraw: List<LendingToDraw>,
    maxNumberOfLendingsPerDay: Int,
    lineHeight: Dp,
    linePadding: Dp,
    firstDayOfWeek: DayOfWeek,
    tooltip: @Composable (CalendarEntry) -> String,
    onClick: (CalendarEntry) -> Unit,
) {
    val isMonthDate = day.position == DayPosition.MonthDate

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(1.dp)
            .background(if (isMonthDate) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 3.dp, end = 4.dp, bottom = 4.dp)
                .size(20.dp)
                .then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.date.day.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isToday -> MaterialTheme.colorScheme.onPrimary
                    isMonthDate -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
                },
            )
        }

        // The days outside the month belong to the next page: they don't show lendings
        if (isMonthDate) {
            Column(verticalArrangement = Arrangement.spacedBy(linePadding)) {
                for (lane in 0 until maxNumberOfLendingsPerDay) {
                    val toDraw = lendingsToDraw.find { it.yIndex == lane }
                    if (toDraw == null) {
                        Spacer(Modifier.height(lineHeight))
                    } else {
                        LendingBar(day.date, toDraw, lineHeight, firstDayOfWeek, tooltip, onClick)
                    }
                }
            }
        }
    }
}

/**
 * The bar of a lending on one day. It spills over the margin of the day, so that the bars of consecutive days join into
 * one line, and it is only rounded where the lending starts and ends.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LendingBar(
    date: LocalDate,
    toDraw: LendingToDraw,
    lineHeight: Dp,
    firstDayOfWeek: DayOfWeek,
    tooltip: @Composable (CalendarEntry) -> String,
    onClick: (CalendarEntry) -> Unit,
) {
    val entry = toDraw.entry
    val isStart = date == toDraw.dates.first()
    val isEnd = date == toDraw.dates.last()
    // Nothing to join with beyond the first and last day of the week, so the bar stops at the edge of the grid there
    val lastDayOfWeek = DayOfWeek.entries[(firstDayOfWeek.ordinal + 6) % 7]
    val joinsPrevious = !isStart && date.dayOfWeek != firstDayOfWeek
    val joinsNext = !isEnd && date.dayOfWeek != lastDayOfWeek
    val radius = lineHeight / 2

    TooltipBox(
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(tooltip(entry)) } },
    ) {
        Box(
            modifier = Modifier
                .bleed(start = if (joinsPrevious) DAY_MARGIN else 0.dp, end = if (joinsNext) DAY_MARGIN else 0.dp)
                .height(lineHeight)
                .background(
                    color = entry.color,
                    shape = RoundedCornerShape(
                        topStart = if (isStart) radius else 0.dp,
                        bottomStart = if (isStart) radius else 0.dp,
                        topEnd = if (isEnd) radius else 0.dp,
                        bottomEnd = if (isEnd) radius else 0.dp,
                    ),
                )
                .clickable { onClick(entry) },
        )
    }
}

/** The margin around every day cell, which the bars of the lendings spill over. */
private val DAY_MARGIN = 1.dp

/**
 * Makes the content [start] wider on the start side and [end] on the end side, without changing the space it takes.
 */
private fun Modifier.bleed(start: Dp, end: Dp): Modifier = layout { measurable, constraints ->
    val startPx = start.roundToPx()
    val endPx = end.roundToPx()
    val placeable = measurable.measure(Constraints.fixedWidth(constraints.maxWidth + startPx + endPx))
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(
            x = if (layoutDirection == LayoutDirection.Ltr) -startPx else -endPx,
            y = 0,
        )
    }
}

@Composable
private fun lendingTooltip(
    entry: CalendarEntry,
    lendings: List<ReferencedLending>?,
    spaceLendings: List<SpaceLending>?,
    spaceNames: Map<Uuid, String>,
    users: List<UserData>,
): String = when (entry.kind) {
    LendingKind.ITEMS -> lendings?.find { it.id == entry.id }?.let { lending ->
        val state = when (lending.status()) {
            Lending.Status.REQUESTED -> Res.string.lending_details_confirmation_pending_title
            Lending.Status.CONFIRMED -> Res.string.lending_details_pickup_pending_title
            Lending.Status.TAKEN -> Res.string.lending_details_return_pending_title
            Lending.Status.RETURNED -> Res.string.lending_details_memory_pending_title
            else -> Res.string.lending_details_complete
        }
        "${stringResource(state)}\n${lending.user.fullName}\n${lending.from} → ${lending.to}"
    }.orEmpty()

    LendingKind.SPACE -> spaceLendings?.find { it.id == entry.id }?.let { lending ->
        listOfNotNull(
            spaceNames[lending.space],
            lending.stage.label(),
            users.find { it.sub == lending.userSub }?.fullName,
            "${lending.checkIn} → ${lending.checkOut}",
        ).joinToString("\n")
    }.orEmpty()
}

private val previewUser = UserData(
    sub = "preview-user",
    memberNumber = 1234u,
    fullName = "Maria Garcia",
    email = "maria@example.com",
    groups = emptyList(),
    departments = emptyList(),
    lendingUser = null,
    insurances = emptyList(),
    isDisabled = false,
)

private val previewItemType = ReferencedInventoryItemType(
    id = Uuid.parse("3b69c54e-3465-4a1d-a194-29dc64058e4a"),
    displayName = "Climbing rope",
    description = null,
    categories = null,
    weight = null,
    department = null,
    image = null,
)

private val previewItem = ReferencedInventoryItem(
    id = Uuid.parse("8c9ccff9-ec17-4918-9327-08f7de3576d3"),
    variation = null,
    type = previewItemType,
    nfcId = null,
    manufacturerTraceabilityCode = null,
)

private fun previewLending(id: String, from: LocalDate, to: LocalDate, confirmed: Boolean, taken: Boolean = false) = ReferencedLending(
    id = Uuid.parse(id),
    user = previewUser,
    timestamp = Instant.parse("2025-10-01T10:00:00Z"),
    confirmed = confirmed,
    taken = taken,
    givenBy = null,
    givenAt = null,
    returned = false,
    receivedItems = emptyList(),
    memorySubmitted = false,
    memorySubmittedAt = null,
    memory = null,
    memoryReviewed = false,
    from = from,
    to = to,
    notes = null,
    items = listOf(previewItem),
)

private val previewSpaceId = Uuid.parse("5d1e0d3e-3a3b-4c0e-8f6e-1c2b3a4d5e6f")

private fun previewSpaceLending(id: String, checkIn: LocalDate, checkOut: LocalDate, pickedUp: Boolean = false) = SpaceLending(
    id = Uuid.parse(id),
    timestamp = Instant.parse("2025-10-01T10:00:00Z"),
    space = previewSpaceId,
    userSub = previewUser.sub,
    checkIn = checkIn,
    checkOut = checkOut,
    attendees = emptyMap(),
    acceptedConditionsAt = null,
    cancelled = false,
    notes = null,
    pickedUpAt = Instant.parse("2025-10-01T10:00:00Z").takeIf { pickedUp },
    pickedUpBy = null,
    returnedAt = null,
    returnedBy = null,
    requestedKeys = emptyMap(),
    keys = emptyList(),
    totalPrice = 60.0,
    paymentStatus = PaymentStatus.PENDING,
    reportNotes = null,
    reportIssues = null,
    reportSubmittedAt = null,
    reportNotesFiles = emptyList(),
    reportIssuesFiles = emptyList(),
    paymentProofs = emptyList(),
)

@Preview
@Composable
private fun LendingsListView_Preview() {
    val month = LocalDate.now()
    AppTheme {
        LendingsListView(
            snackbarHostState = SnackbarHostState(),
            lendings = listOf(
                previewLending("0d8d6a1e-7a3b-4f7e-9d0e-2f1c4b6a8e01", LocalDate(month.year, month.month, 11), LocalDate(month.year, month.month, 15), confirmed = false),
                previewLending("0d8d6a1e-7a3b-4f7e-9d0e-2f1c4b6a8e02", LocalDate(month.year, month.month, 14), LocalDate(month.year, month.month, 19), confirmed = true),
                previewLending("0d8d6a1e-7a3b-4f7e-9d0e-2f1c4b6a8e03", LocalDate(month.year, month.month, 20), LocalDate(month.year, month.month, 22), confirmed = true, taken = true),
            ),
            spaceLendings = listOf(
                previewSpaceLending("7a1b0d3e-3a3b-4c0e-8f6e-1c2b3a4d5e01", LocalDate(month.year, month.month, 12), LocalDate(month.year, month.month, 14)),
                previewSpaceLending("7a1b0d3e-3a3b-4c0e-8f6e-1c2b3a4d5e02", LocalDate(month.year, month.month, 18), LocalDate(month.year, month.month, 21), pickedUp = true),
            ),
            spaceNames = mapOf(previewSpaceId to "Refugi de Montcabrer"),
            users = listOf(previewUser),
            kindFilter = LendingKindFilter.ALL,
            showKindFilter = true,
            onKindFilterChange = {},
            toggleCompletedLendings = false,
            onToggleCompletedLendings = {},
            onConfirmLendingRequest = { Job() },
            onSkipMemoryRequest = { Job() },
            onGiveRequested = {},
            onReceiveRequested = {},
            onDeleteRequested = { _, _ -> Job() },
            onSpaceLendingClick = {},
        )
    }
}

@Preview
@Composable
private fun LendingsCalendar_Preview() {
    val month = LocalDate.now()
    AppTheme {
        LendingsCalendar(
            entries = listOf(
                previewLending("0d8d6a1e-7a3b-4f7e-9d0e-2f1c4b6a8e11", LocalDate(month.year, month.month, 3), LocalDate(month.year, month.month, 8), confirmed = false).toCalendarEntry(),
                previewLending("0d8d6a1e-7a3b-4f7e-9d0e-2f1c4b6a8e12", LocalDate(month.year, month.month, 6), LocalDate(month.year, month.month, 12), confirmed = true).toCalendarEntry(),
                previewSpaceLending("7a1b0d3e-3a3b-4c0e-8f6e-1c2b3a4d5e11", LocalDate(month.year, month.month, 7), LocalDate(month.year, month.month, 9), pickedUp = true).toCalendarEntry(),
            ),
            tooltip = { it.kind.name },
            onClick = {},
        )
    }
}

private val previewUser = UserData(
    sub = "preview-user",
    memberNumber = 1234u,
    fullName = "Maria Garcia",
    email = "maria@example.com",
    groups = emptyList(),
    departments = emptyList(),
    lendingUser = null,
    insurances = emptyList(),
    isDisabled = false,
)

private val previewItemType = ReferencedInventoryItemType(
    id = Uuid.parse("3b69c54e-3465-4a1d-a194-29dc64058e4a"),
    displayName = "Climbing rope",
    description = null,
    categories = null,
    weight = null,
    department = null,
    image = null,
)

private val previewItem = ReferencedInventoryItem(
    id = Uuid.parse("8c9ccff9-ec17-4918-9327-08f7de3576d3"),
    variation = null,
    type = previewItemType,
    nfcId = null,
    manufacturerTraceabilityCode = null,
)

private fun previewLending(id: String, from: LocalDate, to: LocalDate, confirmed: Boolean, taken: Boolean = false) = ReferencedLending(
    id = Uuid.parse(id),
    user = previewUser,
    timestamp = Instant.parse("2025-10-01T10:00:00Z"),
    confirmed = confirmed,
    taken = taken,
    givenBy = null,
    givenAt = null,
    returned = false,
    receivedItems = emptyList(),
    memorySubmitted = false,
    memorySubmittedAt = null,
    memory = null,
    memoryReviewed = false,
    from = from,
    to = to,
    notes = null,
    items = listOf(previewItem),
)

@Preview
@Composable
private fun LendingsListView_Preview() {
    AppTheme {
        LendingsListView(
            snackbarHostState = SnackbarHostState(),
            lendings = listOf(
                previewLending("0d8d6a1e-7a3b-4f7e-9d0e-2f1c4b6a8e01", LocalDate(2025, 10, 11), LocalDate(2025, 10, 15), confirmed = false),
                previewLending("0d8d6a1e-7a3b-4f7e-9d0e-2f1c4b6a8e02", LocalDate(2025, 10, 14), LocalDate(2025, 10, 19), confirmed = true),
                previewLending("0d8d6a1e-7a3b-4f7e-9d0e-2f1c4b6a8e03", LocalDate(2025, 10, 20), LocalDate(2025, 10, 22), confirmed = true, taken = true),
            ),
            users = listOf(previewUser),
            toggleCompletedLendings = false,
            onToggleCompletedLendings = {},
            onConfirmLendingRequest = { Job() },
            onSkipMemoryRequest = { Job() },
            onGiveRequested = {},
            onReceiveRequested = {},
            onDeleteRequested = { _, _ -> Job() },
        )
    }
}
