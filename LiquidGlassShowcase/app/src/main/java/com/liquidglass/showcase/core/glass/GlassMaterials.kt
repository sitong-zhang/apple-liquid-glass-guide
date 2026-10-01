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

package com.liquidglass.showcase.core.glass

import androidx.compose.ui.unit.dp
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * The complete Liquid Glass material set of this application.
 *
 * Every single numeric value below is copied **verbatim** from the upstream repository
 * [Kyant0/AndroidLiquidGlass] (Backdrop 2.0.0). No refraction height, refraction amount,
 * chromatic dispersion flag, blur radius, highlight intensity, shadow radius or corner
 * deformation has been re-tuned or re-invented here — the recipes are exactly the ones the
 * upstream catalog uses, and each one is annotated with its upstream source file.
 *
 * The render pipeline order is fixed by the library contract:
 *
 *     colorFilter  ⇒  blur  ⇒  lens
 *
 * which is why `vibrancy()` / `colorControls()` always come first, `blur()` second and
 * `lens()` last inside every recipe.
 */
object GlassMaterials {

    /** Upstream: `components/LiquidButton.kt` */
    val Button: BackdropEffectScope.() -> Unit = {
        vibrancy()
        blur(2f.dp.toPx())
        lens(12f.dp.toPx(), 24f.dp.toPx())
    }

    /** Upstream: `components/LiquidBottomTabs.kt` — bar background */
    val BottomBar: BackdropEffectScope.() -> Unit = {
        vibrancy()
        blur(8f.dp.toPx())
        lens(24f.dp.toPx(), 24f.dp.toPx())
    }

    /** Upstream: `components/LiquidBottomTabs.kt` — moving selection indicator */
    fun selectionIndicator(progress: Float): BackdropEffectScope.() -> Unit = {
        lens(
            10f.dp.toPx() * progress,
            14f.dp.toPx() * progress,
            chromaticAberration = true
        )
    }

    /** Upstream: `components/LiquidToggle.kt` — draggable knob */
    fun toggleKnob(progress: Float): BackdropEffectScope.() -> Unit = {
        blur(8f.dp.toPx() * (1f - progress))
        lens(
            5f.dp.toPx() * progress,
            10f.dp.toPx() * progress,
            chromaticAberration = true
        )
    }

    /** Upstream: `components/LiquidSlider.kt` — draggable knob */
    fun sliderKnob(progress: Float): BackdropEffectScope.() -> Unit = {
        blur(8f.dp.toPx() * (1f - progress))
        lens(
            10f.dp.toPx() * progress,
            14f.dp.toPx() * progress,
            chromaticAberration = true
        )
    }

    /** Upstream: `destinations/DialogContent.kt` */
    fun dialog(isLightTheme: Boolean): BackdropEffectScope.() -> Unit = {
        colorControls(
            brightness = if (isLightTheme) 0.2f else 0f,
            saturation = 1.5f
        )
        blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
        lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
    }

    /** Upstream: `tutorials/glass-bottom-bar` */
    val NavigationBar: BackdropEffectScope.() -> Unit = {
        vibrancy()
        blur(4f.dp.toPx())
        lens(16f.dp.toPx(), 32f.dp.toPx())
    }

    /**
     * Upstream: `tutorials/glass-bottom-sheet`
     * (`RoundedCornerShape(44.dp)` + `renderEffect` free surface `Color.White @ 50%`).
     */
    val BottomSheet: BackdropEffectScope.() -> Unit = {
        vibrancy()
        blur(4f.dp.toPx())
        lens(24f.dp.toPx(), 48f.dp.toPx(), true)
    }

    /** Upstream: `destinations/LazyScrollContainerContent.kt` */
    val Card: BackdropEffectScope.() -> Unit = {
        vibrancy()
        lens(16f.dp.toPx(), 32f.dp.toPx())
    }

    /** Upstream: `destinations/ControlCenterContent.kt` */
    fun controlCenterItem(progress: Float): BackdropEffectScope.() -> Unit = {
        vibrancy()
        lens(
            24f.dp.toPx() * progress,
            48f.dp.toPx() * progress,
            depthEffect = true
        )
    }
}