package ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.inventory_item_images
import app.composeapp.generated.resources.inventory_item_images_none
import app.composeapp.generated.resources.inventory_item_stock
import app.composeapp.generated.resources.inventory_item_stock_available
import app.composeapp.generated.resources.inventory_item_stock_in_use
import app.composeapp.generated.resources.inventory_item_stock_reserved
import backend.StockManagement
import backend.data.database.InventoryItem
import cafe.adriel.voyager.core.model.rememberScreenModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import org.jetbrains.compose.resources.stringResource
import screenmodel.InventoryItemScreenModel
import ui.reusable.layout.TightColumn

class InventoryItemScreen(
    private val item: InventoryItem
) : BaseScreen(
    title = { item.displayName },
    canGoBack = true
) {
    @Composable
    override fun ScreenContent() {
        val model = rememberScreenModel { InventoryItemScreenModel() }

        TightColumn(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            StockCard(model)
            ImagesCard(model)
        }
    }

    @Composable
    private fun DataCard(title: String, content: @Composable ColumnScope.() -> Unit) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 4.dp)
            )
            content()
        }
    }

    @Composable
    fun StockCard(model: InventoryItemScreenModel) {
        val availableStock by StockManagement.availableStock.collectAsState()

        LaunchedEffect(Unit) {
            model.loadAvailableStock()
        }

        DataCard(
            title = stringResource(Res.string.inventory_item_stock)
        ) {
            val stock = availableStock[item]
            Text(
                text = stringResource(
                    Res.string.inventory_item_stock_available,
                    stock?.available?.toInt() ?: 10u
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .placeholder(
                        visible = stock == null,
                        highlight = PlaceholderHighlight.shimmer()
                    )
            )
            Text(
                text = stringResource(
                    Res.string.inventory_item_stock_in_use,
                    stock?.inUse?.toInt() ?: 10u
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .placeholder(
                        visible = stock == null,
                        highlight = PlaceholderHighlight.shimmer()
                    )
            )
            Text(
                text = stringResource(
                    Res.string.inventory_item_stock_reserved,
                    stock?.reserved?.toInt() ?: 10u
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .placeholder(
                        visible = stock == null,
                        highlight = PlaceholderHighlight.shimmer()
                    )
            )
        }
    }

    @Composable
    private fun ImagesCard(model: InventoryItemScreenModel) {
        val images by model.images.collectAsState(emptyMap())

        DataCard(
            title = stringResource(Res.string.inventory_item_images)
        ) {
            item.images?.let {
                LazyRow(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp)
                ) {
                    items(item.images) { imagePath ->
                        LaunchedEffect(Unit) {
                            model.loadImage(item, imagePath)
                        }
                        val image = images[item]?.get(imagePath)
                        if (image != null) {
                            Image(
                                bitmap = image,
                                contentDescription = item.displayName,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.inventory_item_images_none),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
