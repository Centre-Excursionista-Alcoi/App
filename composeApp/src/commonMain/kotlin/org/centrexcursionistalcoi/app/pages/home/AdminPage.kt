package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.centrexcursionistalcoi.app.database.entity.BookingEntity
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.ItemBooking
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.admin.User
import org.centrexcursionistalcoi.app.pages.home.admin.BookingsCard
import org.centrexcursionistalcoi.app.pages.home.admin.SectionsCard
import org.centrexcursionistalcoi.app.pages.home.admin.SpacesCard
import org.centrexcursionistalcoi.app.pages.home.admin.TypesCard
import org.centrexcursionistalcoi.app.pages.home.admin.UnconfirmedUsersCard

@Composable
fun AdminPage(
    updatingUser: Boolean,
    users: List<User>?,
    onUserConfirmationRequested: (User, () -> Unit) -> Unit,
    onUserDeleteRequested: (User, () -> Unit) -> Unit,
    isCreatingSection: Boolean,
    sections: List<Section>?,
    onSectionOperation: (Section, onCreate: () -> Unit) -> Unit,
    itemTypes: List<ItemType>?,
    isCreatingType: Boolean,
    onTypeOperation: (ItemType, onCreate: () -> Unit) -> Unit,
    items: List<Item>?,
    allItemBookings: List<ItemBooking>?,
    allSpaceBookings: List<SpaceBooking>?,
    isUpdatingBooking: Boolean,
    onCancelBookingRequested: (BookingEntity<*>, () -> Unit) -> Unit,
    onConfirmBookingRequested: (BookingEntity<*>, () -> Unit) -> Unit,
    onMarkAsTakenRequested: (BookingEntity<*>, meta: Map<String, Any>, () -> Unit) -> Unit,
    onMarkAsReturnedRequested: (BookingEntity<*>, () -> Unit) -> Unit,
    spaces: List<Space>?,
    isCreatingSpace: Boolean,
    onSpaceOperation: (Space, onCreate: () -> Unit) -> Unit,
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

        // ItemsCard(items, itemTypes, isCreatingItem, onItemOperation, allItemBookings)

        BookingsCard(
            allItemBookings,
            items,
            itemTypes,
            allSpaceBookings,
            spaces,
            isUpdatingBooking,
            onCancelBookingRequested,
            onConfirmBookingRequested,
            onMarkAsTakenRequested,
            onMarkAsReturnedRequested
        )

        SpacesCard(spaces, isCreatingSpace, onSpaceOperation)
    }
}
