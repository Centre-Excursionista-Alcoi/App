package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.ui.page.main.management.DepartmentsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.InventoryItemTypesListView
import org.centrexcursionistalcoi.app.ui.page.main.management.LendingsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.PostsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.UsersListView
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveTabRow
import org.centrexcursionistalcoi.app.ui.reusable.TabData
import org.centrexcursionistalcoi.app.viewmodel.ManagementViewModel

const val MANAGEMENT_PAGE_COUNT = 5
const val MANAGEMENT_PAGE_LENDINGS = 0
const val MANAGEMENT_PAGE_DEPARTMENTS = 1
const val MANAGEMENT_PAGE_USERS = 2
const val MANAGEMENT_PAGE_POSTS = 3
const val MANAGEMENT_PAGE_INVENTORY = 4

@Composable
fun ManagementPage(
    windowSizeClass: WindowSizeClass,
    snackbarHostState: SnackbarHostState,

    /**
     * The currently selected item in the format Pair(pageIndex, itemId?).
     *
     * Pages: [MANAGEMENT_PAGE_LENDINGS], [MANAGEMENT_PAGE_DEPARTMENTS], [MANAGEMENT_PAGE_USERS], [MANAGEMENT_PAGE_POSTS], [MANAGEMENT_PAGE_INVENTORY].
     */
    selectedItem: Pair<Int, Uuid?>?,

    lendings: List<ReferencedLending>?,
    onGiveRequested: (ReferencedLending) -> Unit,
    onReceiveRequested: (ReferencedLending) -> Unit,

    departments: List<Department>?,

    users: List<UserData>?,

    inventoryItemTypes: List<ReferencedInventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,

    inventoryItems: List<ReferencedInventoryItem>?,

    posts: List<ReferencedPost>?,

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
        onDeleteInventoryItemType = model::delete,
        onCreateInventoryItem = model::createInventoryItem,
        onDeleteInventoryItem = model::delete,
        inventoryItems = inventoryItems,
        posts = posts,
        onCreatePost = model::createPost,
        onUpdatePost = model::updatePost,
        onDeletePost = model::delete,
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
    onUpdateDepartment: (id: Uuid, displayName: String, image: PlatformFile?, progressNotifier: ProgressNotifier?) -> Job,
    onDeleteDepartment: (Department) -> Job,

    users: List<UserData>?,
    onPromote: (UserData) -> Job,

    inventoryItemTypes: List<ReferencedInventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onCreateInventoryItemType: (displayName: String, description: String, categories: List<String>, department: Department?, image: PlatformFile?) -> Job,
    onUpdateInventoryItemType: (id: Uuid, displayName: String, description: String, categories: List<String>, department: Department?, image: PlatformFile?) -> Job,
    onDeleteInventoryItemType: (ReferencedInventoryItemType) -> Job,
    onCreateInventoryItem: (variation: String, ReferencedInventoryItemType, amount: Int) -> Job,
    onDeleteInventoryItem: (ReferencedInventoryItem) -> Job,

    inventoryItems: List<ReferencedInventoryItem>?,

    posts: List<ReferencedPost>?,
    onCreatePost: (title: String, department: Department?, content: RichTextState, link: String, files: List<PlatformFile>, progressNotifier: (Progress) -> Unit) -> Job,
    onUpdatePost: (postId: Uuid, title: String?, department: Department?, content: RichTextState?, link: String?, removedFiles: List<Uuid>, files: List<PlatformFile>, progressNotifier: (Progress) -> Unit) -> Job,
    onDeletePost: (ReferencedPost) -> Job,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { MANAGEMENT_PAGE_COUNT }

    val selectedItemId = selectedItem?.second
    LaunchedEffect(selectedItem) {
        selectedItem ?: return@LaunchedEffect
        val (page) = selectedItem
        pagerState.scrollToPage(page)
    }

    AdaptiveTabRow(
        selectedTabIndex = pagerState.currentPage,
        tabs = listOf(
            TabData.fromResources(Res.string.management_lendings, Icons.Default.Inventory2),
            TabData.fromResources(Res.string.management_departments, Icons.Default.Category),
            TabData.fromResources(Res.string.management_users, Icons.Default.Face),
            TabData.fromResources(Res.string.management_posts, Icons.AutoMirrored.Filled.Feed),
            TabData.fromResources(Res.string.management_inventory,  Icons.Default.Inventory),
        ),
        onTabSelected = { index ->
            scope.launch { pagerState.animateScrollToPage(index) }
        },
    )
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        when (page) {
            MANAGEMENT_PAGE_LENDINGS -> LendingsListView(
                windowSizeClass,
                snackbarHostState,
                lendings,
                users.orEmpty(),
                onConfirmLendingRequest,
                onSkipMemoryRequest,
                onGiveRequested,
                onReceiveRequested
            )

            MANAGEMENT_PAGE_DEPARTMENTS -> DepartmentsListView(windowSizeClass, departments, onCreateDepartment, onUpdateDepartment, onDeleteDepartment)

            MANAGEMENT_PAGE_USERS -> UsersListView(windowSizeClass, users, onPromote)

            MANAGEMENT_PAGE_POSTS -> PostsListView(windowSizeClass, posts, departments, onCreatePost, onUpdatePost, onDeletePost)

            MANAGEMENT_PAGE_INVENTORY -> InventoryItemTypesListView(
                windowSizeClass,
                selectedItemId,
                inventoryItemTypes,
                inventoryItemTypesCategories,
                departments,
                inventoryItems,
                onCreateInventoryItemType,
                onUpdateInventoryItemType,
                onDeleteInventoryItemType,
                onCreateInventoryItem,
                onDeleteInventoryItem,
            )
        }
    }
}
