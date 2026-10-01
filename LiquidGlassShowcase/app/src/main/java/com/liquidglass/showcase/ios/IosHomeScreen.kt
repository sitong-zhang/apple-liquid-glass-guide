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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.liquidglass.showcase.components.GlassCard
import com.liquidglass.showcase.components.GlassIcon
import kotlinx.coroutines.flow.collectLatest

/** Side length of a home-screen icon. */
private val IconSize = 62.dp

/** iOS squircle corner radius (~22 % of the side). */
private val IconCorner = 15.dp

private val LabelStyle = TextStyle(
    color = Color.White,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1f), 3f)
)

/**
 * The simulated home screen: paged icon grid, page dots and the glass dock.
 *
 * Icons are laid out by hand rather than with a lazy grid so the four columns keep identical
 * widths on every page (and on the short last row), which is what makes the dock and the dots
 * line up with the grid above them.
 */
@Composable
fun BoxScope.IosHomeScreen(
    backdrop: Backdrop,
    state: IosShellState,
    isLightTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { IosAppCatalog.pages.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest { state.page = it }
    }

    Box(modifier.fillMaxSize()) {
        // Tapping any empty spot leaves edit mode, exactly like iOS.
        if (state.jiggle) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = null, indication = null) {
                        state.jiggle = false
                    }
            )
        }

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(62.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = !state.jiggle
            ) { index ->
                IosAppGrid(
                    apps = IosAppCatalog.pages[index],
                    jiggle = state.jiggle,
                    onOpen = { app, origin ->
                        state.openApp(app, origin, gallery = app.id == "gallery")
                    },
                    onLongPress = { state.jiggle = true }
                )
            }

            IosPageDots(
                count = IosAppCatalog.pages.size,
                current = state.page,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            IosDock(
                backdrop = backdrop,
                state = state,
                isLightTheme = isLightTheme
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** One page of the grid: four columns, up to five rows. */
@Composable
private fun IosAppGrid(
    apps: List<IosApp>,
    jiggle: Boolean,
    onOpen: (IosApp, Rect) -> Unit,
    onLongPress: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        apps.chunked(4).forEachIndexed { rowIndex, rowApps ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowApps.forEachIndexed { columnIndex, app ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        IosHomeIcon(
                            app = app,
                            jiggle = jiggle,
                            phase = (rowIndex * 4 + columnIndex) % 4,
                            onOpen = onOpen,
                            onLongPress = onLongPress
                        )
                    }
                }
                repeat(4 - rowApps.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** A single icon plus its label, with the iOS "jiggle" wobble in edit mode. */
@Composable
private fun IosHomeIcon(
    app: IosApp,
    jiggle: Boolean,
    phase: Int,
    onOpen: (IosApp, Rect) -> Unit,
    onLongPress: () -> Unit
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }

    val wobble = if (jiggle) {
        val transition = rememberInfiniteTransition(label = "jiggle")
        val angle by transition.animateFloat(
            initialValue = -1.1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(130 + phase * 26, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "jiggleAngle"
        )
        angle
    } else {
        0f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { rotationZ = wobble }
    ) {
        Box(
            Modifier
                .onGloballyPositioned { bounds = it.boundsInRoot() }
                .size(IconSize)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedRectangle(IconCorner),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(RoundedRectangle(IconCorner))
                .background(Brush.linearGradient(listOf(app.top, app.bottom)))
                .combinedClickable(
                    interactionSource = null,
                    indication = null,
                    onClick = {
                        if (!jiggle) onOpen(app, bounds)
                    },
                    onLongClick = onLongPress
                ),
            contentAlignment = Alignment.Center
        ) {
            GlassIcon(app.glyph, app.glyphTint, size = IconSize * 0.5f)

            if (jiggle) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .size(21.dp)
                        .clip(Capsule())
                        .background(Color.White.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    GlassIcon(IosGlyphs.X, Color(0xFF3A3A3C), size = 11.dp)
                }
            }
        }

        Spacer(Modifier.height(7.dp))

        BasicText(
            app.label,
            style = LabelStyle,
            maxLines = 1
        )
    }
}

/** The page indicator dots above the dock. */
@Composable
private fun IosPageDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(7.dp)
                    .clip(Capsule())
                    .background(
                        if (index == current) Color.White.copy(alpha = 0.95f)
                        else Color.White.copy(alpha = 0.38f)
                    )
            )
        }
    }
}

/** The glass dock: one row of four apps that stays on every page. */
@Composable
private fun IosDock(
    backdrop: Backdrop,
    state: IosShellState,
    isLightTheme: Boolean
) {
    GlassCard(
        backdrop = backdrop,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        cornerRadius = 36.dp,
        surfaceColor = Color.White.copy(alpha = if (isLightTheme) 0.34f else 0.22f)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosAppCatalog.dock.forEach { app ->
                IosDockIcon(app, state)
            }
        }
    }
}

/** A dock icon: no label, slightly larger squircle. */
@Composable
private fun IosDockIcon(app: IosApp, state: IosShellState) {
    var bounds by remember { mutableStateOf(Rect.Zero) }

    Box(
        Modifier
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .size(56.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedRectangle(14.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(RoundedRectangle(14.dp))
            .background(Brush.linearGradient(listOf(app.top, app.bottom)))
            .clickable(interactionSource = null, indication = null) {
                state.openApp(app, bounds, gallery = false)
            },
        contentAlignment = Alignment.Center
    ) {
        GlassIcon(app.glyph, app.glyphTint, size = 28.dp)
    }
}
