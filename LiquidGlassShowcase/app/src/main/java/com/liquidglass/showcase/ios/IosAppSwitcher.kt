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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.RoundedRectangle
import com.liquidglass.showcase.components.GlassIcon
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The multitasking view.
 *
 * Cards are ordered newest-first from [IosShellState.recents]; tapping one brings it back to the
 * front, flicking one up removes it from the list. The preview is a procedural stand-in — the shell
 * never captures real screenshots.
 */
@Composable
fun BoxScope.IosAppSwitcher(
    state: IosShellState,
    isLightTheme: Boolean,
    visible: Boolean
) {
    if (!visible) return

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(interactionSource = null, indication = null) {
                state.appSwitcher = false
            },
        contentAlignment = Alignment.Center
    ) {
        if (state.recents.isEmpty()) {
            BasicText(
                "No recently used apps",
                style = TextStyle(Color.White.copy(alpha = 0.75f), 15.sp)
            )
            return@Box
        }

        LazyRow(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(state.recents, key = { it.id }) { app ->
                IosSwitcherCard(
                    app = app,
                    onOpen = {
                        state.appSwitcher = false
                        state.openApp(app, state.openAppOrigin, gallery = app.id == "gallery")
                    },
                    onRemove = { state.recents.remove(app) }
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                "Swipe up to close · Tap to switch",
                style = TextStyle(
                    if (isLightTheme) Color.Black.copy(alpha = 0.6f)
                    else Color.White.copy(alpha = 0.7f),
                    13.sp
                )
            )
        }
    }
}

/** One card: the app's identity on top of a stand-in preview of its screen. */
@Composable
private fun IosSwitcherCard(
    app: IosApp,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }

    Column(
        Modifier
            .width(280.dp)
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .graphicsLayer { alpha = (1f - (-offsetY.value) / 600f).coerceIn(0.15f, 1f) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    scope.launch { offsetY.snapTo((offsetY.value + delta).coerceIn(-800f, 0f)) }
                },
                onDragStopped = { velocity ->
                    if (offsetY.value < -150f || velocity < -900f) {
                        offsetY.animateTo(-800f, tween(200, easing = EaseOutCubic))
                        onRemove()
                    } else {
                        offsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
            )
            .clickable(interactionSource = null, indication = null, onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .padding(bottom = 10.dp)
                .clip(RoundedRectangle(9.dp))
                .background(Brush.linearGradient(listOf(app.top, app.bottom)))
                .size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassIcon(app.glyph, app.glyphTint, size = 19.dp)
        }

        BasicText(
            app.label,
            style = TextStyle(Color.White, 13.sp, FontWeight.Medium),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .clip(RoundedRectangle(28.dp))
                .background(Brush.verticalGradient(listOf(app.bottom, app.top))),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(6.dp))
                PreviewBar(0.52f)
                PreviewBar(0.86f)
                PreviewBar(0.72f)
                Spacer(Modifier.height(8.dp))
                PreviewBar(0.34f)
                PreviewBar(0.64f)
            }
        }
    }
}

/** A translucent bar standing in for a line of text in the preview. */
@Composable
private fun PreviewBar(widthFraction: Float) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(12.dp)
            .clip(RoundedRectangle(6.dp))
            .background(Color.White.copy(alpha = 0.34f))
    )
}
