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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
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
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.liquidglass.showcase.core.glass.GlassMaterials
import com.liquidglass.showcase.core.ios.GlassIcons
import com.liquidglass.showcase.core.ios.IosColors
import com.liquidglass.showcase.core.utils.DampedDragAnimation
import com.liquidglass.showcase.core.utils.InteractiveHighlight
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

internal val LocalGlassTabScale = staticCompositionLocalOf { { 1f } }

/** One entry of the [GlassBottomTabBar]. */
data class GlassTabItem(
    val label: String,
    val icon: ImageVector
)

/**
 * 1:1 port of upstream `components/LiquidBottomTabs.kt` + `components/LiquidBottomTab.kt`.
 *
 * Bar material: `vibrancy()` + `blur(8.dp)` + `lens(24.dp, 24.dp)`.
 * Moving indicator: `lens(10.dp * press, 14.dp * press, chromaticAberration = true)` with
 * `Highlight.Default @ press`, `Shadow @ press` and `InnerShadow(8.dp * press)`.
 */
@Composable
fun GlassBottomTabBar(
    tabs: List<GlassTabItem>,
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true
) {
    val accentColor = IosColors.accent(isLightTheme)
    val containerColor = IosColors.barContainer(isLightTheme)
    val tabsCount = tabs.size

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    currentIndex = index
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, offset ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        val tabContent: @Composable RowScope.() -> Unit = {
            tabs.forEachIndexed { index, tab ->
                GlassBottomTab(
                    onClick = { currentIndex = index }
                ) {
                    GlassIcon(
                        icon = tab.icon,
                        tint = IosColors.content(isLightTheme),
                        size = 24f.dp
                    )
                    BasicText(
                        tab.label,
                        style = TextStyle(
                            color = IosColors.content(isLightTheme),
                            fontSize = 11f.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = GlassMaterials.BottomBar,
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(64f.dp)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = tabContent
        )

        CompositionLocalProvider(
            LocalGlassTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            }
        ) {
            Row(
                Modifier
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(
                                24f.dp.toPx() * progress,
                                24f.dp.toPx() * progress
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(56f.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = tabContent
            )
        }

        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
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
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
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
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}

/** Upstream `components/LiquidBottomTab.kt`. */
@Composable
fun RowScope.GlassBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalGlassTabScale.current
    Column(
        modifier
            .clip(Capsule())
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val scale = scale()
                scaleX = scale
                scaleY = scale
            },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/**
 * iOS navigation bar.
 *
 * Material: the upstream `tutorials/glass-bottom-bar` recipe
 * (`vibrancy()` + `blur(4.dp)` + `lens(16.dp, 32.dp)`) with the documented translucent white
 * surface (`Color.White.copy(alpha = 0.5f)` in light mode / container color in dark mode).
 */
@Composable
fun GlassNavBar(
    title: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val contentColor = IosColors.content(isLightTheme)
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = GlassMaterials.NavigationBar,
                onDrawSurface = {
                    drawRect(
                        if (isLightTheme) Color.White.copy(alpha = 0.5f)
                        else IosColors.barContainer(isLightTheme)
                    )
                }
            )
            .height(56f.dp)
            .padding(horizontal = 8f.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40f.dp), contentAlignment = Alignment.Center) {
            if (onBack != null) {
                GlassIconButton(
                    onClick = onBack,
                    backdrop = backdrop,
                    icon = GlassIcons.ChevronLeft,
                    size = 32f.dp,
                    contentColor = contentColor
                )
            } else {
                leading?.invoke()
            }
        }
        BasicText(
            title,
            Modifier
                .weight(1f)
                .padding(horizontal = 8f.dp),
            style = TextStyle(contentColor, 17f.sp, FontWeight.SemiBold)
        )
        Box(Modifier.size(40f.dp), contentAlignment = Alignment.Center) {
            trailing?.invoke()
        }
    }
}

/**
 * Glass search field — upstream navigation-bar material on a capsule.
 */
@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    isLightTheme: Boolean = true
) {
    val contentColor = IosColors.content(isLightTheme)
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = GlassMaterials.NavigationBar,
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.35f)) }
            )
            .height(40f.dp)
            .padding(horizontal = 14f.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8f.dp)
    ) {
        GlassIcon(GlassIcons.Search, contentColor.copy(alpha = 0.6f), size = 18f.dp)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                BasicText(
                    placeholder,
                    style = TextStyle(contentColor.copy(alpha = 0.45f), 15f.sp)
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(contentColor, 15f.sp),
                cursorBrush = SolidColor(IosColors.accent(isLightTheme)),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Glass text field — upstream card material on a G2 rounded rectangle.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    label: String = "Input Field",
    placeholder: String = "Enter text",
    isLightTheme: Boolean = true
) {
    val contentColor = IosColors.content(isLightTheme)
    val shape = RoundedRectangle(32f.dp)
    Column(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.Card,
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.3f)) }
            )
            .padding(horizontal = 16f.dp, vertical = 12f.dp),
        verticalArrangement = Arrangement.spacedBy(4f.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(contentColor.copy(alpha = 0.55f), 12f.sp, FontWeight.Medium)
        )
        Box(contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                BasicText(
                    placeholder,
                    style = TextStyle(contentColor.copy(alpha = 0.35f), 16f.sp)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(contentColor, 16f.sp),
                cursorBrush = SolidColor(IosColors.accent(isLightTheme)),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}