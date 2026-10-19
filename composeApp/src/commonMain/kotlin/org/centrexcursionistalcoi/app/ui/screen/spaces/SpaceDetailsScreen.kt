package org.centrexcursionistalcoi.app.ui.screen.spaces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.spaces_beds_line
import cea_app.composeapp.generated.resources.spaces_book
import cea_app.composeapp.generated.resources.spaces_book_closed
import cea_app.composeapp.generated.resources.spaces_capacity
import cea_app.composeapp.generated.resources.spaces_climate
import cea_app.composeapp.generated.resources.spaces_closed
import cea_app.composeapp.generated.resources.spaces_closed_reason
import cea_app.composeapp.generated.resources.spaces_conditions
import cea_app.composeapp.generated.resources.spaces_energy
import cea_app.composeapp.generated.resources.spaces_key_line
import cea_app.composeapp.generated.resources.spaces_keys
import cea_app.composeapp.generated.resources.spaces_occupied
import cea_app.composeapp.generated.resources.spaces_occupied_none
import cea_app.composeapp.generated.resources.spaces_occupied_range
import cea_app.composeapp.generated.resources.spaces_price_free
import cea_app.composeapp.generated.resources.spaces_price_line
import cea_app.composeapp.generated.resources.spaces_prices
import cea_app.composeapp.generated.resources.spaces_seats_line
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.PriceUnit
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.data.SpaceClimateControl
import org.centrexcursionistalcoi.app.data.SpaceEnergySource
import org.centrexcursionistalcoi.app.data.SpaceKeyType
import org.centrexcursionistalcoi.app.data.SpaceKeyTypeSpace
import org.centrexcursionistalcoi.app.data.SpaceOccupancy
import org.centrexcursionistalcoi.app.data.rememberImageFiles
import org.centrexcursionistalcoi.app.ui.components.IconWithBackground
import org.centrexcursionistalcoi.app.ui.components.InfoChip
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Altitude
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Hotel
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Person
import org.centrexcursionistalcoi.app.ui.preview.PreviewImagePattern
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.centrexcursionistalcoi.app.viewmodel.spaces.SpaceDetailsViewModel
import org.centrexcursionistalcoi.app.viewmodel.spaces.SpaceKeyOption
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

@Composable
fun SpaceDetailsScreen(
    spaceId: Uuid,
    onBook: () -> Unit,
    onBack: () -> Unit,
    model: SpaceDetailsViewModel = koinViewModel { parametersOf(spaceId) },
) {
    val space by model.space.collectAsState()
    val keys by model.keys.collectAsState()
    val occupancy by model.occupancy.collectAsState()
    val images = space.rememberImageFiles()

    SpaceDetailsScreen(
        space = space,
        keys = keys,
        occupancy = occupancy,
        images = images,
        onBook = onBook,
        onBack = onBack,
    )
}

/**
 * @param space The space to show, or `null` while loading.
 * @param keys The keys a lending of the space can take, or `null` while loading.
 * @param occupancy The nights that are taken, or `null` while loading.
 * @param images The contents of the space's photos by id: `null` while loading, empty if it couldn't be loaded. Passed
 * in (see `rememberImageFiles`) so the screen doesn't depend on Koin or the network, which previews can't have.
 */
@Composable
fun SpaceDetailsScreen(
    space: Space?,
    keys: List<SpaceKeyOption>?,
    occupancy: List<SpaceOccupancy>?,
    images: Map<Uuid, ByteArray?>,
    onBook: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(space?.name.orEmpty()) },
                navigationIcon = {
                    BackButton(onBack)
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { paddingValues ->
        if (space == null) {
            LoadingBox()
            return@Scaffold
        }

        LazyColumnWidthWrapper(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clip(RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp)),
            contentPadding = PaddingValues(8.dp)
        ) {
            item("header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(6f/3)
                        .clip(RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp))
                ) {
                    images.values.firstOrNull()?.let { bytes ->
                        AsyncByteImage(
                            bytes,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        )
                    )
                    if (space.altitude != null) {
                        InfoChip(
                            text = "${space.altitude}m",
                            icon = MaterialSymbols.Altitude,
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        )
                    }
                    Text(
                        text = space.name,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    )
                }
            }
            item("specs") { SpaceSpecs(space) }

            item("description") {
                Text(space.description, modifier = Modifier.padding(vertical = 8.dp))
            }
            if (space.photos.isNotEmpty()) {
                item("photos") { SpacePhotos(space, images) }
            }
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
                items(keys, key = { "key_${it.type.id}" }) { key ->
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
private fun SpacePhotos(space: Space, images: Map<Uuid, ByteArray?>) {
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
        val items = buildList {
            space.seats?.let { seats ->
                add(Triple(
                    MaterialSymbols.Person,
                    stringResource(Res.string.spaces_capacity),
                    stringResource(Res.string.spaces_seats_line, seats)
                ))
            }
            space.beds?.let { beds ->
                add(Triple(
                    MaterialSymbols.Hotel,
                    stringResource(Res.string.spaces_capacity),
                    stringResource(Res.string.spaces_beds_line, beds)
                ))
            }
            space.energySources.forEach { source ->
                add(Triple(
                    source.icon(),
                    stringResource(Res.string.spaces_energy),
                    source.label()
                ))
            }
            space.climateControl.forEach { climate ->
                add(Triple(
                    climate.icon(),
                    stringResource(Res.string.spaces_climate),
                    climate.label()
                ))
            }
        }

        items.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for ((icon, title, text) in row) {
                    SpecTile(icon, title, text, Modifier.weight(1f).padding(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SpecTile(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row {
            IconWithBackground(
                icon,
                title,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SpecTile_Preview() {
    SpecTile(MaterialSymbols.Hotel, "Capacitat", "16 llits (lliteres)")
}

private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

private val previewSpaceId = Uuid.parse("00000000-0000-0000-0000-000000000001")

private val previewPhotoIds = listOf(
    Uuid.parse("00000000-0000-0000-0000-000000000010"),
    Uuid.parse("00000000-0000-0000-0000-000000000011"),
    Uuid.parse("00000000-0000-0000-0000-000000000012"),
)

private val previewSpace = Space(
    id = previewSpaceId,
    name = "Refugi de la Serra",
    description = "Refugi de muntanya amb cuina equipada i sala comuna.",
    conditionsOfUse = "Deixeu el refugi net i recolliu les escombraries.",
    requiresKeys = true,
    prices = listOf(
        CategoryPrice(Category.MEMBER, 8.0, PriceUnit.PER_NIGHT),
        CategoryPrice(Category.NON_MEMBER, 12.5, PriceUnit.PER_NIGHT),
    ),
    seats = 20,
    beds = 12,
    energySources = listOf(SpaceEnergySource.SOLAR, SpaceEnergySource.GAS_GENERATOR),
    climateControl = listOf(SpaceClimateControl.WOOD_STOVE),
    photos = previewPhotoIds.map { FileWithContext(id = it) },
    address = "Camí de la Serra, Alcoi",
    latitude = 38.7,
    longitude = -0.47,
    altitude = 1150.0,
    isClosed = false,
    closedSince = null,
    closedUntil = null,
    closedReason = null,
)

private val previewKeys = listOf(
    SpaceKeyOption(
        type = SpaceKeyType(
            id = Uuid.parse("00000000-0000-0000-0000-000000000002"),
            name = "Clau principal",
            description = null,
            spaces = listOf(SpaceKeyTypeSpace(previewSpaceId, 2)),
        ),
        maxPerLending = 2,
    ),
)

private val previewOccupancy = listOf(
    SpaceOccupancy(LocalDate(2026, 11, 7), LocalDate(2026, 11, 9)),
    SpaceOccupancy(LocalDate(2026, 11, 21), LocalDate(2026, 11, 22)),
)

/** A loaded photo, one still loading (`null`) and one that couldn't be loaded (empty). */
private val previewImages: Map<Uuid, ByteArray?> by lazy {
    mapOf(
        previewPhotoIds[0] to PreviewImagePattern,
        previewPhotoIds[1] to null,
        previewPhotoIds[2] to ByteArray(0),
    )
}

@Preview
@Composable
private fun SpaceDetailsScreen_Preview() {
    AppTheme {
        SpaceDetailsScreen(
            space = previewSpace,
            keys = previewKeys,
            occupancy = previewOccupancy,
            images = previewImages,
            onBook = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SpaceDetailsScreen_Closed_Preview() {
    AppTheme {
        SpaceDetailsScreen(
            space = previewSpace.copy(isClosed = true, closedReason = "Obres a la teulada"),
            keys = emptyList(),
            occupancy = emptyList(),
            images = emptyMap(),
            onBook = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SpaceDetailsScreen_Loading_Preview() {
    AppTheme {
        SpaceDetailsScreen(space = null, keys = null, occupancy = null, images = emptyMap(), onBook = {}, onBack = {})
    }
}
