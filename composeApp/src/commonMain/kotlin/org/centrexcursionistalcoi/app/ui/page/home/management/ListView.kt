package org.centrexcursionistalcoi.app.ui.page.home.management

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.platform.PlatformBackHandler
import org.centrexcursionistalcoi.app.ui.reusable.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.reusable.buttons.DropdownIconButton
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

class SortBy<T>(
    val label: @Composable () -> String,
    val sorted: (Iterable<T>) -> List<T>,
) {
    companion object {
        fun <T> nameAsc(name: (T) -> String): SortBy<T> = SortBy(
            label = { stringResource(Res.string.sort_by_name_asc) },
            sorted = { items -> items.sortedBy { name(it) } }
        )

        fun <T> nameDesc(name: (T) -> String): SortBy<T> = SortBy(
            label = { stringResource(Res.string.sort_by_name_desc) },
            sorted = { items -> items.sortedByDescending { name(it) } }
        )

        fun <T> defaults(name: (T) -> String): List<SortBy<T>> = listOf(
            nameAsc(name),
            nameDesc(name),
        )
    }
}

class Filter<T>(
    val label: @Composable () -> String,
    val predicate: (T) -> Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ListView(
    windowSizeClass: WindowSizeClass,
    items: List<T>?,
    selectedItemIndex: Int? = null,
    itemDisplayName: (T) -> String,
    emptyItemsText: String,
    itemIdProvider: (T) -> Any,
    isCreatingSupported: Boolean = true,
    createTitle: String = stringResource(Res.string.create),
    /**
     * Map of filters to apply to the items.
     * The key is the filter name, and the value is a pair of a composable that returns the filter label and a predicate that returns true if the item should be included.
     */
    filters: Map<String, Filter<T>> = emptyMap(),
    /**
     * List of sorting options for the items.
     *
     * Cannot be empty.
     */
    sortByOptions: List<SortBy<T>> = SortBy.defaults(itemDisplayName),
    itemTextStyle: (@Composable (T) -> TextStyle)? = null,
    itemLeadingContent: (@Composable (T) -> Unit)? = null,
    itemTrailingContent: (@Composable RowScope.(T) -> Unit)? = null,
    itemSupportingContent: (@Composable (T) -> Unit)? = null,
    itemToolbarActions: (@Composable RowScope.(T) -> Unit)? = null,
    editItemContent: (@Composable EditorContext.(T?) -> Unit)? = null,
    onDeleteRequest: ((T) -> Job)? = null,
    itemContent: @Composable ColumnScope.(T) -> Unit,
) {
    var selectedItem by remember { mutableStateOf(selectedItemIndex?.let { items?.getOrNull(it) }) }
    var isEditing by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    // Keep selected item in sync with items list
    LaunchedEffect(items) {
        val item = selectedItem
        if (item != null) {
            items?.find { itemIdProvider(it) == itemIdProvider(item) }?.let { selectedItem = it }
        }
    }

    var isDeleting by remember { mutableStateOf<T?>(null) }
    if (onDeleteRequest != null) isDeleting?.let { item ->
        DeleteDialog(
            item = item,
            displayName = itemDisplayName,
            onDelete = { onDeleteRequest(item).also { job -> job.invokeOnCompletion { selectedItem = null } } },
            onDismissRequested = { isDeleting = null }
        )
    }

    @Composable
    fun ColumnScope.actualItemContent() {
        val item = selectedItem

        val editingContext = remember(isEditing, editItemContent) {
            if (isEditing && editItemContent != null) {
                object : EditorContext(this) {
                    override fun finishEdit() {
                        isEditing = false
                    }
                }
            } else {
                null
            }
        }
        val creatingContext = remember(isCreating, editItemContent) {
            if (isCreating && editItemContent != null) {
                object : EditorContext(this) {
                    override fun finishEdit() {
                        isCreating = false
                    }
                }
            } else {
                null
            }
        }

        when {
            editingContext != null -> editItemContent!!(editingContext, item)
            creatingContext != null -> editItemContent!!(creatingContext, null)
            item != null -> itemContent(item)
        }
    }

    if (windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium) {
        val contentWeight = if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) 2f else 1f
        Row(modifier = Modifier.fillMaxSize()) {
            ListView_ListColumn(
                modifier = Modifier.fillMaxHeight().weight(1f),
                items = items,
                emptyItemsText = emptyItemsText,
                itemDisplayName = itemDisplayName,
                itemIdProvider = itemIdProvider,
                itemLeadingContent = itemLeadingContent,
                itemTrailingContent = itemTrailingContent,
                itemSupportingContent = itemSupportingContent,
                itemTextStyle = itemTextStyle,
                selectedItem = selectedItem,
                filters = filters,
                sortByOptions = sortByOptions,
                onSelectedItemChange = { selectedItem = it },
                isCreatingSupported = isCreatingSupported,
                onCreateRequested = { isCreating = true },
            )

            Spacer(Modifier.width(8.dp))

            if (selectedItem != null || isCreating) {
                ListView_Content(
                    itemDisplayName = selectedItem?.let(itemDisplayName) ?: createTitle,
                    itemToolbarActions = selectedItem?.let { item -> { itemToolbarActions?.invoke(this, item) } },
                    onCloseRequested = {
                        selectedItem = null
                        isEditing = false
                        isCreating = false
                    },
                    itemContent = { actualItemContent() },
                    isEditSupported = editItemContent != null,
                    isEditing = isEditing || isCreating,
                    onEditRequest = { isEditing = true },
                    onDeleteRequest = selectedItem?.let { item ->
                        if (onDeleteRequest != null) {
                            { isDeleting = item }
                        } else null
                    },
                    modifier = Modifier.fillMaxHeight().weight(contentWeight),
                    shape = RoundedCornerShape(12.dp, 0.dp, 0.dp, 12.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    } else {
        if (selectedItem != null || isCreating) {
            ListView_Content(
                itemDisplayName = selectedItem?.let(itemDisplayName) ?: createTitle,
                itemToolbarActions = selectedItem?.let { item -> { itemToolbarActions?.invoke(this, item) } },
                onCloseRequested = {
                    selectedItem = null
                    isEditing = false
                    isCreating = false
                },
                itemContent = { actualItemContent() },
                isEditSupported = editItemContent != null,
                isEditing = isEditing || isCreating,
                onEditRequest = { isEditing = true },
                onDeleteRequest = selectedItem?.let { item ->
                    if (onDeleteRequest != null) {
                        { isDeleting = item }
                    } else null
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ListView_ListColumn(
                modifier = Modifier.fillMaxSize(),
                items = items,
                emptyItemsText = emptyItemsText,
                itemDisplayName = itemDisplayName,
                itemIdProvider = itemIdProvider,
                itemLeadingContent = itemLeadingContent,
                itemTrailingContent = itemTrailingContent,
                itemSupportingContent = itemSupportingContent,
                itemTextStyle = itemTextStyle,
                selectedItem = selectedItem,
                filters = filters,
                sortByOptions = sortByOptions,
                onSelectedItemChange = { selectedItem = it },
                isCreatingSupported = isCreatingSupported,
                onCreateRequested = { isCreating = true },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun <T> ListView_ListColumn(
    items: List<T>?,
    emptyItemsText: String,
    selectedItemIndex: Int? = null,
    itemDisplayName: (T) -> String,
    itemIdProvider: (T) -> Any,
    itemLeadingContent: (@Composable (T) -> Unit)? = null,
    itemTrailingContent: (@Composable RowScope.(T) -> Unit)? = null,
    itemSupportingContent: (@Composable (T) -> Unit)? = null,
    itemTextStyle: (@Composable (T) -> TextStyle)? = null,
    selectedItem: T?,
    onSelectedItemChange: (T) -> Unit,
    isCreatingSupported: Boolean,
    onCreateRequested: (() -> Unit)? = null,
    /**
     * Map of filters to apply to the items.
     * The key is the filter name, and the value is a pair of a composable that returns the filter label and a predicate that returns true if the item should be included.
     */
    filters: Map<String, Filter<T>> = emptyMap(),
    /**
     * List of sorting options for the items.
     *
     * Cannot be empty.
     */
    sortByOptions: List<SortBy<T>> = SortBy.defaults(itemDisplayName),
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(sortByOptions.first()) }
    var activeFilters by remember { mutableStateOf<List<String>>(emptyList()) }
    val filteredItems = remember(items, filters, activeFilters) {
        if (filters.isEmpty() || activeFilters.isEmpty()) items
        else items?.filter { item ->
            activeFilters.all { key ->
                filters.getValue(key).predicate(item)
            }
        }
    }
    val searchedItems = remember(filteredItems, search) {
        filteredItems.orEmpty().filter { itemDisplayName(it).contains(search, ignoreCase = true) }
    }
    val filteredAndSortedItems = remember(searchedItems, sortBy) {
        sortBy.sorted(searchedItems)
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text(stringResource(Res.string.search)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row {
                    AnimatedVisibility(search.isNotEmpty()) {
                        TooltipIconButton(
                            imageVector = Icons.Default.Close,
                            tooltip = stringResource(Res.string.clear),
                            positioning = TooltipAnchorPosition.Left,
                            onClick = { search = "" }
                        )
                    }
                    if (isCreatingSupported && onCreateRequested != null) {
                        TooltipIconButton(
                            imageVector = Icons.Default.Add,
                            tooltip = stringResource(Res.string.create),
                            positioning = TooltipAnchorPosition.Left,
                            onClick = onCreateRequested,
                        )
                    }
                    if (filters.isNotEmpty()) {
                        DropdownIconButton(
                            imageVector = if (activeFilters.isEmpty()) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            items = filters.keys,
                            selectedItems = activeFilters,
                            onItemClicked = { filterKey ->
                                if (activeFilters.contains(filterKey)) {
                                    activeFilters -= filterKey
                                } else {
                                    activeFilters += filterKey
                                }
                            },
                            toString = { filterKey ->
                                filters.getValue(filterKey).label()
                            },
                        )
                    }
                    DropdownIconButton(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        items = sortByOptions,
                        selection = sortBy,
                        toString = { it.label() },
                        onItemSelected = { sortBy = it },
                    )
                }
            }
        )

        val scrollState = rememberLazyListState()
        LaunchedEffect(selectedItemIndex) {
            if (selectedItemIndex != null) {
                scrollState.animateScrollToItem(selectedItemIndex)
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            if (filteredAndSortedItems.isEmpty()) {
                item("empty") {
                    Text(
                        text = emptyItemsText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(filteredAndSortedItems, key = { itemIdProvider(it) }) { item ->
                val isSelected = item == selectedItem
                val containerColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified)

                ListItem(
                    headlineContent = {
                        Text(
                            text = itemDisplayName(item),
                            style = itemTextStyle?.invoke(item) ?: LocalTextStyle.current,
                        )
                    },
                    supportingContent = itemSupportingContent?.let {
                        { it(item) }
                    },
                    leadingContent = { itemLeadingContent?.invoke(item) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            itemTrailingContent?.invoke(this, item)
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSelectedItemChange(item) },
                    colors = ListItemDefaults.colors(
                        containerColor = containerColor,
                        headlineColor = contentColor,
                    )
                )
            }
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            val originalItemsListSize = items?.size ?: 0
            val text = if (filteredAndSortedItems.size != originalItemsListSize) {
                // items are filtered by search
                pluralStringResource(Res.plurals.management_items_amount, filteredAndSortedItems.size, filteredAndSortedItems.size) +
                        " (${pluralStringResource(Res.plurals.management_items_amount, originalItemsListSize, originalItemsListSize)})"
            } else {
                pluralStringResource(Res.plurals.management_items_amount, originalItemsListSize, originalItemsListSize)
            }

            Text(
                text = text,
            )
        }
    }
}

abstract class EditorContext(columnScope: ColumnScope) : ColumnScope by columnScope {
    abstract fun finishEdit()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListView_Content(
    itemDisplayName: String,
    itemToolbarActions: (@Composable RowScope.() -> Unit)? = null,
    onCloseRequested: () -> Unit,
    itemContent: @Composable ColumnScope.() -> Unit,
    isEditSupported: Boolean,
    isEditing: Boolean,
    onEditRequest: () -> Unit,
    onDeleteRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    PlatformBackHandler {
        onCloseRequested()
    }

    Surface(
        // Rounded corners on the left side
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCloseRequested,
                ) {
                    Icon(Icons.Default.Close, stringResource(Res.string.close))
                }
                Text(
                    text = itemDisplayName,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.weight(1f))
                if (isEditSupported) {
                    if (!isEditing) {
                        TooltipIconButton(
                            imageVector = Icons.Default.Edit,
                            tooltip = stringResource(Res.string.edit),
                            positioning = TooltipAnchorPosition.Left,
                            onClick = onEditRequest,
                        )
                    }
                }
                if (onDeleteRequest != null) {
                    TooltipIconButton(
                        imageVector = Icons.Default.Delete,
                        tooltip = stringResource(Res.string.delete),
                        positioning = TooltipAnchorPosition.Left,
                        onClick = onDeleteRequest,
                    )
                }
                if ((!isEditSupported || !isEditing) && itemToolbarActions != null) {
                    itemToolbarActions(this)
                }
            }

            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemContent()
            }
        }
    }
}
