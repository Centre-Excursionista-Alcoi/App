package org.centrexcursionistalcoi.app.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gabrieldrn.carbon.button.Button
import com.gabrieldrn.carbon.button.ButtonType

@Composable
fun CarbonButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonType: ButtonType = ButtonType.Primary,
    onClick: () -> Unit
) {
    Button(
        label = text,
        modifier = modifier,
        isEnabled = enabled,
        buttonType = buttonType,
        onClick = onClick
    )
}
