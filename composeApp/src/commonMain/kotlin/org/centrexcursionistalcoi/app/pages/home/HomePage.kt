package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.health
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePage(
    itemsList: List<ItemD>?,
    itemTypes: List<ItemTypeD>?
) {
    AnimatedContent(
        targetState = itemsList to itemTypes,
        modifier = Modifier.fillMaxWidth()
    ) { (items, types) ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (items == null || types == null) item(key = "loading") {
                PlatformLoadingIndicator(large = false)
            } else {
                item(key = "available-items") {
                    PlatformCard(
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        BasicText(
                            text = items.size.toString(),
                            style = getPlatformTextStyles().titleRegular.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                        BasicText(
                            text = stringResource(Res.string.home_available_items),
                            style = getPlatformTextStyles().heading.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    }
                }
                items(
                    items.map { item ->
                        item to types.find { it.id == item.typeId }
                    }
                ) { (item, type) ->
                    if (type == null) return@items
                    PlatformCard {
                        BasicText(
                            text = type.title,
                            style = getPlatformTextStyles().heading
                                .copy(textAlign = TextAlign.Center, fontSize = 20.sp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).padding(horizontal = 8.dp)
                        )
                        BasicText(
                            text = stringResource(item.health()),
                            style = getPlatformTextStyles().heading
                                .copy(textAlign = TextAlign.Center, fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
