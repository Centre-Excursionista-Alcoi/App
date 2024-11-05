package org.centrexcursionistalcoi.app.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gabrieldrn.carbon.button.Button

@Composable
fun CarbonButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        label = text,
        modifier = modifier,
        isEnabled = enabled,
        onClick = onClick
    )
}
