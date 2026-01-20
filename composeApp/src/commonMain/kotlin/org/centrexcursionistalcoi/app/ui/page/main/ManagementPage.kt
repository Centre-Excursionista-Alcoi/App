package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.composition.LocalNavigationBarVisibility
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Category
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.CategoryFilled
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Event
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.EventFilled
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Face
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.FaceFilled
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Inventory
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Inventory2
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Inventory2Filled
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.InventoryFilled
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Newsmode
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.NewsmodeFilled
import org.centrexcursionistalcoi.app.ui.page.main.ManagementPage.Companion.forIndex
import org.centrexcursionistalcoi.app.ui.page.main.management.DepartmentsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.EventsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.InventoryItemTypesListView
import org.centrexcursionistalcoi.app.ui.page.main.management.LendingsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.PostsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.UsersListView
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveTabRow
import org.centrexcursionistalcoi.app.ui.reusable.TabData
import org.centrexcursionistalcoi.app.ui.utils.departmentsCountBadge
import org.centrexcursionistalcoi.app.ui.utils.lendingsCountBadge
import org.centrexcursionistalcoi.app.viewmodel.ManagementViewModel

const val MANAGEMENT_PAGE_LENDINGS = 0
const val MANAGEMENT_PAGE_DEPARTMENTS = 1
const val MANAGEMENT_PAGE_USERS = 2
const val MANAGEMENT_PAGE_POSTS = 3
const val MANAGEMENT_PAGE_EVENTS = 4
const val MANAGEMENT_PAGE_INVENTORY = 5

private sealed class ManagementPage<IdType: Any, EntityType: Entity<IdType>>(
    private val key: String,
    val tabData: @Composable (badgeText: String?) -> TabData,
) {
    object Lendings : ManagementPage<Uuid, ReferencedLending>(
        key = "lendings",
        tabData = {
            TabData.fromResources(
                Res.string.management_lendings,
                MaterialSymbols.Inventory2,
                MaterialSymbols.Inventory2Filled,
                it
            )
        }
    )

    object Departments : ManagementPage<Uuid, Department>(
        key = "departments",
        tabData = {
            TabData.fromResources(
                Res.string.management_departments,
                MaterialSymbols.Category,
                MaterialSymbols.CategoryFilled,
                it
            )
        }
    ) {
        override fun shouldShow(profile: ProfileResponse, items: List<Department>?): Boolean {
            // Admins can always see the page
            if (profile.isAdmin) return true

            // If items is empty, do not show the page
            val items = items ?: return false

            // If user is not admin, check whether they are manager of at least one department
            val isManager = items.any { department ->
                department.members.orEmpty()
                    // Find the member info for the current user
                    .find { it.userSub == profile.sub }
                    // Check if that member is a manager
                    ?.isManager == true
            }
            return isManager
        }
    }

    object Users : ManagementPage<String, UserData>(
        key = "users",
        tabData = {
            TabData.fromResources(
                Res.string.management_users,
                MaterialSymbols.Face,
                MaterialSymbols.FaceFilled,
                it
            )
        }
    )

    object Posts : ManagementPage<Uuid, ReferencedPost>(
        key = "posts",
        tabData = {
            TabData.fromResources(
                Res.string.management_posts,
                MaterialSymbols.Newsmode,
                MaterialSymbols.NewsmodeFilled,
                it
            )
        }
    )

    object Events : ManagementPage<Uuid, ReferencedEvent>(
        key = "events",
        tabData = {
            TabData.fromResources(
                Res.string.management_events,
                MaterialSymbols.Event,
                MaterialSymbols.EventFilled,
                it
            )
        }
    )

    object Inventory : ManagementPage<Uuid, ReferencedInventoryItemType>(
        key = "inventory",
        tabData = {
            TabData.fromResources(
                Res.string.management_inventory,
                MaterialSymbols.Inventory,
                MaterialSymbols.InventoryFilled,
                it
            )
        }
    )


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ManagementPage<*, *>) return false
        if (key != other.key) return false
        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    open fun shouldShow(profile: ProfileResponse, items: List<EntityType>?): Boolean = profile.isAdmin


    companion object {
        fun allFiltered(
            profile: ProfileResponse,
            lendings: List<ReferencedLending>?,
            departments: List<Department>?,
            users: List<UserData>?,
            posts: List<ReferencedPost>?,
            events: List<ReferencedEvent>?,
            inventoryItemTypes: List<ReferencedInventoryItemType>?,
        ): List<ManagementPage<*, *>> {
            return listOfNotNull(
                Lendings.takeIf { Lendings.shouldShow(profile, lendings) },
                Departments.takeIf { Departments.shouldShow(profile, departments) },
                Users.takeIf { Users.shouldShow(profile, users) },
                Posts.takeIf { Posts.shouldShow(profile, posts) },
                Events.takeIf { EventsRemoteRepository.endpointSupported() && Events.shouldShow(profile, events) },
                Inventory.takeIf { Inventory.shouldShow(profile, inventoryItemTypes) },
            )
        }

        fun List<ManagementPage<*, *>>.forIndex(index: Int): ManagementPage<*, *> {
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

    profile: ProfileResponse,

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
        profile = profile,
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
        onApproveDepartmentJoinRequest = model::approveDepartmentJoinRequest,
        onDenyDepartmentJoinRequest = model::denyDepartmentJoinRequest,
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
        onUpdateInventoryItemManufacturerData = model::updateInventoryItemManufacturerData,
    )
}

@Composable
private fun ManagementPage(
    windowSizeClass: WindowSizeClass,
    snackbarHostState: SnackbarHostState,

    selectedItem: Pair<Int, Uuid?>?,

    profile: ProfileResponse,

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
    onApproveDepartmentJoinRequest: (DepartmentMemberInfo) -> Job,
    onDenyDepartmentJoinRequest: (DepartmentMemberInfo) -> Job,

    users: List<UserData>?,
    onPromote: (UserData) -> Job,

    members: List<Member>?,

    inventoryItemTypes: List<ReferencedInventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onCreateInventoryItemType: (displayName: String, description: String, categories: List<String>, weight: String, department: Department?, image: PlatformFile?) -> Job,
    onUpdateInventoryItemType: (id: Uuid, displayName: String, description: String, categories: List<String>, weight: String, department: Department?, image: PlatformFile?) -> Job,
    onDeleteInventoryItemType: (ReferencedInventoryItemType) -> Job,
    onCreateInventoryItem: (variation: String, ReferencedInventoryItemType, amount: Int) -> Job,
    onDeleteInventoryItem: (ReferencedInventoryItem) -> Job,
    onUpdateInventoryItemManufacturerData: (ReferencedInventoryItem, String) -> Job,

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
    val pages = remember(profile, lendings, departments, users, posts, events, inventoryItemTypes) {
        ManagementPage.allFiltered(profile, lendings, departments, users, posts, events, inventoryItemTypes)
    }
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
            tabs = pages.map { page ->
                val countBadge = when (page) {
                    ManagementPage.Departments -> departments.departmentsCountBadge()
                    ManagementPage.Lendings -> lendings.lendingsCountBadge()
                    else -> null
                }
                page.tabData(countBadge?.takeIf { it > 0 }?.toString())
            },
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

            ManagementPage.Departments -> DepartmentsListView(
                windowSizeClass,
                profile,
                users,
                departments,
                onCreateDepartment,
                onUpdateDepartment,
                onDeleteDepartment,
                onApproveDepartmentJoinRequest,
                onDenyDepartmentJoinRequest,
            )

            ManagementPage.Users -> UsersListView(
                windowSizeClass,
                users,
                members,
                departments,
                onPromote,
                onKickFromDepartment
            )

            ManagementPage.Posts -> PostsListView(
                windowSizeClass,
                posts,
                departments,
                onCreatePost,
                onUpdatePost,
                onDeletePost
            )

            ManagementPage.Events -> EventsListView(
                windowSizeClass,
                events,
                departments,
                onCreateEvent,
                onUpdateEvent,
                onDeleteEvent
            )

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
                onUpdateInventoryItemManufacturerData,
            )
        }
    }
}
