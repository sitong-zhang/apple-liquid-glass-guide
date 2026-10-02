#!/usr/bin/env python3
"""Fetch iOS-style app icons from the `ui-icons-hub` repository into `assets/app-icons/`.

Provenance
----------
Source: https://github.com/sitong-zhang/ui-icons-hub/tree/main/app-icons
That partition is a standalone "iOS26-style app icon" pack: 797 PNGs, already cut to the
Apple squircle with transparent corners, carrying real brand colours.

Two files in this repo make it scriptable:
  * `app-icons/icons.js`      -- `window.APP_ICONS=[{"n":name,"f":path,"src":id}, ...]`
  * `assets/app-icons/manifest.json` -- the same list, normalised (see below)

Usage
-----
    # refresh the manifest from the hub (needs network; jsdelivr works where raw.githubusercontent
    # is unreachable)
    python3 tools/fetch_app_icons.py --refresh-manifest

    # download the icons this project actually uses
    python3 tools/fetch_app_icons.py --out assets/app-icons/png

    # download one icon by display name
    python3 tools/fetch_app_icons.py --name "Camera" --out /tmp

Options
-------
    --cdn       base URL of the hub (default: jsdelivr, see CDN below)
    --resize    edge length of the output; the hub ships 214/216/400/595 px mixed

Notes learned the hard way
--------------------------
1. `icons.js` is JavaScript, not JSON: strip the `window.APP_ICONS=` prefix and any trailing `;`.
2. The `f` field is **URL-encoded** (`png/File%20Manager.png`) -- always `unquote()` before using it
   as a path, and `quote()` again before putting it back into a URL.
3. The upstream display names are mostly Chinese app names. `MAP` below resolves a stable ASCII
   id (used as the Android resource name) to a hub display name, so renaming upstream never breaks
   the build.
"""

from __future__ import annotations

import argparse
import io
import json
import os
import urllib.parse
import urllib.request

CDN = "https://cdn.jsdelivr.net/gh/sitong-zhang/ui-icons-hub@main/app-icons/"
MANIFEST = "assets/app-icons/manifest.json"

# app id (Android resource suffix) -> hub display name.
MAP = {
    "phone": "Phone",
    "safari": "Browser",
    "messages": "Sms",
    "music": "Music",
    "facetime": "Video",
    "calendar": "Calendar",
    "photos": "Gallery",
    "camera": "Camera",
    "mail": "Email",
    "notes": "Notes",
    "reminders": "To Do",
    "clock": "Clock",
    "maps": "Google Maps",
    "weather": "Weather",
    "news": "News",
    "stocks": "Snowball",
    "books": "Bookstore",
    "appstore": "Google Play Store",
    "health": "Health",
    "wallet": "Wallet",
    "files": "File Manager",
    "podcasts": "Ximalaya Fm",
    "tv": "Google Tv",
    "gallery": "Magic Widget",
    "settings": "Settings",
    "calculator": "Calculator",
    "home": "Smart Home",
    "tips": "Tips",
    "translate": "Google Translate",
    "measure": "Ar Measure",
    "shortcuts": "Breeno Commands",
    "fitness": "Keep",
    "voicememos": "Recorder",
    "magnifier": "Scanner",
    "globe": "Chrome",
    "compass": "Compass",
    "headphones": "Nothing X",
    "timer": "Pomotodo",
    "alarm": "Wakeup Schedule",
    "bell": "Sound Assistant",
}


def load_manifest(cdn: str) -> dict[str, str]:
    """name -> path, read from the local manifest, or refreshed from the hub."""
    with open(MANIFEST, encoding="utf-8") as fh:
        data = json.load(fh)
    return {item["name"]: item["file"] for item in data["icons"]}


def refresh_manifest(cdn: str) -> None:
    with urllib.request.urlopen(cdn + "icons.js", timeout=60) as response:
        text = response.read().decode("utf-8")
    payload = text[text.index("["):].strip().rstrip(";")
    items = json.loads(payload)
    os.makedirs(os.path.dirname(MANIFEST), exist_ok=True)
    with open(MANIFEST, "w", encoding="utf-8") as fh:
        json.dump(
            {
                "source": "https://github.com/sitong-zhang/ui-icons-hub/tree/main/app-icons",
                "count": len(items),
                "icons": [
                    {
                        "name": item["n"],
                        "file": urllib.parse.unquote(item["f"]),
                        "src": item.get("src"),
                    }
                    for item in items
                ],
            },
            fh,
            ensure_ascii=False,
            indent=1,
        )
    print(f"manifest refreshed: {len(items)} icons")


def fetch(cdn: str, path: str) -> bytes:
    url = cdn + urllib.parse.quote(path)
    with urllib.request.urlopen(url, timeout=60) as response:
        return response.read()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cdn", default=CDN)
    parser.add_argument("--out", default="assets/app-icons/png")
    parser.add_argument("--name")
    parser.add_argument("--refresh-manifest", action="store_true")
    parser.add_argument("--resize", type=int, default=0)
    args = parser.parse_args()

    if args.refresh_manifest:
        refresh_manifest(args.cdn)

    by_name = load_manifest(args.cdn)
    os.makedirs(args.out, exist_ok=True)

    if args.name:
        targets = {args.name.rsplit(".", 1)[0]: args.name}
    else:
        targets = MAP

    from PIL import Image  # imported lazily: only needed when writing files

    for app_id, name in targets.items():
        path = by_name.get(name)
        if not path:
            print(f"MISSING  {app_id:<12} {name}")
            continue
        image = Image.open(io.BytesIO(fetch(args.cdn, path))).convert("RGBA")
        if args.resize and image.size[0] != args.resize:
            image = image.resize((args.resize, args.resize), Image.LANCZOS)
        target = os.path.join(args.out, f"ios_app_{app_id}.png")
        image.save(target)
        print(f"ok       {app_id:<12} {image.size[0]}px  <- {name}")


if __name__ == "__main__":
    main()
