# Apple app-icon partition (`ui-icons-hub/app-icons`)

## What this directory holds

| Path | Content |
|---|---|
| `manifest.json` | The **complete** index of the hub's app-icon partition: 797 entries, `name → file (+src id)` |
| `png/ios_app_*.png` | The **40 icons this project actually ships** (214 × 214, RGBA) |
| `../tools/fetch_app_icons.py` | Re-downloads the manifest and any icon, reproducibly |

Source partition: <https://github.com/sitong-zhang/ui-icons-hub/tree/main/app-icons>

```js
// app-icons/icons.js — this is JavaScript, not JSON
window.APP_ICONS=[{"n":"Aima","f":"png/Aima.png","src":"icon83676d51"}, ...]
```

| Field | Meaning |
|---|---|
| `n` | Display name. Mostly Chinese app names upstream; `fetch_app_icons.py` maps them to stable ASCII ids |
| `f` | Path relative to `app-icons/`, **URL-encoded** (`png/File%20Manager.png` → `png/File%2520Manager.png`) |
| `src` | Icon id, reverse-lookupable in `rename_map.json` |

## About the format: PNG, not SVG

The brief asked for "the Apple app-icon SVG". Measured against the real partition: **that partition
ships PNGs, not SVGs.** Two pipelines exist in the hub and they are easy to confuse:

| Pipeline | Location | Format | Content |
|---|---|---|---|
| **A** | `data/` + `index.json` | **SVG** (215 sets, 340k+ icons, monochrome line art) | UI glyphs: Lucide, Feather, Material… |
| **B** | `app-icons/` | **PNG** (797, pre-rounded, brand-coloured) | iOS26-style **app** icons |

If you need SVG, you want pipeline A (and `tools/generate_ios_glyphs.py` in the app project converts
those SVG paths into Compose `ImageVector`). If you need app icons that look like a real iPhone home
screen, you want pipeline B — PNG — which is what `png/` here contains.

## Measured facts (measured on this machine, not estimated)

**Count**: 797 PNGs.

**Sizes are not uniform**:

| Pixels | Count |
|---:|---:|
| 214 × 214 | 506 |
| 216 × 216 | 234 |
| 400 × 400 | 56 |
| 595 × 595 | 1 |

**Corner radius ≈ 23 % of the edge** (measured: first opaque pixel on the top row at `x ≈ 48–51`
of 214, i.e. 0.224–0.238). The four corners are already transparent.

> **Consequence: never clip these again.**
> ✅ `Image(painterResource(...), Modifier.size(62.dp))`
> ❌ `.clip(RoundedRectangle(15.dp))` — that cuts a ring off an icon whose corners are already cut.
> If you add a drop shadow, trace the *same* radius: `Modifier.shadow(8.dp, RoundedRectangle(14.dp),
> clip = false)` for a 62 dp icon (0.23 × 62 ≈ 14.3 dp), otherwise the shadow peeks out from under
> the icon's own corners.

## Getting the files

`raw.githubusercontent.com` was unreachable from the build machine (DNS filtering); the jsdelivr
mirror was not. The fetch script therefore defaults to:

```
https://cdn.jsdelivr.net/gh/sitong-zhang/ui-icons-hub@main/app-icons/
```

```bash
python3 tools/fetch_app_icons.py --refresh-manifest     # re-read icons.js -> manifest.json
python3 tools/fetch_app_icons.py --out assets/app-icons/png
python3 tools/fetch_app_icons.py --name "Camera" --out /tmp
```

## License

The hub's README states:

> Icon artwork remains the property of each upstream project. This site ships **no unified license**
> for the assets — always check each collection's own upstream license.

and `app-icons/index.html` only says "Data source: iOS26 icon pack (open source)" without naming a
licence. These are **brand icons of real apps**. That is fine for a local demo and for this
note-taking repository; **before shipping them in a store build, clear the rights yourself.**
