package org.centrexcursionistalcoi.app.ui.screen.spaces

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.data.rememberImageFiles
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ArrowBack
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.spaces.SpaceDetailsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SpaceDetailsScreen(
    spaceId: Uuid,
    onBook: () -> Unit,
    onBack: () -> Unit,
    model: SpaceDetailsViewModel = koinViewModel { parametersOf(spaceId) },
) {
    val space by model.space.collectAsState()
    val keys by model.keys.collectAsState()
    val occupancy by model.occupancy.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(space?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MaterialSymbols.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { paddingValues ->
        val space = space
        if (space == null) {
            LoadingBox()
            return@Scaffold
        }
        LazyColumnWidthWrapper(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            item("description") {
                Text(space.description, modifier = Modifier.padding(vertical = 8.dp))
            }
            if (space.photos.isNotEmpty()) {
                item("photos") { SpacePhotos(space) }
            }
            item("specs") { SpaceSpecs(space) }
            if (space.isClosed) {
                item("closed") {
                    Text(
                        text = space.closedReason?.let { stringResource(Res.string.spaces_closed_reason, it) }
                            ?: stringResource(Res.string.spaces_closed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            item("prices_title") {
                Text(stringResource(Res.string.spaces_prices), style = MaterialTheme.typography.titleMedium)
            }
            if (space.prices.isEmpty()) {
                item("prices_free") { Text(stringResource(Res.string.spaces_price_free)) }
            }
            items(space.prices, key = { "price_${it.category}" }) { price ->
                Text(
                    stringResource(
                        Res.string.spaces_price_line,
                        price.category.label(),
                        formatPrice(price.price),
                        price.unit.label(),
                    )
                )
            }
            if (!keys.isNullOrEmpty()) {
                item("keys_title") {
                    Text(
                        stringResource(Res.string.spaces_keys),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                items(keys.orEmpty(), key = { "key_${it.type.id}" }) { key ->
                    Text(stringResource(Res.string.spaces_key_line, key.type.name, key.maxPerLending))
                }
            }
            space.conditionsOfUse?.let { conditions ->
                item("conditions") {
                    Text(
                        stringResource(Res.string.spaces_conditions),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(conditions)
                }
            }
            item("occupancy_title") {
                Text(
                    stringResource(Res.string.spaces_occupied),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            if (occupancy.isNullOrEmpty()) {
                item("occupancy_none") { Text(stringResource(Res.string.spaces_occupied_none)) }
            }
            items(occupancy.orEmpty(), key = { "occupied_${it.checkIn}_${it.checkOut}" }) { range ->
                Text(stringResource(Res.string.spaces_occupied_range, range.checkIn.formatted(), range.checkOut.formatted()))
            }
            item("book") {
                Button(
                    onClick = onBook,
                    enabled = !(space.isClosed && space.closedUntil == null),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                ) {
                    Text(stringResource(if (space.isClosed && space.closedUntil == null) Res.string.spaces_book_closed else Res.string.spaces_book))
                }
            }
        }
    }
}

/** The photos of the inside of the space, side by side. Tapping one maximizes it. */
@Composable
private fun SpacePhotos(space: Space) {
    val images = space.rememberImageFiles()
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        items(space.images, key = { "photo_$it" }) { id ->
            AsyncByteImage(
                bytes = images[id],
                contentDescription = space.name,
                contentScale = ContentScale.Crop,
                canBeMaximized = true,
                modifier = Modifier.height(180.dp).width(240.dp).clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

/** Capacity, energy, climate control and location of the space. Shows only what is set. */
@Composable
private fun SpaceSpecs(space: Space) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val hasCapacity = space.seats != null || space.beds != null
        if (hasCapacity) {
            Text(stringResource(Res.string.spaces_capacity), style = MaterialTheme.typography.titleMedium)
            space.seats?.let { Text(stringResource(Res.string.spaces_seats_line, it)) }
            space.beds?.let { Text(stringResource(Res.string.spaces_beds_line, it)) }
        }
        if (space.energySources.isNotEmpty()) {
            Text(
                stringResource(Res.string.spaces_energy),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = if (hasCapacity) 8.dp else 0.dp),
            )
            Text(space.energySources.map { it.label() }.joinToString(", "))
        }
        if (space.climateControl.isNotEmpty()) {
            Text(
                stringResource(Res.string.spaces_climate),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(space.climateControl.map { it.label() }.joinToString(", "))
        }
        val latitude = space.latitude
        val longitude = space.longitude
        if (space.address != null || latitude != null || space.altitude != null) {
            Text(
                stringResource(Res.string.spaces_location),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            space.address?.let { Text(it) }
            if (latitude != null && longitude != null) Text(stringResource(Res.string.spaces_coordinates_line, latitude, longitude))
            space.altitude?.let { Text(stringResource(Res.string.spaces_altitude_line, formatNumber(it))) }
        }
    }
}

private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
