package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD
import org.jetbrains.compose.resources.stringResource

@Composable
fun DashboardPage(
    itemsList: List<ItemD>?,
    itemTypes: List<ItemTypeD>?
) {
    AnimatedContent(
        targetState = itemsList to itemTypes,
        modifier = Modifier.fillMaxWidth()
    ) { (items, types) ->
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (items == null || types == null) {
                PlatformLoadingIndicator(large = false)
            } else {
                PlatformCard {
                    BasicText(
                        text = items.sumOf { it.amount ?: 0 }.toString(),
                        style = getPlatformTextStyles().titleRegular.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                    BasicText(
                        text = stringResource(Res.string.dashboard_available_items),
                        style = getPlatformTextStyles().heading.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            }
        }
    }
}
