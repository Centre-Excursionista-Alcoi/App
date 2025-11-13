package org.centrexcursionistalcoi.app.ui.page.home.management

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import cea_app.composeapp.generated.resources.*
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusDays
import com.kizitonwose.calendar.core.plusMonths
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.ui.page.home.LendingItem
import org.centrexcursionistalcoi.app.ui.reusable.InteractiveCanvas
import org.centrexcursionistalcoi.app.ui.reusable.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.screen.admin.lendingManagementScreenContent
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendingsListView(
    windowSizeClass: WindowSizeClass,
    snackbarHostState: SnackbarHostState,
    lendings: List<ReferencedLending>?,
    users: List<UserData>,
    onConfirmLendingRequest: (ReferencedLending) -> Job,
    onSkipMemoryRequest: (ReferencedLending) -> Job,
    onGiveRequested: (ReferencedLending) -> Unit,
    onReceiveRequested: (ReferencedLending) -> Unit,
) {
    var selectedLending by remember { mutableStateOf<ReferencedLending?>(null) }
    LaunchedEffect(lendings) {
        if (selectedLending != null) {
            // Try to re-select the lending after lendings list update
            selectedLending = lendings?.find { it.id == selectedLending!!.id }
        }
    }

    if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.width(400.dp)) {
                LendingsCalendar(
                    lendings = lendings.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 5.dp,
                    linePadding = 1.dp,
                    onClick = { lendingId ->
                        selectedLending = lendings?.find { it.id == lendingId }
                    },
                )
                LendingsLazyColumn(
                    lendings = lendings,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp),
                    selectedLending = selectedLending,
                    onClick = { selectedLending = it }
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
                                when (lending.status()) {
                                    Lending.Status.CONFIRMED -> {
                                        ElevatedButton(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            onClick = { onGiveRequested(lending) }
                                        ) {
                                            Text(stringResource(Res.string.management_lending_give))
                                        }
                                    }
                                    Lending.Status.RETURNED -> {
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
                        imageVector = Icons.Default.Close,
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
                        when (lending.status()) {
                            Lending.Status.CONFIRMED -> {
                                ElevatedButton(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
                                    onClick = { onGiveRequested(lending) }
                                ) {
                                    Text(stringResource(Res.string.management_lending_give))
                                }
                            }
                            Lending.Status.RETURNED -> {
                                ElevatedButton(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
                                    onClick = { onReceiveRequested(lending) }
                                ) {
                                    Text(stringResource(Res.string.management_lending_receive))
                                }
                            }
                            else -> {
                                // nothing
                            }
                        }
                    },
                )
            }
        } ?: run {
            // no lending is selected
            LendingsLazyColumn(
                lendings = lendings,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                selectedLending = selectedLending,
                onClick = { selectedLending = it }
            )
        }
    }
}

@Composable
private fun LendingsLazyColumn(
    lendings: List<ReferencedLending>?,
    selectedLending: ReferencedLending?,
    modifier: Modifier = Modifier,
    onClick: (ReferencedLending) -> Unit
) {
    LazyColumn(modifier = modifier) {
        val activeLendings = lendings?.filter {
            when (it.status()) {
                Lending.Status.REQUESTED,
                Lending.Status.CONFIRMED,
                Lending.Status.TAKEN,
                Lending.Status.RETURNED -> true
                else -> false
            }
        }?.sortedBy { it.from }
        if (!activeLendings.isNullOrEmpty()) {
            item("active_lendings") {
                Text(
                    text = stringResource(Res.string.management_active_lendings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(activeLendings, key = { it.id }) { lending ->
                LendingItem(lending, isActive = selectedLending?.id == lending.id) { onClick(lending) }
            }

            item("active_lendings_divider") {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
        }

        val completedLendings = lendings?.filter {
            when (it.status()) {
                Lending.Status.MEMORY_SUBMITTED,
                Lending.Status.COMPLETE -> true
                else -> false
            }
        }?.sortedByDescending { it.from }
        if (!completedLendings.isNullOrEmpty()) {
            item("completed_lendings_header") {
                Text(
                    text = stringResource(Res.string.management_complete_lendings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(completedLendings, key = { it.id }) { lending ->
                LendingItem(lending, isActive = selectedLending?.id == lending.id) { onClick(lending) }
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
    val lending: ReferencedLending,

    /**
     * The vertical index at which to draw this lending on the given date.
     *
     * This is used for stacking multiple lendings on the same day.
     *
     * Ensures there are no overlaps. Takes into consideration all the other events in the list, so that they are always drawn starting from 0 and increasing.
     */
    val yIndex: Int,
)

private fun computeLendingsDrawings(lendings: List<ReferencedLending>): List<LendingToDraw> {
    val list = mutableListOf<LendingToDraw>()
    for (lending in lendings) {
        val dates = (0 until lending.durationDays).map { offset -> lending.from.plusDays(offset) }
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
        list.add(LendingToDraw(dates = dates, lending = lending, yIndex = yIndex))
    }
    return list.toList()
}

@Composable
fun LendingsCalendar(
    lendings: List<ReferencedLending>,
    modifier: Modifier = Modifier,
    lineHeight: Dp = 8.dp,
    linePadding: Dp = 2.dp,
    onClick: (lendingId: Uuid) -> Unit,
) {
    val computedLendingsToDraw = remember(lendings) { computeLendingsDrawings(lendings) }

    val today = remember { LocalDate.now() }
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) } // Adjust as needed
    val endMonth = remember { currentMonth.plusMonths(100) } // Adjust as needed
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() } // Available from the library

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    val maxNumberOfLendingsPerDay = remember(computedLendingsToDraw) {
        computedLendingsToDraw.maxOfOrNull { it.yIndex + 1 } ?: 0
    }

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
                onClick = onClick,
            )
        },
        monthHeader = { month ->
            Text(
                text = when(month.yearMonth.month) {
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
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            MonthHeader(
                daysOfWeek = remember(firstDayOfWeek) { month.weekDays.first().map { it.date.dayOfWeek } },
            )
        },
        modifier = modifier,
    )
}

@Composable
fun MonthHeader(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
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
    onClick: ((Uuid) -> Unit)? = null,
) {
    val backgroundColor = if (isToday) MaterialTheme.colorScheme.primary else Color.Unspecified
    val contentColor = if (isToday) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current

    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val computedBounds = remember(day, lendingsToDraw, lineHeight, linePadding, canvasSize) {
        if (canvasSize.isEmpty()) emptyMap()
        else computeBounds(
            day = day,
            lendingsToDraw = lendingsToDraw,
            density = density,
            size = canvasSize,
            lineHeight = lineHeight,
            linePadding = linePadding,
        )
    }

    Column(modifier = Modifier.fillMaxWidth().border(1.dp, LocalContentColor.current)) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .then(
                    if (isToday) {
                        Modifier.background(backgroundColor, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.date.day.toString().padStart(2, '0'),
                color = contentColor.copy(
                    alpha = if (day.position == DayPosition.MonthDate) 1f else .7f
                ),
                fontWeight = if (day.position == DayPosition.MonthDate) FontWeight.Bold else FontWeight.Normal,
            )
        }

        InteractiveCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .height((lineHeight + linePadding) * maxNumberOfLendingsPerDay)
                .onGloballyPositioned { canvasSize = it.size.toSize() },
            onClick = onClick,
            hoverBounds = computedBounds.mapValues { it.value.first },
            tooltipContent = { id ->
                lendingsToDraw.find { it.lending.id == id }?.lending?.let { lending ->
                    val state = when (lending.status()) {
                        Lending.Status.REQUESTED -> Res.string.lending_details_confirmation_pending_title
                        Lending.Status.CONFIRMED -> Res.string.lending_details_pickup_pending_title
                        Lending.Status.TAKEN -> Res.string.lending_details_return_pending_title
                        Lending.Status.RETURNED -> Res.string.lending_details_memory_pending_title
                        else -> Res.string.lending_details_complete
                    }
                    "${stringResource(state)}\n${lending.user.fullName}\n${lending.from} → ${lending.to}"
                }
            },
        ) {
            drawBounds(computedBounds)
        }
    }
}

private val lendingStatusColors = mapOf(
    Lending.Status.REQUESTED to Color.Yellow,
    Lending.Status.CONFIRMED to Color.Blue,
    Lending.Status.TAKEN to Color.Cyan,
    Lending.Status.RETURNED to Color.Magenta,
    Lending.Status.MEMORY_SUBMITTED to Color.Gray,
)

private enum class BoundType {
    START,
    END,
    CONTINUATION,
}

private fun computeBounds(
    day: CalendarDay,
    /**
     * A filtered list of all the lendings to draw on this day.
     */
    lendingsToDraw: List<LendingToDraw>,
    density: Density,
    size: Size,
    lineHeight: Dp,
    linePadding: Dp,
): Map<Uuid, Triple<Rect, Color, BoundType>> = with(density) {
    val lendingsBounds = mutableMapOf<Uuid, Triple<Rect, Color, BoundType>>()

    for ((dates, lending, yIndex) in lendingsToDraw) {
        val isStart = day.date == dates.first()
        val isEnd = day.date == dates.last()

        if (isStart) {
            val y = (lineHeight + linePadding).toPx() * yIndex + lineHeight.toPx() / 2

            // Store the bounding box of the drawn lending line
            lendingsBounds[lending.id] = Triple(
                Rect(
                    left = size.width / 2 - lineHeight.toPx() / 2,
                    top = y - lineHeight.toPx() / 2,
                    right = size.width,
                    bottom = y + lineHeight.toPx() / 2,
                ),
                lendingStatusColors.getValue(lending.status()),
                BoundType.START,
            )
        } else if (isEnd) {
            val y = (lineHeight + linePadding).toPx() * yIndex + lineHeight.toPx() / 2

            // Store the bounding box of the drawn lending line
            lendingsBounds[lending.id] = Triple(
                Rect(
                    left = 0f,
                    top = y - lineHeight.toPx() / 2,
                    right = size.width / 2 + lineHeight.toPx() / 2,
                    bottom = y + lineHeight.toPx() / 2,
                ),
                lendingStatusColors.getValue(lending.status()),
                BoundType.END,
            )
        } else { // is continuation
            val y = (lineHeight + linePadding).toPx() * yIndex + lineHeight.toPx() / 2

            // Store the bounding box of the drawn lending line
            lendingsBounds[lending.id] = Triple(
                Rect(
                    left = 0f,
                    top = y - lineHeight.toPx() / 2,
                    right = size.width,
                    bottom = y + lineHeight.toPx() / 2,
                ),
                lendingStatusColors.getValue(lending.status()),
                BoundType.CONTINUATION,
            )
        }
    }

    lendingsBounds.toMap()
}

private fun DrawScope.drawBounds(
    bounds: Map<Uuid, Triple<Rect, Color, BoundType>>
) {
    for ((_, triple) in bounds) {
        val (rect, color, boundType) = triple
        when (boundType) {
            BoundType.START -> {
                // Draw line from middle to end
                drawLine(
                    color = color,
                    strokeWidth = rect.height,
                    start = Offset(rect.left + rect.height / 2, rect.center.y),
                    end = Offset(rect.right, rect.center.y),
                )
                // Draw circle to simulate rounded line start
                drawCircle(
                    color = color,
                    radius = rect.height / 2,
                    center = Offset(rect.left + rect.height / 2, rect.center.y)
                )
            }
            BoundType.END -> {
                // Draw line from start to middle
                drawLine(
                    color = color,
                    strokeWidth = rect.height,
                    start = Offset(rect.left, rect.center.y),
                    end = Offset(rect.right - rect.height / 2, rect.center.y),
                )
                // Draw circle to simulate rounded line end
                drawCircle(
                    color = color,
                    radius = rect.height / 2,
                    center = Offset(rect.right - rect.height / 2, rect.center.y)
                )
            }
            BoundType.CONTINUATION -> {
                // Draw full line
                drawLine(
                    color = color,
                    strokeWidth = rect.height,
                    start = Offset(rect.left, rect.center.y),
                    end = Offset(rect.right, rect.center.y),
                )
            }
        }
    }
}
