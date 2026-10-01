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

package com.liquidglass.showcase.core.ios

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * A tiny, dependency-free icon set (24 x 24 viewport, iOS-like line weight) so that the
 * showcase does not have to pull in an unmaintained icon library.
 */
object GlassIcons {

    private const val STROKE_WIDTH = 1.8f

    private fun strokeIcon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            paths.forEach { data ->
                addPath(
                    addPathNodes(data),
                    fill = null,
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = STROKE_WIDTH,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()

    private fun filledIcon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            paths.forEach { data ->
                addPath(
                    addPathNodes(data),
                    fill = SolidColor(Color.White)
                )
            }
        }.build()

    val Gear: ImageVector = strokeIcon(
        "Gear",
        "M12,12 m-4,0 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0",
        "M17.5,12 L19.5,12",
        "M15.9,15.9 L17.3,17.3",
        "M12,17.5 L12,19.5",
        "M8.1,15.9 L6.7,17.3",
        "M6.5,12 L4.5,12",
        "M8.1,8.1 L6.7,6.7",
        "M12,6.5 L12,4.5",
        "M15.9,8.1 L17.3,6.7"
    )

    val Bell: ImageVector = strokeIcon(
        "Bell",
        "M12,4 C9.5,4 8,6 8,8.5 L8,11.8 L6.5,15 L17.5,15 L16,11.8 L16,8.5 C16,6 14.5,4 12,4 Z",
        "M10,17.5 a2,2 0 0 0 4,0"
    )

    val Wifi: ImageVector = strokeIcon(
        "Wifi",
        "M4,10 C7,7 17,7 20,10",
        "M7,13.5 C9,11.5 15,11.5 17,13.5",
        "M10,17 C11,16 13,16 14,17",
        "M12,19.5 m-0.7,0 a0.7,0.7 0 1,0 1.4,0 a0.7,0.7 0 1,0 -1.4,0"
    )

    val Bluetooth: ImageVector = strokeIcon(
        "Bluetooth",
        "M8,7.5 L16,16.5 L12,20 L12,4 L16,7.5 L8,16.5"
    )

    val Airplane: ImageVector = filledIcon(
        "Airplane",
        "M12,2.6 C12.8,2.6 13.3,3.4 13.3,4.3 L13.3,9.5 L20.5,13.2 L20.5,15.2 L13.3,13.9 L13.3,18.2 " +
                "L15.7,20.2 L15.7,21.8 L12,20.7 L8.3,21.8 L8.3,20.2 L10.7,18.2 L10.7,13.9 L3.5,15.2 " +
                "L3.5,13.2 L10.7,9.5 L10.7,4.3 C10.7,3.4 11.2,2.6 12,2.6 Z"
    )

    val Sun: ImageVector = strokeIcon(
        "Sun",
        "M12,12 m-3.8,0 a3.8,3.8 0 1,0 7.6,0 a3.8,3.8 0 1,0 -7.6,0",
        "M12,4 L12,2",
        "M12,22 L12,20",
        "M4,12 L2,12",
        "M22,12 L20,12",
        "M6.3,6.3 L4.9,4.9",
        "M19.1,19.1 L17.7,17.7",
        "M17.7,6.3 L19.1,4.9",
        "M4.9,19.1 L6.3,17.7"
    )

    val Moon: ImageVector = strokeIcon(
        "Moon",
        "M20,14.5 A8.5,8.5 0 0 1 9.5,4 A8.5,8.5 0 1 0 20,14.5 Z"
    )

    val Play: ImageVector = filledIcon("Play", "M8,5 L19,12 L8,19 Z")

    val Pause: ImageVector = filledIcon(
        "Pause",
        "M8,5 h3 v14 h-3 z",
        "M13,5 h3 v14 h-3 z"
    )

    val Heart: ImageVector = filledIcon(
        "Heart",
        "M12,20.5 C12,20.5 3,14.8 3,8.8 C3,6 5,4 7.5,4 C9.4,4 11,5.2 12,6.9 " +
                "C13,5.2 14.6,4 16.5,4 C19,4 21,6 21,8.8 C21,14.8 12,20.5 12,20.5 Z"
    )

    val Search: ImageVector = strokeIcon(
        "Search",
        "M11,11 m-5.5,0 a5.5,5.5 0 1,0 11,0 a5.5,5.5 0 1,0 -11,0",
        "M15.2,15.2 L20,20"
    )

    val ChevronRight: ImageVector = strokeIcon("ChevronRight", "M9,5 L16,12 L9,19")

    val ChevronLeft: ImageVector = strokeIcon("ChevronLeft", "M15,5 L8,12 L15,19")

    val ChevronDown: ImageVector = strokeIcon("ChevronDown", "M5,9.5 L12,16.5 L19,9.5")

    val Check: ImageVector = strokeIcon("Check", "M5,12.5 L10,17.5 L19,6.5")

    val Plus: ImageVector = strokeIcon("Plus", "M12,5 L12,19", "M5,12 L19,12")

    val Minus: ImageVector = strokeIcon("Minus", "M5,12 L19,12")

    val XMark: ImageVector = strokeIcon("XMark", "M6,6 L18,18", "M18,6 L6,18")

    val Ellipsis: ImageVector = filledIcon(
        "Ellipsis",
        "M6,12 m-1.6,0 a1.6,1.6 0 1,0 3.2,0 a1.6,1.6 0 1,0 -3.2,0",
        "M12,12 m-1.6,0 a1.6,1.6 0 1,0 3.2,0 a1.6,1.6 0 1,0 -3.2,0",
        "M18,12 m-1.6,0 a1.6,1.6 0 1,0 3.2,0 a1.6,1.6 0 1,0 -3.2,0"
    )

    val Person: ImageVector = strokeIcon(
        "Person",
        "M12,8.5 m-3.6,0 a3.6,3.6 0 1,0 7.2,0 a3.6,3.6 0 1,0 -7.2,0",
        "M5,20 C5,16.4 8,14.4 12,14.4 C16,14.4 19,16.4 19,20"
    )

    val Lock: ImageVector = strokeIcon(
        "Lock",
        "M6.5,10.5 h11 v9 h-11 z",
        "M9.3,10.5 V8 a2.7,2.7 0 0 1 5.4,0 V10.5"
    )

    val Share: ImageVector = strokeIcon(
        "Share",
        "M12,3 L12,14.5",
        "M12,3 L8.2,6.8",
        "M12,3 L15.8,6.8",
        "M6,12.5 v7.5 h12 v-7.5"
    )

    val Folder: ImageVector = strokeIcon(
        "Folder",
        "M3.5,7 h5.6 l2,2 h9.4 v10 h-17 z"
    )

    val Camera: ImageVector = strokeIcon(
        "Camera",
        "M4,8.5 h3.8 l1.6,-2 h5.2 l1.6,2 h3.8 v11 h-16 z",
        "M12,13.5 m-3.2,0 a3.2,3.2 0 1,0 6.4,0 a3.2,3.2 0 1,0 -6.4,0"
    )

    val Mic: ImageVector = strokeIcon(
        "Mic",
        "M12,3.6 a2.6,2.6 0 0 1 2.6,2.6 v5 a2.6,2.6 0 0 1 -5.2,0 v-5 a2.6,2.6 0 0 1 2.6,-2.6 z",
        "M6.8,11.2 a5.2,5.2 0 0 0 10.4,0",
        "M12,16.6 v3.8"
    )

    val Battery: ImageVector = strokeIcon(
        "Battery",
        "M3.5,8 h14 v8 h-14 z",
        "M19.5,10.8 v2.4",
        "M5.8,10.3 h7 v3.4 h-7 z"
    )

    val Calendar: ImageVector = strokeIcon(
        "Calendar",
        "M4,6.5 h16 v14 h-16 z",
        "M4,10.5 h16",
        "M8.5,3.6 v4",
        "M15.5,3.6 v4"
    )

    val Star: ImageVector = filledIcon(
        "Star",
        "M12,3.4 L14.7,9.1 L21,9.9 L16.3,14.3 L17.6,20.4 L12,17.3 L6.4,20.4 L7.7,14.3 L3,9.9 " +
                "L9.3,9.1 Z"
    )

    val Cloud: ImageVector = strokeIcon(
        "Cloud",
        "M7.5,18.5 a4.2,4.2 0 0 1 0,-8.4 a5.6,5.6 0 0 1 10.7,1.4 a3.4,3.4 0 0 1 -0.6,7 z"
    )

    val Shield: ImageVector = strokeIcon(
        "Shield",
        "M12,3.4 L19.6,6.4 v6 c0,4.1 -3.3,7.2 -7.6,8.7 c-4.3,-1.5 -7.6,-4.6 -7.6,-8.7 v-6 z",
        "M9,12 L11.4,14.4 L15.4,10.4"
    )

    val Clock: ImageVector = strokeIcon(
        "Clock",
        "M12,12 m-8.5,0 a8.5,8.5 0 1,0 17,0 a8.5,8.5 0 1,0 -17,0",
        "M12,7 L12,12.4 L15.6,14.4"
    )

    val Volume: ImageVector = strokeIcon(
        "Volume",
        "M5,9.5 h3 l4,-3.6 v12.2 l-4,-3.6 h-3 z",
        "M15.2,9.2 a4.2,4.2 0 0 1 0,5.6",
        "M17.8,6.8 a7.6,7.6 0 0 1 0,10.4"
    )

    val Grid: ImageVector = strokeIcon(
        "Grid",
        "M4,4.5 h6.5 v6.5 h-6.5 z",
        "M13.5,4.5 h6.5 v6.5 h-6.5 z",
        "M4,13 h6.5 v6.5 h-6.5 z",
        "M13.5,13 h6.5 v6.5 h-6.5 z"
    )

    val Sliders: ImageVector = strokeIcon(
        "Sliders",
        "M4,8 h9",
        "M17,8 h3",
        "M4,16 h4",
        "M12,16 h8",
        "M15,8 m-2,0 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0",
        "M10,16 m-2,0 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0"
    )

    val Droplet: ImageVector = filledIcon(
        "Droplet",
        "M12,3 C12,3 6,10.2 6,14.6 C6,18 8.7,20.8 12,20.8 C15.3,20.8 18,18 18,14.6 " +
                "C18,10.2 12,3 12,3 Z"
    )

    val CreditCard: ImageVector = strokeIcon(
        "CreditCard",
        "M3,6.5 h18 v11 h-18 z",
        "M3,10.5 h18"
    )
}