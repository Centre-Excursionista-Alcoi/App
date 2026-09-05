package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.management_departments
import cea_app.composeapp.generated.resources.management_events
import cea_app.composeapp.generated.resources.management_inventory
import cea_app.composeapp.generated.resources.management_lendings
import cea_app.composeapp.generated.resources.management_memories
import cea_app.composeapp.generated.resources.management_posts
import cea_app.composeapp.generated.resources.management_users
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Department.Companion.hasAnyDepartmentRole
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.composition.LocalNavigationBarVisibility
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Article
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ArticleFilled
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
import org.centrexcursionistalcoi.app.ui.page.main.management.MemoriesManagementListView
import org.centrexcursionistalcoi.app.ui.page.main.management.PostsListView
import org.centrexcursionistalcoi.app.ui.page.main.management.UsersListView
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveTabRow
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.reusable.TabData
import org.centrexcursionistalcoi.app.ui.utils.departmentsCountBadge
import org.centrexcursionistalcoi.app.ui.utils.lendingsCountBadge
import org.centrexcursionistalcoi.app.viewmodel.ManagementPageScreenModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.Uuid

const val MANAGEMENT_PAGE_LENDINGS = 0
const val MANAGEMENT_PAGE_MEMORIES = 1
const val MANAGEMENT_PAGE_DEPARTMENTS = 2
const val MANAGEMENT_PAGE_USERS = 3
const val MANAGEMENT_PAGE_POSTS = 4
const val MANAGEMENT_PAGE_EVENTS = 5
const val MANAGEMENT_PAGE_INVENTORY = 6

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
    ) {
        override fun shouldShow(profile: ProfileResponse, items: List<ReferencedLending>?, departments: List<Department>?): Boolean {
            return profile.isAdmin || departments.orEmpty().hasAnyDepartmentRole(profile, DepartmentRole.LENDING_MANAGER)
        }
    }

    object Memories : ManagementPage<Uuid, Memory>(
        key = "memories",
        tabData = {
            TabData.fromResources(
                Res.string.management_memories,
                MaterialSymbols.Article,
                MaterialSymbols.ArticleFilled,
                it
            )
        }
    ) {
        override fun shouldShow(profile: ProfileResponse, items: List<Memory>?, departments: List<Department>?): Boolean {
            return profile.isAdmin || departments.orEmpty().hasAnyDepartmentRole(profile, DepartmentRole.MEMORY_MANAGER)
        }
    }

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
        override fun shouldShow(profile: ProfileResponse, items: List<Department>?, departments: List<Department>?): Boolean {
            return profile.isAdmin || items.orEmpty().hasAnyDepartmentRole(profile, DepartmentRole.PEOPLE_MANAGER)
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
    ) {
        override fun shouldShow(profile: ProfileResponse, items: List<UserData>?, departments: List<Department>?): Boolean {
            return profile.isUsersManager || departments.orEmpty().hasAnyDepartmentRole(profile, DepartmentRole.PEOPLE_MANAGER)
        }
    }

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
    ) {
        override fun shouldShow(profile: ProfileResponse, items: List<ReferencedPost>?, departments: List<Department>?): Boolean {
            return profile.isAdmin || departments.orEmpty().hasAnyDepartmentRole(profile, DepartmentRole.CONTENT_MANAGER)
        }
    }

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
    ) {
        override fun shouldShow(profile: ProfileResponse, items: List<ReferencedEvent>?, departments: List<Department>?): Boolean {
            return profile.isAdmin || departments.orEmpty().hasAnyDepartmentRole(profile, DepartmentRole.CONTENT_MANAGER)
        }
    }

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
    ) {
        override fun shouldShow(profile: ProfileResponse, items: List<ReferencedInventoryItemType>?, departments: List<Department>?): Boolean {
            return profile.isAdmin || departments.orEmpty().hasAnyDepartmentRole(profile, DepartmentRole.INVENTORY_MANAGER)
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ManagementPage<*, *>) return false
        if (key != other.key) return false
        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    open fun shouldShow(profile: ProfileResponse, items: List<EntityType>?, departments: List<Department>?): Boolean = profile.isAdmin


    companion object {
        fun allFiltered(
            profile: ProfileResponse,
            lendings: List<ReferencedLending>?,
            departments: List<Department>?,
        ): List<ManagementPage<*, *>> {
            return listOfNotNull(
                Lendings.takeIf { Lendings.shouldShow(profile, lendings, departments) },
                Memories.takeIf { Memories.shouldShow(profile, null, departments) },
                Departments.takeIf { Departments.shouldShow(profile, departments, departments) },
                Users.takeIf { Users.shouldShow(profile, null, departments) },
                Posts.takeIf { Posts.shouldShow(profile, null, departments) },
                Events.takeIf { Events.shouldShow(profile, null, departments) },
                Inventory.takeIf { Inventory.shouldShow(profile, null, departments) },
            )
        }

        fun List<ManagementPage<*, *>>.forIndex(index: Int): ManagementPage<*, *> {
            return this[index]
        }
    }
}

@Composable
fun ManagementPage(
    snackbarHostState: SnackbarHostState,

    /**
     * The currently selected item in the format Pair(pageIndex, itemId?).
     *
     * Pages: [MANAGEMENT_PAGE_LENDINGS], [MANAGEMENT_PAGE_MEMORIES], [MANAGEMENT_PAGE_DEPARTMENTS], [MANAGEMENT_PAGE_USERS], [MANAGEMENT_PAGE_POSTS], [MANAGEMENT_PAGE_EVENTS], [MANAGEMENT_PAGE_INVENTORY].
     */
    selectedItem: Pair<Int, Uuid?>?,
    onGiveRequested: (ReferencedLending) -> Unit,
    onReceiveRequested: (ReferencedLending) -> Unit,
    screenModel: ManagementPageScreenModel = koinViewModel(),
) {
    val profile by screenModel.profile.collectAsState()
    val lendings by screenModel.lendings.collectAsState()
    val departments by screenModel.departments.collectAsState()

    if (profile == null) {
        LoadingBox()
        return
    }

    ManagementPageContent(
        snackbarHostState = snackbarHostState,
        selectedItem = selectedItem,
        profile = profile!!,
        lendings = lendings,
        departments = departments,
        onGiveRequested = onGiveRequested,
        onReceiveRequested = onReceiveRequested,
    )
}

@Composable
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
private fun ManagementPageContent(
    snackbarHostState: SnackbarHostState,

    selectedItem: Pair<Int, Uuid?>?,

    profile: ProfileResponse,
    lendings: List<ReferencedLending>?,
    departments: List<Department>?,

    onGiveRequested: (ReferencedLending) -> Unit,
    onReceiveRequested: (ReferencedLending) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pages = remember(profile, lendings, departments) {
        ManagementPage.allFiltered(profile, lendings, departments)
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
            ManagementPage.Lendings -> LendingsListView(snackbarHostState, onGiveRequested, onReceiveRequested)

            ManagementPage.Memories -> MemoriesManagementListView(snackbarHostState)

            ManagementPage.Departments -> DepartmentsListView()

            ManagementPage.Users -> UsersListView()

            ManagementPage.Posts -> PostsListView()

            ManagementPage.Events -> EventsListView()

            ManagementPage.Inventory -> InventoryItemTypesListView(selectedItemId)
        }
    }
}
