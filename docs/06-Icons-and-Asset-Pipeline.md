# 06 · Icons and Asset Pipeline

> This chapter solves one very concrete problem: **how to use the `ui-icons-hub` repository.**
>
> It actually contains **two completely different asset pipelines**, and mixing them causes trouble:
>
> | Pipeline | Location | Content | Nature |
> |---|---|---|---|
> | **A · SVG icon set** | `data/` + `index.json` | 215 open-source icon sets / 340k+ SVGs | Vector, monochrome line art, needs conversion to `ImageVector` |
> | **B · Apple app-icon partition** | `app-icons/` | **797 iOS26-style app-icon PNGs** | Bitmap, pre-rounded corners, with built-in brand colors |
>
> This repo's shell currently uses pipeline A (`Lucide` line art); while the **home app icons should have used pipeline B** (the Apple app-icon partition). This chapter explains both thoroughly and gives the complete executable plan to switch from A to B in §8.

---

## 1. What is the `ui-icons-hub` repository

- Repo: <https://github.com/sitong-zhang/ui-icons-hub>
- Online site: <https://sitong-zhang.github.io/ui-icons-hub/>
- In one sentence: it **aggregates, normalizes, and chunks** the open-source icon libraries scattered across GitHub, with a **search that understands Chinese intent** (FlexSearch, Apache-2.0).

### 1.1 Directories relevant to this project

| Path | Content | Do we use it |
|---|---|---|
| `data/` | Icon data chunks, **lazily loaded on demand** (each set has several `__N.js`) | ✅ Raw material for pipeline A |
| `index.json` / `index-compact.json` | Index of all sets (8.3 MB, includes SVG outer wrapper info like viewBox) | ✅ Generation script reads `viewBox` |
| `icons.js` | `window.ICON_SETS`: metadata for 215 sets (license, homepage, count, svg wrapper) | Reference |
| `app-icons/` | **iOS26-style app-icon partition: 797 PNGs** | ⭐ Pipeline B (§2) |
| `search.js` / `search-data.js` | Search implementation and lightweight index | ❌ |
| `cdn.js` | Zero-dependency browser SDK (`<script>` one-line integration) | ❌ |
| `mcp/` / `packages/` / `skill/` | MCP server / npm package / AI skill | ❌ |
| `skills/` | Design-skill partition (software / website / game) | ❌ |

### 1.2 Data-chunk format (must know before reading pipeline A code)

The content of `data/<slug>__<n>.js` is **not valid JSON**, but JS executed by the browser. As tested, the original looks like:

```js
window.__ADD2("lucide-icons__lucide",0,[["phone","<path d=\"...\"/>"],["message-circle","<path .../>"], ...]);
```

Key points:

1. The prefix is `window.__ADD2(`, followed by `(setSlug, chunkIndex, payloadArray, ...)`;
2. **The real icon array starts after the first comma and ends at the last `]` of the line**;
3. Each element is **`[name, SVG inner markup, ...optional metadata]`** — there may be extra fields after the 3rd, so **you cannot unpack with `for name, markup in ...`** (this is exactly pitfall 2 in §4.4 below).

> **`ui-icons-hub` itself provides three proper integration channels: `cdn.js` / `packages/` / `skill/`.**
> This project needs icons **compiled into the APK at compile time**, so it chooses to **read the raw `data/` chunks directly** — a deliberate trade-off: zero runtime dependency and zero network requests.

---

## 2. The Apple app-icon partition `app-icons/` (verified inventory)

All numbers in this section were measured by running against real files on this machine, not estimated.

### 2.1 Directory structure

```
ui-icons-hub/app-icons/
├── index.html        52528 B   ← partition page ("iOS26 · Apple-style app-icon partition · Farewell UI icon library")
├── icons.js          9751  B   ← manifest: window.APP_ICONS=[{"n":name,"f":relative path,"src":id}, ...]
├── rename_map.json   24246 B   ← { "icon83676d51": "Aima", ... }  id → name
└── png/              27 MB     ← the 797 PNG bodies
```

Page `<title>` and footer original:

```html
<title>iOS26 · Apple-style app-icon partition · Farewell UI icon library</title>
<meta name="description" content="Complete iOS26-style app-icon pack · 797 app icons (PNG), Apple iOS rounded style" />
...
<footer>Farewell UI icon library · iOS26 app-icon partition · Data source: iOS26 icon pack (open source)</footer>
```

### 2.2 `icons.js` format

```js
window.APP_ICONS=[{"n":"Aima","f":"png/Aima.png","src":"icon83676d51"},
                  {"n":"Bank Sdnxs","f":"png/Bank%20Sdnxs.png","src":"icon049e4ce7"}, ...]
```

| Field | Meaning |
|---|---|
| `n` | Display name (the upstream dataset uses Chinese app names, e.g. Renren Video, Recorder, Industrial Bank, Hongguo; the pipeline localizes them to English downstream) |
| `f` | Path relative to `app-icons/`, **filenames with spaces are URL-encoded as `%20`** |
| `src` | Icon id, can be reverse-looked-up to a name in `rename_map.json` |

**When parsing, note**: `f` is **URL-encoded**, so `urllib.parse.unquote` must be applied before using it as a file path.

### 2.3 Measured statistics (measured)

**Count**: 797 PNGs, totaling **27 MB**.

**Size distribution — not uniform; this is the first thing that must be handled on integration**:

| Pixels | Count |
|---:|---:|
| 214 × 214 | 506 |
| 216 × 216 | 234 |
| 400 × 400 | 56 |
| 595 × 595 | 1 |

**Color format**: `8-bit/color RGBA` (colortype 6, with alpha channel).

**Corner state — already cut, do not cut again**: measured `Camera.png` / `Toutiao.png` / `Aima.png` all have **alpha 0 (transparent) at the four corners**, with the first opaque pixel along the top row appearing at `x ≈ 58` (width 214). That is:

> **These PNGs are already finished "Apple iOS rounded-style" icons** (pre-cut corners + transparent corners), with a corner radius of about **27%** of the edge (58/214 of 214).
>
> So when placing them on the UI:
> - ✅ `Image(painterResource(...), Modifier.size(62.dp))` — use directly;
> - ❌ **Do not** `.clip(RoundedRectangle(15.dp))` again — a second rounding would only cut a ring off the icon, and the radius almost certainly won't match the asset's built-in one;
> - ⚠️ If you must add a shadow, `Modifier.shadow(elevation, shape = RoundedRectangle(built-in radius), clip = false)`, estimate the radius as **0.27 × edge** (62dp icon → ≈ 16.7dp), and eyeball-check it once.

### 2.4 License reminder (read before publishing)

- The first paragraph of `ui-icons-hub`'s README states:
  > Icon artwork remains the property of each upstream project. This site ships **no unified license** for the assets — always check each collection's own upstream license.

- And `app-icons/index.html` only says "Data source: iOS26 icon pack (open source)," **without** giving the specific upstream license of this icon pack.
- These 797 are **brand icons of real apps** (Taobao, Douyin, banks, games...).

**Conclusion: before packaging these `app-icons/` PNGs into a shipping app, you must confirm authorization yourself.** This repo currently does **not** copy any `app-icons/` images into the project, precisely for this reason. **Local demo / personal testing** is one thing; **publishing to a store** is another.

---

## 3. Which pipeline the current implementation uses (status explanation)

**Home icon composition = hand-drawn gradient squircle + monochrome vector glyph.**

```
┌──────────────────────────┐
│  Box(62dp, RoundedRectangle(15dp))          ← hand-drawn
│    background = two-color linear gradient (app.top → app.bottom)   ← hand-drawn (set in IosAppCatalog)
│    shadow(10dp, black 40%)                      ← hand-drawn
│    ┌────────────────┐                        │
│    │  GlassIcon(31dp)│  ← white line-art glyph from IosGlyphs (pipeline A / Lucide)
│    └────────────────┘                        │
└──────────────────────────┘
```

- **Glyph source**: [`IosGlyphs.kt`](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosGlyphs.kt), **68** `ImageVector`s, generated by `tools/generate_ios_glyphs.py` from `data/lucide-icons__lucide__{0,1}.js` (Lucide, **ISC license**).
- **Gradient colors**: each app in `IosAppCatalog.kt` hand-writes a `top/bottom` pair, **part of this project's decoration, containing no Apple artwork**.

**Advantages**: vector, tiny (tens of KB), scales without blur, clean license, zero runtime dependency.
**Disadvantage**: **it is not "Apple app icons"** — it's a set of generic line-art icons with an iOS-style container. This is exactly why the user pointed out "should just use that `app-icons/` partition."

---

## 4. Pipeline A: `tools/generate_ios_glyphs.py` dissected section by section

File: [`../LiquidGlassShowcase/tools/generate_ios_glyphs.py`](../LiquidGlassShowcase/tools/generate_ios_glyphs.py)

Usage:

```bash
python3 tools/generate_ios_glyphs.py \
    --hub /path/to/ui-icons-hub \
    --out app/src/main/java/com/liquidglass/showcase/ios/IosGlyphs.kt
```

### 4.1 `ICONS`: Kotlin identifier → upstream icon name

```python
ICONS: list[tuple[str, str]] = [
    ("Phone", "phone"), ("Messages", "message-circle"), ("Safari", "compass"),
    ("Camera", "camera"), ("Photos", "image"), ("Settings", "settings"),
    ...
    ("X", "x"), ("Plus", "plus"), ("Check", "check"), ("Trash", "trash"),
]
```

Grouped by purpose (home/Dock, lock screen, status bar, control center, media/misc), 68 items total.

> **Pitfall hit**: this originally wrote `trash-2`, but this Lucide set has no `trash-2`, only `trash`. The generation script does not error out on not-found names, it only collects them into a `missing` list and prints it, so the first-version output was missing an icon and I didn't notice at first. **Lesson: always check the final `MISSING:` line after the generation script runs.**

### 4.2 `load_set`: extract `{name: SVG markup}` and `viewBox` from the JS chunks

```python
def load_set(hub: Path, slug: str) -> tuple[dict[str, str], str]:
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
                names[entry[0]] = entry[1]
    return names, view_box
```

Four key points, **each learned the hard way**:

1. **`viewBox` is regex-extracted from `sets[].wrap` in `index.json`.** Because `data/*.js` stores only inner markup like `<path>`, **without the outer `<svg viewBox>`**.
2. **`text.split("window.__ADD2(")[1:]`** — a file may have multiple `window.__ADD2` calls; split them all out and process each (`lucide-icons__lucide` actually has `__0.js` / `__1.js` two files).
3. **`start = chunk.index(",") + 1`** — skip the `"slug",chunkIndex,` part, **starting from the array's `[`**. The first version parsed from the beginning and threw `json.decoder.JSONDecodeError: Extra data: line 1 column 22`.
4. **`chunk.rindex("]") + 1`** — use the **last** `]`, not the first, to cut out the whole array.

### 4.3 `collect_paths`: recursively walk the SVG, keep fill / stroke

```python
def walk(node, inherited):
    here = {**inherited, **node.attrib}          # ← attributes inherit downward (SVG's <g fill=...> relies on this)
    tag = node.tag.replace(f"{{{SVG_NS}}}", "")
    if tag not in ("svg", "g", "defs", "title", "desc", "style", "clipPath"):
        d = to_path(node, here)
        if d:
            is_filled = (here.get("fill") or "").lower() not in ("", "none")
            stroke = (here.get("stroke") or "").lower()
            stroked = stroke not in ("", "none")
            width = _num(here.get("stroke-width"), 2.0)
            out.append((d, is_filled, width if stroked else 0.0))
    for child in node:
        walk(child, here)                        # ← must recurse all child nodes
```

> **Pitfall 3 hit**: originally only "direct children" were processed; when encountering icons wrapped in `<g>`, **not a single path was retrieved**, and the generated `ImageVector` was empty (compiled fine, but the icon was invisible at runtime). **The fix was exactly these two things: "recurse the whole tree + inherit attributes."**

`to_path` converts non-`path` basic shapes into equivalent `d`:

| SVG element | Conversion | Note |
|---|---|---|
| `path` | use `d` directly | |
| `circle` | two semicircle arcs forming a closed path | `_ellipse(cx, cy, r, r)` |
| `ellipse` | same as above | |
| `rect` | generate a rounded-rect path with `A` (arc) | `rx/ry` clamped by `min(rx, w/2)` |
| `line` | `M…L…` | |
| `polyline` / `polygon` | `M…L…` (polygon adds `Z`) | coordinate pairs split by space/comma |

### 4.4 What the generated product looks like

```kotlin
/** ui-icons-hub `lucide/phone` */
val Phone: ImageVector = build(
    "Phone",
    listOf(
        Triple("M13.832 16.568a1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233 14 14 0 0 0 6.392 6.384", false, 2f),
        ...
    )
)
```

The key of `build()` is **using only the `addPath(pathData = addPathNodes(d), ...)` overload**:

```kotlin
addPath(
    pathData = addPathNodes(data),
    fill = if (filled) SolidColor(Color.White) else null,
    stroke = if (strokeWidth > 0f) SolidColor(Color.White) else null,
    strokeLineWidth = strokeWidth,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round
)
```

Two must-know points:

1. **Fill/stroke color is uniformly hardcoded to `Color.White`, and that's not a bug** — the final color is tinted upstream by `GlassIcon`'s `ColorFilter.tint(tint)`, so one set of glyphs can be reused under any color (white icons, dark icons, accent icons all use the same `ImageVector`).
2. **`addPathNodes(String)` is required**: since Compose 1.12, `ImageVector.Builder.path(pathData = ...)` overload is gone, only `path(pathBuilder)` remains. See [07-Build-and-Troubleshooting.md](07-Build-and-Troubleshooting.md).

The file header also states it must not be hand-edited:

```kotlin
/**
 * GENERATED FILE — do not edit by hand. Regenerate with `tools/generate_ios_glyphs.py`.
 */
```

### 4.5 `viewBox` must be 24×24

```python
if view_box != "0 0 24 24":
    raise SystemExit(f"unexpected viewBox {view_box!r}; adjust VIEWPORT in the template")
```

**This is a deliberate "fail fast"**: `IosGlyphs`'s `VIEWPORT = 24f` is hardcoded; if you switch to a set whose `viewBox` is not 24 (e.g. 32, 48), the coordinates would all be misaligned. **Rather than silently draw a wrong figure, just exit.**

---

## 5. Common pitfalls of SVG → Compose `ImageVector`

| Pitfall | Symptom | Fix |
|---|---|---|
| `Builder.path(pathData = ...)` doesn't exist | Compile error | Use `addPath(addPathNodes(d), …)` |
| Only fill, no stroke (or vice versa) | Icon all black / completely invisible | Lucide is **line art**, must give `stroke` + `strokeLineWidth` + `StrokeCap/Join.Round` |
| `viewBox` not 24×24 | Figure clipped or scaled tiny | Change `VIEWPORT`, or scale coordinates at generation time |
| Forgot to recurse `<g>` | Compiles but icon is empty | Recurse + attribute inheritance (§4.3) |
| Color hardcoded on path | Theme switch doesn't take effect | Uniform `Color.White` + `ColorFilter.tint` |
| `stroke-width` missing | Line too thick/thin | Default `2.0f` (Lucide's common value) |

---

## 6. Icon catalog and colors: `IosAppCatalog`

[`IosAppCatalog.kt`](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosAppCatalog.kt)

```kotlin
data class IosApp(
    val id: String,                 // stable id (used by recents / key)
    val label: String,              // home Chinese/English name
    val glyph: ImageVector,         // glyph from IosGlyphs
    val top: Color,                 // squircle gradient start
    val bottom: Color,              // squircle gradient end
    val glyphTint: Color = Color.White   // default white; override for light-background icons
)
```

| Group | Count | Content |
|---|---:|---|
| `dock` | 4 | Phone / Safari / Messages / Music |
| `page1` | 20 | Daily apps (FaceTime, Calendar, Photos, Camera, Mail, Notes, Reminders, Clock, Maps, Weather, News, Stocks, Books, App Store, Health, Wallet, Files, Podcasts, TV, Home) |
| `page2` | 16 | Tools + **widget gallery** (Settings, Calculator, Widget Gallery, Tips, Translate, Measure, Shortcuts, Fitness, Voice Memos, Magnifier, Browser, Flashlight, Headphones, Timer, Alarm, Sounds & Haptics) |
| `allApps` | 40 | `dock + pages.flatten()`, for notification cards / switcher to look up by id |

**Color rule (copy iOS's look):**

- Each app gets a **same-hue** two-color gradient (e.g. Music `#FF6B81 → #F5233F`), **lighter on top, darker at bottom**, simulating the icon's own lighting;
- **Light-background icons must override `glyphTint`**, otherwise the white glyph disappears on a white background:

| App | Background | `glyphTint` |
|---|---|---|
| Calendar | white → gray-white | `#E0342B` (red) |
| Notes | beige → yellow | `#6B5200` (dark brown) |
| Reminders | white → gray-white | `#3A3A3C` (dark gray) |
| Tips | yellow → orange | `#5A3D00` |
| Fitness | bright green → green | `#00401A` |
| Measure | light gray → mid gray | `#3A3A3C` |
| Flashlight | white → light gray | `#3A3A3C` |

> The `id` field is critical: `recents`, the switcher's `key`, and `allApps.first { it.id == "messages" }` in notification cards all depend on it. **Using `label` as a key will blow up when you change the copy.**

---

## 7. `GlassIcons.kt`: zero-dependency hand-drawn component icons

[`GlassIcons.kt`](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/core/ios/GlassIcons.kt) (36 glyphs)

**Why hand-draw**: the original plan was `androidx.compose.material:material-icons-extended`, but that library is **no longer updated** (frozen at version 1.7.8), conflicting with the user's requirement of "only use actively maintained dependencies," so it was dropped.

Implementation is minimal: two private factories + a bunch of constants:

```kotlin
private const val STROKE_WIDTH = 1.8f

private fun strokeIcon(name: String, vararg paths: String): ImageVector   // line-art icon
private fun filledIcon(name: String, vararg paths: String): ImageVector   // filled icon
```

Also follows the convention "paths are white, color comes from `ColorFilter.tint`," so the same glyph can serve both light theme (black) and dark theme (white).

**Don't mix up the positioning of the two icon sets:**

| File | Purpose | Count | Source |
|---|---|---:|---|
| `ios/IosGlyphs.kt` | **App icons** (home/lock screen/Dynamic Island) | 68 | Generated (pipeline A, Lucide) |
| `core/ios/GlassIcons.kt` | **Component icons** (arrows, search, checkmarks in the 26 showcase components…) | 36 | Hand-written |

---

## 8. ⭐ Complete plan to switch to the "Apple app-icon partition"

This section is the construction blueprint for the **successor**. The goal: **replace the home icons from "hand-drawn gradient + line-art glyph" to the real PNGs from `app-icons/`.**

### 8.1 First think through three things

| Question | Conclusion |
|---|---|
| Need all 797? | **No.** Packaging all 27 MB × every pixel density would make the APK huge. **Pick only the ~40 the home needs**, or extend as needed. |
| Uniform size? | **No** (214/216/400/595). **Must normalize to one size first**, otherwise visual sizes differ. |
| Cut corners again? | **No.** PNGs have built-in corners + transparent corners (§2.3). |

### 8.2 Name mapping (required)

`app-icons` names are **for humans** (with spaces, with Chinese, URL-encoded), while Android `res/drawable` filenames can only be lowercase letters, digits, underscores.

So an explicit mapping table is needed. Suggested mapping (the 40 apps on this project's home):

| `IosAppCatalog.id` | candidate name in app-icons (`n` in `icons.js`) | drawable name |
|---|---|---|
| `phone` | `Phone` / `Phone Vivo` | `ic_app_phone` |
| `safari` | `Browser` / `Rains Browser` | `ic_app_browser` |
| `messages` | `Ding Talk` / … | `ic_app_messages` |
| `music` | `Poweramp` / `Netease Ucmooc`… | `ic_app_music` |
| `camera` | `Camera` | `ic_app_camera` |
| `photos` | — | `ic_app_photos` |
| `settings` | — | `ic_app_settings` |

> ⚠️ The right column above is **illustrative**, not an authoritative list — the 797 names may not include the exact native iOS apps. **The successor's first task is to print `app-icons/icons.js` into a human-readable list, then pick one by one.**

### 8.3 Step 1: export `icons.js` into a list

```python
# tools/list_app_icons.py  —— view only, does not modify any file
import json, pathlib, urllib.parse

t = pathlib.Path("app-icons/icons.js").read_text(encoding="utf-8")
data = json.loads(t[len("window.APP_ICONS="):].rstrip().rstrip(";"))
print(f"total = {len(data)}")
for e in sorted(data, key=lambda x: x["n"].lower()):
    print(f'{e["n"]:<42} {urllib.parse.unquote(e["f"])}')
```

> **Pitfall**: `icons.js` is `window.APP_ICONS=[...]`, not pure JSON. Directly `json.load(open(...))` throws `JSONDecodeError`. Also note `"n"` contains Chinese and `"f"` contains `%20`.

### 8.4 Step 2: normalize + rename + output to `res/drawable-nodpi`

```python
# tools/import_app_icons.py
import json, pathlib, urllib.parse
from PIL import Image

HUB   = pathlib.Path("/path/to/ui-icons-hub/app-icons")
OUT   = pathlib.Path("app/src/main/res/drawable-nodpi")
SIZE  = 216                      # normalize to 216×216 (the majority bucket)

# upstream name -> our drawable name (maintain as needed)
PICK = {
    "Camera":          "ic_app_camera",
    "Phone":           "ic_app_phone",
    "Ding Talk":       "ic_app_messages",
    "Poweramp":        "ic_app_music",
    # ...
}

data = json.loads((HUB / "icons.js").read_text(encoding="utf-8")[len("window.APP_ICONS="):].rstrip().rstrip(";"))
by_name = {e["n"]: urllib.parse.unquote(e["f"]) for e in data}

OUT.mkdir(parents=True, exist_ok=True)
missing = []
for upstream, target in PICK.items():
    rel = by_name.get(upstream)
    if rel is None:
        missing.append(upstream); continue
    im = Image.open(HUB / rel).convert("RGBA").resize((SIZE, SIZE), Image.LANCZOS)
    im.save(OUT / f"{target}.png", optimize=True)
print("exported", len(PICK) - len(missing))
if missing:
    print("MISSING:", ", ".join(missing))
```

Key points:

1. **`convert("RGBA")`** — a few assets may not be RGBA; unify to preserve transparent corners;
2. **`resize((SIZE, SIZE), Image.LANCZOS)`** — choose high-quality resampling, otherwise corners get jagged;
3. **Always print `missing`** (same lesson as §4.1);
4. **`drawable-nodpi`** — since we've already unified pixels to 216, we don't want the system to rescale by density;
5. If the final APK is too large, switch output to **WebP** (`im.save(..., "WEBP", quality=90)`), typically cutting 60~70% of the size, and Android supports it natively.

### 8.5 Step 3: change `IosAppCatalog` to use `drawable` resources

**Recommended approach: add a nullable image-resource field to `IosApp`, keeping the glyph as fallback.**

```kotlin
data class IosApp(
    val id: String,
    val label: String,
    val glyph: ImageVector,
    val top: Color,
    val bottom: Color,
    val glyphTint: Color = Color.White,
    @DrawableRes val iconRes: Int? = null      // ← new: use real icon when present
)
```

```kotlin
IosApp("camera", "Camera", IosGlyphs.Camera, Color(0xFF9A9AA0), Color(0xFF45454A),
       iconRes = R.drawable.ic_app_camera)
```

Render site (`IosHomeIcon` / `IosDockIcon` / notification card small icon / switcher card) uniformly goes through one wrapper:

```kotlin
@Composable
fun IosAppIcon(app: IosApp, size: Dp, modifier: Modifier = Modifier) {
    val res = app.iconRes
    if (res != null) {
        Image(
            painter = painterResource(res),
            contentDescription = app.label,
            modifier = modifier.size(size)
            // ⚠️ do not clip: PNG has built-in corners (see §2.3)
        )
    } else {
        // fallback: old gradient squircle + glyph
        Box(
            modifier.size(size)
                .clip(RoundedRectangle(size * 0.24f))
                .background(Brush.linearGradient(listOf(app.top, app.bottom))),
            contentAlignment = Alignment.Center
        ) {
            GlassIcon(app.glyph, app.glyphTint, size = size * 0.5f)
        }
    }
}
```

**Why "nullable + fallback" instead of direct replacement**: among the 797 names you may not find a matching icon for every native iOS app; the fallback path turns "fill in the missing ones" into incremental work, rather than "must gather all 40 before it compiles."

### 8.6 How to handle the shadow (the easiest step to get wrong)

The old implementation's shadow shape was `RoundedRectangle(15.dp)`. After switching to PNG:

```kotlin
.shadow(
    elevation = 10.dp,
    shape = RoundedRectangle(62.dp * 0.27f),   // ≈ 16.7dp, aligned with asset's built-in corner
    clip = false,                               // only cast shadow, don't clip
    ambientColor = Color.Black.copy(alpha = 0.4f),
    spotColor = Color.Black.copy(alpha = 0.4f)
)
```

- `clip = false` is required: `clip = true` would **actually cut along this shape**, and mismatching the PNG's built-in corner produces a "stroked" feel;
- Estimate the radius as `0.27 × edge` (measured data, §2.3); **convert it when changing sizes**, don't hardcode.

### 8.7 Plan B: don't package, decode at runtime from `assets/`

If you later want to support "swap any of the 797 freely," you can go this route:

```kotlin
// assets/app-icons/<name>.png → Bitmap
val bitmap = remember(name) {
    context.assets.open("app-icons/$name.png").use { BitmapFactory.decodeStream(it) }.asImageBitmap()
}
Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(62.dp))
```

Trade-offs:

| | Plan A (res/drawable-nodpi) | Plan B (assets at runtime) |
|---|---|---|
| Compile-time check | ✅ resource missing → compile fails | ❌ only blows up at runtime |
| Memory | system-managed | need your own LRU cache |
| Size | only what's used | whole pack in bundle |
| Best for | **fixed 40 home icons** (recommended) | dynamic / swappable icon packs |

### 8.8 Acceptance checklist

- [ ] Icon sizes visually consistent (already normalized)
- [ ] **No** second rounding (no extra `clip`)
- [ ] Shadow radius aligned with icon's built-in corner, no "ghost ring" on the edge
- [ ] Readable on both light and dark wallpapers
- [ ] APK size increase acceptable (otherwise switch to WebP or shrink `SIZE`)
- [ ] **Authorization confirmed** (§2.4) — if this isn't checked, everything above is wasted

---

## 9. License and attribution summary

| Asset | Source | License | How this repo handles it |
|---|---|---|---|
| `IosGlyphs` (68 glyphs) | ui-icons-hub → Lucide | **ISC** | Generated into the project, file header notes source and "GENERATED" |
| `GlassIcons` (36 glyphs) | Hand-written in this project | Apache-2.0 | Under the project's license |
| Home gradient colors | Hand-written in this project | Apache-2.0 | Contains no Apple artwork |
| Wallpaper | Procedurally generated in this project | Apache-2.0 | **Ships no image assets** |
| `app-icons/` 797 PNGs | ui-icons-hub → "iOS26 icon pack (open source)" | **Not stated** | **Not copied into the project**; confirm on your own before integrating (§2.4) |
| `backdrop` / `shapes` | Kyant0 | Apache-2.0 | Maven dependency, `NOTICE` already declares it |
| Compose / AndroidX | Google | Apache-2.0 | Maven dependency |

> **A hard rule for the successor AI: don't assume something is commercially usable just because "it's on that website."** `ui-icons-hub`'s own README says it **ships no unified license**.

---

## 10. Summary

```
①  ui-icons-hub has two pipelines, don't mix them:
      A  data/*.js    → 215 open-source SVG sets → compile-time ImageVector (what the current shell uses)
      B  app-icons/   → 797 Apple-style PNGs (pre-cut corners, non-uniform sizes) → just place the image (what should be used)

②  Four must-knows of pipeline A: window.__ADD2 is not JSON / start cutting after the first comma /
      use rindex("]") at the array end / must recurse to get paths inside <g>.

③  Four must-knows of pipeline B: 797 files 27MB / sizes 214·216·400·595 /
      transparent corners, built-in 27% corner (don't cut again) / license not stated.

④  Always write color as white, tint with ColorFilter.tint —— one glyph set serves all themes.

⑤  The generation script must print MISSING and you must check it —— silently missing an icon is the hardest bug to spot.
```

**Next steps**: to recompile or debug in a new environment, read [07-Build-and-Troubleshooting.md](07-Build-and-Troubleshooting.md).
