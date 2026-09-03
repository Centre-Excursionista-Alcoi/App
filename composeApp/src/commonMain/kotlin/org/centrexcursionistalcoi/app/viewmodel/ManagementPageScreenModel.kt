package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ManagementPageScreenModel(
    departmentsRepository: DepartmentsRepository,
    usersRepository: UsersRepository,
    membersRepository: MembersRepository,
    inventoryItemTypesRepository: InventoryItemTypesRepository,
    inventoryItemsRepository: InventoryItemsRepository,
    lendingsRepository: LendingsRepository,
    postsRepository: PostsRepository,
    eventsRepository: EventsRepository,
) : ViewModel() {
    val profile = ProfileRepository.profile.stateInViewModel()
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()
    val users = usersRepository.selectAllAsFlow().stateInViewModel()
    val members = membersRepository.selectAllAsFlow().stateInViewModel()
    val inventoryItemTypes = inventoryItemTypesRepository.selectAllAsFlow().stateInViewModel()
    val inventoryItemTypesCategories = inventoryItemTypesRepository.categoriesAsFlow().stateInViewModel()
    val inventoryItems = inventoryItemsRepository.selectAllAsFlow().stateInViewModel()
    val lendings = lendingsRepository.selectAllAsFlow().stateInViewModel()
    val posts = postsRepository.selectAllAsFlow().stateInViewModel()
    val events = eventsRepository.selectAllAsFlow().stateInViewModel()

    val ready = combine(
        profile,
        departments,
        users,
        members,
        inventoryItemTypes,
        inventoryItems,
        lendings,
        posts,
        events,
    ) { values -> values.all { it != null } }.stateInViewModel(initialValue = false)
}
