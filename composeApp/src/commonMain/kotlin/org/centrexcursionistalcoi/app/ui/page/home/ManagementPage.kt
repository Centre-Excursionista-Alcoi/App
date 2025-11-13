package org.centrexcursionistalcoi.app.ui.page.home

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.dialog.QRCodeDialog
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.reusable.buttons.DropdownIconButton
import org.centrexcursionistalcoi.app.ui.reusable.form.AutocompleteMultipleFormField
import org.centrexcursionistalcoi.app.ui.reusable.form.FormImagePicker
import org.centrexcursionistalcoi.app.ui.reusable.form.ReadOnlyFormField
import org.centrexcursionistalcoi.app.viewmodel.ManagementViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ManagementPage(
    windowSizeClass: WindowSizeClass,

    departments: List<Department>?,

    users: List<UserData>?,

    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onClickInventoryItemType: (InventoryItemType) -> Unit,

    inventoryItems: List<ReferencedInventoryItem>?,

    onManageLendingsRequested: () -> Unit,

    model: ManagementViewModel = viewModel { ManagementViewModel() },
) {
    ManagementPage(
        windowSizeClass = windowSizeClass,
        departments = departments,
        onCreateDepartment = model::createDepartment,
        onUpdateDepartment = model::updateDepartment,
        onDeleteDepartment = model::delete,
        users = users,
        onPromote = model::promote,
        inventoryItemTypes = inventoryItemTypes,
        inventoryItemTypesCategories = inventoryItemTypesCategories,
        onCreateInventoryItemType = model::createInventoryItemType,
        onUpdateInventoryItemType = model::updateInventoryItemType,
        onDeleteInventoryItem = model::delete,
        inventoryItems = inventoryItems,
    )
}

@Composable
private fun ManagementPage(
    windowSizeClass: WindowSizeClass,

    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?, progressNotifier: ProgressNotifier?) -> Job,
    onUpdateDepartment: (id: Int, displayName: String, image: PlatformFile?, progressNotifier: ProgressNotifier?) -> Job,
    onDeleteDepartment: (Department) -> Job,

    users: List<UserData>?,
    onPromote: (UserData) -> Job,

    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onCreateInventoryItemType: (displayName: String, description: String, categories: List<String>, image: PlatformFile?) -> Job,
    onUpdateInventoryItemType: (id: Uuid, displayName: String, description: String, categories: List<String>, image: PlatformFile?) -> Job,
    onDeleteInventoryItem: (ReferencedInventoryItem) -> Job,

    inventoryItems: List<ReferencedInventoryItem>?,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 3 }

    PrimaryTabRow(pagerState.currentPage) {
        listOf(
            Res.string.management_departments,
            Res.string.management_users,
            Res.string.management_inventory,
        ).forEachIndexed { index, titleRes ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                text = { Text(stringResource(titleRes)) }
            )
        }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        when (page) {
            0 -> DepartmentsListView(windowSizeClass, departments, onCreateDepartment, onUpdateDepartment, onDeleteDepartment)
            1 -> UsersListView(windowSizeClass, users, onPromote)
            2 -> InventoryItemTypesListView(
                windowSizeClass,
                inventoryItemTypes,
                inventoryItemTypesCategories,
                inventoryItems,
                onCreateInventoryItemType,
                onUpdateInventoryItemType,
                onDeleteInventoryItem,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentsListView(
    windowSizeClass: WindowSizeClass,
    departments: List<Department>?,
    onCreate: (displayName: String, image: PlatformFile?, progressNotifier: ProgressNotifier?) -> Job,
    onUpdate: (id: Int, displayName: String, image: PlatformFile?, progressNotifier: ProgressNotifier?) -> Job,
    onDelete: (Department) -> Job,
) {
    var deleting by remember { mutableStateOf<Department?>(null) }
    deleting?.let { department ->
        DeleteDialog(
            item = department,
            displayName = { it.displayName },
            onDelete = { onDelete(department) },
            onDismissRequested = { deleting = null }
        )
    }

    ListView(
        windowSizeClass = windowSizeClass,
        items = departments,
        itemIdProvider = { it.id },
        itemDisplayName = { it.displayName },
        itemLeadingContent = { department ->
            department.image ?: return@ListView
            val image by department.rememberImageFile()
            AsyncByteImage(
                bytes = image,
                contentDescription = department.displayName,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
            )
        },
        emptyItemsText = stringResource(Res.string.management_no_departments),
        itemToolbarActions = {
            TooltipIconButton(
                imageVector = Icons.Default.Delete,
                tooltip = stringResource(Res.string.delete),
                onClick = { deleting = it }
            )
        },
        editItemContent = { department: Department? ->
            var isLoading by remember { mutableStateOf(false) }
            var progress by remember { mutableStateOf<Progress?>(null) }
            var displayName by remember { mutableStateOf(department?.displayName ?: "") }
            var image by remember { mutableStateOf<PlatformFile?>(null) }

            val isDirty = if (department == null) true else displayName != department.displayName || image != null

            FormImagePicker(
                image = image,
                container = department,
                onImagePicked = { image = it },
                isLoading = isLoading,
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp)),
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(Res.string.form_display_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
            )

            Spacer(Modifier.height(64.dp))

            LinearLoadingIndicator(progress)

            OutlinedButton(
                enabled = !isLoading && isDirty,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                onClick = {
                    isLoading = true
                    val job = if (department == null) {
                        onCreate(displayName, image) {
                            progress = it
                        }
                    } else {
                        onUpdate(department.id, displayName, image) {
                            progress = it
                        }
                    }
                    job.invokeOnCompletion {
                        isLoading = false
                        finishEdit()
                    }
                }
            ) {
                Text(stringResource(Res.string.submit))
            }
        },
    ) { department ->
        if (department.image != null) {
            val image by department.rememberImageFile()
            AsyncByteImage(
                bytes = image,
                contentDescription = department.displayName,
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp))
            )
        }

        ReadOnlyFormField(
            value = department.displayName,
            label = stringResource(Res.string.form_display_name),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemTypesListView(
    windowSizeClass: WindowSizeClass,
    types: List<InventoryItemType>?,
    allCategories: Set<String>,
    items: List<ReferencedInventoryItem>?,
    onCreate: (displayName: String, description: String, categories: List<String>, image: PlatformFile?) -> Job,
    onUpdate: (id: Uuid, displayName: String, description: String, categories: List<String>, image: PlatformFile?) -> Job,
    onDeleteInventoryItem: (ReferencedInventoryItem) -> Job,
) {
    val groupedItems = remember(items, types) {
        items.orEmpty().groupBy { it.type }.toList()
    }
    val typesWithoutItems = remember(items, types) {
        types.orEmpty().filter { type ->
            items?.none { it.type.id == type.id } ?: true
        }.map { type -> type to emptyList<ReferencedInventoryItem>() }
    }
    ListView(
        windowSizeClass = windowSizeClass,
        items = groupedItems + typesWithoutItems,
        itemIdProvider = { (type) -> type.id },
        itemDisplayName = { (type) -> type.displayName },
        itemLeadingContent = { (type) ->
            type.image ?: return@ListView
            val image by type.rememberImageFile()
            AsyncByteImage(
                bytes = image,
                contentDescription = type.displayName,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
            )
        },
        itemTrailingContent = { (_, items) ->
            Badge { Text(items.size.toString(), style = MaterialTheme.typography.labelLarge) }
        },
        emptyItemsText = stringResource(Res.string.management_no_item_types),
        isCreatingSupported = true,
        editItemContent = { typeAndItems ->
            val type = typeAndItems?.first

            var isLoading by remember { mutableStateOf(false) }
            var image by remember { mutableStateOf<PlatformFile?>(null) }
            var categories by remember { mutableStateOf(type?.categories ?: emptyList()) }
            var displayName by remember { mutableStateOf(type?.displayName ?: "") }
            var description by remember { mutableStateOf(type?.description ?: "") }

            val isDirty = if (type == null) true else
                displayName != type.displayName ||
                description != type.description ||
                categories != type.categories ||
                image != null

            FormImagePicker(
                image = image,
                container = type,
                onImagePicked = { image = it },
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp)),
                isLoading = isLoading,
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(Res.string.management_inventory_item_type_display_name)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                singleLine = true,
                enabled = !isLoading,
            )

            AutocompleteMultipleFormField(
                entries = type?.categories.orEmpty(),
                onEntryAdded = { categories += it },
                onEntryRemoved = { categories -= it },
                suggestions = allCategories,
                label = { Text(stringResource(Res.string.management_inventory_item_type_categories)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                enabled = !isLoading,
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(Res.string.management_inventory_item_type_description)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                singleLine = true,
                enabled = !isLoading,
            )

            Spacer(Modifier.height(64.dp))

            OutlinedButton(
                enabled = !isLoading && isDirty,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                onClick = {
                    isLoading = true
                    val job = if (type == null) {
                        onCreate(displayName, description, categories, image)
                    } else {
                        onUpdate(type.id, displayName, description, categories, image)
                    }
                    job.invokeOnCompletion {
                        isLoading = false
                        finishEdit()
                    }
                }
            ) {
                Text(stringResource(Res.string.submit))
            }
        },
    ) { (type, items) ->
        if (type.image != null) {
            val image by type.rememberImageFile()
            AsyncByteImage(
                bytes = image,
                contentDescription = type.displayName,
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp))
            )
        }

        Text(
            text = stringResource(Res.string.management_inventory_item_type_categories),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(type.categories.orEmpty()) { category ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(category) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        val description = type.description
        if (description != null) {
            Text(
                text = stringResource(Res.string.management_inventory_item_type_description),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            text = stringResource(Res.string.management_inventory_item_type_identifiers),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        var highlightItemId by remember { mutableStateOf<Uuid?>(null) }
        var highlightItemNfcId by remember { mutableStateOf<ByteArray?>(null) }
        LaunchedEffect(highlightItemId, highlightItemNfcId) {
            if (highlightItemId != null || highlightItemNfcId != null) {
                delay(3000) // Highlight for 3 seconds
                highlightItemId = null
                highlightItemNfcId = null
            }
        }

        var showingItemDialog by remember { mutableStateOf<ReferencedInventoryItem?>(null) }
        showingItemDialog?.let { item ->
            QRCodeDialog(
                value = item.id.toString(),
                onReadNfc = { payload ->
                    payload.uuid()?.let { highlightItemId = it }
                    payload.id?.let { highlightItemNfcId = it }
                },
                onDismissRequest = { showingItemDialog = null }
            )
        }

        var deletingItem by remember { mutableStateOf<ReferencedInventoryItem?>(null) }
        deletingItem?.let { item ->
            DeleteDialog(
                item = item,
                displayName = { it.id.toString() },
                onDelete = { onDeleteInventoryItem(item) },
                onDismissRequested = { deletingItem = null }
            )
        }

        for (item in items) {
            val isHighlighted = item.id == highlightItemId || (item.nfcId != null && item.nfcId contentEquals highlightItemNfcId)
            val backgroundColor by animateColorAsState(if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            ListItem(
                headlineContent = { Text(item.id.toString(), fontFamily = FontFamily.Monospace) },
                supportingContent = item.variation?.let { { Text(stringResource(Res.string.inventory_item_variation, it)) } },
                overlineContent = item.nfcId?.let { { Text(stringResource(Res.string.inventory_item_nfc_id, it.toHexString())) } },
                colors = ListItemDefaults.colors(containerColor = backgroundColor),
                modifier = Modifier.clickable { showingItemDialog = item },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UsersListView(
    windowSizeClass: WindowSizeClass,
    users: List<UserData>?,
    onPromote: (UserData) -> Job,
) {
    var promotingUser by remember { mutableStateOf<UserData?>(null) }
    promotingUser?.let { user ->
        var isPromoting by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isPromoting) promotingUser = null },
            title = { Text(stringResource(Res.string.management_promote_user_title)) },
            text = { Text(stringResource(Res.string.management_promote_user_confirmation, user.fullName)) },
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

    ListView(
        windowSizeClass = windowSizeClass,
        items = users,
        itemIdProvider = { it.id },
        itemDisplayName = { it.fullName },
        emptyItemsText = stringResource(Res.string.management_no_users),
        itemToolbarActions = {
            TooltipIconButton(
                imageVector = Icons.Default.AddModerator,
                tooltip = stringResource(Res.string.management_promote_user),
                positioning = TooltipAnchorPosition.Left,
                onClick = { promotingUser = it },
            )
        },
        // users cannot be created or edited
        isCreatingSupported = false,
        editItemContent = null,
    ) { user ->
        ReadOnlyFormField(
            value = user.fullName,
            label = stringResource(Res.string.personal_info_full_name),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private enum class SortBy(val displayNameRes: StringResource) {
    NAME_ASC(Res.string.sort_by_name_asc),
    NAME_DESC(Res.string.sort_by_name_desc),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ListView(
    windowSizeClass: WindowSizeClass,
    items: List<T>?,
    itemDisplayName: (T) -> String,
    emptyItemsText: String,
    itemIdProvider: (T) -> Any,
    isCreatingSupported: Boolean = true,
    itemLeadingContent: (@Composable (T) -> Unit)? = null,
    itemTrailingContent: (@Composable RowScope.(T) -> Unit)? = null,
    itemToolbarActions: (@Composable RowScope.(T) -> Unit)? = null,
    editItemContent: (@Composable EditorContext.(T?) -> Unit)? = null,
    itemContent: @Composable ColumnScope.(T) -> Unit,
) {
    var selectedItem by remember { mutableStateOf<T?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

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
                selectedItem = selectedItem,
                onSelectedItemChange = { selectedItem = it },
                isCreatingSupported = isCreatingSupported,
                onCreateRequested = { isCreating = true },
            )

            Spacer(Modifier.width(8.dp))

            if (selectedItem != null || isCreating) {
                ListView_Content(
                    itemDisplayName = selectedItem?.let(itemDisplayName) ?: stringResource(Res.string.management_department_create),
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
                itemDisplayName = selectedItem?.let(itemDisplayName) ?: stringResource(Res.string.management_department_create),
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
                selectedItem = selectedItem,
                onSelectedItemChange = { selectedItem = it },
                isCreatingSupported = isCreatingSupported,
                onCreateRequested = { isCreating = true },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <T> ListView_ListColumn(
    items: List<T>?,
    emptyItemsText: String,
    itemDisplayName: (T) -> String,
    itemIdProvider: (T) -> Any,
    itemLeadingContent: (@Composable (T) -> Unit)? = null,
    itemTrailingContent: (@Composable RowScope.(T) -> Unit)? = null,
    selectedItem: T?,
    onSelectedItemChange: (T) -> Unit,
    isCreatingSupported: Boolean,
    onCreateRequested: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(SortBy.NAME_ASC) }
    val filteredItems = remember(items, search) {
        items.orEmpty().filter { itemDisplayName(it).contains(search, ignoreCase = true) }
    }
    val filteredAndSortedItems = remember(filteredItems, sortBy) {
        when (sortBy) {
            SortBy.NAME_ASC -> filteredItems.sortedBy { itemDisplayName(it) }
            SortBy.NAME_DESC -> filteredItems.sortedByDescending { itemDisplayName(it) }
        }
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
                    DropdownIconButton(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        items = SortBy.entries,
                        toString = { stringResource(it.displayNameRes) },
                        onItemSelected = { sortBy = it },
                    )
                }
            }
        )
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                    headlineContent = { Text(itemDisplayName(item)) },
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

private abstract class EditorContext(columnScope: ColumnScope) : ColumnScope by columnScope {
    abstract fun finishEdit()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListView_Content(
    itemDisplayName: String,
    itemToolbarActions: (@Composable RowScope.() -> Unit)? = null,
    onCloseRequested: () -> Unit,
    itemContent: @Composable ColumnScope.() -> Unit,
    isEditSupported: Boolean,
    isEditing: Boolean,
    onEditRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
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
