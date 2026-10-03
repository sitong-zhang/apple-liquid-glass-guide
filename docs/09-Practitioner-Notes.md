# 09 · Practitioner Notes — Building Liquid Glass and Putting It on Real UI

> This chapter is **written after the thing was actually built, installed and complained about.**
> Chapters 01–08 were written from reading the upstream code; this one is written from
> `gradlew assembleRelease` runs, from an APK opened on a phone, and from eight defects the user
> reported in one message.
>
> Two jobs:
>
> 1. **Correct** the places where 01–08 turned out to be wrong or over-engineered (§1);
> 2. **Continue** past them with the practical knowledge that only appears once you ship (§2–§9).

---

## 1. Corrections to chapters 01–08

Everything below was stated or implied earlier and **did not survive contact with a real device.**

### 1.1 "A procedural gradient wallpaper is the right call" — wrong

Ch. 05 §12 argued for a hand-painted gradient-mesh wallpaper, on the grounds of "no third-party
art assets." The reasoning about *contrast* was right; the conclusion was wrong.

What the user said after installing it:

> Why isn't the home screen using that most classic Apple wallpaper?

A procedural mesh looks like a procedural mesh. Even with four blurred blobs it reads as "a
developer's placeholder," because it has **no high-frequency detail** — and glass is judged on
exactly that. Refraction is only *visible* where the background has edges: thin bright rims,
small colour boundaries, fine texture. A smooth gradient gives the shader almost nothing to bend.

**Correction**: ship a real photographic/illustrative wallpaper. It does not have to be Apple's —
any artwork with fine structure works — but the moment you replace the gradient with a real image,
every glass surface in the app looks one generation better, with **zero** parameter changes.

Practicalities measured while doing it:

| Item | Value |
|---|---|
| Source used | iOS 26 "Sky" Home/Lock, light + dark editions, 1290 × 2796 |
| Stored as | 1080 × 2340 WebP q88 in `drawable-nodpi` — 78–122 KB each |
| Scale type | `ContentScale.Crop`, always |
| Why `nodpi` | The bitmap is already screen-shaped; density resampling would only cost memory |

### 1.2 "Lock screen and home must become one shared space driven by `unlockProgress`" — over-engineered

Ch. 05 §14.2 proposed replacing `surface: IosSurface` with a continuous `unlockProgress`, so that
"seeing the home through the lock screen becomes the design intent."

**Don't.** The user's complaint was not "I want a fancy cross-fade." It was:

> Shouldn't I land on the lock screen? And why can I see the home screen straight through it?

On a real iPhone the lock screen is **opaque**: it shows the wallpaper, the clock and the
notifications, and *no icons*. Home-screen icons only appear as you swipe up. The bug was simply
that `IosLockScreen` was a transparent `Column` of spacers floating over a fully-composed home
screen.

**Correction** — the fix is one idea, not a state-machine rewrite:

> The lock screen owns its **own opaque wallpaper** and records **its own backdrop**.

```kotlin
Box(modifier.fillMaxSize().offset { IntOffset(0, offsetY.roundToInt()) }.draggable( ... )) {
    Image(                                   // opaque: nothing behind can bleed through
        painter = painterResource(lockWallpaper),
        modifier = Modifier.fillMaxSize().layerBackdrop(backdrop),   // glass here refracts *this*
        contentScale = ContentScale.Crop
    )
    Column(Modifier.fillMaxSize()) { /* clock, notifications, torch/camera */ }
}
```

Three things fall out for free:

1. The home screen underneath is invisible while locked — complaint gone;
2. The glass on the lock screen refracts the lock wallpaper, which is physically correct;
3. Swiping up moves wallpaper + content together, exactly like the device.

Two smaller corrections in the same area:

* `surfaceIsRead` no longer includes `isLocked` — the content layer is only recorded while a
  pull-down panel is reading it (one full-screen offscreen pass saved while locked);
* the boot → lock sequence (Ch. 05 §14.3) is now real: black screen → centred logo → fade to lock.

### 1.3 "Vector line-art glyphs are fine for home icons" — wrong

Ch. 05 §14.1 already flagged this and Ch. 06 §8 planned the migration. Confirmed: the user asked
twice, and the second time with emphasis. The migration is done — see §5.

### 1.4 "Press feedback: animate `Highlight.Default.copy(alpha = progress)`" — wrong, it makes grey fog

Upstream's `InteractiveHighlight.modifier` paints a **full-bleed** `White @ 8 %` rect with
`BlendMode.Plus`. On a dark wallpaper that is not a highlight, it is a **grey veil**: press any
button and a flat fog appears over the whole control. The user's words:

> When I press and hold the wallpaper button, why does a layer of grey-black fog appear over it?

**Correction**: press feedback must be **localised and shaped**, not a full-surface addition.

```kotlin
fun pressSheen(highlight: InteractiveHighlight, shape: Shape): Modifier = Modifier
    .clip(shape)
    .drawWithContent {
        drawContent()
        val progress = highlight.pressProgress
        if (progress > 0.01f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.13f * progress),
                        Color.Transparent
                    ),
                    center = highlight.touchPosition,     // follows the finger
                    radius = size.maxDimension * 0.8f
                )
            )
        }
    }
```

The two properties that matter: it **falls off** (no flat addition), and it is **clipped to the
control's shape** (no rectangle bleeding past rounded corners). Every `highlight.modifier` call
site was replaced by `pressSheen(highlight, shape)`.

### 1.5 "Boarding a real device is out of scope" — it is the only acceptance test that counts

Ch. 05 §14.4 admits there was no device check. Every single defect in §6 of this chapter was
invisible at compile time and obvious within ten seconds on a phone. **Budget for it.** Compile
success means "the shader compiled," nothing more.

---

## 2. How liquid glass is actually made

### 2.1 Choose the engine first, and choose AGSL

Three ways people try to build this, and why only one works:

| Approach | Result |
|---|---|
| `Modifier.blur()` / `RenderEffect.createBlurEffect()` | Frosted acrylic. No refraction, no edge bend, no dispersion. Reads as "Android blur," not glass |
| Fake it with gradients + a translucent fill | Flat. Breaks the instant anything moves behind it |
| **AGSL `RuntimeShader` sampling a recorded layer** (Backdrop 2.0.0) | Real refraction, real lens, real chromatic aberration |

```kotlin
implementation("io.github.kyant0:backdrop:2.0.0")   // the AGSL shaders + pipeline
implementation("io.github.kyant0:shapes:1.2.0")     // G2-continuous iOS corner shapes
```

`shapes` matters more than it looks: iOS corners are **G2-continuous** (curvature does not jump at
the tangent point). Ordinary `RoundedCornerShape` produces a visible "kink" at the corner seam,
which is exactly the "why does it look like a rounded rectangle, not a squircle" complaint.

### 2.2 The pipeline is fixed: `colorControls / vibrancy ⇒ blur ⇒ lens`

Order is not negotiable and each stage has one job:

| Stage | Job | What happens if you drop it |
|---|---|---|
| `colorControls` + `vibrancy` | Pull colour *out of* the background so the glass has something to show | Glass goes grey |
| `blur` | Remove high-frequency noise before sampling | Refraction sparkles / aliases |
| `lens` | The actual refraction: height `H`, displacement `A ≈ 2H`, plus optional chromatic aberration | It stops being glass and becomes frosted plastic |

> **A ≈ 2H** is the single most useful number in the whole recipe. Set displacement independently
> of height and the edge either "cuts" (too much displacement) or does nothing (too little).

### 2.3 Record, then sample — that is the whole architecture

```kotlin
val backdrop = rememberLayerBackdrop()

Image(painter, null, Modifier.fillMaxSize().layerBackdrop(backdrop))   // 1. record

Box(                                                                   // 2. sample
    Modifier.drawBackdrop(
        backdrop = backdrop,
        // ⚠ CORRECTED IN PRACTICE (docs/10 §1): with lens() present the shape MUST be a Compose
        // CornerBasedShape. RoundedRectangle(24.dp) from com.kyant.shapes crashes at attach time.
        shape = { RoundedCornerShape(24.dp) },
        effects = { blur(16.dp); lens(12.dp, 24.dp, chromaticAberration = true) },
        highlight = { Highlight { angle = gravityAngle } },
        shadow = { Shadow(8.dp, Color.Black.copy(0.2f)) },
        onDrawSurface = { drawRect(Color.White.copy(0.12f)) }
    )
)
```

Rules that keep it coherent:

1. **Glass refracts the layer it visually sits on.** Dock → wallpaper; in-app glass → the app
   window's own backdrop; overlays → `combined(wallpaper, content)`.
2. **Never sample a layer you are currently recording** — that is a feedback loop. If a surface
   needs to refract its own content, give it a *second* backdrop and record the content first.
3. Recording costs a full-screen offscreen pass per frame. Record conditionally.

### 2.4 Parameters that read as "Apple"

| Surface | Blur | Lens (H, A) | Surface tint | Notes |
|---|---|---|---|---|
| Small control (button, toggle) | 8 dp | 5 / 10 | white 10–14 % | Add `InnerShadow(4.dp * press)` |
| Card / list row | 16–24 dp | 10 / 20 | white 12–18 % | Corner 22–28 dp |
| Dock / tab bar | 20–28 dp | 12 / 24 | white 18–24 % | Capsule shape |
| Control-centre tile | 12 dp | 8 / 16 | white 22 % | Rounded by half the tile |
| Lock-screen notification | 20 dp | 10 / 20 | white **14 %** | More translucent: it sits on raw wallpaper |
| Overlay panel | 24–32 dp | 12 / 24 | white 20 % | Plus a 30 % black scrim behind |

The lock-screen row is the one people get wrong: a notification card on the lock screen must be
**more** transparent than the same card in the notification centre, because on the lock screen it
sits directly on the wallpaper.

---

## 3. Putting it on every kind of UI

### 3.1 Buttons, toggles, sliders — small controls

* Shape: `Capsule`. Nothing else looks right at this size.
* Press: scale `1.0 → 0.96` with `spring(MediumLow)` + `pressSheen` (§1.4). Never a full-bleed
  highlight.
* Keep the **hit area** at 48 dp even when the pill is 36 dp tall; iOS does this and users feel it.

### 3.2 Cards and lists

* One backdrop per list, not per row. Per-row backdrops multiply offscreen passes by the row count.
* Corner radius scales with size: 22 dp on a small row, 28 dp on a full-width card.

### 3.3 Bars — dock, tab bar, tool bar

* The dock is the classic: a capsule, blurred wallpaper, 0.5 dp inner top highlight.
* It must survive **all** paging: mount it outside the pager, otherwise it scrolls with the page.

### 3.4 Big overlays — control centre and notification centre

* They refract `combined(wallpaper, content)`, i.e. *the screen as the user sees it*.
* Dismiss needs **two** gestures: tap the scrim, or push the panel back up. Anything less feels
  broken.
* Entry/exit: `slideInVertically(tween(320, EaseOutCubic))` in, `fadeOut(180)` + slide out.

### 3.5 Lock screen

See §1.2. Text is **always white**, in light and dark theme alike — it always sits on artwork.

### 3.6 App window and the zoom transition

The window is laid out once at real screen size and only *scaled* by `graphicsLayer`; that way
text never reflows mid-animation.

Closing is **two beats**, and this is what the user was missing when he said "it just disappears":

```
beat 1 (0 → 0.62)   full screen  →  the icon rectangle it grew out of
beat 2 (0.62 → 1)   icon rect    →  the Dynamic Island, while fading out
```

```kotlin
val t = 1f - fraction            // 0 = open, 1 = closed
if (t <= IconLandingFraction) {  // beat 1
    from = screen; to = iconRect; corner = lerp(0f, iconCorner, t / IconLandingFraction)
} else {                         // beat 2
    from = iconRect; to = islandRect; corner = lerp(iconCorner, 18.dp, u); alpha = 1f - u
}
```

The window has to stay mounted for the whole animation even though `openApp` is already `null` —
so hold a copy of the app in the shell and unmount it from the `onFinished` callback.

### 3.7 What must **not** be glass

| Thing | Why |
|---|---|
| Dynamic Island | It is a hardware cut-out: pure black, fixed colours, no blur |
| Home indicator | It is chrome, not material |
| Status-bar glyphs | Too small to refract; they only get muddy |

---

## 4. Gestures: the part that makes it feel like iOS

### 4.1 The pull-down zones were 0 px tall (the reason both centres "would not open")

```kotlin
Row(Modifier.fillMaxWidth().height(56.dp).align(TopCenter)) {
    PullZone(Modifier.weight(0.4f)) { notificationCenter = true }
    Spacer(Modifier.weight(0.2f))
    PullZone(Modifier.weight(0.4f)) { controlCenter = true }
}

@Composable
private fun PullZone(modifier: Modifier, onPull: () -> Unit) {
    Box(modifier.fillMaxHeight().draggable(...))   // ← fillMaxHeight is not optional
}
```

**A `weight()` inside a fixed-height `Row` still measures children as wrap-content.** The zones had
no children, therefore no height, therefore no hit area. The gesture code was correct and completely
unreachable. Any invisible gesture strip must assert its own size (`fillMaxHeight` /
`fillMaxSize`), or you will debug the gesture logic for an hour for nothing.

### 4.2 Gesture map (must match the device, not the developer's taste)

| Gesture | Meaning |
|---|---|
| Pull down from top-left | Notification centre |
| Pull down from top-right | Control centre |
| Swipe up from bottom edge | Home: close switcher → close app → open switcher |
| Swipe up on lock screen | Unlock |
| Tap island | Expand / collapse |

Threshold + velocity, always both: `travel > 56 px || velocity > 600 px/s`. Velocity alone makes
the panel open on a flick you did not mean; distance alone makes it feel heavy.

### 4.3 The system back key

Without `BackHandler`, back exits the whole app from anywhere — including from inside an app
window, which is exactly what the user reported. Walk the shell down one level per press:

```kotlin
BackHandler(enabled = !state.isLocked) {
    when {
        state.openApp != null                          -> state.goHome()
        state.controlCenter || state.notificationCenter -> state.closeOverlays()
        state.appSwitcher                              -> state.appSwitcher = false
        state.islandExpanded                           -> state.islandExpanded = false
        else                                           -> state.lock()
    }
}
```

Disabled (`enabled = false`) on the lock screen, so the *last* back press still exits — the user
can always get out, but never by accident.

---

## 5. Icons: two pipelines, one rule

The hub has two asset pipelines and mixing them is what caused the "your icons are not Apple
icons" complaint. Full detail in [06](06-Icons-and-Asset-Pipeline.md) and in
[`assets/app-icons/README.md`](../assets/app-icons/README.md).

| Pipeline | Location | Format | Use for |
|---|---|---|---|
| **A** | `data/` + `index.json` | SVG, monochrome | In-app UI glyphs → convert to `ImageVector` |
| **B** | `app-icons/` | PNG, 797, pre-rounded, brand-coloured | Home-screen app icons |

**The one rule**: pipeline B icons are already cut to the Apple squircle, corner radius ≈ 23 % of
the edge. **Never clip them again** — a second rounding cuts a ring off the artwork. If you want a
shadow, trace the same radius:

```kotlin
@Composable
fun IosAppIcon(app: IosApp, modifier: Modifier = Modifier, elevation: Dp = 8.dp) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize().shadow(elevation, RoundedRectangle(14.dp), clip = false))
        Image(painterResource(app.icon), null, Modifier.matchParentSize())   // no clip
    }
}
```

Mapping tip: upstream display names are mostly Chinese app names, so bind a **stable ASCII id** to
the hub name in code (`"files" -> "File Manager"`) instead of using the hub name as the resource
name. See [`tools/fetch_app_icons.py`](../tools/fetch_app_icons.py).

**About "the SVG"**: the app-icon partition ships PNG, not SVG. If you truly need SVG, that is
pipeline A (Lucide/Feather/…), which is what `tools/generate_ios_glyphs.py` consumes.

---

## 6. Bug diary — 12 defects, root cause, fix

Every one of these compiled cleanly. Every one was found by a human looking at the screen.

| # | Symptom | Root cause | Fix |
|---|---|---|---|
| 1 | Grey-black fog appears on press | Upstream `InteractiveHighlight` adds a full-bleed `White @ 8 %` with `BlendMode.Plus` | `pressSheen`: radial, finger-centred, clipped to the shape |
| 2 | Four shadow blobs at the corners | Shadow drawn with a shape whose radius did not match the clip | Single shape constant shared by `shadow` and `clip` |
| 3 | Hard cut at top/bottom edges | `lens` displacement independent of height | `A ≈ 2H` |
| 4 | "It looks like a rounded rectangle" (non-refracting outline) | `RoundedCornerShape` instead of a G2-continuous shape | `RoundedRectangle` / `Capsule` from `shapes` (clip / non-lens glass only) |
| 4b | App crashes on open with `UnsupportedOperationException … in lens effects` | `com.kyant.shapes.RoundedRectangle` handed to `drawBackdrop` **with `lens()`** — `lens` only accepts a Compose `CornerBasedShape` (`Capsule` was observed to work, `RoundedRectangle` does not) | `RoundedCornerShape(...)` whenever `lens()` is in `effects` (docs/10 §1) |
| 5 | Transition stutters | Content re-laid out at every interpolated size | Lay out at final size, `graphicsLayer { scaleX/Y }` only |
| 6 | Lands on the home screen, and the desk is visible through the lock screen | `IosLockScreen` was transparent; lock wallpaper missing | Lock screen owns an opaque wallpaper + its own backdrop (§1.2) |
| 7 | White stripe across the bottom | `navigationBarColor = transparent` + `windowLightNavigationBar = true` on a dark, edge-to-edge surface | Black navigation bar, `isAppearanceLightNavigationBars = false`, `setNavigationBarContrastEnforced(false)` |
| 8 | Home icons are line art, not Apple icons | Pipeline A used where pipeline B belongs | `IosAppCatalog.icon` → `app-icons` PNG, `IosAppIcon` (§5) |
| 9 | Wallpaper looks like a placeholder | Procedural gradient mesh, no high-frequency detail | Real iOS 26 wallpaper, WebP in `drawable-nodpi` |
| 10 | Closing an app "just disappears" | Exit animation was a 300 ms one-beat shrink, easy to miss | Two-beat close: icon, then the island (§3.6) |
| 11 | Control centre / notification centre never open | Pull zones had **zero height** (§4.1) | `fillMaxHeight()` |
| 12 | Back exits the whole app | No `BackHandler` | Level-by-level back stack (§4.3) |

Also fixed, cheaply: app label ("Liquid Glass" → the name the user asked for) lives in
`strings.xml`, not in the manifest; and `IosAppSwitcher`'s `if (!visible) return` is what keeps an
invisible `LazyRow` out of layout — keep it.

---

## 7. Performance rules that survived

| Rule | Why |
|---|---|
| Record a layer only while someone reads it | One offscreen pass per recorded layer per frame |
| One backdrop per list, not per row | Row count × offscreen passes |
| `if (!visible) return` at the top of a hidden overlay | No composition, no layout, no draw |
| Small dedicated backdrops for small controls (e.g. 64 × 28) | Offscreen cost ∝ area |
| Scale with `graphicsLayer`, never by re-layout | Text reflow is the most expensive "animation" there is |
| Wallpaper in `drawable-nodpi`, already screen-shaped | No density resampling, no extra memory |

---

## 8. Release checklist

```
[ ] Every glass answers "what do I sit on?" — and samples that layer, not another one
[ ] No layer samples itself
[ ] Press feedback is a shaped sheen, not a full-surface addition
[ ] Wallpaper is real artwork with high-frequency detail
[ ] Home icons are pre-cut PNGs and are never clipped again
[ ] Lock screen is opaque; home is unreachable and invisible while locked
[ ] Every invisible gesture strip asserts its own size
[ ] Back walks one level, and only exits from the outermost level
[ ] System bars match the surface (dark surface → dark bars, light icons)
[ ] Closing a window is a visible two-beat animation, and the window survives it
[ ] Wallpaper/artwork provenance and licence recorded in NOTICE
```

---

## 9. The one-paragraph version

Liquid glass is **not** a translucent fill — it is a recorded background, bent by an AGSL shader,
clipped to a G2-continuous shape. Get the engine (AGSL, not blur), the pipeline order
(`vibrancy → blur → lens`, `A ≈ 2H`) and the layer graph (each surface refracts what it sits on)
right, and the material appears. Everything after that is **craft**: real artwork behind it, real
icons on top of it, gestures that match the device, and a willingness to fix the ten defects that a
compiler will never tell you about.
