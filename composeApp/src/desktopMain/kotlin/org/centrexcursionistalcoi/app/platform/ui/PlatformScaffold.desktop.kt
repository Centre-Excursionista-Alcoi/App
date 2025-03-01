package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import ceaapp.composeapp.generated.resources.*
import com.gabrieldrn.carbon.button.ButtonType
import com.gabrieldrn.carbon.foundation.color.CarbonLayer
import com.gabrieldrn.carbon.foundation.color.containerBackground
import org.centrexcursionistalcoi.app.component.CarbonButton
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalComposeUiApi::class)
actual fun PlatformScaffold(
    title: String?,
    actions: List<Action>,
    navigationBar: (@Composable () -> Unit)?,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.(paddingValues: PaddingValues) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Press) { event ->
                // For some reason, the back button seems to be index 5
                if (event.button == PointerButton.Back || event.buttons.isBackPressed || event.button?.index == 5) {
                    onBack?.invoke()
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let {
                PlatformButton(stringResource(Res.string.back), onClick = it)
            }
            AnimatedContent(
                targetState = title,
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            ) { state ->
                state?.let {
                    BasicText(
                        text = it,
                        style = getPlatformTextStyles().heading
                    )
                }
            }
            for (action in actions) {
                Box {
                    var showingPopup by remember { mutableStateOf(false) }
                    if (showingPopup) {
                        Popup(
                            popupPositionProvider = object : PopupPositionProvider {
                                override fun calculatePosition(
                                    anchorBounds: IntRect,
                                    windowSize: IntSize,
                                    layoutDirection: LayoutDirection,
                                    popupContentSize: IntSize
                                ): IntOffset {
                                    val (anchorX, anchorY) = anchorBounds.left to anchorBounds.top
                                    val (width) = popupContentSize

                                    // The popup should be aligned to the right of the anchor
                                    val x = anchorX + anchorBounds.width - width
                                    val y = anchorY + anchorBounds.height
                                    return IntOffset(x, y)
                                }
                            },
                            onDismissRequest = { showingPopup = false }
                        ) {
                            CarbonLayer {
                                Column(
                                    modifier = Modifier
                                        .width(240.dp)
                                        .containerBackground()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    action.popupContent?.invoke(this)
                                }
                            }
                        }
                    }

                    CarbonButton(
                        text = action.label + (action.badge?.let { " ($it)" } ?: ""),
                        buttonType = if (action.isPrimary) ButtonType.Primary else ButtonType.Secondary,
                        enabled = action.enabled,
                        onClick = {
                            if (action.popupContent != null) {
                                showingPopup = true
                            } else {
                                action.onClick()
                            }
                        }
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            content(PaddingValues(0.dp))
        }

        navigationBar?.invoke()
    }
}
