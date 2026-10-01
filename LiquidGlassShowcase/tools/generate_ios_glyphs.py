#!/usr/bin/env python3
"""Generate Compose ImageVector glyphs for the iOS shell from ui-icons-hub assets.

Provenance
----------
Icon geometry is extracted verbatim from the user's own asset hub
(https://github.com/sitong-zhang/ui-icons-hub), specifically the bundled
``Lucide`` set (ISC licence, https://lucide.dev), stored as
``data/lucide-icons__lucide__<chunk>.js`` in that repository.

Nothing is re-drawn here: the SVG path data is copied 1:1 and only converted into
an equivalent Compose ``ImageVector``. Re-run this script to regenerate.

Usage
-----
    python3 tools/generate_ios_glyphs.py \
        --hub /path/to/ui-icons-hub \
        --out app/src/main/java/com/liquidglass/showcase/ios/IosGlyphs.kt
"""

from __future__ import annotations

import argparse
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

SVG_NS = "http://www.w3.org/2000/svg"

# (Kotlin identifier, Lucide icon name) — the glyphs the iOS shell needs.
ICONS: list[tuple[str, str]] = [
    # --- home screen / dock ---
    ("Phone", "phone"),
    ("Messages", "message-circle"),
    ("Safari", "compass"),
    ("Camera", "camera"),
    ("Photos", "image"),
    ("Settings", "settings"),
    ("Clock", "clock"),
    ("Calendar", "calendar"),
    ("Notes", "notebook-pen"),
    ("Calculator", "calculator"),
    ("Music", "music"),
    ("Weather", "cloud-sun"),
    ("Maps", "map"),
    ("Mail", "mail"),
    ("AppStore", "store"),
    ("Health", "heart-pulse"),
    ("Wallet", "wallet"),
    ("Reminders", "list-todo"),
    ("Files", "folder"),
    ("News", "newspaper"),
    ("Podcasts", "podcast"),
    ("Tv", "tv"),
    ("Home", "house"),
    ("Stocks", "trending-up"),
    ("Tips", "lightbulb"),
    ("Books", "book-open"),
    ("Translate", "languages"),
    ("Measure", "ruler"),
    ("Shortcuts", "workflow"),
    ("Fitness", "dumbbell"),
    ("Headphones", "headphones"),
    ("Magnifier", "search"),
    ("FaceTime", "video"),
    ("Globe", "globe"),
    ("Flashlight", "flashlight"),
    ("VoiceMemos", "audio-lines"),
    # --- lock screen ---
    ("ChevronUp", "chevron-up"),
    ("ChevronDown", "chevron-down"),
    ("ChevronLeft", "chevron-left"),
    ("ChevronRight", "chevron-right"),
    ("Lock", "lock"),
    # --- status bar / dynamic island ---
    ("Wifi", "wifi"),
    ("Bluetooth", "bluetooth"),
    ("Plane", "plane"),
    ("Signal", "signal-high"),
    ("BatteryCharging", "battery-charging"),
    # --- control center ---
    ("Sun", "sun"),
    ("Moon", "moon"),
    ("Volume", "volume-2"),
    ("Airplay", "airplay"),
    ("Cast", "cast"),
    ("Timer", "timer"),
    ("AlarmClock", "alarm-clock"),
    ("Globe2", "globe"),
    # --- media / misc ui ---
    ("Play", "play"),
    ("Pause", "pause"),
    ("SkipForward", "skip-forward"),
    ("SkipBack", "skip-back"),
    ("X", "x"),
    ("Plus", "plus"),
    ("Check", "check"),
    ("Trash", "trash"),
    ("Copy", "copy"),
    ("Mic", "mic"),
    ("Bell", "bell"),
    ("BellOff", "bell-off"),
    ("Grip", "grip"),
    ("Pen", "square-pen"),
]


def load_set(hub: Path, slug: str) -> tuple[dict[str, str], str]:
    """Return (name -> inner SVG markup, viewBox) for one ui-icons-hub set."""
    names: dict[str, str] = {}
    view_box = "0 0 24 24"
    index = json.loads((hub / "index.json").read_text(encoding="utf-8"))
    for entry in index["sets"]:
        if entry["slug"] == slug:
            found = re.search(r'viewBox="([^"]+)"', entry.get("wrap", ""))
            if found:
                view_box = found.group(1)
            break

    for path in sorted((hub / "data").glob(f"{slug}__*.js")):
        text = path.read_text(encoding="utf-8")
        for chunk in text.split("window.__ADD2(")[1:]:
            start = chunk.index(",") + 1
            payload = chunk[start : chunk.rindex("]") + 1]
            for entry in json.loads(payload):
                # ui-icons-hub entries are [name, svg_markup, ...optional metadata]
                names[entry[0]] = entry[1]
    return names, view_box


def _num(value: str | None, default: float = 0.0) -> float:
    return float(value) if value not in (None, "") else default


def _rounded_rect(x: float, y: float, w: float, h: float, rx: float, ry: float) -> str:
    if rx <= 0 and ry <= 0:
        return f"M{x:g} {y:g}H{x + w:g}V{y + h:g}H{x:g}Z"
    rx, ry = min(rx, w / 2), min(ry, h / 2)
    return (
        f"M{x + rx:g} {y:g}H{x + w - rx:g}A{rx:g} {ry:g} 0 0 1 {x + w:g} {y + ry:g}"
        f"V{y + h - ry:g}A{rx:g} {ry:g} 0 0 1 {x + w - rx:g} {y + h:g}"
        f"H{x + rx:g}A{rx:g} {ry:g} 0 0 1 {x:g} {y + h - ry:g}"
        f"V{y + ry:g}A{rx:g} {ry:g} 0 0 1 {x + rx:g} {y:g}Z"
    )


def _ellipse(cx: float, cy: float, rx: float, ry: float) -> str:
    return (
        f"M{cx - rx:g} {cy:g}A{rx:g} {ry:g} 0 1 0 {cx + rx:g} {cy:g}"
        f"A{rx:g} {ry:g} 0 1 0 {cx - rx:g} {cy:g}Z"
    )


def to_path(element: ET.Element, attrs: dict[str, str]) -> str | None:
    """Convert one basic SVG shape into an equivalent path `d` string."""
    tag = element.tag.replace(f"{{{SVG_NS}}}", "")

    if tag == "path":
        return attrs.get("d")
    if tag == "circle":
        r = _num(attrs.get("r"))
        return _ellipse(_num(attrs.get("cx")), _num(attrs.get("cy")), r, r)
    if tag == "ellipse":
        return _ellipse(
            _num(attrs.get("cx")), _num(attrs.get("cy")),
            _num(attrs.get("rx")), _num(attrs.get("ry")),
        )
    if tag == "rect":
        x, y = _num(attrs.get("x")), _num(attrs.get("y"))
        w, h = _num(attrs.get("width")), _num(attrs.get("height"))
        rx = _num(attrs.get("rx"))
        ry = _num(attrs.get("ry"), rx)
        if rx and not ry:
            ry = rx
        return _rounded_rect(x, y, w, h, rx, ry)
    if tag == "line":
        return (
            f"M{_num(attrs.get('x1')):g} {_num(attrs.get('y1')):g}"
            f"L{_num(attrs.get('x2')):g} {_num(attrs.get('y2')):g}"
        )
    if tag in ("polyline", "polygon"):
        points = attrs.get("points", "").replace(",", " ").split()
        if len(points) < 2:
            return None
        pairs = list(zip(points[0::2], points[1::2]))
        d = f"M{pairs[0][0]} {pairs[0][1]}" + "".join(f"L{x} {y}" for x, y in pairs[1:])
        return d + ("Z" if tag == "polygon" else "")
    return None


def collect_paths(markup: str) -> list[tuple[str, bool, float]]:
    """Return [(path_d, is_filled, stroke_width)] for one icon."""
    root = ET.fromstring(f'<svg xmlns="{SVG_NS}">{markup}</svg>')
    out: list[tuple[str, bool, float]] = []

    def walk(node: ET.Element, inherited: dict[str, str]) -> None:
        here = {**inherited, **node.attrib}
        tag = node.tag.replace(f"{{{SVG_NS}}}", "")

        if tag not in ("svg", "g", "defs", "title", "desc", "style", "clipPath"):
            d = to_path(node, here)
            if d:
                fill = (here.get("fill") or "").lower()
                stroke = (here.get("stroke") or "").lower()
                is_filled = fill not in ("", "none")
                stroked = stroke not in ("", "none")
                width = _num(here.get("stroke-width"), 2.0)
                out.append((d, is_filled, width if stroked else 0.0))

        for child in node:
            walk(child, here)

    walk(root, {})
    return out


HEADER = '''/*
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
'''


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--hub", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--slug", default="lucide-icons__lucide")
    args = parser.parse_args()

    available, view_box = load_set(args.hub, args.slug)
    if view_box != "0 0 24 24":
        raise SystemExit(f"unexpected viewBox {view_box!r}; adjust VIEWPORT in the template")

    lines = [HEADER]
    missing: list[str] = []
    seen: set[str] = set()

    for kotlin_name, icon_name in ICONS:
        if kotlin_name in seen:
            continue
        seen.add(kotlin_name)
        markup = available.get(icon_name)
        if markup is None:
            missing.append(f"{kotlin_name} ({icon_name})")
            continue
        paths = collect_paths(markup)
        if not paths:
            missing.append(f"{kotlin_name} ({icon_name}, no path)")
            continue
        lines.append(f'\n    /** ui-icons-hub `lucide/{icon_name}` */')
        lines.append(f'    val {kotlin_name}: ImageVector = build(\n        "{kotlin_name}",')
        lines.append("        listOf(")
        for d, filled, width in paths:
            lines.append(f'            Triple({json.dumps(d)}, {str(filled).lower()}, {width:g}f),')
        lines.append("        )")
        lines.append("    )")

    lines.append("}\n")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(lines), encoding="utf-8")

    print(f"wrote {args.out} with {len(seen) - len(missing)} glyphs")
    if missing:
        print("MISSING:", ", ".join(missing))


if __name__ == "__main__":
    main()
