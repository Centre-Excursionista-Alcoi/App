package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.network.SectionsBackend
import org.centrexcursionistalcoi.app.network.UserDataBackend
import org.centrexcursionistalcoi.app.server.response.data.DatabaseData
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD
import org.centrexcursionistalcoi.app.server.response.data.SectionD
import org.centrexcursionistalcoi.app.server.response.data.UserD

class HomeViewModel : ViewModel() {
    private val _userData = MutableStateFlow<UserD?>(null)
    val userData get() = _userData.asStateFlow()


    private val _availableItems = MutableStateFlow<List<ItemD>?>(null)
    val availableItems get() = _availableItems.asStateFlow()


    private val _sections = MutableStateFlow<List<SectionD>?>(null)
    val sections get() = _sections.asStateFlow()

    private val _creatingSection = MutableStateFlow(false)
    val creatingSection get() = _creatingSection.asStateFlow()

    private val _itemTypes = MutableStateFlow<List<ItemTypeD>?>(null)
    val itemTypes get() = _itemTypes.asStateFlow()

    private val _creatingType = MutableStateFlow(false)
    val creatingType get() = _creatingType.asStateFlow()

    private val _items = MutableStateFlow<List<ItemD>?>(null)
    val items get() = _items.asStateFlow()

    private val _creatingItem = MutableStateFlow(false)
    val creatingItem get() = _creatingItem.asStateFlow()

    fun load() {
        launch {
            val data = UserDataBackend.getUserData()
            _userData.emit(data)

            val sections = SectionsBackend.list()
            _sections.emit(sections)

            val types = InventoryBackend.listTypes()
            _itemTypes.emit(types)

            val items = InventoryBackend.listItems()
            _items.emit(items)
        }
    }

    fun logout() {
        launch {
            AccountManager.logout()
        }
    }

    fun availability(from: LocalDate, to: LocalDate) {
        launch {
            val items = InventoryBackend.availability(
                from.atStartOfDayIn(TimeZone.currentSystemDefault()),
                to.atStartOfDayIn(TimeZone.currentSystemDefault())
            )
            _availableItems.emit(items)
        }
    }

    private fun <Type: DatabaseData> onCreateOrUpdate(
        value: Type,
        creating: MutableStateFlow<Boolean>,
        creator: suspend (Type) -> Unit,
        updater: suspend (Type) -> Unit,
        onCreate: () -> Unit
    ) {
        launch {
            try {
                creating.emit(true)
                if (value.id == null) {
                    creator(value)
                } else {
                    updater(value)
                }
                load()
                uiThread { onCreate() }
            } finally {
                creating.emit(false)
            }
        }
    }

    fun onCreateOrUpdate(sectionD: SectionD, onCreate: () -> Unit) {
        onCreateOrUpdate(
            sectionD,
            _creatingSection,
            SectionsBackend::create,
            SectionsBackend::update,
            onCreate
        )
    }

    fun createOrUpdate(itemTypeD: ItemTypeD, onCreate: () -> Unit) {
        onCreateOrUpdate(
            itemTypeD,
            _creatingType,
            InventoryBackend::create,
            InventoryBackend::update,
            onCreate
        )
    }

    fun createOrUpdate(itemD: ItemD, onCreate: () -> Unit) {
        onCreateOrUpdate(
            itemD,
            _creatingItem,
            InventoryBackend::create,
            InventoryBackend::update,
            onCreate
        )
    }
}
