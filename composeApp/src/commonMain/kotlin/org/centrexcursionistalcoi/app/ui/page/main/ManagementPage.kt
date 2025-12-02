package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.ui.page.main.ManagementPage.Companion.forIndex
import org.centrexcursionistalcoi.app.ui.page.main.management.DepartmentsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.EventsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.InventoryItemTypesListView
import org.centrexcursionistalcoi.app.ui.page.main.management.LendingsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.PostsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.UsersListView
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveTabRow
import org.centrexcursionistalcoi.app.ui.reusable.TabData
import org.centrexcursionistalcoi.app.viewmodel.ManagementViewModel

const val MANAGEMENT_PAGE_LENDINGS = 0
const val MANAGEMENT_PAGE_DEPARTMENTS = 1
const val MANAGEMENT_PAGE_USERS = 2
const val MANAGEMENT_PAGE_POSTS = 3
const val MANAGEMENT_PAGE_EVENTS = 4
const val MANAGEMENT_PAGE_INVENTORY = 5

private sealed class ManagementPage(
    private val key: String,
    val tabData: @Composable () -> TabData,
) {
    object Lendings : ManagementPage(
        key = "lendings",
        tabData = { TabData.fromResources(Res.string.management_lendings, Icons.Default.Inventory2) }
    )

    object Departments : ManagementPage(
        key = "departments",
        tabData = { TabData.fromResources(Res.string.management_departments, Icons.Default.Category) }
    )

    object Users : ManagementPage(
        key = "users",
        tabData = { TabData.fromResources(Res.string.management_users, Icons.Default.Face) }
    )

    object Posts : ManagementPage(
        key = "posts",
        tabData = { TabData.fromResources(Res.string.management_posts, Icons.AutoMirrored.Filled.Feed) }
    )

    object Events : ManagementPage(
        key = "events",
        tabData = { TabData.fromResources(Res.string.management_events, Icons.Default.Event) }
    )

    object Inventory : ManagementPage(
        key = "inventory",
        tabData = { TabData.fromResources(Res.string.management_inventory,  Icons.Default.Inventory) }
    )


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ManagementPage) return false
        if (key != other.key) return false
        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }


    companion object {
        val all = listOfNotNull(
            Lendings,
            Departments,
            Users,
            Posts,
            Events.takeIf { EventsRemoteRepository.endpointSupported() },
            Inventory,
        )

        fun List<ManagementPage>.forIndex(index: Int): ManagementPage {
            return this[index]
        }
    }
}

@Composable
fun ManagementPage(
    windowSizeClass: WindowSizeClass,
    snackbarHostState: SnackbarHostState,

    /**
     * The currently selected item in the format Pair(pageIndex, itemId?).
     *
     * Pages: [MANAGEMENT_PAGE_LENDINGS], [MANAGEMENT_PAGE_DEPARTMENTS], [MANAGEMENT_PAGE_USERS], [MANAGEMENT_PAGE_POSTS], [MANAGEMENT_PAGE_EVENTS], [MANAGEMENT_PAGE_INVENTORY].
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

    events: List<ReferencedEvent>?,

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
        events = events,
        onCreateEvent = model::createEvent,
        onUpdateEvent = model::updateEvent,
        onDeleteEvent = model::delete,
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

    events: List<ReferencedEvent>?,
    onCreateEvent: (start: LocalDateTime, end: LocalDateTime?, place: String, title: String, description: RichTextState, maxPeople: String, requiresConfirmation: Boolean, department: Department?, image: PlatformFile?, progressNotifier: (Progress) -> Unit) -> Job,
    onUpdateEvent: (eventId: Uuid, start: LocalDateTime?, end: LocalDateTime?, place: String?, title: String?, description: RichTextState?, maxPeople: String?, requiresConfirmation: Boolean?, department: Department?, image: PlatformFile?, progressNotifier: (Progress) -> Unit) -> Job,
    onDeleteEvent: (ReferencedEvent) -> Job,
) {
    val scope = rememberCoroutineScope()
    val pages = remember { ManagementPage.all }
    val pagerState = rememberPagerState { pages.size }

    val selectedItemId = selectedItem?.second
    LaunchedEffect(selectedItem) {
        selectedItem ?: return@LaunchedEffect
        val (page) = selectedItem
        pagerState.scrollToPage(page)
    }

    AdaptiveTabRow(
        selectedTabIndex = pagerState.currentPage,
        tabs = pages.map { it.tabData() },
        onTabSelected = { index ->
            scope.launch { pagerState.animateScrollToPage(index) }
        },
    )
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { index ->
        val page = pages.forIndex(index)
        when (page) {
            ManagementPage.Lendings -> LendingsListView(
                windowSizeClass,
                snackbarHostState,
                lendings,
                users.orEmpty(),
                onConfirmLendingRequest,
                onSkipMemoryRequest,
                onGiveRequested,
                onReceiveRequested
            )

            ManagementPage.Departments -> DepartmentsListView(windowSizeClass, departments, onCreateDepartment, onUpdateDepartment, onDeleteDepartment)

            ManagementPage.Users -> UsersListView(windowSizeClass, users, departments, onPromote)

            ManagementPage.Posts -> PostsListView(windowSizeClass, posts, departments, onCreatePost, onUpdatePost, onDeletePost)

            ManagementPage.Events -> EventsListView(windowSizeClass, events, departments, onCreateEvent, onUpdateEvent, onDeleteEvent)

            ManagementPage.Inventory -> InventoryItemTypesListView(
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
