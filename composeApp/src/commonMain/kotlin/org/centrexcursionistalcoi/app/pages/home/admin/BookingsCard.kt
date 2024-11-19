package org.centrexcursionistalcoi.app.pages.home.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import org.centrexcursionistalcoi.app.data.IBookingD
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.data.fromDate
import org.centrexcursionistalcoi.app.data.returnedDate
import org.centrexcursionistalcoi.app.data.takenDate
import org.centrexcursionistalcoi.app.data.toDate
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformCheckbox
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.jetbrains.compose.resources.stringResource

@Composable
fun BookingsCard(
    itemBookings: List<ItemLendingD>?,
    items: List<ItemD>?,
    itemTypes: List<ItemTypeD>?,
    spaceBookings: List<SpaceBookingD>?,
    spaces: List<SpaceD>?,
    isUpdatingBooking: Boolean,
    onConfirmBookingRequested: (IBookingD, () -> Unit) -> Unit,
    onMarkAsTakenRequested: (IBookingD, () -> Unit) -> Unit,
    onMarkAsReturnedRequested: (IBookingD, () -> Unit) -> Unit
) {
    var confirmBooking by remember { mutableStateOf<IBookingD?>(null) }
    confirmBooking?.let { booking ->
        PlatformDialog(
            onDismissRequest = { confirmBooking = null }
        ) {
            BasicText(
                text = stringResource(if (booking.confirmed) Res.string.bookings_info else Res.string.bookings_confirm),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            BasicText(
                text = stringResource(Res.string.bookings_made_by, booking.userId ?: "N/A"),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            BasicText(
                text = stringResource(Res.string.bookings_from, booking.fromDate().toString()),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            BasicText(
                text = stringResource(Res.string.bookings_to, booking.toDate().toString()),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)
            )

            if (booking.confirmed) {
                if (booking.takenAt == null) {
                    BasicText(
                        text = stringResource(Res.string.bookings_not_taken),
                        style = getPlatformTextStyles().heading,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                } else {
                    BasicText(
                        text = stringResource(Res.string.bookings_taken_at, booking.takenDate().toString()),
                        style = getPlatformTextStyles().heading,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                    if (booking.returnedAt == null) {
                        BasicText(
                            text = stringResource(Res.string.bookings_not_returned),
                            style = getPlatformTextStyles().heading,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    } else {
                        BasicText(
                            text = stringResource(Res.string.bookings_returned_at, booking.returnedDate().toString()),
                            style = getPlatformTextStyles().heading,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            if (booking is ItemLendingD) {
                BasicText(
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
                    BasicText(
                        text = "· ${type.title} (#${item.id})",
                        style = getPlatformTextStyles().heading,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
                    )
                }
            } else if (booking is SpaceBookingD) {
                BasicText(
                    text = spaces?.find { it.id == booking.spaceId }?.name ?: "N/A",
                    style = getPlatformTextStyles().heading,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
                )
            }

            if (!booking.confirmed) {
                PlatformButton(
                    text = stringResource(Res.string.confirm),
                    modifier = Modifier.align(Alignment.End).padding(8.dp).padding(top = 8.dp),
                    enabled = !isUpdatingBooking
                ) {
                    onConfirmBookingRequested(booking) { confirmBooking = null }
                }
            } else if (booking.takenAt == null) {
                PlatformButton(
                    text = stringResource(Res.string.bookings_mark_taken),
                    modifier = Modifier.align(Alignment.End).padding(8.dp).padding(top = 8.dp),
                    enabled = !isUpdatingBooking
                ) {
                    onMarkAsTakenRequested(booking) { confirmBooking = null }
                }
            } else if (booking.returnedAt == null) {
                PlatformButton(
                    text = stringResource(Res.string.bookings_mark_returned),
                    modifier = Modifier.align(Alignment.End).padding(8.dp).padding(top = 8.dp),
                    enabled = !isUpdatingBooking
                ) {
                    onMarkAsReturnedRequested(booking) { confirmBooking = null }
                }
            } else {
                Spacer(Modifier.height(8.dp))
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
                    BasicText(
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
                                BasicText(
                                    text = "${item.itemIds?.size} items",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        ) { confirmBooking = item }
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
                                BasicText(
                                    text = space.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        ) { confirmBooking = booking }
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
        BasicText(
            text = booking.userId ?: "N/A",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp),
            style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
        )
        supportingContent?.invoke()

        if (!booking.confirmed) {
            BasicText(
                text = stringResource(Res.string.bookings_pending),
                modifier = Modifier.padding(8.dp)
            )
        } else if (booking.takenAt == null) {
            BasicText(
                text = stringResource(Res.string.bookings_not_taken),
                modifier = Modifier.padding(8.dp)
            )
        } else if (booking.returnedAt == null) {
            BasicText(
                text = stringResource(Res.string.bookings_not_returned),
                modifier = Modifier.padding(8.dp)
            )
        } else {
            BasicText(
                text = stringResource(Res.string.bookings_returned),
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
