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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * App glyphs for the iOS shell.
 *
 * GENERATED FILE — do not edit by hand. Regenerate with
 * `tools/generate_ios_glyphs.py`.
 *
 * Geometry is taken 1:1 from the `Lucide` set bundled in the user's own asset hub
 * (ui-icons-hub, ISC licence). Only the container format changed (SVG -> Compose
 * [ImageVector]); no path has been redrawn or re-tuned.
 */
object IosGlyphs {

    private const val VIEWPORT = 24f

    @Suppress("LongMethod")
    private fun build(name: String, paths: List<Triple<String, Boolean, Float>>): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = VIEWPORT,
            viewportHeight = VIEWPORT
        ).apply {
            paths.forEach { (data, filled, strokeWidth) ->
                addPath(
                    pathData = addPathNodes(data),
                    fill = if (filled) SolidColor(Color.White) else null,
                    stroke = if (strokeWidth > 0f) SolidColor(Color.White) else null,
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()


    /** ui-icons-hub `lucide/phone` */
    val Phone: ImageVector = build(
        "Phone",
        listOf(
            Triple("M13.832 16.568a1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233a14 14 0 0 0 6.392 6.384", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/message-circle` */
    val Messages: ImageVector = build(
        "Messages",
        listOf(
            Triple("M2.992 16.342a2 2 0 0 1 .094 1.167l-1.065 3.29a1 1 0 0 0 1.236 1.168l3.413-.998a2 2 0 0 1 1.099.092a10 10 0 1 0-4.777-4.719", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/compass` */
    val Safari: ImageVector = build(
        "Safari",
        listOf(
            Triple("M2 12A10 10 0 1 0 22 12A10 10 0 1 0 2 12Z", false, 2f),
            Triple("m16.24 7.76l-1.804 5.411a2 2 0 0 1-1.265 1.265L7.76 16.24l1.804-5.411a2 2 0 0 1 1.265-1.265z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/camera` */
    val Camera: ImageVector = build(
        "Camera",
        listOf(
            Triple("M13.997 4a2 2 0 0 1 1.76 1.05l.486.9A2 2 0 0 0 18.003 7H20a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h1.997a2 2 0 0 0 1.759-1.048l.489-.904A2 2 0 0 1 10.004 4z", false, 2f),
            Triple("M9 13A3 3 0 1 0 15 13A3 3 0 1 0 9 13Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/image` */
    val Photos: ImageVector = build(
        "Photos",
        listOf(
            Triple("M5 3H19A2 2 0 0 1 21 5V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V5A2 2 0 0 1 5 3Z", false, 2f),
            Triple("M7 9A2 2 0 1 0 11 9A2 2 0 1 0 7 9Z", false, 2f),
            Triple("m21 15l-3.086-3.086a2 2 0 0 0-2.828 0L6 21", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/settings` */
    val Settings: ImageVector = build(
        "Settings",
        listOf(
            Triple("M9.671 4.136a2.34 2.34 0 0 1 4.659 0a2.34 2.34 0 0 0 3.319 1.915a2.34 2.34 0 0 1 2.33 4.033a2.34 2.34 0 0 0 0 3.831a2.34 2.34 0 0 1-2.33 4.033a2.34 2.34 0 0 0-3.319 1.915a2.34 2.34 0 0 1-4.659 0a2.34 2.34 0 0 0-3.32-1.915a2.34 2.34 0 0 1-2.33-4.033a2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915", false, 2f),
            Triple("M9 12A3 3 0 1 0 15 12A3 3 0 1 0 9 12Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/clock` */
    val Clock: ImageVector = build(
        "Clock",
        listOf(
            Triple("M2 12A10 10 0 1 0 22 12A10 10 0 1 0 2 12Z", false, 2f),
            Triple("M12 6v6l4 2", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/calendar` */
    val Calendar: ImageVector = build(
        "Calendar",
        listOf(
            Triple("M8 2v3m8-3v3", false, 2f),
            Triple("M5 3H19A2 2 0 0 1 21 5V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V5A2 2 0 0 1 5 3Z", false, 2f),
            Triple("M3 9h18", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/notebook-pen` */
    val Notes: ImageVector = build(
        "Notes",
        listOf(
            Triple("M13.4 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-7.4M2 6h4m-4 4h4m-4 4h4m-4 4h4", false, 2f),
            Triple("M21.378 5.626a1 1 0 1 0-3.004-3.004l-5.01 5.012a2 2 0 0 0-.506.854l-.837 2.87a.5.5 0 0 0 .62.62l2.87-.837a2 2 0 0 0 .854-.506z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/calculator` */
    val Calculator: ImageVector = build(
        "Calculator",
        listOf(
            Triple("M6 2H18A2 2 0 0 1 20 4V20A2 2 0 0 1 18 22H6A2 2 0 0 1 4 20V4A2 2 0 0 1 6 2Z", false, 2f),
            Triple("M8 6h8m0 8v4m0-8h.01M12 10h.01M8 10h.01M12 14h.01M8 14h.01M12 18h.01M8 18h.01", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/music` */
    val Music: ImageVector = build(
        "Music",
        listOf(
            Triple("M9 18V5l12-2v13", false, 2f),
            Triple("M3 18A3 3 0 1 0 9 18A3 3 0 1 0 3 18Z", false, 2f),
            Triple("M15 16A3 3 0 1 0 21 16A3 3 0 1 0 15 16Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/cloud-sun` */
    val Weather: ImageVector = build(
        "Weather",
        listOf(
            Triple("M12 2v2m-7.07.93l1.41 1.41M20 12h2m-2.93-7.07l-1.41 1.41m-1.713 6.31a4 4 0 0 0-5.925-4.128M13 22H7a5 5 0 1 1 4.9-6H13a3 3 0 0 1 0 6", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/map` */
    val Maps: ImageVector = build(
        "Maps",
        listOf(
            Triple("M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894l4.553-2.277a2 2 0 0 1 1.788 0zm.894.211v15M9 3.236v15", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/mail` */
    val Mail: ImageVector = build(
        "Mail",
        listOf(
            Triple("m22 7l-8.991 5.727a2 2 0 0 1-2.009 0L2 7", false, 2f),
            Triple("M4 4H20A2 2 0 0 1 22 6V18A2 2 0 0 1 20 20H4A2 2 0 0 1 2 18V6A2 2 0 0 1 4 4Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/store` */
    val AppStore: ImageVector = build(
        "AppStore",
        listOf(
            Triple("M15 21v-5a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v5m8.774-10.69a1.12 1.12 0 0 0-1.549 0a2.5 2.5 0 0 1-3.451 0a1.12 1.12 0 0 0-1.548 0a2.5 2.5 0 0 1-3.452 0a1.12 1.12 0 0 0-1.549 0a2.5 2.5 0 0 1-3.77-3.248l2.889-4.184A2 2 0 0 1 7 2h10a2 2 0 0 1 1.653.873l2.895 4.192a2.5 2.5 0 0 1-3.774 3.244", false, 2f),
            Triple("M4 10.95V19a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8.05", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/heart-pulse` */
    val Health: ImageVector = build(
        "Health",
        listOf(
            Triple("M2 9.5a5.5 5.5 0 0 1 9.591-3.676a.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5", false, 2f),
            Triple("M3.22 13H9.5l.5-1l2 4.5l2-7l1.5 3.5h5.27", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/wallet` */
    val Wallet: ImageVector = build(
        "Wallet",
        listOf(
            Triple("M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1", false, 2f),
            Triple("M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/list-todo` */
    val Reminders: ImageVector = build(
        "Reminders",
        listOf(
            Triple("M13 5h8m-8 7h8m-8 7h8M3 17l2 2l4-4", false, 2f),
            Triple("M4 4H8A1 1 0 0 1 9 5V9A1 1 0 0 1 8 10H4A1 1 0 0 1 3 9V5A1 1 0 0 1 4 4Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/folder` */
    val Files: ImageVector = build(
        "Files",
        listOf(
            Triple("M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/newspaper` */
    val News: ImageVector = build(
        "News",
        listOf(
            Triple("M15 18h-5m8-4h-8m-6 8h16a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v16a2 2 0 0 1-4 0v-9a2 2 0 0 1 2-2h2", false, 2f),
            Triple("M11 6H17A1 1 0 0 1 18 7V9A1 1 0 0 1 17 10H11A1 1 0 0 1 10 9V7A1 1 0 0 1 11 6Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/podcast` */
    val Podcasts: ImageVector = build(
        "Podcasts",
        listOf(
            Triple("M13 17a1 1 0 1 0-2 0l.5 4.5a.5.5 0 0 0 1 0z", true, 2f),
            Triple("M16.85 18.58a9 9 0 1 0-9.7 0", false, 2f),
            Triple("M8 14a5 5 0 1 1 8 0", false, 2f),
            Triple("M11 11A1 1 0 1 0 13 11A1 1 0 1 0 11 11Z", true, 2f),
        )
    )

    /** ui-icons-hub `lucide/tv` */
    val Tv: ImageVector = build(
        "Tv",
        listOf(
            Triple("m17 2l-5 5l-5-5", false, 2f),
            Triple("M4 7H20A2 2 0 0 1 22 9V20A2 2 0 0 1 20 22H4A2 2 0 0 1 2 20V9A2 2 0 0 1 4 7Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/house` */
    val Home: ImageVector = build(
        "Home",
        listOf(
            Triple("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8", false, 2f),
            Triple("M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/trending-up` */
    val Stocks: ImageVector = build(
        "Stocks",
        listOf(
            Triple("M16 7h6v6", false, 2f),
            Triple("m22 7l-8.5 8.5l-5-5L2 17", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/lightbulb` */
    val Tips: ImageVector = build(
        "Tips",
        listOf(
            Triple("M15 14c.2-1 .7-1.7 1.5-2.5c1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5c.7.7 1.3 1.5 1.5 2.5m0 4h6m-5 4h4", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/book-open` */
    val Books: ImageVector = build(
        "Books",
        listOf(
            Triple("M12 5v16m8.001-2A2 2 0 0 0 22 17V5a2 2 0 0 0-1.999-2L16 3.002A5 5 0 0 0 12 5a5 5 0 0 0-4-2H4a2 2 0 0 0-2 2v12a2 2 0 0 0 1.999 2H8a5 5 0 0 1 4 2a5 5 0 0 1 4-2z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/languages` */
    val Translate: ImageVector = build(
        "Translate",
        listOf(
            Triple("m5 8l6 6m-7 0l6-6l2-3M2 5h12M7 2h1m14 20l-5-10l-5 10m2-4h6", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/ruler` */
    val Measure: ImageVector = build(
        "Measure",
        listOf(
            Triple("M21.3 15.3a2.4 2.4 0 0 1 0 3.4l-2.6 2.6a2.4 2.4 0 0 1-3.4 0L2.7 8.7a2.41 2.41 0 0 1 0-3.4l2.6-2.6a2.41 2.41 0 0 1 3.4 0Zm-6.8-2.8l2-2m-5-1l2-2m-5-1l2-2m7 11l2-2", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/workflow` */
    val Shortcuts: ImageVector = build(
        "Shortcuts",
        listOf(
            Triple("M5 3H9A2 2 0 0 1 11 5V9A2 2 0 0 1 9 11H5A2 2 0 0 1 3 9V5A2 2 0 0 1 5 3Z", false, 2f),
            Triple("M7 11v4a2 2 0 0 0 2 2h4", false, 2f),
            Triple("M15 13H19A2 2 0 0 1 21 15V19A2 2 0 0 1 19 21H15A2 2 0 0 1 13 19V15A2 2 0 0 1 15 13Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/dumbbell` */
    val Fitness: ImageVector = build(
        "Fitness",
        listOf(
            Triple("M17.596 12.768a2 2 0 1 0 2.829-2.829l-1.768-1.767a2 2 0 0 0 2.828-2.829l-2.828-2.828a2 2 0 0 0-2.829 2.828l-1.767-1.768a2 2 0 1 0-2.829 2.829zM2.5 21.5l1.4-1.4M20.1 3.9l1.4-1.4M5.343 21.485a2 2 0 1 0 2.829-2.828l1.767 1.768a2 2 0 1 0 2.829-2.829l-6.364-6.364a2 2 0 1 0-2.829 2.829l1.768 1.767a2 2 0 0 0-2.828 2.829zM9.6 14.4l4.8-4.8", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/headphones` */
    val Headphones: ImageVector = build(
        "Headphones",
        listOf(
            Triple("M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/search` */
    val Magnifier: ImageVector = build(
        "Magnifier",
        listOf(
            Triple("m21 21l-4.34-4.34", false, 2f),
            Triple("M3 11A8 8 0 1 0 19 11A8 8 0 1 0 3 11Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/video` */
    val FaceTime: ImageVector = build(
        "FaceTime",
        listOf(
            Triple("m16 13l5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5", false, 2f),
            Triple("M4 6H14A2 2 0 0 1 16 8V16A2 2 0 0 1 14 18H4A2 2 0 0 1 2 16V8A2 2 0 0 1 4 6Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/globe` */
    val Globe: ImageVector = build(
        "Globe",
        listOf(
            Triple("M2 12A10 10 0 1 0 22 12A10 10 0 1 0 2 12Z", false, 2f),
            Triple("M12 2a14.5 14.5 0 0 0 0 20a14.5 14.5 0 0 0 0-20M2 12h20", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/flashlight` */
    val Flashlight: ImageVector = build(
        "Flashlight",
        listOf(
            Triple("M12 13v1m5-12a1 1 0 0 1 1 1v4a3 3 0 0 1-.6 1.8l-.6.8A4 4 0 0 0 16 12v8a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-8a4 4 0 0 0-.8-2.4l-.6-.8A3 3 0 0 1 6 7V3a1 1 0 0 1 1-1zM6 6h12", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/audio-lines` */
    val VoiceMemos: ImageVector = build(
        "VoiceMemos",
        listOf(
            Triple("M2 10v3m4-7v11m4-14v18m4-13v7m4-10v13m4-8v3", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/chevron-up` */
    val ChevronUp: ImageVector = build(
        "ChevronUp",
        listOf(
            Triple("m18 15l-6-6l-6 6", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/chevron-down` */
    val ChevronDown: ImageVector = build(
        "ChevronDown",
        listOf(
            Triple("m6 9l6 6l6-6", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/chevron-left` */
    val ChevronLeft: ImageVector = build(
        "ChevronLeft",
        listOf(
            Triple("m15 18l-6-6l6-6", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/chevron-right` */
    val ChevronRight: ImageVector = build(
        "ChevronRight",
        listOf(
            Triple("m9 18l6-6l-6-6", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/lock` */
    val Lock: ImageVector = build(
        "Lock",
        listOf(
            Triple("M5 11H19A2 2 0 0 1 21 13V20A2 2 0 0 1 19 22H5A2 2 0 0 1 3 20V13A2 2 0 0 1 5 11Z", false, 2f),
            Triple("M7 11V7a5 5 0 0 1 10 0v4", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/wifi` */
    val Wifi: ImageVector = build(
        "Wifi",
        listOf(
            Triple("M12 20h.01M2 8.82a15 15 0 0 1 20 0M5 12.859a10 10 0 0 1 14 0m-10.5 3.57a5 5 0 0 1 7 0", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/bluetooth` */
    val Bluetooth: ImageVector = build(
        "Bluetooth",
        listOf(
            Triple("m7 7l10 10l-5 5V2l5 5L7 17", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/plane` */
    val Plane: ImageVector = build(
        "Plane",
        listOf(
            Triple("M17.8 19.2L16 11l3.5-3.5C21 6 21.5 4 21 3c-1-.5-3 0-4.5 1.5L13 8L4.8 6.2c-.5-.1-.9.1-1.1.5l-.3.5c-.2.5-.1 1 .3 1.3L9 12l-2 3H4l-1 1l3 2l2 3l1-1v-3l3-2l3.5 5.3c.3.4.8.5 1.3.3l.5-.2c.4-.3.6-.7.5-1.2", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/signal-high` */
    val Signal: ImageVector = build(
        "Signal",
        listOf(
            Triple("M2 20h.01M7 20v-4m5 4v-8m5 8V8", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/battery-charging` */
    val BatteryCharging: ImageVector = build(
        "BatteryCharging",
        listOf(
            Triple("m11 7l-3 5h4l-3 5m5.856-11H16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-2.935M22 14v-4M5.14 18H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h2.936", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/sun` */
    val Sun: ImageVector = build(
        "Sun",
        listOf(
            Triple("M8 12A4 4 0 1 0 16 12A4 4 0 1 0 8 12Z", false, 2f),
            Triple("M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32l1.41 1.41M2 12h2m16 0h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/moon` */
    val Moon: ImageVector = build(
        "Moon",
        listOf(
            Triple("M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/volume-2` */
    val Volume: ImageVector = build(
        "Volume",
        listOf(
            Triple("M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298zM16 9a5 5 0 0 1 0 6m3.364 3.364a9 9 0 0 0 0-12.728", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/airplay` */
    val Airplay: ImageVector = build(
        "Airplay",
        listOf(
            Triple("M5 17H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-1", false, 2f),
            Triple("m12 15l5 6H7Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/cast` */
    val Cast: ImageVector = build(
        "Cast",
        listOf(
            Triple("M2 8V6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6M2 12a9 9 0 0 1 8 8m-8-4a5 5 0 0 1 4 4m-4 0h.01", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/timer` */
    val Timer: ImageVector = build(
        "Timer",
        listOf(
            Triple("M10 2h4m-2 12l3-3", false, 2f),
            Triple("M4 14A8 8 0 1 0 20 14A8 8 0 1 0 4 14Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/alarm-clock` */
    val AlarmClock: ImageVector = build(
        "AlarmClock",
        listOf(
            Triple("M4 13A8 8 0 1 0 20 13A8 8 0 1 0 4 13Z", false, 2f),
            Triple("M12 9v4l2 2M5 3L2 6m20 0l-3-3M6.38 18.7L4 21m13.64-2.33L20 21", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/globe` */
    val Globe2: ImageVector = build(
        "Globe2",
        listOf(
            Triple("M2 12A10 10 0 1 0 22 12A10 10 0 1 0 2 12Z", false, 2f),
            Triple("M12 2a14.5 14.5 0 0 0 0 20a14.5 14.5 0 0 0 0-20M2 12h20", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/play` */
    val Play: ImageVector = build(
        "Play",
        listOf(
            Triple("M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/pause` */
    val Pause: ImageVector = build(
        "Pause",
        listOf(
            Triple("M15 3H18A1 1 0 0 1 19 4V20A1 1 0 0 1 18 21H15A1 1 0 0 1 14 20V4A1 1 0 0 1 15 3Z", false, 2f),
            Triple("M6 3H9A1 1 0 0 1 10 4V20A1 1 0 0 1 9 21H6A1 1 0 0 1 5 20V4A1 1 0 0 1 6 3Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/skip-forward` */
    val SkipForward: ImageVector = build(
        "SkipForward",
        listOf(
            Triple("M21 4v16M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/skip-back` */
    val SkipBack: ImageVector = build(
        "SkipBack",
        listOf(
            Triple("M17.971 4.285A2 2 0 0 1 21 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432zM3 20V4", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/x` */
    val X: ImageVector = build(
        "X",
        listOf(
            Triple("M18 6L6 18M6 6l12 12", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/plus` */
    val Plus: ImageVector = build(
        "Plus",
        listOf(
            Triple("M5 12h14m-7-7v14", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/check` */
    val Check: ImageVector = build(
        "Check",
        listOf(
            Triple("M20 6L9 17l-5-5", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/trash` */
    val Trash: ImageVector = build(
        "Trash",
        listOf(
            Triple("M10 11v6m4-6v6m5-11v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/copy` */
    val Copy: ImageVector = build(
        "Copy",
        listOf(
            Triple("M10 8H20A2 2 0 0 1 22 10V20A2 2 0 0 1 20 22H10A2 2 0 0 1 8 20V10A2 2 0 0 1 10 8Z", false, 2f),
            Triple("M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/mic` */
    val Mic: ImageVector = build(
        "Mic",
        listOf(
            Triple("M12 19v3m7-12v2a7 7 0 0 1-14 0v-2", false, 2f),
            Triple("M12 2H12A3 3 0 0 1 15 5V12A3 3 0 0 1 12 15H12A3 3 0 0 1 9 12V5A3 3 0 0 1 12 2Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/bell` */
    val Bell: ImageVector = build(
        "Bell",
        listOf(
            Triple("M10.268 21a2 2 0 0 0 3.464 0m-10.47-5.674A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/bell-off` */
    val BellOff: ImageVector = build(
        "BellOff",
        listOf(
            Triple("M10.268 21a2 2 0 0 0 3.464 0M17 17H4a1 1 0 0 1-.74-1.673C4.59 13.956 6 12.499 6 8a6 6 0 0 1 .258-1.742M2 2l20 20M8.668 3.01A6 6 0 0 1 18 8c0 2.687.77 4.653 1.707 6.05", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/grip` */
    val Grip: ImageVector = build(
        "Grip",
        listOf(
            Triple("M11 5A1 1 0 1 0 13 5A1 1 0 1 0 11 5Z", false, 2f),
            Triple("M18 5A1 1 0 1 0 20 5A1 1 0 1 0 18 5Z", false, 2f),
            Triple("M4 5A1 1 0 1 0 6 5A1 1 0 1 0 4 5Z", false, 2f),
            Triple("M11 12A1 1 0 1 0 13 12A1 1 0 1 0 11 12Z", false, 2f),
            Triple("M18 12A1 1 0 1 0 20 12A1 1 0 1 0 18 12Z", false, 2f),
            Triple("M4 12A1 1 0 1 0 6 12A1 1 0 1 0 4 12Z", false, 2f),
            Triple("M11 19A1 1 0 1 0 13 19A1 1 0 1 0 11 19Z", false, 2f),
            Triple("M18 19A1 1 0 1 0 20 19A1 1 0 1 0 18 19Z", false, 2f),
            Triple("M4 19A1 1 0 1 0 6 19A1 1 0 1 0 4 19Z", false, 2f),
        )
    )

    /** ui-icons-hub `lucide/square-pen` */
    val Pen: ImageVector = build(
        "Pen",
        listOf(
            Triple("M12 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7", false, 2f),
            Triple("M18.375 2.625a1 1 0 0 1 3 3l-9.013 9.014a2 2 0 0 1-.853.505l-2.873.84a.5.5 0 0 1-.62-.62l.84-2.873a2 2 0 0 1 .506-.852z", false, 2f),
        )
    )
}
