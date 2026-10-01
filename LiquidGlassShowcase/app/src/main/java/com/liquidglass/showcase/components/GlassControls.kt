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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.liquidglass.showcase.core.glass.GlassMaterials
import com.liquidglass.showcase.core.ios.GlassIcons
import com.liquidglass.showcase.core.ios.IosColors
import com.liquidglass.showcase.core.utils.DampedDragAnimation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 1:1 port of upstream `components/LiquidToggle.kt`.
 *
 * Material: `blur(8.dp * (1 - press))` + `lens(5.dp * press, 10.dp * press, chromaticAberration = true)`,
 * `Highlight.Ambient / 1.5 @ press`, `Shadow(4.dp, black 5%)`, `InnerShadow(4.dp * press)`.
 */
@Composable
fun GlassToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true
) {
    val accentColor = IosColors.switchAccent(isLightTheme)
    val trackColor = IosColors.track(isLightTheme)

    val density = androidx.compose.ui.platform.LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onSelect(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (selected()) 0f else 1f
                    onSelect(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction =
                    if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                    else (fraction - delta).fastCoerceIn(0f, 1f)
            }
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { fraction ->
                dampedDragAnimation.updateValue(fraction)
            }
    }
    LaunchedEffect(selected) {
        snapshotFlow { selected() }
            .collectLatest { isSelected ->
                val target = if (isSelected) 1f else 0f
                if (target != fraction) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    val trackBackdrop = rememberLayerBackdrop()

    Box(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind {
                    val fraction = dampedDragAnimation.value
                    drawRect(lerp(trackColor, accentColor, fraction))
                }
                .size(64f.dp, 28f.dp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val fraction = dampedDragAnimation.value
                    val padding = 2f.dp.toPx()
                    translationX =
                        if (isLtr) lerp(padding, padding + dragWidth, fraction)
                        else lerp(-padding, -(padding + dragWidth), fraction)
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            5f.dp.toPx() * progress,
                            10f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}

/**
 * 1:1 port of upstream `components/LiquidSlider.kt`.
 *
 * Knob material: `blur(8.dp * (1 - press))` + `lens(10.dp * press, 14.dp * press, chromaticAberration = true)`.
 */
@Composable
fun GlassSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    visibilityThreshold: Float = 0.01f,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true
) {
    val accentColor = IosColors.accent(isLightTheme)
    val trackColor = IosColors.track(isLightTheme)

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        onValueChange(targetValue)
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                    onValueChange(
                        if (isLtr) (targetValue + delta).coerceIn(valueRange)
                        else (targetValue - delta).coerceIn(valueRange)
                    )
                }
            )
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }
                .collectLatest { value ->
                    if (dampedDragAnimation.targetValue != value) {
                        dampedDragAnimation.updateValue(value)
                    }
                }
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(Capsule())
                    .background(trackColor)
                    .pointerInput(animationScope) {
                        detectTapGestures { position ->
                            val delta = (valueRange.endInclusive - valueRange.start) * (position.x / trackWidth)
                            val targetValue =
                                (if (isLtr) valueRange.start + delta
                                else valueRange.endInclusive - delta)
                                    .coerceIn(valueRange)
                            dampedDragAnimation.animateToValue(targetValue)
                            onValueChange(targetValue)
                        }
                    }
                    .height(6f.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(Capsule())
                    .background(accentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}

/**
 * iOS segmented control.
 *
 * Container: upstream `LiquidBottomTabs.kt` bar material (`vibrancy()` + `blur(8.dp)` + `lens(24.dp, 24.dp)`).
 * Thumb: upstream `LiquidBottomTabs.kt` moving indicator
 * (`lens(10.dp * press, 14.dp * press, chromaticAberration = true)`, `Highlight.Default @ press`,
 * `Shadow @ press`, `InnerShadow(8.dp * press)`) — the pump-and-release comes from the upstream
 * [DampedDragAnimation], so the liquid press feedback is identical.
 */
@Composable
fun GlassSegmentedControl(
    segments: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true
) {
    val barContainer = IosColors.barContainer(isLightTheme)
    val contentColor = IosColors.content(isLightTheme)

    val animationScope = rememberCoroutineScope()
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(segments.size - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1f,
            onDragStarted = {},
            onDragStopped = {
                val target = targetValue.fastRoundToInt().coerceIn(0, segments.size - 1)
                onSelected(target)
                animateToValue(target.toFloat())
            },
            onDrag = { _, _ -> }
        )
    }
    LaunchedEffect(selectedIndex) {
        dampedDragAnimation.animateToValue(selectedIndex.toFloat())
    }

    BoxWithConstraints(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = GlassMaterials.BottomBar,
                onDrawSurface = { drawRect(barContainer) }
            )
            .height(40f.dp)
            .padding(3f.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val segmentWidth = constraints.maxWidth.toFloat() / segments.size

        Box(
            Modifier
                .padding(horizontal = 0f.dp)
                .graphicsLayer {
                    translationX = dampedDragAnimation.value * segmentWidth
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 8f.dp * progress,
                            alpha = progress
                        )
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.1f)
                            else Color.White.copy(0.1f),
                            alpha = 1f - progress
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                )
                .height(34f.dp)
                .fillMaxWidth(1f / segments.size)
        )

        Row(Modifier.fillMaxWidth()) {
            segments.forEachIndexed { index, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(34f.dp)
                        .clip(Capsule())
                        .clickable(role = Role.Tab) {
                            onSelected(index)
                            dampedDragAnimation.animateToValue(index.toFloat())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        label,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14f.sp,
                            fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

/** Glass checkbox — upstream card material on a G2 squircle, accent fill when checked. */
@Composable
fun GlassCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true
) {
    val shape = RoundedRectangle(12f.dp)
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.Card,
                onDrawSurface = if (checked) {
                    { drawRect(IosColors.accent(isLightTheme)) }
                } else {
                    { drawRect(Color.White.copy(alpha = 0.25f)) }
                }
            )
            .clip(shape)
            .clickable { onCheckedChange(!checked) }
            .size(30f.dp),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            GlassIcon(GlassIcons.Check, IosColors.onAccent(), size = 18f.dp)
        }
    }
}

/** Glass radio button — upstream card material on a capsule, accent dot when selected. */
@Composable
fun GlassRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true
) {
    val shape = Capsule()
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.Card,
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.25f)) }
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .size(30f.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(14f.dp)
                    .clip(Capsule())
                    .background(IosColors.accent(isLightTheme))
            )
        }
    }
}

/**
 * iOS linear progress bar.
 *
 * Track: upstream button material on a capsule; fill: the very same accent capsule the upstream
 * slider paints (`clip(Capsule()) + background(accentColor)`); leading knob: the upstream slider
 * knob expression evaluated at `progress = 1`
 * (`blur(8.dp * 0) + lens(10.dp, 14.dp, chromaticAberration = true)`).
 */
@Composable
fun GlassProgressBar(
    progress: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true,
    showKnob: Boolean = true
) {
    val accentColor = IosColors.accent(isLightTheme)
    val safeProgress = progress.fastCoerceIn(0f, 1f)

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        Box(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = GlassMaterials.Button,
                    onDrawSurface = { drawRect(IosColors.track(isLightTheme)) }
                )
                .fillMaxWidth()
                .height(14f.dp)
        ) {
            Box(
                Modifier
                    .clip(Capsule())
                    .background(accentColor)
                    .height(14f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * safeProgress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        if (showKnob) {
            Box(
                Modifier
                    .graphicsLayer {
                        translationX =
                            (-size.width / 2f + trackWidth * safeProgress)
                                .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f)
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = GlassMaterials.sliderKnob(1f),
                        highlight = { Highlight.Ambient.copy(alpha = 1f) },
                        shadow = {
                            Shadow(
                                radius = 4f.dp,
                                color = Color.Black.copy(alpha = 0.05f)
                            )
                        },
                        onDrawSurface = { drawRect(Color.White) }
                    )
                    .size(40f.dp, 24f.dp)
            )
        }
    }
}

/**
 * Liquid glass activity indicator: upstream button material around a rotating dot ring.
 */
@Composable
fun GlassActivityIndicator(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true,
    tint: Color = IosColors.accent(isLightTheme)
) {
    val transition = rememberInfiniteTransition(label = "activity")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = GlassMaterials.Button,
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.25f)) }
            )
            .size(56f.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(28f.dp)
                .graphicsLayer { rotationZ = rotation }
        ) {
            repeat(8) { index ->
                val angle = index * 45f
                val dotAlpha = 0.15f + 0.85f * ((index + 1) / 8f)
                Box(
                    Modifier
                        .graphicsLayer {
                            rotationZ = angle
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                        }
                        .size(width = 3f.dp, height = 28f.dp)
                        .graphicsLayer { alpha = dotAlpha },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        Modifier
                            .size(3f.dp)
                            .clip(Capsule())
                            .background(tint)
                    )
                }
            }
        }
    }
}

/** Small caption used by the catalog for the progress / indicator demo. */
@Composable
internal fun Caption(text: String, color: Color, modifier: Modifier = Modifier) {
    BasicText(
        text,
        modifier,
        style = TextStyle(color = color, fontSize = 13f.sp, fontWeight = FontWeight.Medium)
    )
}

