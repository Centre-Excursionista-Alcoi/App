package org.centrexcursionistalcoi.app.pages.home.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.data.IBookingD
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformCheckbox
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.centrexcursionistalcoi.app.platform.ui.PlatformDropdown
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel.Companion.BOOKING_CONFIRM_META_SPACE_KEY
import org.jetbrains.compose.resources.stringResource

@Composable
fun BookingsCard(
    itemBookings: List<ItemLendingD>?,
    items: List<ItemD>?,
    itemTypes: List<ItemTypeD>?,
    spaceBookings: List<SpaceBookingD>?,
    spaces: List<SpaceD>?,
    isUpdatingBooking: Boolean,
    onCancelBookingRequested: (IBookingD, () -> Unit) -> Unit,
    onConfirmBookingRequested: (IBookingD, () -> Unit) -> Unit,
    onMarkAsTakenRequested: (IBookingD, meta: Map<String, Any>, () -> Unit) -> Unit,
    onMarkAsReturnedRequested: (IBookingD, () -> Unit) -> Unit
) {
    var bookingDialog by remember { mutableStateOf<IBookingD?>(null) }
    bookingDialog?.let { booking ->
        PlatformDialog(
            onDismissRequest = { bookingDialog = null },
            title = stringResource(if (booking.confirmed) Res.string.bookings_info else Res.string.bookings_confirm),
            actions = {
                if (booking.takenAt == null) {
                    DestructiveButton(
                        text = stringResource(Res.string.cancel),
                        enabled = !isUpdatingBooking
                    ) {
                        onCancelBookingRequested(booking) { bookingDialog = null }
                    }
                }

                if (!booking.confirmed) {
                    PositiveButton(
                        text = stringResource(Res.string.confirm),
                        enabled = !isUpdatingBooking
                    ) {
                        onConfirmBookingRequested(booking) { bookingDialog = null }
                    }
                } else if (booking.takenAt == null) {
                    var isEnabled by remember { mutableStateOf(true) }
                    val meta by remember { mutableStateOf(mutableMapOf<String, Any>()) }

                    if (booking is SpaceBookingD) {
                        val space = spaces?.find { it.id == booking.spaceId }
                        val keys = space?.keys?.takeIf { it.isNotEmpty() }
                        if (keys != null) {
                            LaunchedEffect(Unit) {
                                isEnabled = false
                            }

                            PlatformDropdown(
                                value = meta[BOOKING_CONFIRM_META_SPACE_KEY] as Int?,
                                onValueChange = {
                                    meta[BOOKING_CONFIRM_META_SPACE_KEY] = it
                                    isEnabled = meta.containsKey(BOOKING_CONFIRM_META_SPACE_KEY)
                                },
                                options = keys.mapNotNull { it.id },
                                label = stringResource(Res.string.spaces_key),
                                toString = { id -> keys.find { it.id == id }?.name ?: "" },
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                        }
                    }

                    PositiveButton(
                        text = stringResource(Res.string.bookings_mark_taken),
                        enabled = !isUpdatingBooking && isEnabled
                    ) {
                        onMarkAsTakenRequested(booking, meta) { bookingDialog = null }
                    }
                } else if (booking.returnedAt == null) {
                    PositiveButton(
                        text = stringResource(Res.string.bookings_mark_returned),
                        enabled = !isUpdatingBooking
                    ) {
                        onMarkAsReturnedRequested(booking) { bookingDialog = null }
                    }
                }
            }
        ) {
            AppText(
                text = stringResource(Res.string.bookings_made_by, booking.userId ?: "N/A"),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            AppText(
                text = stringResource(Res.string.bookings_from, booking.from.toString()),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            AppText(
                text = stringResource(Res.string.bookings_to, booking.to.toString()),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)
            )

            if (booking.confirmed) {
                if (booking.takenAt == null) {
                    AppText(
                        text = stringResource(Res.string.bookings_not_taken),
                        style = getPlatformTextStyles().heading,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                } else {
                    AppText(
                        text = stringResource(Res.string.bookings_taken_at, booking.takenAt.toString()),
                        style = getPlatformTextStyles().heading,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )

                    if (booking is SpaceBookingD) {
                        val space = spaces?.find { it.id == booking.spaceId }
                        val key = space?.keys?.find { it.id == booking.keyId }
                        if (space != null && key != null) {
                            AppText(
                                text = stringResource(Res.string.bookings_taken_key, key.name),
                                style = getPlatformTextStyles().heading,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                        }
                    }

                    if (booking.returnedAt == null) {
                        AppText(
                            text = stringResource(Res.string.bookings_not_returned),
                            style = getPlatformTextStyles().heading,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    } else {
                        AppText(
                            text = stringResource(Res.string.bookings_returned_at, booking.returnedAt.toString()),
                            style = getPlatformTextStyles().heading,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            if (booking is ItemLendingD) {
                AppText(
                    text = stringResource(Res.string.bookings_items),
                    style = getPlatformTextStyles().heading,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
                )
                val itemsAndTypes = remember(booking.itemIds) {
                    booking.itemIds
                        ?.mapNotNull { itemId -> items?.find { it.id == itemId } }
                        ?.associateWith { item -> itemTypes?.find { it.id == item.typeId }!! }
                        ?: emptyMap()
                }
                for ((item, type) in itemsAndTypes) {
                    AppText(
                        text = "· ${type.title} (#${item.id})",
                        style = getPlatformTextStyles().heading,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
                    )
                }
            } else if (booking is SpaceBookingD) {
                AppText(
                    text = spaces?.find { it.id == booking.spaceId }?.name ?: "N/A",
                    style = getPlatformTextStyles().heading,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
                )
            }
        }
    }

    var hideComplete by remember { mutableStateOf(true) }

    PlatformCard(
        title = stringResource(Res.string.bookings_list),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        AnimatedContent(
            targetState = itemBookings to spaceBookings,
            modifier = Modifier.fillMaxWidth()
        ) { (bookedItems, bookedSpaces) ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (bookedItems == null || bookedSpaces == null) {
                    PlatformLoadingIndicator(large = false)
                } else if (bookedItems.isEmpty() && bookedSpaces.isEmpty()) {
                    AppText(
                        text = stringResource(Res.string.bookings_empty),
                        style = getPlatformTextStyles().label.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                } else {
                    PlatformCheckbox(
                        checked = hideComplete,
                        onCheckedChanged = { hideComplete = it },
                        label = stringResource(Res.string.bookings_hide_complete),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )

                    val filteredItemsList = bookedItems.filter { booking ->
                        if (hideComplete) {
                            booking.returnedAt == null
                        } else {
                            true
                        }
                    }
                    for (item in filteredItemsList) {
                        BookingCard(
                            booking = item,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            supportingContent = {
                                AppText(
                                    text = "${item.itemIds?.size} items",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        ) { bookingDialog = item }
                    }

                    val filteredSpacesList = bookedSpaces.filter { booking ->
                        if (hideComplete) {
                            booking.returnedAt == null
                        } else {
                            true
                        }
                    }
                    for (booking in filteredSpacesList) {
                        val space = spaces?.find { it.id == booking.spaceId } ?: continue

                        BookingCard(
                            booking = booking,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            supportingContent = {
                                AppText(
                                    text = space.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        ) { bookingDialog = booking }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: IBookingD,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    PlatformCard(
        modifier = modifier.clickable { onClick() }
    ) {
        AppText(
            text = booking.userId ?: "N/A",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp),
            style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
        )
        supportingContent?.invoke()

        if (!booking.confirmed) {
            AppText(
                text = stringResource(Res.string.bookings_pending),
                modifier = Modifier.padding(8.dp)
            )
        } else if (booking.takenAt == null) {
            AppText(
                text = stringResource(Res.string.bookings_not_taken),
                modifier = Modifier.padding(8.dp)
            )
        } else if (booking.returnedAt == null) {
            AppText(
                text = stringResource(Res.string.bookings_not_returned),
                modifier = Modifier.padding(8.dp)
            )
        } else {
            AppText(
                text = stringResource(Res.string.bookings_returned),
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
