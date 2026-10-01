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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import com.liquidglass.showcase.components.GlassCard
import com.liquidglass.showcase.components.GlassIcon
import com.liquidglass.showcase.core.ios.IosColors
import com.liquidglass.showcase.screens.CatalogScreen
import kotlin.math.roundToInt

/** iOS zoom transition duration. */
private const val ZoomInMillis = 420

/** Corner radius the squircle of a home-screen icon starts at, in dp. */
private const val IconCornerDp = 15f

/**
 * An open app, drawn as a window that grows out of its home-screen icon.
 *
 * The window is a plain rectangle whose position *and* size are interpolated between the icon
 * bounds and the full screen, so the content is laid out once at real screen size and then merely
 * scaled by the graphics layer — text never reflows mid-transition. When [visible] flips to false
 * the same interpolation runs backwards and [onFinished] fires once the window has shrunk back
 * into its icon, which is what lets the shell unmount it afterwards.
 */
@Composable
fun BoxScope.IosAppWindow(
    app: IosApp,
    origin: Rect,
    visible: Boolean,
    backdrop: Backdrop,
    isLightTheme: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(if (visible) 0f else 0f) }
    val density = LocalDensity.current
    var screen by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(visible) {
        if (visible) {
            progress.animateTo(1f, tween(ZoomInMillis, easing = EaseOutCubic))
        } else {
            progress.animateTo(0f, tween(300, easing = EaseOutCubic))
            onFinished()
        }
    }

    val appBackdrop = rememberLayerBackdrop()

    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { screen = it.boundsInRoot() }
    ) {
        if (screen.width <= 0f || origin.width <= 0f) return@Box

        val fraction = progress.value
        val left = lerp(origin.left, screen.left, fraction)
        val top = lerp(origin.top, screen.top, fraction)
        val width = lerp(origin.width, screen.width, fraction)
        val height = lerp(origin.height, screen.height, fraction)
        val corner = lerp(with(density) { IconCornerDp.dp.toPx() }, 0f, fraction)

        Box(
            Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(
                    width = with(density) { width.toDp() },
                    height = with(density) { height.toDp() }
                )
                .clip(RoundedRectangle(with(density) { corner.toDp() }))
        ) {
            Box(
                Modifier
                    .requiredSize(
                        width = with(density) { screen.width.toDp() },
                        height = with(density) { screen.height.toDp() }
                    )
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = width / screen.width
                        scaleY = height / screen.height
                    }
            ) {
                IosAppSurface(
                    app = app,
                    backdrop = backdrop,
                    appBackdrop = appBackdrop,
                    isLightTheme = isLightTheme
                )
            }
        }
    }
}

/**
 * The inside of an app window: a system background tinted with the app colour, plus the app's own
 * screen. Everything glass inside refracts [appBackdrop] — the app's background — not the wallpaper
 * behind the window, which is what keeps the material believable once the window covers the screen.
 */
@Composable
private fun IosAppSurface(
    app: IosApp,
    backdrop: Backdrop,
    appBackdrop: LayerBackdrop,
    isLightTheme: Boolean
) {
    val systemBackground = if (isLightTheme) Color(0xFFF2F2F7) else Color(0xFF000000)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(systemBackground)
                .layerBackdrop(appBackdrop)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(app.bottom.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
            )
        }

        if (app.id == "gallery") {
            CatalogScreen(backdrop = appBackdrop, isLightTheme = isLightTheme)
        } else {
            IosAppPlaceholder(app, appBackdrop, isLightTheme)
        }
    }
}

/**
 * The stand-in screen every app opens to.
 *
 * Deliberately minimal: the shell is the deliverable, not 36 fake apps. Each one shows its own
 * identity so the icon → window transition is readable, and says so honestly.
 */
@Composable
private fun IosAppPlaceholder(
    app: IosApp,
    backdrop: Backdrop,
    isLightTheme: Boolean
) {
    val contentColor = IosColors.content(isLightTheme)

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 62.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(RoundedRectangle(20.dp))
                .background(Brush.linearGradient(listOf(app.top, app.bottom))),
            contentAlignment = Alignment.Center
        ) {
            GlassIcon(app.glyph, app.glyphTint, size = 42.dp)
        }

        Spacer(Modifier.height(14.dp))

        BasicText(
            app.label,
            style = TextStyle(contentColor, 26.sp, FontWeight.SemiBold)
        )

        Spacer(Modifier.height(6.dp))

        BasicText(
            "System shell demo · App screen placeholder",
            style = TextStyle(IosColors.secondaryContent(isLightTheme), 14.sp)
        )

        Spacer(Modifier.height(28.dp))

        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 26.dp
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PlaceholderRow("Lock Screen & Home Screen", "Completed", isLightTheme)
                PlaceholderRow("Dynamic Island", "Tap to expand", isLightTheme)
                PlaceholderRow("Control Center", "Pull down from top right", isLightTheme)
                PlaceholderRow("Notification Center", "Pull down from top left", isLightTheme)
            }
        }

        Spacer(Modifier.weight(1f))

        BasicText(
            "Swipe up from the bottom to return home",
            style = TextStyle(IosColors.secondaryContent(isLightTheme), 13.sp)
        )

        Spacer(Modifier.height(34.dp))
    }
}

@Composable
private fun PlaceholderRow(title: String, value: String, isLightTheme: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(title, style = TextStyle(IosColors.content(isLightTheme), 15.sp))
        Spacer(Modifier.weight(1f))
        BasicText(value, style = TextStyle(IosColors.accent(isLightTheme), 14.sp))
    }
}
