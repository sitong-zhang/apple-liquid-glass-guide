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

package com.liquidglass.showcase.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.liquidglass.showcase.core.glass.GlassMaterials
import com.liquidglass.showcase.core.ios.GlassIcons
import com.liquidglass.showcase.core.ios.IosColors
import com.liquidglass.showcase.components.GlassIcon

/**
 * Liquid-glass loading screen.
 *
 * Every glass surface here is one of the upstream recipes:
 *  - panel  → `destinations/DialogContent.kt` (`colorControls` + `blur(16.dp)` + `lens(24.dp, 48.dp, depthEffect)`,
 *             `Highlight.Plain`);
 *  - track  → `components/LiquidBottomTabs.kt` (`vibrancy()` + `blur(8.dp)` + `lens(24.dp, 24.dp)`);
 *  - knob   → `components/LiquidButton.kt` (`vibrancy()` + `blur(2.dp)` + `lens(12.dp, 24.dp)`).
 *
 * The knob sits on top of the panel glass and therefore uses the documented `exportedBackdrop`
 * technique instead of nesting two `layerBackdrop` modifiers.
 */
@Composable
fun BoxScope.SplashScreen(
    backdrop: Backdrop,
    progress: Float,
    isLightTheme: Boolean
) {
    val contentColor = IosColors.content(isLightTheme)
    val accentColor = IosColors.accent(isLightTheme)
    val panelShape = RoundedRectangle(48f.dp)
    val panelBackdrop = rememberLayerBackdrop()
    val safeProgress = progress.fastCoerceIn(0f, 1f)

    Column(
        Modifier
            .align(Alignment.Center)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { panelShape },
                effects = GlassMaterials.dialog(isLightTheme),
                highlight = { Highlight.Plain },
                exportedBackdrop = panelBackdrop,
                onDrawSurface = { drawRect(IosColors.dialogContainer(isLightTheme)) }
            )
            .widthIn(max = 320f.dp)
            .fillMaxWidth()
            .padding(28f.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18f.dp)
    ) {
        Box(
            Modifier
                .drawBackdrop(
                    backdrop = panelBackdrop,
                    shape = { Capsule() },
                    effects = GlassMaterials.BottomBar,
                    onDrawSurface = { drawRect(accentColor) }
                )
                .size(64f.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassIcon(GlassIcons.Droplet, IosColors.onAccent(), size = 34f.dp)
        }

        BasicText(
            "Apple System",
            style = TextStyle(contentColor, 22f.sp, FontWeight.SemiBold)
        )
        BasicText(
            "iOS 26 · AGSL Liquid Glass",
            style = TextStyle(contentColor.copy(alpha = 0.55f), 13f.sp)
        )

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(28f.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val trackWidth = constraints.maxWidth

            Box(
                Modifier
                    .drawBackdrop(
                        backdrop = panelBackdrop,
                        shape = { Capsule() },
                        effects = GlassMaterials.BottomBar,
                        onDrawSurface = { drawRect(IosColors.track(isLightTheme)) }
                    )
                    .fillMaxSize()
            )

            Box(
                Modifier
                    .clip(Capsule())
                    .background(accentColor)
                    .height(28f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * safeProgress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )

            Box(
                Modifier
                    .graphicsLayer {
                        translationX =
                            (-size.width / 2f + trackWidth * safeProgress)
                                .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f)
                    }
                    .drawBackdrop(
                        backdrop = panelBackdrop,
                        shape = { Capsule() },
                        effects = GlassMaterials.Button,
                        onDrawSurface = { drawRect(Color.White) }
                    )
                    .size(40f.dp, 24f.dp)
            )
        }

        BasicText(
            "${(safeProgress * 100f).toInt()}%",
            style = TextStyle(contentColor.copy(alpha = 0.7f), 14f.sp, FontWeight.Medium)
        )
    }
}