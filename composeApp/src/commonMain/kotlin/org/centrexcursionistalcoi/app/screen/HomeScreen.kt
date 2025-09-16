package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.centrexcursionistalcoi.app.auth.tokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect

@OptIn(ExperimentalOpenIdConnect::class)
@Composable
fun HomeScreen() {
    val accessToken by tokenStore.accessTokenFlow.collectAsState(initial = null)

    Scaffold { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Text("Token: $accessToken")
        }
    }
}
