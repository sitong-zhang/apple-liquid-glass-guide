# 01 · The Optical Model of Liquid Glass

> This chapter answers one question: **what separable visual cues actually make something look like "glass"?**
> Only by breaking these cues apart do you know which API parameter is responsible for which cue — and stop ending up with "I tweaked it for ages and it still doesn't look right."

---

## 1. Real glass has 6 visual cues — not one can be missing

Hold a real piece of frosted glass (say, the camera glass on the back of an iPhone) up to your eye, and you are seeing 6 things happen at once:

| # | Cue | Physical cause | What happens if it's missing |
|---|---|---|---|
| 1 | **Background distortion** | Light refracts at the medium boundary; the glass acts as a "lens," shrinking / shifting / stretching what's behind it | Missing → looks like a sticker pasted on |
| 2 | **Background blur** | Surface roughness + thickness cause scattering | Missing → background too sharp, looks invisible |
| 3 | **Chromatic dispersion (color fringing)** | Different wavelengths refract differently (red bends less than blue) | Missing → looks "weightless" **while moving** |
| 4 | **Edge highlight** | Grazing angle at the edge → high reflectance (Fresnel effect) | Missing → the strongest hint that "glass is present" is gone; looks cut-out |
| 5 | **Outer shadow** | The glass has thickness and occludes light | Missing → the glass "won't lift off," it sticks to the background |
| 6 | **Self tint** | The glass itself is colored / reflective | Missing → feels too "empty," but don't overdo it (see below) |

**Key insight: of these 6 cues, 1, 2, 3, 6 are "looking at the background through the glass," while 4, 5 are "the glass's own sense of presence."**

Apple doesn't win by making any of these 6 stronger — it wins because **each one is just enough, and cue 6 is extremely restrained.**
The vast majority of knock-offs fail at cue 6 — they paint the glass as a semi-transparent white block.

---

## 2. Six cues → six APIs

| Cue | API | Where it lives |
|---|---|---|
| ① Background distortion | `lens(refractionHeight, refractionAmount, chromaticAberration, depthEffect)` | the **last line** inside `effects` |
| ② Background blur | `blur(radius)` | the **second-to-last line** inside `effects` |
| ③ Chromatic dispersion | `lens(..., chromaticAberration = true)` | the same `lens` call |
| ④ Edge highlight | `highlight = { Highlight(...) }` | a standalone parameter of `drawBackdrop` |
| ⑤ Outer shadow | `shadow = { Shadow(...) }` (inner dark edge uses `innerShadow`) | a standalone parameter of `drawBackdrop` |
| ⑥ Self tint | `onDrawSurface = { drawRect(color) }` | a standalone parameter of `drawBackdrop` |
| (brightening correction) | `vibrancy()` / `colorControls(brightness, saturation)` | the **first line** inside `effects` |

```kotlin
Modifier.drawBackdrop(
    backdrop = backdrop,
    shape = { RoundedRectangle(24.dp) },
    effects = {
        vibrancy()                      // ⑦ color correction (corrects the gray from ②)
        blur(8f.dp.toPx())              // ② background blur
        lens(24f.dp.toPx(), 24f.dp.toPx(), chromaticAberration = false)  // ①③ refraction + dispersion
    },
    highlight = { Highlight.Default },  // ④
    shadow = { Shadow.Default },        // ⑤
    innerShadow = { ... },              // ⑤'
    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.22f)) }  // ⑥
)
```

**Each parameter is responsible for exactly one cue.** Figure out "which cue am I missing right now," then go change the matching parameter — don't blindly fiddle with the two numbers in `lens`.

---

## 3. Why the pipeline order is `colorFilter → blur → lens` (non-negotiable)

This isn't a style choice — it's **mathematically required**:

```
original background texture
   │
   ├─ ① colorFilter / vibrancy / colorControls
   │     Color transform on the "sharp original pixels." At this point pixels still
   │     map 1:1, so the color matrix is most accurate.
   │
   ├─ ② blur
   │     Convolution on the color-transformed image. Larger blur radius → wider sampling.
   │
   └─ ③ lens
         Geometric sample offset on the "already-blurred image."
         If you put lens before blur: refract first → out-of-bounds pixels appear at the
         sampling edge → then blur smears those out → a dirty / washed-out white edge appears.
```

**Engineering conclusion: the write order inside the `effects` block IS the execution order, and `lens` must be the last line.**
All 12 recipes in `GlassMaterials.kt` obey this order without exception.

> Addendum: if you need extreme color control, there are also `colorFilter(ColorFilter)` and `opacity(alpha)`, which belong to phase 1 alongside `vibrancy()`.

---

## 4. What the two `lens()` parameters are actually tuning

This is the place in the whole system where you most need to build intuition.

### 4.1 Parameter semantics

```kotlin
lens(
    refractionHeight = 24f.dp.toPx(),   // first parameter
    refractionAmount = 48f.dp.toPx(),   // second parameter
    chromaticAberration = false,
    depthEffect = false
)
```

Imagine the glass as a lens that is **raised at the edges and flat in the middle** (that's exactly how Apple models Liquid Glass):

| Parameter | Intuitive model | When increased | When decreased |
|---|---|---|---|
| `refractionHeight` (refraction height) | How **high** the lens "bulges"; decides how **wide** the refraction band reaches from the edge inward | Refraction band widens; the whole pane warps, like a thick water drop | Refraction band narrows; only the 1~2mm along the edge distorts |
| `refractionAmount` (refraction amount) | The maximum **pixel displacement**; decides how **hard** it twists | Background pushed further, more "convex," stronger magnification | Flatter, closer to ordinary frosted glass |

**Rule of thumb:**

```
refractionAmount ≈ 2 × refractionHeight
```

Every recipe in the repo follows this ratio (`12/24`, `24/24`, `24/48`, `16/32`, `10/14`… the second is slightly ≥ twice the first).
**When deriving a new recipe yourself, start at 2:1, then fine-tune.** See `04-Material-Recipe-Table.md`.

### 4.2 Why it must be `dp.toPx()`

The unit of `lens`'s parameters is **pixels** (`Float`, physical screen pixels).
If you write `24f` without `toPx()`, on a 3x screen the refraction is only 1/3 of what it should be — which is exactly why many people say "I copied the parameters but the effect is wrong."

**Rule: every length passed to `blur()` / `lens()` / `Highlight(width=)` / `Shadow(radius=)` must go through `dp.toPx()`.**

Exception: inside the `drawBackdrop` lambda, e.g. `InnerShadow(radius = 8f.dp * progress)`, `BackdropEffectScope` is itself a `Density`, so you **can write `8f.dp.toPx()` directly** — which is why both styles appear upstream; `GlassMaterials.kt` standardizes on `.toPx()`.

### 4.3 `depthEffect`

When enabled, the refraction direction changes from "in-plane displacement" to a "depth"-bearing spherical mapping — the middle of the glass looks more "bulged."
Upstream only uses it on large-area, strongly-suggestive materials: **dialogs** and the **control center**. Don't enable it on small widgets — it looks distorted.

### 4.4 `chromaticAberration` — only turn it on while moving

Physically, dispersion = sampling the R/G/B channels with **slightly different offsets**. At rest the eye easily sees through it ("why does this glass edge always have color fringing").

Upstream's uniform approach is to bind dispersion to a `progress`:

```kotlin
// Upstream components/LiquidBottomTabs.kt — the moving selection indicator
fun selectionIndicator(progress: Float): BackdropEffectScope.() -> Unit = {
    lens(
        10f.dp.toPx() * progress,
        14f.dp.toPx() * progress,
        chromaticAberration = true     // always on, but overall strength → 0 with progress
    )
}
```

`progress` comes from press or drag progress; at rest `progress = 0`, and `lens(0, 0)` is a no-op, so the dispersion naturally disappears.

**This is the core of the "Apple feel": every exaggerated optical effect only appears at the instant of interaction.**

---

## 5. `vibrancy()` — the easiest-to-miss line, yet the one that decides success

Mathematically, Gaussian blur is a **low-pass filter**: it removes high frequencies (detail) while **reducing local contrast and perceived saturation.**
The result: the blurred background goes **gray**.

What `vibrancy()` does is compensate for exactly that: **raise saturation + slightly raise brightness**, turning "gray glass" back into "clear glass."

Compare:

```kotlin
// ❌ Dull and gray, like frosted plastic
effects = { blur(16f.dp.toPx()); lens(24f.dp.toPx(), 48f.dp.toPx()) }

// ✅ Clear and bright, like real glass
effects = { vibrancy(); blur(16f.dp.toPx()); lens(24f.dp.toPx(), 48f.dp.toPx()) }
```

For **finer** control, use the explicit parameters of `colorControls` (upstream dialogs do exactly this):

```kotlin
colorControls(
    brightness = if (isLightTheme) 0.2f else 0f,
    saturation = 1.5f
)
```

- Light theme: bright background → brighten 0.2, saturate 1.5 (otherwise white glass looks gray)
- Dark theme: dark background → don't brighten, only saturate (brightening further would blow it out)

**Remember this correspondence: the larger the `blur`, the more you need `vibrancy` to catch it.**

---

## 6. `Highlight` — the source of the glass's "sense of presence"

### 6.1 What it is

It's not a stroke. It's **a highlight band that runs along the shape outline, at a specified angle, fading inward.**

```
        ┌─────────────────────┐
        │  ╭───────────────╮  │   ← highlight band (width, softened blurRadius,
        │  │               │  │      opacity alpha, falloff curve)
        │  │    glass interior   │  │
        │  ╰───────────────╯  │
        └─────────────────────┘
         ↑ highlight only appears on the side "facing the light," rotates with angle
```

### 6.2 Three usable presets

| Preset | Meaning | Where to use |
|---|---|---|
| `Highlight.Default` | Default direction (diagonal highlight lit from top-left) | The vast majority of components; the library auto-uses it when the parameter is omitted |
| `Highlight.Ambient` | Ambient light (more even, weaker) | Large flat panels, to avoid an obvious one-directional bright edge |
| `Highlight.Plain` | Minimal | When you need almost no highlight |

`Highlight.Default.copy(alpha = progress)` is the most common idiom: **the highlight strengthens on press.**

### 6.3 Gravity highlight — the key detail of the Apple feel

Upstream's control-center idiom:

```kotlin
val uiSensor = rememberUISensor()      // accelerometer → gravity direction

val glassHighlight = {
    Highlight(
        style = HighlightStyle.Default(
            angle = uiSensor.gravityAngle,   // ← highlight direction follows the phone's tilt
            falloff = 2f                     // ← falloff curve; larger = "falls off faster"
        )
    )
}
```

`gravityAngle` is smoothly derived from `TYPE_ACCELEROMETER`'s `atan2(y, x)` (a first-order low-pass with `alpha = 0.5f`), initial value `45f`. **When the phone tilts, the highlight on the glass "slides" along with it** — a hidden detail that makes people instantly feel it's "real."

Implementation in `core/utils/UISensor.kt` (a 1:1 port of upstream's `utils/UISensor.kt`).

> There is already `InteractiveHighlight` (`core/utils/InteractiveHighlight.kt`) for "a local highlight appears where the finger presses," used by upstream's bottom-bar selection indicator.

### 6.4 Don't use `border()`

```kotlin
// ❌ Uniform hard edge, like a plastic frame
Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))

// ✅ Bright edge on the light-facing side, none on the other
highlight = { Highlight.Default }
```

---

## 7. `Shadow` and `InnerShadow`

| Parameter | Effect | Typical value |
|---|---|---|
| `shadow` | Outer shadow, gives the glass "thickness" and a "lifted" feel | Library default `Shadow.Default`; in animations often `Shadow(alpha = progress)` |
| `innerShadow` | Inner shadow, presses a dark ring along the glass's inner edge to simulate **beveled thickness** | `InnerShadow(radius = 8f.dp * progress, alpha = progress)` |

`innerShadow` is the invisible source of "premium feel": if the glass edge only has a bright line and no dark edge, it looks very thin, like paper.
**Bright edge + inner dark edge = a solid object with thickness.**

Upstream only adds `innerShadow` in the **pressed dynamic state** (`progress` from 0 → 1), never at rest, to avoid dirty edges.

---

## 8. The "ratio table" of the six cues (why Apple looks expensive)

| Cue | Apple's strength | Common knock-off | Consequence |
|---|---|---|---|
| Background blur | Restrained, 2~16dp, scaled to size | Jumps straight to 40dp | Background smeared into one blob; glass becomes "jelly" |
| Refraction | Clear, and proportional to size | No refraction or over-refraction | Former = sticker, latter = funhouse mirror |
| Dispersion | Only in motion, very weak | Always on, very strong | Cheap color fringing |
| Edge highlight | Soft, ~1~2dp visual magnitude, with falloff | 1px hard white stroke | Plastic frame |
| Outer shadow | Very weak, very diffuse | Deep black hard shadow | Sticker |
| Self tint | **5%~35%** | 60%+ white | White card, not glass |

**In one sentence: push ⑥ to the minimum, get ①④ right, and let the rest follow.**

---

## 9. Anti-pattern list (these approaches will never look right)

| ❌ Approach | Why it's wrong |
|---|---|
| `Modifier.background(Color.White.copy(0.3f))` + `Modifier.blur()` | You're blurring **yourself**, not the background. This is frosted plastic |
| `Modifier.alpha(0.5f)` | Translucent ≠ glass — no refraction, no highlight, no edge |
| `border(1.dp, Color.White)` | Uniform hard edge |
| `RoundedCornerShape(24.dp)` | Ordinary rounded corners, not G2-continuous curvature |
| Hand-written `RenderEffect.createBlurEffect` | Only blur — loses refraction / dispersion / highlight, and has no concept of "background texture" |
| Stacking two images | The background shows through the moment it scrolls |
| `lens(24f, 48f)` without `toPx()` | Parameters are only 1/3 strength on a 3x screen |
| Glass over the entire screen | Glass is a "local emphasis material"; all-glass = no contrast = not premium |

---

## 10. Chapter summary

```
Realism = refraction(①) says "there's a pane of glass here"
       + blur(②) says "the glass is frosted"
       + highlight(④) says "light bounced off here"
       + shadow(⑤) says "the glass has thickness"
       + tint(⑥) says "the glass has color" — but only a little
       + dispersion(③) says "the glass is moving"
```

**Next step**: read `02-Backdrop-Layered-Architecture.md` to understand where that "sampled texture" comes from and how it's composed.
That is the prerequisite for actually getting the parameters above to run.
