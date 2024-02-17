package ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.waiting_message
import app.composeapp.generated.resources.waiting_retry
import app.composeapp.generated.resources.waiting_title
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.stringResource
import screenmodel.ConfirmationScreenModel
import ui.state.ConfirmationStatusWatcher
import ui.state.SessionStatusWatcher

class WaitingConfirmationScreen : BaseScreen() {
    @Composable
    override fun ScreenContent() {
        val navigator = LocalNavigator.currentOrThrow

        val model = rememberScreenModel { ConfirmationScreenModel() }

        SessionStatusWatcher()
        ConfirmationStatusWatcher {
            if (it == null) navigator.push(LoadingScreen())
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            OutlinedCard(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.waiting_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(Res.string.waiting_message),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp, top = 8.dp)
                )
                TextButton(
                    onClick = model::verify,
                    modifier = Modifier.align(Alignment.End).padding(end = 12.dp, bottom = 16.dp)
                ) {
                    Text(stringResource(Res.string.waiting_retry))
                }
            }
        }
    }
}
