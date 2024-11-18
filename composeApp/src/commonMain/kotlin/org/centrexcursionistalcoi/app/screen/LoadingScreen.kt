package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.viewmodel.LoadingViewModel
import org.jetbrains.compose.resources.stringResource

object LoadingScreen : Screen<Loading, LoadingViewModel>(vmConstructor = ::LoadingViewModel) {
    @Composable
    override fun Content(viewModel: LoadingViewModel) {
        val navController = LocalNavController.current

        val serverAvailable by viewModel.serverAvailable.collectAsState()
        val serverError by viewModel.serverError.collectAsState()

        LaunchedEffect(viewModel) {
            viewModel.load(navController)
        }

        if (serverAvailable != false) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                PlatformLoadingIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val textStyle = getPlatformTextStyles().heading.copy(color = Color.Red, textAlign = TextAlign.Center)

                BasicText(
                    text = stringResource(Res.string.error_ping_message),
                    style = textStyle
                )
                serverError?.let { (body, exception) ->
                    body?.let {
                        BasicText(
                            text = stringResource(Res.string.error_ping_body, it),
                            style = textStyle
                        )
                    }
                    exception?.let {
                        BasicText(
                            text = it.toString(),
                            style = textStyle
                        )
                    }
                }
            }
        }
    }
}
