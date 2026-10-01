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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.liquidglass.showcase.core.glass.GlassMaterials
import com.liquidglass.showcase.core.ios.GlassIcons
import com.liquidglass.showcase.core.ios.IosColors
import com.liquidglass.showcase.core.utils.rememberUISensor

/**
 * iOS modal dialog.
 *
 * Material: upstream `destinations/DialogContent.kt`
 * (`colorControls(brightness, saturation = 1.5)` + `blur(16/8.dp)` + `lens(24.dp, 48.dp, depthEffect)`)
 * with `Highlight.Plain` and `RoundedRectangle(48.dp)`, plus the upstream dim layer.
 * The two action buttons are glass-on-glass and use the documented `exportedBackdrop` technique.
 */
@Composable
fun BoxScope.GlassDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    title: String,
    message: String,
    cancelText: String = "Cancel",
    confirmText: String = "OK",
    onConfirm: () -> Unit = {},
    isLightTheme: Boolean = true
) {
    val contentColor = IosColors.content(isLightTheme)
    val containerColor = IosColors.dialogContainer(isLightTheme)
    val dimColor = IosColors.dim(isLightTheme)
    val accentColor = IosColors.accent(isLightTheme)
    val shape = RoundedRectangle(48f.dp)
    val panelBackdrop = rememberLayerBackdrop()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300, easing = EaseOutCubic)),
        exit = fadeOut(tween(200, easing = EaseOutCubic)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(dimColor)
                }
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(360, easing = EaseOutCubic)) +
                        scaleIn(
                            initialScale = 0.86f,
                            animationSpec = tween(1200, easing = EaseOutCubic)
                        ),
                exit = fadeOut(tween(200, easing = EaseOutCubic)) +
                        scaleOut(
                            targetScale = 0.92f,
                            animationSpec = tween(200, easing = EaseOutCubic)
                        )
            ) {
                Column(
                    Modifier
                        .padding(40f.dp)
                        .widthIn(max = 340f.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = GlassMaterials.dialog(isLightTheme),
                            highlight = { Highlight.Plain },
                            exportedBackdrop = panelBackdrop,
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .fillMaxWidth()
                ) {
                    BasicText(
                        title,
                        Modifier.padding(28f.dp, 24f.dp, 28f.dp, 12f.dp),
                        style = TextStyle(contentColor, 24f.sp, FontWeight.Medium)
                    )
                    BasicText(
                        message,
                        Modifier.padding(24f.dp, 12.dp, 24f.dp, 12f.dp),
                        style = TextStyle(contentColor.copy(alpha = 0.68f), 15f.sp)
                    )
                    Row(
                        Modifier
                            .padding(24f.dp, 12f.dp, 24f.dp, 24f.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16f.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DialogActionButton(
                            text = cancelText,
                            backdrop = panelBackdrop,
                            containerColor = containerColor.copy(alpha = 0.2f),
                            contentColor = contentColor,
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss
                        )
                        DialogActionButton(
                            text = confirmText,
                            backdrop = panelBackdrop,
                            containerColor = accentColor,
                            contentColor = IosColors.onAccent(),
                            modifier = Modifier.weight(1f),
                            onClick = onConfirm
                        )
                    }
                }
            }
        }
    }
}

/** Upstream `DialogContent.kt` capsule action button (raised on top of the dialog glass). */
@Composable
private fun DialogActionButton(
    text: String,
    backdrop: Backdrop,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = Capsule()
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.Button,
                onDrawSurface = { drawRect(containerColor) }
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .height(48f.dp)
            .padding(horizontal = 16f.dp),
        horizontalArrangement = Arrangement.spacedBy(4f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(text, style = TextStyle(contentColor, 16f.sp))
    }
}

/**
 * iOS alert — the dialog glass in a compact, centered layout.
 */
@Composable
fun BoxScope.GlassAlert(
    visible: Boolean,
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    title: String,
    message: String,
    confirmText: String = "OK",
    isLightTheme: Boolean = true
) {
    val contentColor = IosColors.content(isLightTheme)
    val containerColor = IosColors.dialogContainer(isLightTheme)
    val dimColor = IosColors.dim(isLightTheme)
    val shape = RoundedRectangle(48f.dp)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300, easing = EaseOutCubic)),
        exit = fadeOut(tween(200, easing = EaseOutCubic)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(dimColor)
                }
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(360, easing = EaseOutCubic)) +
                        scaleIn(
                            initialScale = 0.88f,
                            animationSpec = tween(1200, easing = EaseOutCubic)
                        ),
                exit = fadeOut(tween(200, easing = EaseOutCubic)) +
                        scaleOut(targetScale = 0.94f, animationSpec = tween(200, easing = EaseOutCubic))
            ) {
                Column(
                    Modifier
                        .padding(48f.dp)
                        .widthIn(max = 300f.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = GlassMaterials.dialog(isLightTheme),
                            highlight = { Highlight.Plain },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .fillMaxWidth()
                        .padding(24f.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12f.dp)
                ) {
                    GlassIcon(GlassIcons.Shield, contentColor, size = 32f.dp)
                    BasicText(
                        title,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 19f.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    )
                    BasicText(
                        message,
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.68f),
                            fontSize = 14f.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    Box(
                        Modifier
                            .padding(top = 4f.dp)
                            .fillMaxWidth()
                            .clip(Capsule())
                            .background(IosColors.accent(isLightTheme))
                            .clickable(onClick = onDismiss)
                            .height(44f.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(confirmText, style = TextStyle(IosColors.onAccent(), 16f.sp))
                    }
                }
            }
        }
    }
}

/**
 * iOS action sheet.
 *
 * Material: upstream `tutorials/glass-bottom-sheet`
 * (`vibrancy()` + `blur(4.dp)` + `lens(24.dp, 48.dp, depthEffect = true)` on `RoundedRectangle(44.dp)`,
 * surface `Color.White @ 50%`).
 */
@Composable
fun BoxScope.GlassActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    title: String,
    actions: List<String>,
    onAction: (String) -> Unit = {},
    isLightTheme: Boolean = true
) {
    val contentColor = IosColors.content(isLightTheme)
    val dimColor = IosColors.dim(isLightTheme)
    val shape = RoundedRectangle(44f.dp)
    val sheetBackdrop = rememberLayerBackdrop()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300, easing = EaseOutCubic)),
        exit = fadeOut(tween(200, easing = EaseOutCubic)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(dimColor)
                }
                .clickable(interactionSource = null, indication = null, onClick = onDismiss)
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    Modifier
                        .padding(16f.dp)
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = GlassMaterials.BottomSheet,
                            exportedBackdrop = sheetBackdrop,
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
                        )
                        .padding(16f.dp),
                    verticalArrangement = Arrangement.spacedBy(10f.dp)
                ) {
                    BasicText(
                        title,
                        Modifier.padding(start = 8f.dp, top = 4f.dp, bottom = 4f.dp),
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 13f.sp, FontWeight.Medium)
                    )
                    actions.forEach { action ->
                        SheetActionRow(
                            text = action,
                            backdrop = sheetBackdrop,
                            contentColor = contentColor,
                            onClick = {
                                onAction(action)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Glass-on-glass row inside the action sheet (documented `exportedBackdrop` usage). */
@Composable
private fun SheetActionRow(
    text: String,
    backdrop: Backdrop,
    contentColor: Color,
    onClick: () -> Unit
) {
    val shape = Capsule()
    Row(
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.NavigationBar,
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .height(52f.dp)
            .fillMaxWidth()
            .padding(horizontal = 20f.dp),
        horizontalArrangement = Arrangement.spacedBy(4f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(text, style = TextStyle(contentColor, 16f.sp))
    }
}

/**
 * iOS toast / HUD — upstream navigation-bar material on a capsule.
 */
@Composable
fun BoxScope.GlassToast(
    visible: Boolean,
    backdrop: Backdrop,
    text: String,
    isLightTheme: Boolean = true
) {
    val contentColor = IosColors.content(isLightTheme)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300, easing = EaseOutCubic)) +
                scaleIn(initialScale = 0.9f, animationSpec = tween(1200, easing = EaseOutCubic)),
        exit = fadeOut(tween(200, easing = EaseOutCubic)) +
                scaleOut(targetScale = 0.95f, animationSpec = tween(200, easing = EaseOutCubic)),
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        Row(
            Modifier
                .padding(top = 24f.dp)
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
                .height(44f.dp)
                .padding(horizontal = 18f.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8f.dp)
        ) {
            GlassIcon(GlassIcons.Check, contentColor, size = 18f.dp)
            BasicText(text, style = TextStyle(contentColor, 15f.sp, FontWeight.Medium))
        }
    }
}

/**
 * iOS popover / context menu — dialog glass on a smaller G2 rectangle.
 */
@Composable
fun BoxScope.GlassPopoverMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    items: List<String>,
    onItemClick: (String) -> Unit = {},
    isLightTheme: Boolean = true
) {
    val contentColor = IosColors.content(isLightTheme)
    val containerColor = IosColors.dialogContainer(isLightTheme)
    val shape = RoundedRectangle(48f.dp)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(240, easing = EaseOutCubic)) +
                scaleIn(initialScale = 0.9f, animationSpec = tween(1200, easing = EaseOutCubic)),
        exit = fadeOut(tween(160, easing = EaseOutCubic)) +
                scaleOut(targetScale = 0.95f, animationSpec = tween(160, easing = EaseOutCubic)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .widthIn(max = 280f.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = GlassMaterials.dialog(isLightTheme),
                        highlight = { Highlight.Plain },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .padding(vertical = 8f.dp)
            ) {
                items.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onItemClick(item)
                                onDismiss()
                            }
                            .height(48f.dp)
                            .padding(horizontal = 20f.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12f.dp)
                    ) {
                        GlassIcon(GlassIcons.Ellipsis, contentColor.copy(alpha = 0.7f), size = 18f.dp)
                        BasicText(item, style = TextStyle(contentColor, 16f.sp))
                    }
                }
            }
        }
    }
}

/**
 * iOS wheel picker.
 *
 * Container: upstream bottom-sheet material; the selection band inside is the upstream
 * tab-bar indicator expression (`lens(10.dp * press, 14.dp * press, chromaticAberration = true)`).
 */
@Composable
fun GlassPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true
) {
    val contentColor = IosColors.content(isLightTheme)
    val shape = RoundedRectangle(44f.dp)
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.BottomSheet,
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
            )
            .height(180f.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(40f.dp)
                .padding(horizontal = 12f.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        lens(10f.dp.toPx(), 14f.dp.toPx(), chromaticAberration = true)
                    },
                    shadow = null,
                    onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.08f)) }
                )
        )
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.take(4).forEachIndexed { index, item ->
                val distance = kotlin.math.abs(index - selectedIndex)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(40f.dp)
                        .clickable { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        item,
                        style = TextStyle(
                            color = contentColor.copy(alpha = (1f - distance * 0.28f).coerceIn(0.3f, 1f)),
                            fontSize = (17f - distance * 1.5f).sp,
                            fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

/**
 * iOS control center.
 *
 * Material: upstream `destinations/ControlCenterContent.kt` (`vibrancy()` +
 * `lens(24.dp * progress, 48.dp * progress, depthEffect = true)`, surface `Color.Black @ 5%`,
 * `shadow = null`) with the gravity-driven highlight
 * (`HighlightStyle.Default(angle = gravityAngle, falloff = 2f)`).
 */
@Composable
fun GlassControlCenter(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true
) {
    val accentColor = IosColors.accent(isLightTheme)
    val uiSensor = rememberUISensor()

    val itemSpacing = 16f.dp
    val itemSize = 68f.dp
    val itemTwoSpanSize = itemSize * 2 + itemSpacing
    val itemShape: Shape = RoundedRectangle(itemSize / 2f)

    val glassHighlight = {
        Highlight(
            style = HighlightStyle.Default(
                angle = uiSensor.gravityAngle,
                falloff = 2f
            )
        )
    }

    @Composable
    fun ControlCenterItem(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit = {}
    ) {
        Box(
            modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { itemShape },
                    effects = GlassMaterials.controlCenterItem(1f),
                    highlight = glassHighlight,
                    shadow = null,
                    onDrawSurface = { drawRect(IosColors.ControlCenterSurface) }
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    }

    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(itemSpacing)) {
            ControlCenterItem(Modifier.size(itemTwoSpanSize)) {
                Box(
                    Modifier
                        .clip(Capsule())
                        .background(accentColor)
                        .size(56f.dp)
                )
            }
            ControlCenterItem(Modifier.size(itemTwoSpanSize)) {
                GlassIcon(GlassIcons.Wifi, IosColors.content(isLightTheme), size = 28f.dp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(itemSpacing)) {
            ControlCenterItem(Modifier.size(itemSize, itemTwoSpanSize)) {
                GlassIcon(GlassIcons.Volume, IosColors.content(isLightTheme), size = 28f.dp)
            }
            ControlCenterItem(Modifier.size(itemSize, itemTwoSpanSize)) {
                GlassIcon(GlassIcons.Sun, IosColors.content(isLightTheme), size = 28f.dp)
            }
            ControlCenterItem(Modifier.size(itemSize, itemTwoSpanSize)) {
                GlassIcon(GlassIcons.Bluetooth, IosColors.content(isLightTheme), size = 28f.dp)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlCenterItem(Modifier.size(itemSize)) {
                GlassIcon(GlassIcons.Airplane, IosColors.content(isLightTheme), size = 28f.dp)
            }
            Spacer(Modifier.height(itemSpacing))
            ControlCenterItem(Modifier.size(itemSize)) {
                GlassIcon(GlassIcons.Moon, IosColors.content(isLightTheme), size = 28f.dp)
            }
        }
    }
}