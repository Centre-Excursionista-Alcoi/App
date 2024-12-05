package org.centrexcursionistalcoi.app.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = getPlatformTextStyles().body,
    onTextLayout: (textLayoutResult: Any) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    textAlign: TextAlign = TextAlign.Start,
    color: ColorProducer? = null
) {
    BasicText(
        text,
        modifier,
        style.copy(textAlign = textAlign),
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        minLines,
        color
    )
}
