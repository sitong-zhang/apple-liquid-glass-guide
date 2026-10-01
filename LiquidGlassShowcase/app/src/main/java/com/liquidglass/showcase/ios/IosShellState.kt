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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.geometry.Rect

/** The two full-screen states of the simulated system. */
enum class IosSurface { Lock, Home }

/**
 * Everything the simulated iOS shell needs to remember.
 *
 * Kept in one holder so the lock screen, home screen, system chrome and the overlay surfaces can
 * all be driven from a single source of truth without threading a dozen lambdas through.
 */
@Stable
class IosShellState {

    /** Lock screen or home screen. */
    var surface by mutableStateOf(IosSurface.Lock)

    /** Current home-screen page index. */
    var page by mutableIntStateOf(0)

    /** The app that currently owns the screen, if any. */
    var openApp by mutableStateOf<IosApp?>(null)

    /** Home-screen bounds of the icon that launched [openApp] — drives the zoom transition. */
    var openAppOrigin by mutableStateOf(Rect.Zero)

    /** Most-recently-used order, newest first (drives the app switcher). */
    val recents = mutableListOf<IosApp>().toMutableStateList()

    var controlCenter by mutableStateOf(false)
    var notificationCenter by mutableStateOf(false)
    var appSwitcher by mutableStateOf(false)

    /** iOS "jiggle" edit mode on the home screen. */
    var jiggle by mutableStateOf(false)

    /** Dynamic Island expansion. */
    var islandExpanded by mutableStateOf(false)

    /** Wallpaper variant, cycled from the control center. */
    var wallpaper by mutableIntStateOf(0)

    /** True while the 26-component catalogue (a "built-in app") is on screen. */
    var galleryOpen by mutableStateOf(false)

    val isLocked: Boolean get() = surface == IosSurface.Lock

    fun unlock() {
        surface = IosSurface.Home
    }

    fun lock() {
        jiggle = false
        closeApp()
        closeOverlays()
        islandExpanded = false
        surface = IosSurface.Lock
    }

    fun openApp(app: IosApp, origin: Rect, gallery: Boolean) {
        openAppOrigin = origin
        openApp = app
        recents.remove(app)
        recents.add(0, app)
        if (recents.size > 6) recents.removeAt(recents.size - 1)
        galleryOpen = gallery
    }

    fun closeApp() {
        openApp = null
        galleryOpen = false
    }

    fun closeOverlays() {
        controlCenter = false
        notificationCenter = false
        appSwitcher = false
    }

    fun goHome() {
        closeApp()
        appSwitcher = false
        jiggle = false
    }

    /**
     * The swipe-up-from-the-bottom action.
     *
     * One gesture does three jobs, depending on what is on screen: it closes the app switcher, or
     * the open app, or — from the home screen — reveals the app switcher itself.
     */
    fun homeGesture() {
        when {
            appSwitcher -> appSwitcher = false
            openApp != null -> goHome()
            else -> {
                jiggle = false
                appSwitcher = true
            }
        }
    }
}

@Composable
fun rememberIosShellState(): IosShellState = remember { IosShellState() }
