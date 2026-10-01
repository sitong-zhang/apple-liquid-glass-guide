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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.liquidglass.showcase.components.GlassButton
import com.liquidglass.showcase.components.GlassCard
import com.liquidglass.showcase.components.GlassControlCenter
import com.liquidglass.showcase.components.GlassIcon
import com.liquidglass.showcase.core.ios.IosColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** How far the pull-to-dismiss gesture has to travel before the panel closes. */
private const val DismissThresholdPx = -64f

/**
 * Control centre — pulled down from the top right.
 *
 * The panel itself is the shipped [GlassControlCenter]; the shell only adds the wallpaper switch and
 * a lock button so the shown wallpaper variant and the lock transition stay reachable.
 */
@Composable
fun BoxScope.IosControlCenter(
    backdrop: Backdrop,
    state: IosShellState,
    isLightTheme: Boolean,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    IosPullDownPanel(visible = visible, entryOffset = -1f, onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassButton(
                    onClick = { state.wallpaper++ },
                    backdrop = backdrop
                ) {
                    GlassIcon(
                        IosGlyphs.Sun,
                        IosColors.content(isLightTheme),
                        size = 18.dp
                    )
                    BasicText(
                        "Switch Wallpaper",
                        style = TextStyle(IosColors.content(isLightTheme), 15.sp)
                    )
                }
                Spacer(Modifier.weight(1f))
                GlassButton(
                    onClick = {
                        onDismiss()
                        state.lock()
                    },
                    backdrop = backdrop
                ) {
                    GlassIcon(
                        IosGlyphs.Lock,
                        IosColors.content(isLightTheme),
                        size = 18.dp
                    )
                    BasicText(
                        "Lock",
                        style = TextStyle(IosColors.content(isLightTheme), 15.sp)
                    )
                }
            }

            GlassControlCenter(
                backdrop = backdrop,
                isLightTheme = isLightTheme
            )
        }
    }
}

/** Notification centre — pulled down from the top left. */
@Composable
fun BoxScope.IosNotificationCenter(
    backdrop: Backdrop,
    isLightTheme: Boolean,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val contentColor = IosColors.content(isLightTheme)

    IosPullDownPanel(visible = visible, entryOffset = -1f, onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            BasicText(
                rememberIosClock(),
                style = TextStyle(contentColor, 68.sp, FontWeight.Light)
            )
            BasicText(
                "October 1, Thursday",
                style = TextStyle(contentColor.copy(alpha = 0.75f), 16.sp, FontWeight.Medium)
            )

            Spacer(Modifier.height(28.dp))

            IosNotificationCard(
                backdrop = backdrop,
                app = IosAppCatalog.allApps.first { it.id == "messages" },
                title = "Messages",
                body = "Great job with the liquid glass — now make an Apple system simulator",
                time = "Now"
            )

            Spacer(Modifier.height(12.dp))

            IosNotificationCard(
                backdrop = backdrop,
                app = IosAppCatalog.allApps.first { it.id == "news" },
                title = "News",
                body = "Top Story: A Full Breakdown of iOS 26 Liquid Glass Design Language",
                time = "12 minutes ago"
            )

            Spacer(Modifier.height(12.dp))

            IosNotificationCard(
                backdrop = backdrop,
                app = IosAppCatalog.allApps.first { it.id == "fitness" },
                title = "Fitness",
                body = "Time to move — you're still 180 kcal short today",
                time = "1 hour ago"
            )
        }
    }
}

/**
 * The shared pull-down behaviour of both centres.
 *
 * The scrim fades while the panel slides, and the panel can be pushed back up with a drag —
 * matching the two ways iOS lets you dismiss them.
 */
@Composable
private fun BoxScope.IosPullDownPanel(
    visible: Boolean,
    entryOffset: Float,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(180))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(interactionSource = null, indication = null) { onDismiss() }
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(320, easing = EaseOutCubic)) { (it * entryOffset).roundToInt() },
        exit = slideOutVertically(tween(240, easing = EaseOutCubic)) { (it * entryOffset).roundToInt() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            dragOffset.snapTo((dragOffset.value + delta).coerceAtMost(0f))
                        }
                    },
                    onDragStopped = { velocity ->
                        if (dragOffset.value < DismissThresholdPx || velocity < -900f) {
                            dragOffset.animateTo(-320f, tween(180, easing = EaseOutCubic))
                            onDismiss()
                            dragOffset.snapTo(0f)
                        } else {
                            dragOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(62.dp))
            content()
            Spacer(Modifier.height(40.dp))
        }
    }
}

/**
 * A glass notification card — the shared look of the lock screen and the notification centre.
 *
 * @param surfaceColor the fill of the glass; the lock screen uses a more translucent value because
 * it sits directly on the raw wallpaper.
 */
@Composable
fun IosNotificationCard(
    backdrop: Backdrop,
    app: IosApp,
    title: String,
    body: String,
    time: String,
    modifier: Modifier = Modifier,
    surfaceColor: Color = Color.White.copy(alpha = 0.22f)
) {
    GlassCard(
        backdrop = backdrop,
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        surfaceColor = surfaceColor
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(listOf(app.top, app.bottom))),
                contentAlignment = Alignment.Center
            ) {
                GlassIcon(app.glyph, app.glyphTint, size = 20.dp)
            }

            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        title,
                        style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                    )
                    Spacer(Modifier.weight(1f))
                    BasicText(
                        time,
                        style = TextStyle(Color.White.copy(alpha = 0.6f), 12.sp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                BasicText(
                    body,
                    style = TextStyle(Color.White.copy(alpha = 0.9f), 14.sp)
                )
            }
        }
    }
}
