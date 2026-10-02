# Apple Liquid Glass Implementation Guide

> **This document is written for an AI assistant.**
> It has one goal only: **to let any AI, by reading only this repository, build the Apple iOS 26 "liquid glass" effect on native Android**,
> and get it right — not the cheap counterfeit of "translucency + Gaussian blur", but real glass with **refraction, chromatic aberration, edge highlights, and gravity response**.
>
> Every conclusion comes from a real, end-to-end development process that ran through to a compiled APK (Jetpack Compose + Backdrop 2.0.0).
> Every parameter, every line of code, and every pitfall in this document has been verified in practice, not guessed.
>
> Last updated: 2026-10-01

---

## ★ The Authoritative Upstream — READ THIS BEFORE ANYTHING ELSE

> **This project is built entirely from the upstream [**Kyant0/AndroidLiquidGlass**](https://github.com/Kyant0/AndroidLiquidGlass) (the Compose Multiplatform "Backdrop" liquid-glass engine, published as `io.github.kyant0:backdrop:2.0.0`).**
> Everything in this repository — every effect, every API, every material recipe — is explained and implemented **strictly according to that link**. There is no other source of truth.

**If you are an AI and someone hands you this repository, do this before touching anything:**

1. **Read the upstream first**: `https://github.com/Kyant0/AndroidLiquidGlass`
   — its `README.md` and `Documentation` (the Backdrop catalog) are the ground truth for how liquid glass actually works.
2. **Then read this repository's `docs/`** — they are a practice-hardened, Android-native port of that same upstream,
   with exact recipes and every pitfall we hit. They do not replace the upstream; they are the bridge from upstream theory to a compiled APK.
3. **Never invent parameters.** All core visual parameters in `docs/04-Material-Recipe-Table.md` are copied verbatim from the upstream.
   If you change any, record why.

**In short: this repo says what the upstream means in practice. Read the link above first, then this repo.**

The full upstream & license notes live at the end of this file in [Section 6](#6-upstream-and-license).

---

## 0. Read This First: Build the Right Mental Model in 30 Seconds

**If you remember only one thing, remember this:**

> Liquid glass is not a "material property"; it is **a real-time geometric refraction of the already-rendered image behind the glass**.

This sentence determines the entire architecture:

| Wrong understanding | Correct understanding |
|---|---|
| Add `alpha = 0.3f` + `Modifier.blur()` to a Box | First you need an **offscreen texture** of "the image behind", then sample and warp it |
| The glass component draws itself | The glass component **only samples someone else's texture** |
| `Modifier.blur()` blurs its own content | What gets blurred is the **background**; the glass's own content stays sharp (this is the key to the glass feel) |

So the whole tech stack has just three steps:

```
① Record the background into an offscreen texture   →   rememberLayerBackdrop() + Modifier.layerBackdrop()
② The glass component "samples" that texture        →   Modifier.drawBackdrop(backdrop = ...)
③ Warp it by the optical formula when sampling       →   effects = { vibrancy(); blur(..); lens(..) }
```

**Without step ①, step ② draws nothing.** This is the root cause of 90% of failures.

---

## 1. Ten Iron Rules (Read Before Coding — All Learned the Hard Way)

1. **No backdrop, no glass.**
   The first argument of `drawBackdrop` is a `Backdrop` object. You must first create one with `rememberLayerBackdrop()`,
   then attach it to the "background node" with `Modifier.layerBackdrop(it)` so it has content to sample.

2. **The render pipeline order is fixed: `colorFilter → blur → lens`.**
   So when writing the `effects` block, `vibrancy()` / `colorControls()` always go on the first line, `blur()` on the second, and `lens()` last.
   Putting `lens()` first = refraction happens on the pre-blur image, and the edges get smeared.

3. **Both arguments of `lens()` are dp, not pixels.** You must use `12f.dp.toPx()`.
   The first argument is the **refraction height** (how far the lens "bulges" — determines the refraction's area of effect),
   the second is the **refraction amount** (how strong the offset is). Increasing both = thicker glass, more like a water drop.

4. **Chromatic aberration (`chromaticAberration`) is only on during "motion".**
   Static glass with aberration on → colored red/blue fringes at the edges → looks obviously fake.
   The upstream approach: when dragging/pressing, `progress → 1`, and aberration opens up with progress.

5. **`Highlight` is not a stroke; it is an "edge highlight band that fades by angle along the rim".**
   For that Apple "bright line along the edge" look, use `Highlight`, not your own `border(1.dp, Color.White)` —
   the latter is a hard, uniform edge and looks like a plastic frame.

6. **`onDrawSurface` is the glass's "tint", not a background color. Exercise extreme restraint.**
   Control Center 5% black, Dock 22%–34% white, notification card 14% white.
   Once it exceeds 50%, the glass becomes a solid rounded rectangle and all refraction is covered up.

7. **Layering: a content layer can only refract its "layer below"; an upper overlay that wants to refract "the layer below the below" must merge backdrops.**
   Use `rememberCombinedBackdrop(a, b)`. This is the only correct way to make "opening the Control Center blurs the home-screen icons".

8. **Never create a "refract-itself" loop.**
   A node must not both `layerBackdrop(itself)` and `drawBackdrop(itself)`, nor attach two recording nodes to the same `LayerBackdrop` (the second overwrites the first). The upstream approach: use an `alpha(0f)` **shadow node** to record,
   while a separate node does the actual glass drawing.

9. **The recording layer = one full-screen offscreen render per frame; it is the only performance cost of this approach.**
   So do **conditional recording**: only attach `layerBackdrop` when an upper overlay actually needs to read it.
   Leaving it attached on a static page just wastes GPU for nothing.

10. **Use `RoundedRectangle` from `com.kyant.shapes`, not `androidx.compose.foundation.shape`.**
    The former is Apple's **G2-continuous curvature** (superellipse/squircle); the latter is plain rounded corners.
    All Apple icon corners are G2-continuous; pick the wrong shape and the outline alone gives it away as fake.

---

## 2. Repository Map

```
experiments/                      ← you are here
├── README.md                     ← this document (the overview; read this first)
├── docs/
│   ├── 01-Optical-Model-of-Liquid-Glass.md      ← Principle: what refraction / dispersion / blur / highlight / shadow actually compute
│   ├── 02-Backdrop-Layered-Architecture.md      ← Architecture: LayerBackdrop / Combined / coordinate space / feedback prevention
│   ├── 03-API-Complete-Reference.md             ← API: all drawBackdrop parameters + effects + Highlight/Shadow explained one by one
│   ├── 04-Material-Recipe-Table.md              ← Recipes: exact parameters for 12 verified materials + how to derive new ones
│   ├── 05-iOS-Shell-in-Practice.md               ← In practice: how to assemble home / lock / control center / transitions
│   ├── 06-Icons-and-Asset-Pipeline.md            ← Assets: how to use ui-icons-hub (with the real "Apple app-icon zoning" list)
│   ├── 07-Build-and-Troubleshooting.md           ← Engineering: environment, build commands, all errors and fixes
│   └── 08-Previous-Handover-Notes.md             ← History: full conversation handover from the previous round (26-component showcase), kept for reference
└── LiquidGlassShowcase/          ← complete compilable project (Jetpack Compose)
```

> ⚠️ **This repository does not contain an APK.** The APK is a build artifact, not source.
> To verify the effect, build one yourself following [07-Build-and-Troubleshooting.md](docs/07-Build-and-Troubleshooting.md):
> write `sdk.dir` in `local.properties` → point `JAVA_HOME` at JDK 17 → `./gradlew assembleRelease`.
> **If you can build a 1.25 MB `app-release.apk`, the whole liquid-glass environment has been successfully reproduced.**

**The 5 source files most worth reading** (in order of importance):

| File | Why it matters |
|---|---|
| `core/glass/GlassMaterials.kt` | **The single source of truth for all materials**: exact parameters for 12 recipes, each annotated with its upstream origin |
| `core/glass/GlassScaffold.kt` | Minimal working example: how to record the wallpaper into a LayerBackdrop, in 18 lines |
| `components/GlassFoundation.kt` | How to wrap `drawBackdrop` into a reusable `GlassCard` |
| `components/GlassBars.kt` | **The most complex example**: combined backdrop + shadow recording node + dynamic dispersion; understand this and you understand it all |
| `ios/IosShell.kt` | Layered architecture in practice: the z-order and backdrop assignment of the whole Apple system shell |

---

## 3. Up and Running in Five Minutes: Minimal Runnable Example

This is **all the code** needed to get "liquid glass" running. Copy it in and you'll see the effect.

### 3.1 Dependencies

```kotlin
// gradle/libs.versions.toml
[versions]
backdrop = "2.0.0"       // io.github.kyant0:backdrop —— liquid-glass engine
kyantShapes = "1.2.0"    // io.github.kyant0:shapes  —— Apple G2-continuous-curvature shapes (a transitive dependency of backdrop)

[libraries]
backdrop = { group = "io.github.kyant0", name = "backdrop", version.ref = "backdrop" }
kyant-shapes = { group = "io.github.kyant0", name = "shapes", version.ref = "kyantShapes" }
```

```kotlin
// app/build.gradle.kts
android {
    minSdk = 31          // hard requirement: AGSL needs API 31+
    compileSdk = 37
}
dependencies {
    implementation(libs.backdrop)
    implementation(libs.kyant.shapes)
}
```

> ⚠️ **`minSdk` must be ≥ 31.** This effect is built on **AGSL (Android Graphics Shading Language)**,
> a runtime shader; below API 31 there is no `RuntimeShader`, which fails with ClassNotFound.

### 3.2 Step One: Record the Background into a Backdrop

```kotlin
@Composable
fun WallpaperScaffold(content: @Composable BoxScope.(backdrop: Backdrop) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        // ① Create an offscreen texture
        val backdrop = rememberLayerBackdrop()

        Image(
            painter = painterResource(R.drawable.wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)   // ② Record this node's per-frame image into the texture
        )

        content(backdrop)                  // ③ Hand the texture to all upper glass components
    }
}
```

**These two lines are the key to the whole approach.** Whatever the node modified by `layerBackdrop` draws ends up in the backdrop.

### 3.3 Step Two: Draw a Piece of Glass

```kotlin
@Composable
fun GlassCard(backdrop: Backdrop, modifier: Modifier = Modifier) {
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,                       // which texture to sample
            shape = { RoundedRectangle(32.dp) },       // G2-continuous rounded corners (Apple's squircle)
            effects = {
                vibrancy()                             // ① boost saturation so the glass looks "luminous" not "grey"
                lens(16f.dp.toPx(), 32f.dp.toPx())     // ② refraction: the whole secret of glass thickness
            }
        )
    )
}
```

### 3.4 Step Three: How to Tune It to Look "Like Apple"

Only three knobs are at work; remember their directions:

| Desired effect | What to change | Direction |
|---|---|---|
| Glass looks "thicker", more like a water drop | Increase both `lens()` arguments together | `16→24`, `32→48` |
| Background blurrier / more "foggy" | Increase `blur()` | `2→8→16` |
| Glass more "solid", more obscuring | Deepen `onDrawSurface = { drawRect(color) }` | white 0.05 → 0.34 |
| The bright edge line stronger | `highlight = { Highlight.Default.copy(alpha = 1f) }` | `0.5→1.0` |

**Only these three directions.** Everything else is detail. In Apple's official demo, the Control Center uses "large lens + strong highlight",
the Dock uses "small lens + medium tint", and dialogs use "large lens + large blur + depthEffect".
For concrete values see `docs/04-Material-Recipe-Table.md`.

---

## 4. One-Line Answers to Common Questions

| Question | Answer |
|---|---|
| Why is my glass grey? | Missing `vibrancy()`. Blur greys out colors; `vibrancy()` restores the saturation |
| Why can't I see anything through my glass? | `onDrawSurface` color is too dark and covers the refraction. Drop it to 5%–35% |
| Why are there red/blue fringes at the edges? | `chromaticAberration = true` is always on. It should only open on press/drag |
| Why are my glass edges hard-edged? | You used `border()`. You should use `highlight = { Highlight.Default }` |
| Why do the rounded corners look wrong? | You used `RoundedCornerShape`. You should use `com.kyant.shapes.RoundedRectangle` |
| Why is my glass empty/black inside? | The backdrop recorded nothing. Check whether `layerBackdrop` is attached to the node that actually draws the background |
| Why does the glass show the previous frame when I open a panel? | The recording node and the drawing node are ordered backwards. The content layer must draw before the overlay |
| Why is it laggy? | `layerBackdrop` is attached unconditionally. Change it to "record only when needed" |
| Why does it crash on older phones? | `minSdk < 31`, no AGSL |
| Can I change the parameters myself? | Yes, but change them **proportionally**; see the "Deriving new recipes" section in `docs/04-Material-Recipe-Table.md` |

---

## 5. Suggested Reading Order

- **Never touched it before** → read `01` Optical Model → `02` Architecture → `03` API → copy the code from `03.2/03.3`
- **Just want the parameters** → go straight to `04-Material-Recipe-Table.md`
- **Building a full app** → see `05-iOS-Shell-in-Practice.md`, which has the layered diagram of the whole iOS shell
- **Need icons** → `06-Icons-and-Asset-Pipeline.md` (includes the real inventory of Apple icons from ui-icons-hub)
- **Build failing** → `07-Build-and-Troubleshooting.md`

---

## 6. Upstream and License

- Liquid-glass engine: [**Kyant0/AndroidLiquidGlass**](https://github.com/Kyant0/AndroidLiquidGlass) → published as the Maven artifact `io.github.kyant0:backdrop:2.0.0`
- Shape library: [**Kyant0/AndroidShapes**](https://github.com/Kyant0/AndroidShapes) → `io.github.kyant0:shapes:1.2.0` (Apache-2.0)
- Several files in `core/utils/*` and `components/*` of this repository are 1:1 ports of the upstream samples; the original copyright notices are preserved in their file headers
  (`Copyright 2025 Kyant`, Apache-2.0); **do not remove these notices**.
- Newly added code in this repository (the `ios/` directory, the organisation of `GlassMaterials.kt`, etc.) is `Copyright 2026 The Liquid Glass Showcase authors`.
- **All core visual parameters — refraction strength, dispersion coefficient, blur radius, highlight brightness, etc. — are copied verbatim from upstream; no self-tuned parameters.**
  If you change any, please record what you changed and why in `docs/04-Material-Recipe-Table.md`.
