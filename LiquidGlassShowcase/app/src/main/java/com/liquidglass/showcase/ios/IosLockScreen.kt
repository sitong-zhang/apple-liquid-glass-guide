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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.liquidglass.showcase.components.GlassIconButton
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The simulated lock screen.
 *
 * Text is always white here regardless of theme: the lock screen always sits on the raw wallpaper,
 * exactly like the real thing. Swiping up past the threshold (or flicking with enough velocity)
 * unlocks; anything shorter springs back.
 */
@Composable
fun BoxScope.IosLockScreen(
    backdrop: Backdrop,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val time = rememberIosClock()

    Column(
        modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    scope.launch {
                        offsetY.snapTo((offsetY.value + delta).coerceIn(-2_400f, 0f))
                    }
                },
                onDragStopped = { velocity ->
                    if (offsetY.value < -180f || velocity < -1_100f) {
                        offsetY.animateTo(-2_400f, tween(300, easing = EaseOutCubic))
                        onUnlock()
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
            // Swallow stray taps so they cannot reach the home screen behind the lock screen.
            .clickable(interactionSource = null, indication = null) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(84.dp))

        GlassIconButton(
            onClick = {},
            backdrop = backdrop,
            icon = IosGlyphs.Lock,
            contentColor = Color.White,
            size = 42.dp
        )

        Spacer(Modifier.height(18.dp))

        BasicText(
            "October 1, Thursday",
            style = TextStyle(Color.White.copy(alpha = 0.9f), 17.sp, FontWeight.Medium)
        )
        BasicText(
            time,
            style = TextStyle(
                color = Color.White,
                fontSize = 88.sp,
                fontWeight = FontWeight.Light
            )
        )

        Spacer(Modifier.height(28.dp))

        IosNotificationCard(
            backdrop = backdrop,
            app = IosAppCatalog.allApps.first { it.id == "messages" },
            title = "Messages",
            body = "Great job with the liquid glass — now make an Apple system simulator",
            time = "Now",
            modifier = Modifier.padding(horizontal = 20.dp),
            surfaceColor = Color.White.copy(alpha = 0.14f)
        )

        Spacer(Modifier.height(12.dp))

        IosNotificationCard(
            backdrop = backdrop,
            app = IosAppCatalog.allApps.first { it.id == "music" },
            title = "Music",
            body = "Now Playing · Liquid Glass Demo Track",
            time = "5 minutes ago",
            modifier = Modifier.padding(horizontal = 20.dp),
            surfaceColor = Color.White.copy(alpha = 0.14f)
        )

        Spacer(Modifier.weight(1f))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                onClick = {},
                backdrop = backdrop,
                icon = IosGlyphs.Flashlight,
                contentColor = Color.White,
                size = 52.dp
            )
            Spacer(Modifier.weight(1f))
            GlassIconButton(
                onClick = {},
                backdrop = backdrop,
                icon = IosGlyphs.Camera,
                contentColor = Color.White,
                size = 52.dp
            )
        }

        Spacer(Modifier.height(26.dp))

        BasicText(
            "Swipe up to unlock",
            style = TextStyle(Color.White.copy(alpha = 0.75f), 14.sp, FontWeight.Medium)
        )

        Spacer(Modifier.height(30.dp))
    }
}
