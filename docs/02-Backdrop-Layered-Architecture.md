# 02 · Backdrop Layered Architecture

> Chapter 01 explained "which 6 optical cues the glass must satisfy."
> This chapter tackles the **more fundamental question**: where does the **image** the glass refracts come from, when it gets drawn, who goes first,
> and why getting the "layering" wrong inevitably produces black blocks, ghosting of the previous frame, or simply nothing at all.
>
> **One-line conclusion: `drawBackdrop` is only the "sampler"; `layerBackdrop` is the "recorder". Without a recorder, the sampler samples thin air.**

---

## 1. Three Core Objects — Don't Confuse Them

In Backdrop 2.0.0 there are only three kinds of things; once you understand their roles, everything else falls into place.

| Role | Type | How to create | Purpose |
|---|---|---|---|
| **Read-only interface** | `Backdrop` | provided by the library | the input type of `drawBackdrop`. It only has the "you can read me" capability, not the "write into me" capability |
| **Recording implementation** | `LayerBackdrop` | `rememberLayerBackdrop()` | actually holds that **offscreen texture**. Attach it to a node via `Modifier.layerBackdrop(it)`; **whatever that node draws each frame becomes the texture's content** |
| **Derived Backdrop** | also `Backdrop` | `rememberCombinedBackdrop(...)` / `rememberBackdrop(...)` / `drawBackdrop(exportedBackdrop = ...)` | doesn't hold its own texture; instead it "composites/transforms on top of someone else's texture" before handing it to samplers |

**Key distinction: `rememberLayerBackdrop()` is "create a new canvas"; `Modifier.layerBackdrop()` is "connect the brush to this canvas."**

Many people only write the first one, so the canvas stays empty forever — this is the #1 cause of "there's nothing inside the glass."

```kotlin
val backdrop = rememberLayerBackdrop()          // ① create an empty canvas

Image(
    modifier = Modifier
        .layerBackdrop(backdrop)                // ② attach this node to the canvas → from now on what it draws gets recorded
        .fillMaxSize()
)

GlassCard(backdrop = backdrop)                  // ③ someone else samples it
```

---

## 2. Recording: What Exactly Does `Modifier.layerBackdrop` Record

### 2.1 Semantics

`Modifier.layerBackdrop(backdrop)` is a **draw modifier**:

1. It opens an **offscreen layer** for the host node;
2. The host node **itself + all its children** are drawn into this layer;
3. After the layer finishes drawing, **the entire image is written into the texture held by `backdrop`.**

So "what gets recorded" depends on **which node you attach the modifier to**:

```kotlin
// ✅ correct: attach to the node that actually draws the background → texture contains the wallpaper
Image(painter = wallpaper, modifier = Modifier.fillMaxSize().layerBackdrop(backdrop))

// ❌ wrong: attach to an empty Box → texture is transparent
Box(Modifier.layerBackdrop(backdrop))

// ❌ wrong: attach to the glass's own node → the glass records itself and refracts itself = feedback loop
Box(Modifier.layerBackdrop(backdrop).drawBackdrop(backdrop))
```

### 2.2 Coordinate Space: Screen Coordinates, Not Local Coordinates

The layer records the image in the **root coordinate system (root / screen coordinates)**, and `drawBackdrop` samples using the same coordinate system.
So you **don't need** any coordinate conversion — place the glass anywhere on screen and it automatically samples the background directly behind it.

This is also why `IosWallpaper` and `IosHomeScreen` in this repo simply use `fillMaxSize()`:
record full-screen, sample locally — naturally aligned.

### 2.3 Cost: This Is the Only Major Overhead in the Whole Approach

Recording one frame = **one full-screen offscreen render**. Its cost is proportional to the number of screen pixels, and has little to do with "how complex the recorded content is."

From this we get the most important engineering rule in this repo (see [IosShell.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShell.kt#L77-L99)):

```kotlin
// only attach layerBackdrop when something above actually reads it
val surfaceIsRead = state.isLocked || state.controlCenter || state.notificationCenter

Box(
    Modifier
        .fillMaxSize()
        .then(if (surfaceIsRead) Modifier.layerBackdrop(surfaceBackdrop) else Modifier)
) { /* home screen + open app */ }
```

- When the user is normally swiping gestures on the home screen, **no overlay is reading it** → no recording → zero extra cost;
- Only when pulling down the control center / the lock screen appears → start recording.
- A static page keeps `layerBackdrop` attached and idle, purely wasting GPU.

> The upstream (Kyant0) demo pages are few and simple, so you can just leave it always on;
> but for a system-level shell you **must** do this conditional recording, otherwise a full-screen offscreen pass keeps running all the time.

### 2.4 Timing: The Recording Node Must "Finish Drawing Before Being Sampled"

Draw order = composition order (children of `Box` are drawn in writing order). So **the content layer must be written before the overlay**:

```kotlin
Box {
    // ① draw first: the content layer (recorded into surfaceBackdrop)
    Box(Modifier.layerBackdrop(surfaceBackdrop)) {
        IosHomeScreen(...)
        IosOpenAppWindow(...)
    }

    // ② draw later: the overlay (samples the texture recorded in the previous step)
    IosControlCenter(backdrop = overlayBackdrop, ...)
}
```

If written in reverse, the overlay will sample **the previous frame's** content (or even blank) — manifesting as "when opening the control center, the glass shows ghosting/black blocks of the home screen."

---

## 3. Compositing: `rememberCombinedBackdrop` — "The Overlay Needs to See the Lower Layer + the One Below It"

### 3.1 The Problem

This repo has two recording layers (see [IosScaffold.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosScaffold.kt#L120-L148)):

| Layer | Records | Who samples it |
|---|---|---|
| `wallpaperBackdrop` | wallpaper | glass inside the home screen (Dock cards, etc.), glass after opening an app |
| `surfaceBackdrop` | home screen / opened app (recorded only when an overlay appears) | no direct sampler; it exists purely for compositing |
| `overlayBackdrop` = combination of the above two | wallpaper **+** home screen/app | lock screen, control center, notification center |

Why can't the overlay just sample `surfaceBackdrop`?
Because `surfaceBackdrop` only records the "home screen / app" layer's content, **without the wallpaper**; and most of the home screen is transparent,
so sampling only it → empty behind the glass. Why can't it just sample `wallpaperBackdrop`?
Because then the glass wouldn't show the app icons on the home screen — yet on real iOS when pulling down the control center, **the home-screen icons are blurred and distorted**.

### 3.2 The Solution

```kotlin
val overlayBackdrop = rememberCombinedBackdrop(wallpaperBackdrop, surfaceBackdrop)
```

The semantics is **layer stacking (painter's algorithm)**: from bottom to top, `wallpaperBackdrop` → `surfaceBackdrop`,
and the result is handed to the sampler as a new virtual texture. So the overlay glass shows both wallpaper and home-screen icons at once, and the whole thing is refracted together.

> `rememberCombinedBackdrop` is a vararg: two layers go through `Combined2Backdrops`, three through `Combined3Backdrops`,
> and more through `CombinedBackdrops`. When your world has more than three layers (wallpaper / content / the previous overlay), you can keep stacking upward.

### 3.3 The Full Layering Diagram of This Repo

```
IosShell                                    z-axis (bottom to top)
├─ IosWallpaper ──layerBackdrop──▶ wallpaperBackdrop        ← layer 0: wallpaper
│
├─ Box ──layerBackdrop(conditional)────▶ surfaceBackdrop    ← layer 1: screen content
│   ├─ IosHomeScreen (Dock samples wallpaperBackdrop)
│   └─ IosOpenAppWindow (in-app glass samples appBackdrop)
│
├─ IosStatusBar                (not recorded, floats on top, stays readable when covering an app)
├─ IosLockScreen               ← samples overlayBackdrop
├─ IosAppSwitcher
├─ IosControlCenter            ← samples overlayBackdrop
├─ IosNotificationCenter       ← samples overlayBackdrop
├─ IosPullDownZones / IosHomeGesture
└─ IosDynamicIsland            (never glassified, just a solid-black hardware cutout)
```

The corresponding code: `overlayBackdrop` in `IosShell` is created at [IosShell.kt L75](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShell.kt#L75),
the content layer is at [L83-L99](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShell.kt#L83-L99),
and the overlays are at [L102-L141](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShell.kt#L102-L141).

### 3.4 Why the App Uses a Separate Backdrop Internally

After opening an app, the window has an **opaque system base color** (`Color(0xFFF2F2F7)`). At this point:

- If the glass **inside** the window keeps sampling `wallpaperBackdrop`, it would refract "the wallpaper hidden behind the app" —
  physically nonsensical (light can't pass through an opaque base), and visually it floats;
- The correct approach is to record a layer for the window **itself**: `appBackdrop` at [IosAppWindow.kt L109](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosAppWindow.kt#L109),
  recording "system base color + the app's top gradient", and all in-app glass samples it ([L171-L188](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosAppWindow.kt#L171-L188)).

**Rule: the glass must refract "what it visually sits on top of", not "the first layer that got recorded."**

---

## 4. Derived: `rememberBackdrop` — Transform a Texture Before Using It

`rememberBackdrop(source) { drawBackdrop -> ... }` creates a **derived texture**:
Calling `drawBackdrop()` inside the lambda is equivalent to "draw `source` out", and you can wrap it in any `DrawScope` transform.

This repo uses it to solve a very specific problem: **a toggle / slider's "knob" should look like a drop of liquid sucked up out of the track**,
whereas `drawBackdrop` can only sample the "raw, untransformed" background.

The approach ([GlassControls.kt L184-L194](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/components/GlassControls.kt#L184-L194)):

```kotlin
val trackBackdrop = rememberLayerBackdrop()   // record this small "track" region

// the track itself: recorded transparently (can be sampled by the knob above)
Box(Modifier.layerBackdrop(trackBackdrop).clip(Capsule()).size(64.dp, 28.dp))

// the knob: sample = background + (the track squashed to 2/3)
drawBackdrop(
    backdrop = rememberCombinedBackdrop(
        backdrop,                                   // real background
        rememberBackdrop(trackBackdrop) { drawBackdrop ->
            val scaleX = lerp(2f / 3f, 0.75f, progress)   // when pressed, the track is "pulled away"
            val scaleY = lerp(0f, 0.75f, progress)
            scale(scaleX, scaleY) { drawBackdrop() }      // ← drawn after the transform
        }
    ),
    shape = { Capsule() },
    effects = GlassMaterials.toggleKnob(...)
)
```

Effect: at rest the knob shows the full track inside it; when pressed the track is pulled away 1/3 horizontally and squashed vertically → it looks like the liquid in the track is sucked into the knob.

> This is the most "showing-off" piece of code in the whole repo. **Read it three times first.** Once you understand it, you've grasped the abstraction levels of `Backdrop`.
> The slider is written exactly the same way; see [GlassControls.kt L350-L360](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/components/GlassControls.kt#L350-L360).

---

## 5. `exportedBackdrop` — Glass on Top of Glass

### 5.1 The Problem

You need to place one piece of glass **on top of** another: e.g., the two buttons in a dialog (buttons sit on the dialog glass),
each row of a bottom action sheet (rows sit on the panel glass), the splash-screen progress slider (slider sits on the panel glass).

The naive approach is "parent node records a layer, child node samples it", but the parent itself is produced by `drawBackdrop`,
and its drawing result includes "the already-refracted background", and this result **needs a recorder before the child can sample it** —
so it's easy to end up writing "both parent and child attach `layerBackdrop`", and then they feed back into each other.

### 5.2 The Solution: `drawBackdrop(exportedBackdrop = ...)`

```kotlin
val panelBackdrop = rememberLayerBackdrop()

Column(
    Modifier.drawBackdrop(
        backdrop = backdrop,          // sample the real background
        shape = { panelShape },
        effects = GlassMaterials.dialog(isLightTheme),
        exportedBackdrop = panelBackdrop   // ← export my "post-draw" appearance as a new texture
    )
) {
    // child buttons sample panelBackdrop → they refract the "parent glass", not the background
    DialogActionButton(backdrop = panelBackdrop, ...)
}
```

The semantics of `exportedBackdrop` is: **this node itself (including the effects result) is exposed as a new Backdrop.**
It's the library's officially recommended way to do "glass-on-glass", safer than hand-rolling a double `layerBackdrop`, because it's managed by the node's own lifecycle,
so there's no "record one frame, sample one frame" race condition.

This repo uses it in three places:

| Location | Code |
|---|---|
| Dialog's two buttons | [GlassOverlays.kt L137](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/components/GlassOverlays.kt#L137) (`panelBackdrop`) |
| Each row of the bottom action sheet | [GlassOverlays.kt L357](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/components/GlassOverlays.kt#L357) (`sheetBackdrop`) |
| Splash-screen panel + progress bar | [SplashScreen.kt L88](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/screens/SplashScreen.kt#L88) (`panelBackdrop`) |

---

## 6. The Feedback Loop: The Only Place That "Looks Like It Should Be Written That Way, But Absolutely Must Not Be"

### 6.1 What Is a Feedback Loop

```
Node A: Modifier.layerBackdrop(bd)   ← draws A into bd
Node A: Modifier.drawBackdrop(bd)    ← then reads bd
```

What A reads contains A itself → self-excitation every frame → flickering / smearing into a black blob.

**An iron rule: for the same `LayerBackdrop`, the recording node and the sampling node must be two different nodes.**

### 6.2 The Upstream "Shadow Recording Node" Trick

The bottom bar's "selection indicator" needs to **refract the bottom bar itself** (the indicator is a piece of glass on the bottom bar, and must be able to blur out the bottom bar's text/icons).

The approach ([GlassBars.kt L253-L291](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/components/GlassBars.kt#L253-L291)):

```kotlin
val tabsBackdrop = rememberLayerBackdrop()

// ① shadow node: alpha(0f) → invisible to the user, but still drawn every frame → records the "tab bar's icons and text" into tabsBackdrop
Row(
    Modifier
        .alpha(0f)                     // ← key: fully transparent
        .layerBackdrop(tabsBackdrop)
        .drawBackdrop(backdrop = backdrop, shape = { Capsule() }, effects = { ... })
) { tabContent }

// ② the real indicator: samples combined(background, shadow layer) → so it refracts the "tab bar's icons"
Box(
    Modifier.drawBackdrop(
        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
        shape = { Capsule() },
        effects = GlassMaterials.selectionIndicator(progress)
    )
)
```

Why this works: `alpha(0f)` only affects **visibility at final compositing**, and **does not affect it being recorded into the layer**.
So the shadow node "exists but is invisible" — it provides the recording content without the user seeing duplicate icons.

> This is a very "counter-intuitive yet extremely useful" trick, worth its own note:
> **To refract some content but not show it twice → duplicate it, set `alpha(0f)`, and attach `layerBackdrop`.**

---

## 7. When to Record, and Which Layer? Decision Tree

Ask yourself these 4 questions in order:

```
Q1  What does this glass visually sit on top of?
      ├─ wallpaper / gradient      → sample wallpaperBackdrop
      ├─ opaque app base color     → create appBackdrop, sample it
      └─ another piece of glass    → use exportedBackdrop

Q2  Has that "thing being sat on" been recorded?
      ├─ yes → use it directly
      └─ no  → create rememberLayerBackdrop(), attach to the node that draws that thing

Q3  Need to "see two layers at once"? (overlay needs both the content layer and the wallpaper)
      └─ yes → rememberCombinedBackdrop(lower, upper)

Q4  Need the sampled content "transformed"? (water drop pulls the track away)
      └─ yes → rememberBackdrop(source) { drawBackdrop -> transform { drawBackdrop() } }
```

---

## 8. Coordinates, Transforms, and "Why the Glass Moves Along"

- **Recording** happens in root coordinates; **sampling** also uses root coordinates. So when the glass **translates**, it samples the background under its new region → visually "the glass slides over the background", correct.
- If you add `graphicsLayer { translationX = ... }` to the glass (e.g., the bottom-bar indicator sliding with the finger),
  the **position** it samples also changes — this is exactly the source of the "liquid" feel: the glass keeps refracting different content while moving.
- **Do not** add `scale/rotation` to a `layerBackdrop` node to "fix the picture". To transform the sampled content, use `rememberBackdrop` from §4.
- `shadow` / `innerShadow` / `highlight` are all drawn in the **glass's own layer**, and don't participate in background refraction nor affect backdrop content.

---

## 9. Performance Checklist (Just Copy It)

| Rule | Reason |
|---|---|
| Recording node **conditionally attached** (`state.isLocked \|\| ...`) | a full-screen offscreen pass every frame is the only major cost |
| Each backdrop **attaches exactly one** recording node | a later attachment overrides the earlier one |
| Keep recording nodes as **small** as possible (the slider only records the 64×28 track) | offscreen layer area ∝ cost; this repo's `trackBackdrop` is the example |
| You can have many sampling nodes | sampling is just a shader read, far cheaper than recording |
| Avoid per-frame randomness/IO inside `effects` | `effects` runs every frame |
| Stronger `lens()` → larger `padding` → wider sampling range | the library auto-expands padding (`getPadding/setPadding`); don't manually add another clip that trims it away |

---

## 10. Error Reference Table for This Chapter

| Symptom | Root cause | Fix |
|---|---|---|
| Glass is fully black / fully transparent | only `rememberLayerBackdrop()` was called but `layerBackdrop` wasn't attached | attach the modifier to the node that actually draws the background |
| Glass shows "previous frame" content | sampling node drawn before the recording node | write the content layer before the overlay |
| Screen flickers / smears into black blocks | same node both records and samples (feedback loop) | split into two nodes, or use an `alpha(0f)` shadow node |
| Overlay glass shows only wallpaper, no home-screen icons | only `wallpaperBackdrop` was sampled | use `rememberCombinedBackdrop(wallpaper, surface)` |
| Overlay glass shows only blank | sampled `surfaceBackdrop` but recording was conditionally disabled | check whether the conditional-recording switch covers the current state |
| Glass-on-glass turns into a solid color block | child node sampled the background instead of the parent glass | add `exportedBackdrop` to the parent, child samples it |
| Glass looks "floaty" after opening an app | in-app glass still samples the wallpaper | create an `appBackdrop` for the app's opaque base |
| Stuttering while scrolling | unconditional recording | switch to conditional recording |

---

## 11. Summary

```
rememberLayerBackdrop()  →  an empty canvas
Modifier.layerBackdrop() →  attach a node to the canvas (records one frame per frame)
Modifier.drawBackdrop()  →  sample the canvas (can happen elsewhere, on another layer, at another time)
rememberCombinedBackdrop →  stack two canvases into one (solves "overlay must see the content layer")
rememberBackdrop         →  transform a canvas before using it (solves "water drop pulls the track away")
exportedBackdrop         →  turn "the glass itself" into a canvas (solves "glass-on-glass")
alpha(0f) + layerBackdrop→  invisible recording (solves "refract but don't show twice")
```

**Next step**: read `03-API-Complete-Reference.md` and go through each parameter of `drawBackdrop` one by one.
