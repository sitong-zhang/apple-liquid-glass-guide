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

package com.liquidglass.showcase

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.liquidglass.showcase.core.glass.GlassScaffold
import com.liquidglass.showcase.core.ios.isLightTheme
import com.liquidglass.showcase.ios.IosShell
import com.liquidglass.showcase.screens.SplashScreen

/** Duration of the liquid-glass loading animation before the transition starts. */
private const val SplashDurationMillis = 1600

/** The requested splash → catalog transition: 1.2 s with an easeOutCubic interpolator. */
private const val TransitionMillis = 1200

/**
 * Application root.
 *
 * The splash plays over the [GlassScaffold] wallpaper and then hands over to [IosShell], which
 * brings its own procedural wallpaper of the same size. Both screens are cross-faded with the
 * required 1.2 s `easeOutCubic` transition, so the glass keeps refracting a continuous wallpaper
 * layer while it moves — no hard cut, and the material stays coherent.
 */
@Composable
fun LiquidGlassApp() {
    val isLight = isLightTheme()

    val progress = remember { Animatable(0f) }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = SplashDurationMillis, easing = LinearEasing)
        )
        isLoaded = true
    }

    GlassScaffold { backdrop ->
        AnimatedVisibility(
            visible = !isLoaded,
            enter = EnterTransition.None,
            exit = fadeOut(tween(TransitionMillis, easing = EaseOutCubic)) +
                    scaleOut(
                        targetScale = 1.12f,
                        animationSpec = tween(TransitionMillis, easing = EaseOutCubic)
                    )
        ) {
            Box(Modifier.fillMaxSize()) {
                SplashScreen(
                    backdrop = backdrop,
                    progress = progress.value,
                    isLightTheme = isLight
                )
            }
        }

        AnimatedVisibility(
            visible = isLoaded,
            enter = fadeIn(tween(TransitionMillis, easing = EaseOutCubic)) +
                    scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(TransitionMillis, easing = EaseOutCubic)
                    ),
            exit = ExitTransition.None
        ) {
            Box(Modifier.fillMaxSize()) {
                IosShell(isLightTheme = isLight)
            }
        }
    }
}