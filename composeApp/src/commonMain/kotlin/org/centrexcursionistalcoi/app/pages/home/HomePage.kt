package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.database.entity.ItemBooking
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.maxGridItemSpan
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.route.Reservation
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomePage(
    itemBookings: List<ItemBooking>?,
    spaceBookings: List<SpaceBooking>?,
    spaces: List<Space>?
) {
    val navigator = LocalNavController.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(200.dp),
        modifier = Modifier.fillMaxSize().padding(8.dp)
    ) {
        if (itemBookings != null && spaceBookings != null) {
            val incompleteItemBookings = itemBookings.filter { it.returnedAt == null }
            val completeItemBookings = itemBookings.filter { it.returnedAt != null }

            val incompleteSpaceBookings = spaceBookings.filter { it.returnedAt == null }
            val completeSpaceBookings = spaceBookings.filter { it.returnedAt != null }

            if (itemBookings.isEmpty() && spaceBookings.isEmpty()) {
                item(
                    key = "no-bookings",
                    span = maxGridItemSpan
                ) {
                    AppText(
                        text = stringResource(Res.string.bookings_no_bookings),
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        style = getPlatformTextStyles().titleRegular.copy(
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            if (incompleteItemBookings.isNotEmpty() || incompleteSpaceBookings.isNotEmpty()) {
                item(
                    key = "incomplete-header",
                    span = maxGridItemSpan
                ) {
                    AppText(
                        text = stringResource(Res.string.bookings_your),
                        modifier = Modifier.fillMaxWidth(),
                        style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp)
                    )
                }
                items(incompleteItemBookings) { booking ->
                    PlatformCard(
                        title = stringResource(Res.string.bookings_items_count, booking.itemIds?.size ?: 0),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                navigator.navigate(Reservation(booking.id, null))
                            }
                    ) {
                        if (!booking.confirmed) {
                            AppText(
                                text = stringResource(Res.string.bookings_pending_confirmation),
                                modifier = Modifier.padding(8.dp)
                            )
                        } else if (booking.takenAt == null) {
                            AppText(
                                text = stringResource(Res.string.bookings_not_taken),
                                modifier = Modifier.padding(8.dp)
                            )
                        } else if (booking.returnedAt == null) {
                            AppText(
                                text = stringResource(Res.string.bookings_pending_return),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        AppText(
                            text = stringResource(Res.string.bookings_from, booking.from.toString()),
                            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp)
                        )
                        AppText(
                            text = stringResource(Res.string.bookings_to, booking.to.toString()),
                            modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
                        )
                    }
                }
                items(
                    items = incompleteSpaceBookings.map { booking -> booking to spaces?.find { it.id == booking.spaceId } }
                ) { (booking, space) ->
                    space ?: return@items

                    PlatformCard(
                        title = space.name,
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                navigator.navigate(Reservation(null, booking.id))
                            }
                    ) {
                        if (!booking.confirmed) {
                            AppText(
                                text = stringResource(Res.string.bookings_pending_confirmation),
                                modifier = Modifier.padding(8.dp)
                            )
                        } else if (booking.takenAt == null) {
                            AppText(
                                text = stringResource(Res.string.bookings_not_taken),
                                modifier = Modifier.padding(8.dp)
                            )
                        } else if (booking.returnedAt == null) {
                            AppText(
                                text = stringResource(Res.string.bookings_pending_return),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        AppText(
                            text = stringResource(Res.string.bookings_from, booking.from.toString()),
                            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp)
                        )
                        AppText(
                            text = stringResource(Res.string.bookings_to, booking.to.toString()),
                            modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
                        )
                    }
                }

                if (completeItemBookings.isNotEmpty() || completeSpaceBookings.isNotEmpty()) {
                    item(key = "middle-space", contentType = "spacer") { Spacer(Modifier.height(16.dp)) }
                }
            }

            if (completeItemBookings.isNotEmpty() || completeSpaceBookings.isNotEmpty()) {
                item(
                    key = "old-header",
                    span = maxGridItemSpan
                ) {
                    AppText(
                        text = stringResource(Res.string.bookings_old),
                        modifier = Modifier.fillMaxWidth(),
                        style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp)
                    )
                }
                items(completeItemBookings) { booking ->
                    PlatformCard(
                        title = stringResource(Res.string.bookings_items_count, booking.itemIds.size),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        AppText(
                            text = stringResource(Res.string.bookings_from, booking.from.toString()),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        AppText(
                            text = stringResource(Res.string.bookings_to, booking.to.toString()),
                            modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
                        )
                    }
                }
                items(
                    items = completeSpaceBookings.map { booking -> booking to spaces?.find { it.id == booking.spaceId } }
                ) { (booking, space) ->
                    space ?: return@items
                    PlatformCard(
                        title = space.name,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        AppText(
                            text = stringResource(Res.string.bookings_from, booking.from.toString()),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        AppText(
                            text = stringResource(Res.string.bookings_to, booking.to.toString()),
                            modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
