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
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One app shown on the simulated iOS home screen.
 *
 * @param glyph line/fill geometry from [IosGlyphs] (generated from ui-icons-hub).
 * @param top / [bottom] the two stops of the icon squircle gradient.
 */
data class IosApp(
    val id: String,
    val label: String,
    val glyph: ImageVector,
    val top: Color,
    val bottom: Color,
    val glyphTint: Color = Color.White
)

/**
 * The simulator's app set: one dock row plus two home-screen pages.
 *
 * Every glyph is taken 1:1 from the `Lucide` set bundled in the project's own asset hub
 * (ui-icons-hub, ISC licence) — see `tools/generate_ios_glyphs.py`. The squircle gradients
 * are the simulator's own decoration and carry no Apple artwork.
 */
object IosAppCatalog {

    val dock: List<IosApp> = listOf(
        IosApp("phone", "Phone", IosGlyphs.Phone, Color(0xFF6BE585), Color(0xFF25C55B)),
        IosApp("safari", "Safari", IosGlyphs.Safari, Color(0xFF5AC8FA), Color(0xFF0A6CFF)),
        IosApp("messages", "Messages", IosGlyphs.Messages, Color(0xFF74E86B), Color(0xFF1FBF3F)),
        IosApp("music", "Music", IosGlyphs.Music, Color(0xFFFF6B81), Color(0xFFF5233F))
    )

    /** Page 1 — the "everyday" apps. */
    private val page1: List<IosApp> = listOf(
        IosApp("facetime", "FaceTime", IosGlyphs.FaceTime, Color(0xFF5CE86B), Color(0xFF12B93C)),
        IosApp("calendar", "Calendar", IosGlyphs.Calendar, Color(0xFFFFFFFF), Color(0xFFE6E6EC), Color(0xFFE0342B)),
        IosApp("photos", "Photos", IosGlyphs.Photos, Color(0xFFFFD60A), Color(0xFFFF375F)),
        IosApp("camera", "Camera", IosGlyphs.Camera, Color(0xFF9A9AA0), Color(0xFF45454A)),
        IosApp("mail", "Mail", IosGlyphs.Mail, Color(0xFF63B4FF), Color(0xFF0A6CFF)),
        IosApp("notes", "Notes", IosGlyphs.Notes, Color(0xFFFFF3C4), Color(0xFFFFD84D), Color(0xFF6B5200)),
        IosApp("reminders", "Reminders", IosGlyphs.Reminders, Color(0xFFFFFFFF), Color(0xFFE6E6EC), Color(0xFF3A3A3C)),
        IosApp("clock", "Clock", IosGlyphs.Clock, Color(0xFF3A3A3C), Color(0xFF101012)),
        IosApp("maps", "Maps", IosGlyphs.Maps, Color(0xFF7BE38F), Color(0xFF2E9E4F)),
        IosApp("weather", "Weather", IosGlyphs.Weather, Color(0xFF5BC8FF), Color(0xFF0A7BFF)),
        IosApp("news", "News", IosGlyphs.News, Color(0xFFFF7A7A), Color(0xFFE01E43)),
        IosApp("stocks", "Stocks", IosGlyphs.Stocks, Color(0xFF4A4A4E), Color(0xFF1C1C1E)),
        IosApp("books", "Books", IosGlyphs.Books, Color(0xFFFF9F5A), Color(0xFFE56500)),
        IosApp("appstore", "App Store", IosGlyphs.AppStore, Color(0xFF3ED1FF), Color(0xFF0A84FF)),
        IosApp("health", "Health", IosGlyphs.Health, Color(0xFFFF7A9A), Color(0xFFE01542)),
        IosApp("wallet", "Wallet", IosGlyphs.Wallet, Color(0xFF5A5A60), Color(0xFF1C1C1E)),
        IosApp("files", "Files", IosGlyphs.Files, Color(0xFF6FC8FF), Color(0xFF0A84FF)),
        IosApp("podcasts", "Podcasts", IosGlyphs.Podcasts, Color(0xFFC97BFF), Color(0xFF7B2DE2)),
        IosApp("tv", "TV", IosGlyphs.Tv, Color(0xFF3A3A3C), Color(0xFF0B0B0C)),
        IosApp("home", "Home", IosGlyphs.Home, Color(0xFFFFB464), Color(0xFFE06A00))
    )

    /** Page 2 — utilities, plus the door back to the 26-component catalogue. */
    private val page2: List<IosApp> = listOf(
        IosApp("settings", "Settings", IosGlyphs.Settings, Color(0xFFC4C4CB), Color(0xFF7E7E86)),
        IosApp("calculator", "Calculator", IosGlyphs.Calculator, Color(0xFFFFB340), Color(0xFFE07E00)),
        IosApp("gallery", "Component Gallery", IosGlyphs.Grip, Color(0xFF8E7BFF), Color(0xFF5A3BFF)),
        IosApp("tips", "Tips", IosGlyphs.Tips, Color(0xFFFFD452), Color(0xFFE08C00), Color(0xFF5A3D00)),
        IosApp("translate", "Translate", IosGlyphs.Translate, Color(0xFF6FA8FF), Color(0xFF1F6FFF)),
        IosApp("measure", "Measure", IosGlyphs.Measure, Color(0xFFE0E0E6), Color(0xFF9A9AA0), Color(0xFF3A3A3C)),
        IosApp("shortcuts", "Shortcuts", IosGlyphs.Shortcuts, Color(0xFF7A8CFF), Color(0xFF3B4FFF)),
        IosApp("fitness", "Fitness", IosGlyphs.Fitness, Color(0xFF7CFFB2), Color(0xFF00A03F), Color(0xFF00401A)),
        IosApp("voicememos", "Voice Memos", IosGlyphs.VoiceMemos, Color(0xFF3A3A3C), Color(0xFF1C1C1E)),
        IosApp("magnifier", "Magnifier", IosGlyphs.Magnifier, Color(0xFF9AA0A6), Color(0xFF5A5A60)),
        IosApp("globe", "Browser", IosGlyphs.Globe, Color(0xFF7ED4FF), Color(0xFF1E88E5)),
        IosApp("flashlight", "Flashlight", IosGlyphs.Flashlight, Color(0xFFFFFFFF), Color(0xFFD6D6DC), Color(0xFF3A3A3C)),
        IosApp("headphones", "Headphones", IosGlyphs.Headphones, Color(0xFFB0B0B8), Color(0xFF5A5A60)),
        IosApp("timer", "Timer", IosGlyphs.Timer, Color(0xFF3A3A3C), Color(0xFF101012)),
        IosApp("alarm", "Alarm", IosGlyphs.AlarmClock, Color(0xFF3A3A3C), Color(0xFF101012)),
        IosApp("bell", "Sound & Haptics", IosGlyphs.Bell, Color(0xFFFFC56B), Color(0xFFE07E00), Color(0xFF4A2A00))
    )

    val pages: List<List<IosApp>> = listOf(page1, page2)

    /** Every app that can be opened, dock included. */
    val allApps: List<IosApp> = dock + pages.flatten()
}
