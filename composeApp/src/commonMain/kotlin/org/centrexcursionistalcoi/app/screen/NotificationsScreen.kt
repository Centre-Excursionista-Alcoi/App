package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformScaffold
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.route.NotificationsRoute
import org.centrexcursionistalcoi.app.viewmodel.NotificationsViewModel
import org.jetbrains.compose.resources.stringResource

object NotificationsScreen : Screen<NotificationsRoute, NotificationsViewModel>(::NotificationsViewModel) {
    @Composable
    override fun Content(viewModel: NotificationsViewModel) {
        val navigator = LocalNavController.current

        val notifications by viewModel.notifications.collectAsState()

        PlatformScaffold(
            title = stringResource(Res.string.notifications_title),
            onBack = navigator::navigateUp
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(notifications) { notification ->
                    val route = remember(notification) { notification.route() }

                    PlatformCard(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .padding(12.dp)
                            .clickable { navigator.navigate(route) }
                    ) {
                        BasicText(
                            text = notification.title(),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
                            style = getPlatformTextStyles().heading
                        )
                        BasicText(
                            text = notification.message(),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                            style = getPlatformTextStyles().body
                        )
                    }
                }
            }
        }
    }
}
