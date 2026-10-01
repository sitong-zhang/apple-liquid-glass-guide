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

package com.liquidglass.showcase.core.ios

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * The exact iOS system colors / containers used by the upstream catalog components.
 * Sources: `LiquidToggle.kt`, `LiquidSlider.kt`, `LiquidBottomTabs.kt`, `DialogContent.kt`.
 */
object IosColors {

    val AccentLight = Color(0xFF0088FF)
    val AccentDark = Color(0xFF0091FF)

    val SwitchAccentLight = Color(0xFF34C759)
    val SwitchAccentDark = Color(0xFF30D158)

    fun accent(isLightTheme: Boolean): Color =
        if (isLightTheme) AccentLight else AccentDark

    fun switchAccent(isLightTheme: Boolean): Color =
        if (isLightTheme) SwitchAccentLight else SwitchAccentDark

    /** Upstream track color: `0xFF787878 @ 20%` (light) / `0xFF787880 @ 36%` (dark). */
    fun track(isLightTheme: Boolean): Color =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    /** Upstream container color of bars / tab bars: `0xFFFAFAFA @ 40%` / `0xFF121212 @ 40%`. */
    fun barContainer(isLightTheme: Boolean): Color =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    /** Upstream container color of dialogs: `0xFFFAFAFA @ 60%` / `0xFF121212 @ 40%`. */
    fun dialogContainer(isLightTheme: Boolean): Color =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f)
        else Color(0xFF121212).copy(0.4f)

    /** Upstream dim color of `DialogContent`: `0xFF29293A @ 23%` / `0xFF121212 @ 56%`. */
    fun dim(isLightTheme: Boolean): Color =
        if (isLightTheme) Color(0xFF29293A).copy(0.23f)
        else Color(0xFF121212).copy(0.56f)

    /** Upstream control-center item surface: `Color.Black @ 5%`. */
    val ControlCenterSurface: Color = Color.Black.copy(0.05f)

    fun content(isLightTheme: Boolean): Color =
        if (isLightTheme) Color.Black else Color.White

    fun secondaryContent(isLightTheme: Boolean): Color =
        content(isLightTheme).copy(alpha = 0.68f)

    fun onAccent(): Color = Color.White
}

@Composable
@ReadOnlyComposable
fun isLightTheme(): Boolean = !isSystemInDarkTheme()