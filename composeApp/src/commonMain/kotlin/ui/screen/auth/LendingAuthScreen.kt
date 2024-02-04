package ui.screen.auth

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.compose.stringResource
import resources.MR
import ui.screen.BaseScreen

class LendingAuthScreen : BaseScreen({ stringResource(MR.strings.lending_auth_title) }, true) {
    @Composable
    override fun ScreenContent() {
        Scaffold { paddingValues ->

        }
    }
}
