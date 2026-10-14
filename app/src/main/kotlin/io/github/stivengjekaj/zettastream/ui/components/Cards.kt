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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import io.github.stivengjekaj.zettastream.ui.FocusMemory
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
import io.github.stivengjekaj.zettastream.addon.Display
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.ui.theme.Background
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.OnAccent
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary

object Sizes {
    fun posterWidth(tv: Boolean): Dp = if (tv) 150.dp else 112.dp
    fun gutter(tv: Boolean): Dp = if (tv) 48.dp else 16.dp
}

/** The shape of a card image. Addons say "poster", "landscape", or "square". */
enum class CardShape(val widthScale: Float, val heightRatio: Float) {
    Poster(1f, 1.5f), Landscape(1.8f, 9f / 16f), Square(1.15f, 1f);

    companion object {
        fun of(posterShape: String?): CardShape = when (posterShape?.lowercase()) {
            "landscape" -> Landscape
            "square" -> Square
            else -> Poster
        }
    }
}

@Composable
fun PosterCard(
    meta: MetaPreview,
    width: Dp,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    progress: Float? = null,
    caption: String? = null,
    focusKey: String? = null,
    badge: String? = null,
) {
    val shape = Corner
    val card = CardShape.of(meta.posterShape)
    val cardWidth = width * card.widthScale
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    // After Back, the card that had the focus takes it again.
    LaunchedEffect(focusKey) {
        if (focusKey != null && FocusMemory.pending == focusKey) {
            FocusMemory.pending = null
            runCatching { requester.requestFocus() }
        }
    }
    Column(Modifier.width(cardWidth)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(cardWidth * card.heightRatio)
                .focusRequester(requester)
                .onFocusChanged { focused = it.isFocused }
                .focusRing(shape, onFocus = {
                    if (focusKey != null) FocusMemory.last = focusKey
                    onFocus()
                })
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
                    model = Display.cardPoster(meta.poster),
                    contentDescription = meta.name,
                    // A square image is often a channel logo, so show all of it.
                    contentScale = if (card == CardShape.Square) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().then(if (card == CardShape.Square) Modifier.padding(10.dp) else Modifier),
                )
            }
            // A mark such as "NEW" sits at the top left of the art.
            if (badge != null) {
                Text(
                    badge, color = OnAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(Corner).background(Accent).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            // The rating sits on the art, as on Miruro 2.0.
            Display.rating(meta.imdbRating)?.let { rating ->
                Row(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).clip(Corner).background(Background.copy(alpha = 0.75f)).padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Star, null, tint = TextPrimary, modifier = Modifier.size(12.dp))
                    Text(rating, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 3.dp))
                }
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
        // The focused card grows by 5 percent. This space keeps it off the title.
        Spacer(Modifier.height(12.dp))
        // One line, as on Miruro 2.0. A long name scrolls when the card has the focus.
        ScrollingText(meta.name, active = focused, color = TextPrimary, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 1, fontWeight = FontWeight.Medium)
        // The caption, such as the episode to continue, or else the kind and the year.
        val facts = caption?.takeIf { it != meta.name } ?: Display.cardFacts(meta.type, meta.releaseInfo).ifEmpty { null }
        if (facts != null) {
            Text(facts, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** The large title at the top of a screen, as on Miruro 2.0. */
@Composable
fun PageTitle(text: String, tv: Boolean, modifier: Modifier = Modifier) {
    Text(
        text, color = TextPrimary, fontSize = if (tv) 36.sp else 26.sp, fontWeight = FontWeight.ExtraBold,
        modifier = modifier.padding(horizontal = Sizes.gutter(tv)).padding(bottom = 4.dp),
    )
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
    keyPrefix: String = "",
) {
    TvRow(tv) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = Sizes.gutter(tv), vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 12.dp),
        ) {
            items(metas, key = { it.type + it.id }) { meta ->
                PosterCard(
                meta, Sizes.posterWidth(tv), onClick = { onClick(meta) }, onFocus = { onFocus(meta) },
                focusKey = "$keyPrefix|${meta.type}|${meta.id}",
            )
            }
        }
    }
}

@Composable
fun PlaceholderRow(tv: Boolean) {
    TvRow(tv) {
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
}

@Composable
fun Message(title: String, body: String, tv: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.padding(Sizes.gutter(tv)).fillMaxWidth()) {
        Text(title, color = TextPrimary, fontSize = if (tv) 24.sp else 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(body, color = TextSecondary, fontSize = if (tv) 17.sp else 15.sp)
    }
}
