package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.ui.reusable.CardWithIcon
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.LoadingViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadingScreen(
    onLoggedIn: () -> Unit,
    onNotLoggedIn: () -> Unit
) {
    val vm = viewModel { LoadingViewModel(onLoggedIn, onNotLoggedIn) }

    val error by vm.error.collectAsState()
    val progress by vm.progress.collectAsState()

    LoadingScreen(error, progress, stringResource(Res.string.loading_screen_error))
}

@Composable
fun LoadingScreen(
    error: Throwable?,
    progress: Progress?,
    errorTitle: String,
    errorMessageConverter: @Composable (Throwable) -> String = { it.message ?: stringResource(Res.string.error_unknown, it::class.simpleName!!) },
) {
    Scaffold { paddingValues ->
        AnimatedContent(
            targetState = error to progress,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
        ) { (err, pro) ->
            if (err != null) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    CardWithIcon(
                        title = errorTitle,
                        message = errorMessageConverter(err),
                        icon = Icons.Default.Error,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    )
                }
            } else {
                LoadingBox(paddingValues, pro)
            }
        }
    }
}
