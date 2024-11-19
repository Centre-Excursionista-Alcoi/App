package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.fromDate
import org.centrexcursionistalcoi.app.data.toDate
import org.centrexcursionistalcoi.app.maxGridItemSpan
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomePage(bookings: List<ItemLendingD>?) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(200.dp),
        modifier = Modifier.fillMaxSize().padding(8.dp)
    ) {
        if (bookings != null) {
            val incompleteBookings = bookings.filter { it.returnedAt == null }
            val completeBookings = bookings.filter { it.returnedAt != null }

            if (incompleteBookings.isEmpty() && completeBookings.isEmpty()) {
                item(
                    key = "no-bookings",
                    span = maxGridItemSpan
                ) {
                    BasicText(
                        text = stringResource(Res.string.bookings_no_bookings),
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        style = getPlatformTextStyles().titleRegular.copy(fontSize = 18.sp, textAlign = TextAlign.Center)
                    )
                }
            }

            if (incompleteBookings.isNotEmpty()) {
                item(
                    key = "incomplete-header",
                    span = maxGridItemSpan
                ) {
                    BasicText(
                        text = stringResource(Res.string.bookings_your),
                        modifier = Modifier.fillMaxWidth(),
                        style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp)
                    )
                }
                items(incompleteBookings) { booking ->
                    PlatformCard(
                        title = stringResource(Res.string.bookings_items_count, booking.itemIds?.size ?: 0),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        if (!booking.confirmed) {
                            BasicText(
                                text = stringResource(Res.string.bookings_pending_confirmation),
                                modifier = Modifier.padding(8.dp)
                            )
                        } else if (booking.takenAt == null) {
                            BasicText(
                                text = stringResource(Res.string.bookings_not_taken),
                                modifier = Modifier.padding(8.dp)
                            )
                        } else if (booking.takenAt != null && booking.returnedAt == null) {
                            BasicText(
                                text = stringResource(Res.string.bookings_pending_return),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        BasicText(
                            text = stringResource(Res.string.bookings_from, booking.fromDate().toString()),
                            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp)
                        )
                        BasicText(
                            text = stringResource(Res.string.bookings_to, booking.toDate().toString()),
                            modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
                        )
                    }
                }

                if (completeBookings.isNotEmpty()) {
                    item(key = "middle-space", contentType = "spacer") { Spacer(Modifier.height(16.dp)) }
                }
            }

            if (completeBookings.isNotEmpty()) {
                item(
                    key = "old-header",
                    span = maxGridItemSpan
                ) {
                    BasicText(
                        text = stringResource(Res.string.bookings_old),
                        modifier = Modifier.fillMaxWidth(),
                        style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp)
                    )
                }
                items(completeBookings) { booking ->
                    PlatformCard(
                        title = stringResource(Res.string.bookings_items_count, booking.itemIds?.size ?: 0),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        BasicText(
                            text = stringResource(Res.string.bookings_from, booking.fromDate().toString()),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        BasicText(
                            text = stringResource(Res.string.bookings_to, booking.toDate().toString()),
                            modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
