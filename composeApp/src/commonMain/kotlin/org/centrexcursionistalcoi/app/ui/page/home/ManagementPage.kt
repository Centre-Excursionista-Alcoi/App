package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusDays
import com.kizitonwose.calendar.core.plusMonths
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.ui.data.IconAction
import org.centrexcursionistalcoi.app.ui.dialog.CreateInventoryItemTypeDialog
import org.centrexcursionistalcoi.app.ui.dialog.LendingDetailsDialog
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.ListCard
import org.centrexcursionistalcoi.app.ui.utils.modIf
import org.jetbrains.compose.resources.stringResource

@Composable
fun ManagementPage(
    windowSizeClass: WindowSizeClass,

    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?) -> Job,
    onDeleteDepartment: (Department) -> Job,

    users: List<UserData>?,
    onPromote: (UserData) -> Job,

    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onCreateInventoryItemType: (displayName: String, description: String, category: String, image: PlatformFile?) -> Job,
    onClickInventoryItemType: (InventoryItemType) -> Unit,

    inventoryItems: List<ReferencedInventoryItem>?,

    lendings: List<ReferencedLending>,
    onManageLendingsRequested: () -> Unit,
) {
    if (windowSizeClass.widthSizeClass <= WindowWidthSizeClass.Medium) {
        ManagementPage_Small(
            windowSizeClass,
            departments,
            onCreateDepartment,
            onDeleteDepartment,
            users,
            onPromote,
            inventoryItemTypes,
            inventoryItemTypesCategories,
            onCreateInventoryItemType,
            onClickInventoryItemType,
            inventoryItems,
            onManageLendingsRequested,
        )
    } else {
        ManagementPage_Large(
            lendings,
        )
    }
}

@Composable
fun ManagementPage_Small(
    windowSizeClass: WindowSizeClass,

    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?) -> Job,
    onDeleteDepartment: (Department) -> Job,

    users: List<UserData>?,
    onPromote: (UserData) -> Job,

    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onCreateInventoryItemType: (displayName: String, description: String, category: String, image: PlatformFile?) -> Job,
    onClickInventoryItemType: (InventoryItemType) -> Unit,

    inventoryItems: List<ReferencedInventoryItem>?,

    onManageLendingsRequested: () -> Unit,
) {
    AdaptiveVerticalGrid(
        windowSizeClass,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        item(key = "lendings") {
            Button(
                onClick = onManageLendingsRequested
            ) { Text(stringResource(Res.string.management_lendings)) }
        }
        item(key = "departments") {
            DepartmentsCard(departments, onCreateDepartment, onDeleteDepartment)
        }
        item(key = "users") {
            UsersCard(users, onPromote)
        }
        item(key = "items") {
            InventoryItemTypesCard(
                inventoryItemTypes,
                inventoryItemTypesCategories,
                inventoryItems,
                onCreateInventoryItemType,
                onClickInventoryItemType,
            )
        }
    }
}


@Composable
fun ManagementPage_Large(
    lendings: List<ReferencedLending>
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(300.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        item(key = "top-spacer", contentType = "spacer") { Spacer(Modifier.height(12.dp)) }

        item(key = "lendings") {
            LendingsCalendar(lendings)
        }
    }
}

@Composable
fun DepartmentsCard(
    departments: List<Department>?,
    onCreate: (displayName: String, image: PlatformFile?) -> Job,
    onDelete: (Department) -> Job,
) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateDepartmentDialog(onCreate) { creating = false }
    }

    ListCard(
        list = departments,
        titleResource = Res.string.management_departments,
        emptyTextResource = Res.string.management_no_departments,
        displayName = { it.displayName },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onCreate = { creating = true },
        onDelete = onDelete
    )
}

@Composable
fun InventoryItemTypesCard(
    types: List<InventoryItemType>?,
    categories: Set<String>,
    items: List<ReferencedInventoryItem>?,
    onCreate: (displayName: String, description: String, category: String, image: PlatformFile?) -> Job,
    onClick: (InventoryItemType) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateInventoryItemTypeDialog(categories, onCreate) { creating = false }
    }

    val groupedItems = remember(items, types) {
        items.orEmpty().groupBy { it.type }.toList()
    }
    val typesWithoutItems = remember(items, types) {
        types.orEmpty().filter { type ->
            items?.none { it.type.id == type.id } ?: true
        }.map { type -> type to emptyList<ReferencedInventoryItem>() }
    }
    ListCard(
        list = groupedItems + typesWithoutItems,
        titleResource = Res.string.management_inventory_item_types,
        emptyTextResource = Res.string.management_no_item_types,
        displayName = { (type) -> type.displayName },
        trailingContent = { (_, items) -> Badge { Text(items.size.toString()) } },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onCreate = { creating = true },
        onClick = { (type) -> onClick(type) },
        sharedContentStateKey = { (type) -> "iit_${type.id}" },
        fileContainerProvider = { (type) -> type },
    )
}

@Composable
fun UsersCard(users: List<UserData>?, onPromote: (UserData) -> Job) {
    var promotingUser by remember { mutableStateOf<UserData?>(null) }
    promotingUser?.let { user ->
        var isPromoting by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isPromoting) promotingUser = null },
            title = { Text(stringResource(Res.string.management_promote_user_title)) },
            text = { Text(stringResource(Res.string.management_promote_user_confirmation, user.username)) },
            confirmButton = {
                TextButton(
                    enabled = !isPromoting,
                    onClick = {
                        isPromoting = true
                        onPromote(user).invokeOnCompletion {
                            isPromoting = false
                            promotingUser = null
                        }
                    }
                ) { Text(stringResource(Res.string.management_promote_user)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !isPromoting,
                    onClick = { if (!isPromoting) promotingUser = null }
                ) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }

    ListCard(
        list = users,
        titleResource = Res.string.management_users,
        emptyTextResource = Res.string.management_no_departments,
        displayName = { it.username },
        trailingContent = { if (it.isAdmin()) Badge { Text(stringResource(Res.string.admin)) } },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        actions = { user ->
            listOfNotNull(
                IconAction(
                    icon = Icons.Default.AddModerator,
                    onClick = { promotingUser = user },
                    contentDescription = stringResource(Res.string.management_promote_user),
                ).takeUnless { user.isAdmin() }
            )
        }
    )
}

@Composable
fun CreateDepartmentDialog(
    onCreate: (displayName: String, image: PlatformFile?) -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<PlatformFile?>(null) }
    val imagePicker = rememberFilePickerLauncher(
        type = FileKitType.Image
    ) { file -> image = file }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text("Create department") },
        text = {
            Column {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !isLoading,
                    onClick = { imagePicker.launch() }
                ) {
                    image?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                    } ?: Text(
                        "Select image (optional)",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && displayName.isNotBlank(),
                onClick = {
                    isLoading = true
                    onCreate(displayName, image).invokeOnCompletion { onDismissRequested() }
                }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text("Cancel") }
        },
    )
}

@Composable
fun LendingsCalendar(lendings: List<ReferencedLending>) {
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

    var displayingLending by remember { mutableStateOf<Uuid?>(null) }
    displayingLending?.let { lendingId ->
        val lending = lendings.find { it.id == lendingId }
        if (lending != null) {
            // TODO: Implement the callbacks
            LendingDetailsDialog(
                lending = lending,
                onCancelRequest = {},
                memoryUploadProgress = null,
                onMemorySubmitted = null,
                onMemoryEditorRequested = null,
                onDismissRequest = { displayingLending = null }
            )
        } else {
            // If the lending is not found, just dismiss the dialog
            displayingLending = null
        }
    }

    HorizontalCalendar(
        state = state,
        dayContent = { day ->
            val filteredLendings = remember(lendings, day) {
                lendings.filter { lending ->
                    // Lending is active on this day
                    (lending.from <= day.date) && (lending.to >= day.date)
                }
            }
            Day(day = day, isToday = day.date == today, filteredLendings) { lendingId ->
                displayingLending = lendingId
            }
        },
        monthHeader = { month ->
            // Clear the drawing stack at the start of a new month
            drawingLendings.clear()

            MonthHeader(
                daysOfWeek = remember(firstDayOfWeek) { month.weekDays.first().map { it.date.dayOfWeek } },
            )
        },
    )
}

@Composable
fun MonthHeader(daysOfWeek: List<DayOfWeek>) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
}

@Composable
fun Day(
    day: CalendarDay,
    isToday: Boolean,
    lendings: List<ReferencedLending>,
    lineHeight: Dp = 8.dp,
    linePadding: Dp = 2.dp,
    onClick: ((Uuid) -> Unit)? = null,
) {
    val backgroundColor = if (isToday) MaterialTheme.colorScheme.primary else Color.Unspecified
    val contentColor = if (isToday) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .modIf(isToday) {
                    background(backgroundColor, CircleShape)
                },
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

        val maxNumberOfLendingsPerDay = remember(lendings) {
            lendings.groupBy { lending ->
                // For each lending, get all the days it spans
                (0 until lending.durationDays).map { offset -> lending.from.plusDays(offset) }
            }.values.maxOfOrNull { it.size } ?: 0
        }

        var bounds by remember { mutableStateOf<Map<Uuid, Rect>>(emptyMap()) }
        fun setBounds(newBounds: Map<Uuid, Rect>) {
            bounds = newBounds
        }

        Canvas(
            modifier = Modifier.fillMaxWidth().height((lineHeight + linePadding) * maxNumberOfLendingsPerDay)
                .pointerInput(Unit) {
                    detectTapGestures { position ->
                        // Check if the tap position intersects with any lending bounds
                        for ((lendingId, rect) in bounds) {
                            if (rect.contains(Offset(position.x, position.y))) {
                                onClick?.invoke(lendingId)
                                break
                            }
                        }
                    }
                }
        ) {
            val bounds = drawLendings(day, lendings, lineHeight, linePadding)
            setBounds(bounds)
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

// A stack that stores the currently drawn lendings to avoid drawing ones over each other
private val drawingLendings = mutableMapOf<Uuid, Int>()

private fun DrawScope.drawLendings(
    day: CalendarDay,
    lendings: List<ReferencedLending>,
    lineHeight: Dp = 8.dp,
    linePadding: Dp = 2.dp,
): Map<Uuid, Rect> {
    val singleDayLendings = lendings.filter { it.durationDays == 1 && it.from == day.date}
    // All the lendings that start on this day
    val startingLendings = lendings.filter { it.from == day.date && it !in singleDayLendings }
    // All the lendings that contain this day, but don't start or end on it
    val containedLendings = lendings.filter { it.from < day.date && it.to > day.date && it !in singleDayLendings }
    // All the lendings that end on this day
    val endingLendings = lendings.filter { it.to == day.date && it !in singleDayLendings }

    // If there is nothing to draw, skip
    if (startingLendings.isEmpty() && containedLendings.isEmpty() && endingLendings.isEmpty()) {
        return emptyMap()
    }

    val lendingsBounds = mutableMapOf<Uuid, Rect>()

    for (lending in startingLendings) {
        val index = if (drawingLendings.isEmpty()) { 0 } else { drawingLendings.maxOf { it.value } + 1 }
        Napier.d { "Drawing ${lending.id} start at index $index" }
        drawingLendings += lending.id to index
        Napier.d { "  Added to stack: $drawingLendings" }
        val y = (lineHeight + linePadding).toPx() * index + lineHeight.toPx() / 2
        // Start line from the middle
        drawLine(
            color = lendingStatusColors.getValue(lending.status()),
            strokeWidth = lineHeight.toPx(),
            start = Offset(size.width / 2, y),
            end = Offset(size.width, y),
        )
        // Draw circle to simulate rounded line start
        drawCircle(
            color = lendingStatusColors.getValue(lending.status()),
            radius = lineHeight.toPx() / 2,
            center = Offset(size.width / 2, y)
        )

        // Store the bounding box of the drawn lending line
        lendingsBounds[lending.id] = Rect(
            left = size.width / 2 - lineHeight.toPx() / 2,
            top = y - lineHeight.toPx() / 2,
            right = size.width,
            bottom = y + lineHeight.toPx() / 2,
        )
    }
    for (lending in containedLendings) {
        val index = drawingLendings[lending.id]
        if (index == null) {
            Napier.e { "Lending ${lending.id} (continuation) not found in drawingLendings: $drawingLendings" }
            continue
        }
        Napier.d { "Drawing ${lending.id} continuation at index $index" }
        val y = (lineHeight + linePadding).toPx() * index + lineHeight.toPx() / 2
        // Full line
        drawLine(
            color = lendingStatusColors.getValue(lending.status()),
            strokeWidth = lineHeight.toPx(),
            start = Offset(0f, y),
            end = Offset(size.width, y),
        )

        // Store the bounding box of the drawn lending line
        lendingsBounds[lending.id] = Rect(
            left = 0f,
            top = y - lineHeight.toPx() / 2,
            right = size.width,
            bottom = y + lineHeight.toPx() / 2,
        )
    }
    for (lending in endingLendings) {
        val index = drawingLendings[lending.id]
        if (index == null) {
            Napier.e { "Lending ${lending.id} (ending) not found in drawingLendings: $drawingLendings" }
            continue
        }
        Napier.d { "Drawing ${lending.id} end at index $index" }
        drawingLendings -= lending.id
        Napier.d { "  Removed from stack: $drawingLendings" }
        val y = (lineHeight + linePadding).toPx() * index + lineHeight.toPx() / 2
        // End line to the middle
        drawLine(
            color = lendingStatusColors.getValue(lending.status()),
            strokeWidth = lineHeight.toPx(),
            start = Offset(0f, y),
            end = Offset(size.width / 2, y),
        )
        // Draw circle to simulate rounded line end
        drawCircle(
            color = lendingStatusColors.getValue(lending.status()),
            radius = lineHeight.toPx() / 2,
            center = Offset(size.width / 2, y)
        )

        // Store the bounding box of the drawn lending line
        lendingsBounds[lending.id] = Rect(
            left = 0f,
            top = y - lineHeight.toPx() / 2,
            right = size.width / 2 + lineHeight.toPx() / 2,
            bottom = y + lineHeight.toPx() / 2,
        )
    }

    return lendingsBounds
}
