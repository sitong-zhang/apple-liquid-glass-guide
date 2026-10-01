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

package com.liquidglass.showcase.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.shapes.RoundedRectangle
import com.liquidglass.showcase.core.glass.GlassMaterials

/**
 * Renders one of the dependency-free [com.liquidglass.showcase.core.ios.GlassIcons] glyphs.
 */
@Composable
fun GlassIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint)
    )
}

/**
 * iOS-style glass card.
 *
 * Material: upstream `LazyScrollContainerContent.kt` recipe
 * (`vibrancy()` + `lens(16.dp, 32.dp)`) on a G2-continuous [RoundedRectangle];
 * highlight and shadow are the library defaults (`Highlight.Default`, `Shadow.Default`),
 * i.e. exactly what upstream's `drawBackdrop` applies when the parameters are omitted.
 */
@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32f.dp,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit
) {
    val shape: Shape = RoundedRectangle(cornerRadius)
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = GlassMaterials.Card,
            onDrawSurface = if (surfaceColor.isSpecified) {
                { drawRect(surfaceColor) }
            } else {
                null
            }
        ),
        contentAlignment = Alignment.Center,
        content = content
    )
}