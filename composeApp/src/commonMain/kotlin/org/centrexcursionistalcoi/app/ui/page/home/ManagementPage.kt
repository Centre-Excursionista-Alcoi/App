package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.ui.page.home.management.DepartmentsListView
import org.centrexcursionistalcoi.app.ui.page.home.management.InventoryItemTypesListView
import org.centrexcursionistalcoi.app.ui.page.home.management.LendingsListView
import org.centrexcursionistalcoi.app.ui.page.home.management.UsersListView
import org.centrexcursionistalcoi.app.viewmodel.ManagementViewModel
import org.jetbrains.compose.resources.stringResource

const val MANAGEMENT_PAGE_COUNT = 4
const val MANAGEMENT_PAGE_LENDINGS = 0
const val MANAGEMENT_PAGE_DEPARTMENTS = 1
const val MANAGEMENT_PAGE_USERS = 2
const val MANAGEMENT_PAGE_INVENTORY = 3

@Composable
fun ManagementPage(
    windowSizeClass: WindowSizeClass,
    snackbarHostState: SnackbarHostState,

    /**
     * The currently selected item in the format Pair(pageIndex, itemId?).
     *
     * Pages: [MANAGEMENT_PAGE_LENDINGS], [MANAGEMENT_PAGE_DEPARTMENTS], [MANAGEMENT_PAGE_USERS], [MANAGEMENT_PAGE_INVENTORY].
     */
    selectedItem: Pair<Int, Uuid?>?,

    lendings: List<ReferencedLending>?,
    onGiveRequested: (ReferencedLending) -> Unit,
    onReceiveRequested: (ReferencedLending) -> Unit,

    departments: List<Department>?,

    users: List<UserData>?,

    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,

    inventoryItems: List<ReferencedInventoryItem>?,

    model: ManagementViewModel = viewModel { ManagementViewModel() },
) {
    ManagementPage(
        windowSizeClass = windowSizeClass,
        snackbarHostState = snackbarHostState,
        selectedItem = selectedItem,
        lendings = lendings,
        onConfirmLendingRequest = model::confirmLending,
        onSkipMemoryRequest = model::skipLendingMemory,
        onGiveRequested = onGiveRequested,
        onReceiveRequested = onReceiveRequested,
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
    snackbarHostState: SnackbarHostState,

    selectedItem: Pair<Int, Uuid?>?,

    lendings: List<ReferencedLending>?,
    onConfirmLendingRequest: (ReferencedLending) -> Job,
    onSkipMemoryRequest: (ReferencedLending) -> Job,
    onGiveRequested: (ReferencedLending) -> Unit,
    onReceiveRequested: (ReferencedLending) -> Unit,

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
    val pagerState = rememberPagerState { MANAGEMENT_PAGE_COUNT }

    val selectedItemId = selectedItem?.second
    LaunchedEffect(selectedItem) {
        selectedItem ?: return@LaunchedEffect
        val (page) = selectedItem
        pagerState.scrollToPage(page)
    }

    PrimaryTabRow(pagerState.currentPage) {
        listOf(
            Res.string.management_lendings,
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
            MANAGEMENT_PAGE_LENDINGS -> LendingsListView(windowSizeClass, snackbarHostState, lendings, onConfirmLendingRequest, onSkipMemoryRequest, onGiveRequested, onReceiveRequested)
            MANAGEMENT_PAGE_DEPARTMENTS -> DepartmentsListView(windowSizeClass, departments, onCreateDepartment, onUpdateDepartment, onDeleteDepartment)
            MANAGEMENT_PAGE_USERS -> UsersListView(windowSizeClass, users, onPromote)
            MANAGEMENT_PAGE_INVENTORY -> InventoryItemTypesListView(
                windowSizeClass,
                selectedItemId,
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
