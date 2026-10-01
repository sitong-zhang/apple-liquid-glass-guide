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

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import kotlinx.coroutines.launch

/** Height of the invisible strips along the top edge that start a pull-down. */
private val PullZoneHeight = 44.dp

/** Distance the pull-down gesture has to travel before the panel opens. */
private const val PullThresholdPx = 70f

/** Height of the invisible strip along the bottom edge that owns the home gesture. */
private val HomeGestureHeight = 40.dp

/**
 * The iOS system shell.
 *
 * Composition follows the real device's z-order: wallpaper, then everything that belongs to the
 * screen *content* (status bar, home screen, open app) recorded as one layer, then the surfaces
 * that float above it and refract it — lock screen, app switcher, control centre, notification
 * centre — and finally the hardware cut-out and the home indicator.
 *
 * Nothing here needs a navigation library: [IosShellState] is the whole state machine.
 */
@Composable
fun IosShell(isLightTheme: Boolean) {
    val state = rememberIosShellState()

    IosScaffold(
        isLightTheme = isLightTheme,
        wallpaperVariant = state.wallpaper
    ) { wallpaperBackdrop, surfaceBackdrop ->
        val overlayBackdrop = rememberCombinedBackdrop(wallpaperBackdrop, surfaceBackdrop)

        // Recording the content layer costs a full-screen offscreen pass every frame, so it only
        // runs while something above actually reads it. The content is drawn before the overlays,
        // so the layer is already filled by the time they sample it.
        val surfaceIsRead = state.isLocked || state.controlCenter || state.notificationCenter

        // ---- the content layer: what the pull-down surfaces blur ----------------------------
        Box(
            Modifier
                .fillMaxSize()
                .then(if (surfaceIsRead) Modifier.layerBackdrop(surfaceBackdrop) else Modifier)
        ) {
            IosHomeScreen(
                backdrop = wallpaperBackdrop,
                state = state,
                isLightTheme = isLightTheme
            )

            IosOpenAppWindow(
                state = state,
                backdrop = wallpaperBackdrop,
                isLightTheme = isLightTheme
            )
        }

        // ---- surfaces above the content -----------------------------------------------------
        // The status bar sits above the content layer so it stays readable over an open app, whose
        // window is opaque. It is not part of the recorded layer, which also means it survives the
        // pull-down panels drawn later.
        IosStatusBar(
            isLightTheme = isLightTheme,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (state.isLocked) {
            IosLockScreen(
                backdrop = overlayBackdrop,
                onUnlock = { state.unlock() }
            )
        }

        IosAppSwitcher(
            state = state,
            isLightTheme = isLightTheme,
            visible = state.appSwitcher
        )

        IosControlCenter(
            backdrop = overlayBackdrop,
            state = state,
            isLightTheme = isLightTheme,
            visible = state.controlCenter,
            onDismiss = { state.controlCenter = false }
        )

        IosNotificationCenter(
            backdrop = overlayBackdrop,
            isLightTheme = isLightTheme,
            visible = state.notificationCenter,
            onDismiss = { state.notificationCenter = false }
        )

        if (!state.isLocked && !state.appSwitcher) {
            IosPullDownZones(state)
            IosHomeGesture(state = state, isLightTheme = isLightTheme)
        }

        // The cut-out stays above every surface, exactly like the hardware.
        IosDynamicIsland(
            expanded = state.islandExpanded,
            onToggle = { state.islandExpanded = !state.islandExpanded },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 11.dp)
        )
    }
}

/**
 * Keeps the open app's window mounted across the close animation.
 *
 * [IosShellState.openApp] drops to `null` the moment the user goes home, but the window still has
 * to shrink back into its icon before it can be unmounted — so the shell holds its own copy and
 * only clears it once [IosAppWindow] reports that it has finished animating out.
 */
@Composable
private fun BoxScope.IosOpenAppWindow(
    state: IosShellState,
    backdrop: Backdrop,
    isLightTheme: Boolean
) {
    val openApp = state.openApp
    var windowApp by remember { mutableStateOf<IosApp?>(null) }
    var windowOrigin by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(openApp) {
        if (openApp != null) {
            windowOrigin = state.openAppOrigin
            windowApp = openApp
        }
    }

    val app = windowApp ?: return

    key(app.id) {
        IosAppWindow(
            app = app,
            origin = windowOrigin,
            visible = openApp != null,
            backdrop = backdrop,
            isLightTheme = isLightTheme,
            onFinished = { if (state.openApp == null) windowApp = null }
        )
    }
}

/**
 * The two invisible strips along the top edge.
 *
 * Pulling down on the left third opens the notification centre, on the right third the control
 * centre — the same hit areas iOS uses, and the same reason the Dynamic Island can still be tapped
 * in the middle: the strips leave a gap around it.
 */
@Composable
private fun BoxScope.IosPullDownZones(state: IosShellState) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(PullZoneHeight)
            .align(Alignment.TopCenter)
    ) {
        PullZone(Modifier.weight(0.4f)) { state.notificationCenter = true }
        Spacer(Modifier.weight(0.2f))
        PullZone(Modifier.weight(0.4f)) { state.controlCenter = true }
    }
}

/** One pull-down hit area. */
@Composable
private fun PullZone(modifier: Modifier = Modifier, onPull: () -> Unit) {
    val scope = rememberCoroutineScope()
    val travel = remember { Animatable(0f) }

    Box(
        modifier.draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { delta ->
                scope.launch { travel.snapTo(travel.value + delta) }
            },
            onDragStopped = { velocity ->
                val pulled = travel.value > PullThresholdPx || velocity > 800f
                travel.snapTo(0f)
                if (pulled) onPull()
            }
        )
    )
}

/**
 * The home indicator plus the swipe-up-from-the-bottom gesture.
 *
 * One gesture, three meanings — see [IosShellState.homeGesture].
 */
@Composable
private fun BoxScope.IosHomeGesture(
    state: IosShellState,
    isLightTheme: Boolean
) {
    val scope = rememberCoroutineScope()
    val travel = remember { Animatable(0f) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(HomeGestureHeight)
            .align(Alignment.BottomCenter)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    scope.launch { travel.snapTo(travel.value + delta) }
                },
                onDragStopped = { velocity ->
                    val triggered = travel.value < -70f || velocity < -900f
                    travel.snapTo(0f)
                    if (triggered) state.homeGesture()
                }
            )
    ) {
        IosHomeIndicator(
            isLightTheme = isLightTheme,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
