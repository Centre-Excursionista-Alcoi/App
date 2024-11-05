package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.centrexcursionistalcoi.app.composition.AccountStateNavigator
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.route.Home
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel

object HomeScreen: Screen<Home, HomeViewModel>(::HomeViewModel) {
    @Composable
    override fun Content(viewModel: HomeViewModel) {
        AccountStateNavigator(onLoggedOut = Loading)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            // TODO: Fetch user data from UserDataEndpoint (/me)
            BasicText(
                text = "Welcome, ${viewModel.user.name}",
            )
            PlatformButton("Logout") {
                viewModel.logout()
            }
        }
    }
}
