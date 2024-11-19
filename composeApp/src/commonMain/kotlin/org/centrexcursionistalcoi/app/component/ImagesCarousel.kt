package org.centrexcursionistalcoi.app.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.utils.humanReadableSize
import org.jetbrains.compose.resources.stringResource

@Composable
fun ImagesCarousel(
    images: List<ByteArray>,
    modifier: Modifier = Modifier,
    pageSize: PageSize = PageSize.Fixed(150.dp),
    pageSpacing: Dp = 4.dp,
    onRemove: ((index: Int) -> Unit)? = null
) {
    HorizontalPager(
        state = rememberPagerState { images.size },
        modifier = modifier,
        pageSize = pageSize,
        pageSpacing = pageSpacing
    ) { page ->
        val image = images[page]

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            ) {
                onRemove?.let {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.remove),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clickable {
                                onRemove(page)
                            }
                    )
                }

                AsyncImage(
                    model = image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Inside
                )
            }

            BasicText(
                text = image.humanReadableSize(),
                style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
