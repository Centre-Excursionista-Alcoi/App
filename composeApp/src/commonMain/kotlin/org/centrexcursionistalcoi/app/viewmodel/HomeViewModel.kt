package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.network.SectionsBackend
import org.centrexcursionistalcoi.app.network.UserDataBackend
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD
import org.centrexcursionistalcoi.app.server.response.data.SectionD
import org.centrexcursionistalcoi.app.server.response.data.UserD

class HomeViewModel : ViewModel() {
    private val _userData = MutableStateFlow<UserD?>(null)
    val userData get() = _userData.asStateFlow()

    private val _sections = MutableStateFlow<List<SectionD>?>(null)
    val sections get() = _sections.asStateFlow()

    private val _creatingSection = MutableStateFlow(false)
    val creatingSection get() = _creatingSection.asStateFlow()

    private val _itemTypes = MutableStateFlow<List<ItemTypeD>?>(null)
    val itemTypes get() = _itemTypes.asStateFlow()

    private val _creatingType = MutableStateFlow(false)
    val creatingType get() = _creatingType.asStateFlow()

    fun load() {
        launch {
            val data = UserDataBackend.getUserData()
            _userData.emit(data)

            val sections = SectionsBackend.list()
            _sections.emit(sections)

            val types = InventoryBackend.listTypes()
            _itemTypes.emit(types)
        }
    }

    fun logout() {
        launch {
            AccountManager.logout()
        }
    }

    fun create(sectionD: SectionD, onCreate: () -> Unit) {
        launch {
            try {
                _creatingSection.emit(true)
                SectionsBackend.create(sectionD)
                load()
                uiThread { onCreate() }
            } finally {
                _creatingSection.emit(false)
            }
        }
    }

    fun create(itemTypeD: ItemTypeD, onCreate: () -> Unit) {
        launch {
            try {
                _creatingType.emit(true)
                InventoryBackend.create(itemTypeD)
                load()
                uiThread { onCreate() }
            } finally {
                _creatingType.emit(false)
            }
        }
    }
}
