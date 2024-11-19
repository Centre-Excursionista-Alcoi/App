package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.centrexcursionistalcoi.app.pages.home.admin.BookingsCard
import org.centrexcursionistalcoi.app.pages.home.admin.ItemsCard
import org.centrexcursionistalcoi.app.pages.home.admin.SectionsCard
import org.centrexcursionistalcoi.app.pages.home.admin.SpacesCard
import org.centrexcursionistalcoi.app.pages.home.admin.TypesCard
import org.centrexcursionistalcoi.app.pages.home.admin.UnconfirmedUsersCard
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.centrexcursionistalcoi.app.server.response.data.ItemLendingD
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD
import org.centrexcursionistalcoi.app.server.response.data.SectionD
import org.centrexcursionistalcoi.app.server.response.data.SpaceD
import org.centrexcursionistalcoi.app.server.response.data.UserD

@Composable
fun AdminPage(
    updatingUser: Boolean,
    users: List<UserD>?,
    onUserConfirmationRequested: (UserD, () -> Unit) -> Unit,
    onUserDeleteRequested: (UserD, () -> Unit) -> Unit,
    isCreatingSection: Boolean,
    sections: List<SectionD>?,
    onSectionOperation: (SectionD, onCreate: () -> Unit) -> Unit,
    itemTypes: List<ItemTypeD>?,
    isCreatingType: Boolean,
    onTypeOperation: (ItemTypeD, onCreate: () -> Unit) -> Unit,
    items: List<ItemD>?,
    isCreatingItem: Boolean,
    onItemOperation: (ItemD, onCreate: () -> Unit) -> Unit,
    allBookings: List<ItemLendingD>?,
    isUpdatingBooking: Boolean,
    onConfirmBookingRequested: (ItemLendingD, () -> Unit) -> Unit,
    onMarkAsTakenRequested: (ItemLendingD, () -> Unit) -> Unit,
    onMarkAsReturnedRequested: (ItemLendingD, () -> Unit) -> Unit,
    spaces: List<SpaceD>?,
    isCreatingSpace: Boolean,
    onSpaceOperation: (SpaceD, onCreate: () -> Unit) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        users
            ?.filter { !it.isConfirmed }
            ?.takeIf { it.isNotEmpty() }
            ?.let { unconfirmedUsers ->
                UnconfirmedUsersCard(unconfirmedUsers, updatingUser, onUserConfirmationRequested, onUserDeleteRequested)
            }

        SectionsCard(sections, isCreatingSection, onSectionOperation)

        TypesCard(itemTypes, sections, isCreatingType, onTypeOperation)

        ItemsCard(items, itemTypes, isCreatingItem, onItemOperation, allBookings)

        BookingsCard(
            allBookings,
            items,
            itemTypes,
            isUpdatingBooking,
            onConfirmBookingRequested,
            onMarkAsTakenRequested,
            onMarkAsReturnedRequested
        )

        SpacesCard(spaces, isCreatingSpace, onSpaceOperation)
    }
}
