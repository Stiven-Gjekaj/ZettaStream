package io.github.stivengjekaj.zettastream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary

object Sizes {
    fun posterWidth(tv: Boolean): Dp = if (tv) 150.dp else 112.dp
    fun gutter(tv: Boolean): Dp = if (tv) 48.dp else 16.dp
}

@Composable
fun PosterCard(
    meta: MetaPreview,
    width: Dp,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    progress: Float? = null,
    caption: String? = null,
) {
    val shape = Corner
    Column(Modifier.width(width)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(width * 1.5f)
                .focusRing(shape, onFocus = onFocus)
                .clip(shape)
                .background(SurfaceHigh)
                .clickable(onClick = onClick),
        ) {
            Text(
                meta.name,
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(8.dp),
            )
            if (meta.poster != null) {
                AsyncImage(
                    model = meta.poster,
                    contentDescription = meta.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = Accent,
                    trackColor = SurfaceHigh,
                    modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(meta.name, color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (caption != null) {
            Text(caption, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SectionTitle(text: String, tv: Boolean, detail: String? = null) {
    Column(Modifier.padding(start = Sizes.gutter(tv), end = Sizes.gutter(tv), top = 18.dp, bottom = 8.dp)) {
        Text(text, color = TextPrimary, fontSize = if (tv) 22.sp else 18.sp, fontWeight = FontWeight.SemiBold)
        if (detail != null) Text(detail, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun PosterRow(
    metas: List<MetaPreview>,
    tv: Boolean,
    onClick: (MetaPreview) -> Unit,
    onFocus: (MetaPreview) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Sizes.gutter(tv), vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 12.dp),
    ) {
        items(metas, key = { it.type + it.id }) { meta ->
            PosterCard(meta, Sizes.posterWidth(tv), onClick = { onClick(meta) }, onFocus = { onFocus(meta) })
        }
    }
}

@Composable
fun PlaceholderRow(tv: Boolean) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Sizes.gutter(tv), vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 12.dp),
        userScrollEnabled = false,
    ) {
        items(6) {
            Box(
                Modifier
                    .size(Sizes.posterWidth(tv), Sizes.posterWidth(tv) * 1.5f)
                    .clip(Corner)
                    .background(SurfaceHigh.copy(alpha = 0.5f)),
            )
        }
    }
}

@Composable
fun Message(title: String, body: String, tv: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.padding(Sizes.gutter(tv)).fillMaxWidth()) {
        Text(title, color = TextPrimary, fontSize = if (tv) 24.sp else 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(body, color = TextSecondary, fontSize = if (tv) 17.sp else 15.sp)
    }
}
