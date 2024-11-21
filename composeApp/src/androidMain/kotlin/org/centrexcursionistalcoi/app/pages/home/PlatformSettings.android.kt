package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CircleNotifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mmk.kmpnotifier.notification.NotifierManager
import org.centrexcursionistalcoi.app.R
import org.centrexcursionistalcoi.app.platform.ui.PlatformSettingsItem

@Composable
actual fun PlatformSettings() {
    var token by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        token = NotifierManager.getPushNotifier().getToken()
    }

    PlatformSettingsItem(
        title = stringResource(R.string.fcm_token),
        summary = token ?: "N/A",
        icon = Icons.Default.CircleNotifications,
        modifier = Modifier.fillMaxWidth()
    )

    HorizontalDivider()
}
