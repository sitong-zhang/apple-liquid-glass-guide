/*
 * Copyright 2026 The Liquid Glass Showcase authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liquidglass.showcase.ios

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.liquidglass.showcase.components.GlassIcon
import com.liquidglass.showcase.core.ios.IosColors
import kotlinx.coroutines.delay
import java.util.Calendar

/** Ticks once a second and returns the wall clock as `HH:mm`. */
@Composable
fun rememberIosClock(): String {
    var text by remember { mutableStateOf(iosClockText()) }
    LaunchedEffect(Unit) {
        while (true) {
            text = iosClockText()
            delay(1_000)
        }
    }
    return text
}

private fun iosClockText(): String {
    val now = Calendar.getInstance()
    return "%02d:%02d".format(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
}

/**
 * iOS status bar: clock on the left, radio/battery cluster on the right.
 *
 * The Dynamic Island sits in the middle and is drawn separately so it can stay on screen above
 * every other surface.
 */
@Composable
fun IosStatusBar(
    isLightTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val contentColor = IosColors.content(isLightTheme)
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            rememberIosClock(),
            style = TextStyle(contentColor, 17.sp, FontWeight.SemiBold)
        )
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CellularBars(contentColor)
            GlassIcon(IosGlyphs.Wifi, contentColor, size = 17.dp)
            BatteryGlyph(contentColor)
        }
    }
}

/** Four ascending cellular bars. */
@Composable
private fun CellularBars(color: Color) {
    Canvas(Modifier.size(18.dp, 12.dp)) {
        val bars = 4
        val gap = size.width * 0.14f
        val barWidth = (size.width - gap * (bars - 1)) / bars
        repeat(bars) { index ->
            val fraction = 0.36f + index * 0.2133f
            val barHeight = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3f)
            )
        }
    }
}

/** Battery outline with a 78 % fill, matching the mock device state. */
@Composable
private fun BatteryGlyph(color: Color) {
    Canvas(Modifier.size(26.dp, 13.dp)) {
        val bodyWidth = size.width - size.width * 0.09f
        val radius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.34f)
        drawRoundRect(
            color = color.copy(alpha = 0.42f),
            size = Size(bodyWidth, size.height),
            cornerRadius = radius,
            style = Stroke(width = size.height * 0.09f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.height * 0.16f, size.height * 0.17f),
            size = Size(bodyWidth * 0.72f, size.height * 0.66f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.2f)
        )
        drawRoundRect(
            color = color.copy(alpha = 0.42f),
            topLeft = Offset(bodyWidth + size.width * 0.02f, size.height * 0.32f),
            size = Size(size.width * 0.05f, size.height * 0.36f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.025f)
        )
    }
}

/**
 * Dynamic Island.
 *
 * Collapsed it is a 124 x 36 dp black pill; tapping it plays the iOS spring into a 336 x 140 dp
 * now-playing card. Colours on the island are intentionally fixed (never glass) — that is what the
 * hardware cut-out looks like.
 */
@Composable
fun IosDynamicIsland(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val width by animateDpAsState(
        targetValue = if (expanded) 336.dp else 124.dp,
        animationSpec = tween(360, easing = EaseOutCubic),
        label = "islandWidth"
    )
    val height by animateDpAsState(
        targetValue = if (expanded) 142.dp else 36.dp,
        animationSpec = tween(360, easing = EaseOutCubic),
        label = "islandHeight"
    )
    val corner by animateDpAsState(
        targetValue = if (expanded) 46.dp else 18.dp,
        animationSpec = tween(360, easing = EaseOutCubic),
        label = "islandCorner"
    )

    Box(
        modifier
            .size(width, height)
            .clip(RoundedCornerShape(corner))
            .background(Color.Black)
            .clickable(interactionSource = null, indication = null, onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (expanded) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF6B81), Color(0xFFF5233F))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    GlassIcon(IosGlyphs.Music, Color.White, size = 26.dp)
                }
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    BasicText(
                        "Now Playing",
                        style = TextStyle(Color.White.copy(alpha = 0.6f), 11.sp, FontWeight.Medium)
                    )
                    BasicText(
                        "Liquid Glass · Demo Track",
                        style = TextStyle(Color.White, 15.sp, FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(4.dp))
                    NowPlayingWaveform()
                }
                GlassIcon(IosGlyphs.Pause, Color.White, size = 22.dp)
            }
        }
    }
}

/** A small static "playing" waveform. */
@Composable
private fun NowPlayingWaveform() {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        val heights = listOf(0.35f, 0.7f, 1f, 0.55f, 0.85f, 0.4f, 0.95f, 0.6f, 0.3f, 0.75f, 0.5f, 0.9f)
        val gap = size.width * 0.012f
        val barWidth = (size.width - gap * (heights.size - 1)) / heights.size
        heights.forEachIndexed { index, fraction ->
            val barHeight = size.height * fraction
            drawRoundRect(
                color = Color.White.copy(alpha = 0.75f),
                topLeft = Offset(index * (barWidth + gap), (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}

/** The home indicator bar; also the visual anchor for the swipe-up gesture. */
@Composable
fun IosHomeIndicator(
    isLightTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(26.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(140.dp)
                .height(5.dp)
                .clip(Capsule())
                .background(IosColors.content(isLightTheme).copy(alpha = 0.9f))
        )
    }
}
