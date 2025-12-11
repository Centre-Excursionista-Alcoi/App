package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.centrexcursionistalcoi.app.data.*
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.ui.composition.LocalNavigationBarVisibility
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.*
import org.centrexcursionistalcoi.app.ui.page.main.ManagementPage.Companion.forIndex
import org.centrexcursionistalcoi.app.ui.page.main.management.*
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveTabRow
import org.centrexcursionistalcoi.app.ui.reusable.TabData
import org.centrexcursionistalcoi.app.viewmodel.ManagementViewModel
import kotlin.uuid.Uuid

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
        tabData = { TabData.fromResources(Res.string.management_lendings, MaterialSymbols.Inventory2, MaterialSymbols.Inventory2Filled) }
    )

    object Departments : ManagementPage(
        key = "departments",
        tabData = { TabData.fromResources(Res.string.management_departments, MaterialSymbols.Category, MaterialSymbols.CategoryFilled) }
    )

    object Users : ManagementPage(
        key = "users",
        tabData = { TabData.fromResources(Res.string.management_users, MaterialSymbols.Face, MaterialSymbols.FaceFilled) }
    )

    object Posts : ManagementPage(
        key = "posts",
        tabData = { TabData.fromResources(Res.string.management_posts, MaterialSymbols.Newsmode, MaterialSymbols.NewsmodeFilled) }
    )

    object Events : ManagementPage(
        key = "events",
        tabData = { TabData.fromResources(Res.string.management_events, MaterialSymbols.Event, MaterialSymbols.EventFilled) }
    )

    object Inventory : ManagementPage(
        key = "inventory",
        tabData = { TabData.fromResources(Res.string.management_inventory,  MaterialSymbols.Inventory, MaterialSymbols.InventoryFilled) }
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

    members: List<Member>?,

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
        onKickFromDepartment = model::kickFromDepartment,
        users = users,
        onPromote = model::promote,
        members = members,
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
    onKickFromDepartment: (UserData, Department) -> Job,

    users: List<UserData>?,
    onPromote: (UserData) -> Job,

    members: List<Member>?,

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
    onCreateEvent: (start: LocalDateTime, end: LocalDateTime?, place: String, title: String, description: RichTextState, maxPeople: String, requiresConfirmation: Boolean, requiresInsurance: Boolean, department: Department?, image: PlatformFile?, progressNotifier: (Progress) -> Unit) -> Job,
    onUpdateEvent: (eventId: Uuid, start: LocalDateTime?, end: LocalDateTime?, place: String?, title: String?, description: RichTextState?, maxPeople: String?, requiresConfirmation: Boolean?, requiresInsurance: Boolean?, department: Department?, image: PlatformFile?, progressNotifier: (Progress) -> Unit) -> Job,
    onDeleteEvent: (ReferencedEvent) -> Job,
) {
    val scope = rememberCoroutineScope()
    val pages = remember { ManagementPage.all }
    val pagerState = rememberPagerState { pages.size }

    val navigationBarVisibility = LocalNavigationBarVisibility.current ?: MutableStateFlow(true)
    val isNavigationBarVisible by navigationBarVisibility.collectAsState()

    val selectedItemId = selectedItem?.second
    LaunchedEffect(selectedItem) {
        selectedItem ?: return@LaunchedEffect
        val (page) = selectedItem
        pagerState.scrollToPage(page)
    }

    AnimatedVisibility(
        isNavigationBarVisible,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
    ) {
        AdaptiveTabRow(
            selectedTabIndex = pagerState.currentPage,
            tabs = pages.map { it.tabData() },
            onTabSelected = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            },
        )
    }
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = isNavigationBarVisible,
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

            ManagementPage.Users -> UsersListView(windowSizeClass, users, members, departments, onPromote, onKickFromDepartment)

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
