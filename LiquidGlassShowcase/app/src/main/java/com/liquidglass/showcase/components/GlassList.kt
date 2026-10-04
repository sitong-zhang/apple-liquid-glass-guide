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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.shapes.Capsule
import androidx.compose.foundation.shape.RoundedCornerShape
import com.liquidglass.showcase.core.glass.GlassMaterials
import com.liquidglass.showcase.core.ios.GlassIcons
import com.liquidglass.showcase.core.ios.IosColors

/**
 * iOS inset list group.
 *
 * Material: upstream `LazyScrollContainerContent.kt` card recipe
 * (`vibrancy()` + `lens(16.dp, 32.dp)`) on a `RoundedCornerShape(32.dp)`.
 */
@Composable
fun GlassListGroup(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(32f.dp)
    Column(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = GlassMaterials.Card,
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.22f)) }
            )
            .fillMaxWidth(),
        content = content
    )
}

/**
 * One row of a [GlassListGroup] — icon, title, optional subtitle and an iOS chevron.
 */
@Composable
fun GlassListItem(
    title: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    isLightTheme: Boolean = true,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val contentColor = IosColors.content(isLightTheme)
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(if (subtitle == null) 56f.dp else 68f.dp)
            .padding(horizontal = 16f.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12f.dp)
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = GlassMaterials.Button,
                        onDrawSurface = { drawRect(IosColors.accent(isLightTheme)) }
                    )
                    .size(34f.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassIcon(icon, IosColors.onAccent(), size = 18f.dp)
            }
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2f.dp)
        ) {
            BasicText(
                title,
                style = TextStyle(contentColor, 16f.sp, FontWeight.Medium)
            )
            if (subtitle != null) {
                BasicText(
                    subtitle,
                    style = TextStyle(contentColor.copy(alpha = 0.55f), 13f.sp)
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (showChevron) {
            GlassIcon(GlassIcons.ChevronRight, contentColor.copy(alpha = 0.35f), size = 16f.dp)
        }
    }
}

/** iOS hairline separator. */
@Composable
fun GlassListDivider(isLightTheme: Boolean, startPadding: Dp = 16f.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = startPadding)
            .height(0.5f.dp)
            .background(IosColors.content(isLightTheme).copy(alpha = 0.12f))
    )
}