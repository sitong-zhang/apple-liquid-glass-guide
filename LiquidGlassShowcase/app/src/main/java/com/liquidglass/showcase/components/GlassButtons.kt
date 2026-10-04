/*
 * Copyright 2025 Kyant
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

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.shapes.Capsule
import androidx.compose.foundation.shape.RoundedCornerShape
import com.liquidglass.showcase.core.glass.GlassMaterials
import com.liquidglass.showcase.core.ios.GlassIcons
import com.liquidglass.showcase.core.ios.IosColors
import com.liquidglass.showcase.core.utils.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 1:1 port of upstream `components/LiquidButton.kt`
 * (material: `vibrancy()` + `blur(2.dp)` + `lens(12.dp, 24.dp)` on a [Capsule]).
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = GlassMaterials.Button,
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height

                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 4f.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .height(48f.dp)
            .padding(horizontal = 16f.dp),
        horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * Circular glass icon button — same upstream material as [GlassButton], capsule silhouette.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    contentColor: Color = Color.Black,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    val shape = Capsule()
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.Button,
                onDrawSurface = if (tint.isSpecified) {
                    {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                } else {
                    null
                }
            )
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        GlassIcon(
            icon = icon,
            tint = if (tint.isSpecified) IosColors.onAccent() else contentColor,
            size = size / 2f
        )
    }
}

/**
 * Small glass tag / chip — upstream button material on a shorter capsule.
 */
@Composable
fun GlassChip(
    text: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    selectedColor: Color = IosColors.AccentLight,
    contentColor: Color = Color.Black
) {
    val shape = Capsule()
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.Button,
                onDrawSurface = if (isSelected) {
                    { drawRect(selectedColor) }
                } else {
                    null
                }
            )
            .clip(shape)
            .height(32f.dp)
            .padding(horizontal = 14f.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text,
            style = TextStyle(
                color = if (isSelected) IosColors.onAccent() else contentColor,
                fontSize = 14f.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Glass avatar with initials — upstream card material on a circular silhouette.
 */
@Composable
fun GlassAvatar(
    initials: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    surfaceColor: Color = Color.White.copy(alpha = 0.3f),
    contentColor: Color = Color.Black
) {
    val shape = Capsule()
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.Card,
                onDrawSurface = { drawRect(surfaceColor) }
            )
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            initials,
            style = TextStyle(
                color = contentColor,
                fontSize = 20f.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

/**
 * iOS-like notification badge layered on top of a glass avatar / icon.
 */
@Composable
fun GlassBadge(
    count: Int,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFF3B30)
) {
    val shape = Capsule()
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.Button,
                onDrawSurface = { drawRect(color) }
            )
            .height(22f.dp)
            .padding(horizontal = 8f.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            count.toString(),
            style = TextStyle(
                color = IosColors.onAccent(),
                fontSize = 12f.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

/**
 * iOS stepper — two glass buttons (upstream button material) around a value, G2 corner shape.
 */
@Composable
fun GlassStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    range: IntRange = 0..10,
    contentColor: Color = Color.Black
) {
    val shape = RoundedCornerShape(20f.dp)
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.Card,
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.25f)) }
            )
            .clip(shape)
            .height(44f.dp)
            .padding(horizontal = 6f.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4f.dp)
    ) {
        Box(
            Modifier
                .clip(Capsule())
                .clickable(enabled = value > range.first) {
                    onValueChange((value - 1).coerceIn(range))
                }
                .size(32f.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassIcon(GlassIcons.Minus, contentColor, size = 18.dp)
        }
        BasicText(
            value.toString(),
            Modifier.padding(horizontal = 8f.dp),
            style = TextStyle(contentColor, 17f.sp, FontWeight.Medium)
        )
        Box(
            Modifier
                .clip(Capsule())
                .clickable(enabled = value < range.last) {
                    onValueChange((value + 1).coerceIn(range))
                }
                .size(32f.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassIcon(GlassIcons.Plus, contentColor, size = 18f.dp)
        }
    }
}