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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** One soft colour blob of the procedural wallpaper. */
private data class WallpaperBlob(val x: Float, val y: Float, val radius: Float, val color: Color)

/** Base gradient plus its blobs, all in 0..1 relative coordinates. */
private data class WallpaperPalette(val base: List<Color>, val blobs: List<WallpaperBlob>)

private val Wallpapers: List<WallpaperPalette> = listOf(
    WallpaperPalette(
        base = listOf(Color(0xFF070A22), Color(0xFF2A1152), Color(0xFF0A1230)),
        blobs = listOf(
            WallpaperBlob(0.20f, 0.16f, 0.58f, Color(0xFF3D6BFF)),
            WallpaperBlob(0.88f, 0.26f, 0.52f, Color(0xFF9B3DFF)),
            WallpaperBlob(0.68f, 0.84f, 0.62f, Color(0xFFFF5A3C)),
            WallpaperBlob(0.08f, 0.74f, 0.46f, Color(0xFF00C2FF))
        )
    ),
    WallpaperPalette(
        base = listOf(Color(0xFF1A0A18), Color(0xFF4A1233), Color(0xFF120A20)),
        blobs = listOf(
            WallpaperBlob(0.80f, 0.14f, 0.50f, Color(0xFFFF8A3D)),
            WallpaperBlob(0.18f, 0.30f, 0.55f, Color(0xFFFF2E63)),
            WallpaperBlob(0.55f, 0.80f, 0.60f, Color(0xFF7B2DE2)),
            WallpaperBlob(0.08f, 0.88f, 0.40f, Color(0xFFFFB03A))
        )
    ),
    WallpaperPalette(
        base = listOf(Color(0xFF041C1A), Color(0xFF0B3A38), Color(0xFF04201F)),
        blobs = listOf(
            WallpaperBlob(0.24f, 0.20f, 0.55f, Color(0xFF19C8A6)),
            WallpaperBlob(0.86f, 0.34f, 0.50f, Color(0xFF2E9BFF)),
            WallpaperBlob(0.60f, 0.86f, 0.58f, Color(0xFF7BE38F)),
            WallpaperBlob(0.10f, 0.70f, 0.42f, Color(0xFF00E5C0))
        )
    )
)

/**
 * Themed procedural wallpaper.
 *
 * Deliberately *not* an Apple asset: the simulator paints its own abstract gradient mesh so no
 * wallpapers are redistributed. Light theme lifts the whole palette so the glass still reads on a
 * bright background.
 */
@Composable
fun IosWallpaper(
    isLightTheme: Boolean,
    variant: Int,
    modifier: Modifier = Modifier
) {
    val palette = Wallpapers[variant.mod(Wallpapers.size)]
    Canvas(modifier) {
        drawRect(
            Brush.linearGradient(
                colors = palette.base,
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
        palette.blobs.forEach { blob ->
            val center = Offset(size.width * blob.x, size.height * blob.y)
            val radius = size.minDimension * blob.radius
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isLightTheme) blob.color.lift() else blob.color,
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}

/** Mixes a blob colour towards white for the light theme. */
private fun Color.lift(): Color = lerpColor(this, Color.White, 0.34f)

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color = Color(
    red = start.red + (stop.red - start.red) * fraction,
    green = start.green + (stop.green - start.green) * fraction,
    blue = start.blue + (stop.blue - start.blue) * fraction,
    alpha = start.alpha + (stop.alpha - start.alpha) * fraction
)

/**
 * Builds the shell's two backdrop layers.
 *
 * [wallpaperBackdrop] records the wallpaper; [surfaceBackdrop] is left for the caller to attach to
 * whatever is currently on screen (lock screen, home screen, or an open app). Glass *inside* that
 * content must refract `wallpaperBackdrop` — refracting a layer while it is still being recorded
 * would feed back on itself — while the pull-down surfaces floating above it refract
 * `rememberCombinedBackdrop(wallpaperBackdrop, surfaceBackdrop)` so they blur the real screen
 * behind them.
 */
@Composable
fun IosScaffold(
    isLightTheme: Boolean,
    wallpaperVariant: Int,
    content: @Composable BoxScope.(wallpaperBackdrop: LayerBackdrop, surfaceBackdrop: LayerBackdrop) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        val wallpaperBackdrop = rememberLayerBackdrop()
        val surfaceBackdrop = rememberLayerBackdrop()

        IosWallpaper(
            isLightTheme = isLightTheme,
            variant = wallpaperVariant,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(wallpaperBackdrop)
        )
        content(wallpaperBackdrop, surfaceBackdrop)
    }
}
