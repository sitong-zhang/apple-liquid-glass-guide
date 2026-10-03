# 03 · API Complete Reference

> This chapter breaks down, one by one, every API that Backdrop 2.0.0 exposes to users.
> Parameter names, preset names, and shader names were all verified against **the artifact itself of `io.github.kyant0:backdrop:2.0.0`**
> (the class constant pools of `backdrop-api.jar` / `backdrop-runtime.jar`), not written from memory.
>
> How to read: **First read the skeleton in §1, then look up details in §2–§7, and finally read the complete signature summary in §8.**

---

## 1. Skeleton: What Actually Happens Inside a `drawBackdrop` Call

```kotlin
Modifier.drawBackdrop(
    backdrop        = backgroundTexture, // required: which to sample
    shape           = { shape },          // outline: decides the refraction-band position + clipping
    effects         = { … },              // shading pipeline: color → blur → refraction (fixed order)
    highlight       = { … },              // edge highlight (lipstick effect / Fresnel)
    shadow          = { … },              // outer shadow
    innerShadow     = { … },              // inner dark edge (thickness)
    layerBlock      = { … },              // apply a graphicsLayer transform to the "glass layer"
    onDrawBehind    = { … },              // draw "below" the sampled result
    onDrawSurface   = { … },              // draw "above" the sampled result (the glass's own tint)
    onDrawFront     = { … },              // draw "on top of" everything (outline, arrows, etc.)
    onDrawBackdrop  = { … },              // fully take over drawing of the sampled result (advanced)
    exportedBackdrop= childTexture,       // export myself as a texture for child nodes to sample
)
```

Execution order (**this is the key to understanding everything**):

```
① Generate rounded-rect SDF from shape
② Expand the sampling range using the shape's padding (refraction "grabs" pixels outward)
③ Sample the backdrop texture
④ Apply effects to the sampled result in order: colorFilter/vibrancy/colorControls → blur → lens
⑤ Clip the result to the shape
⑥ onDrawBehind → [result of ④] → onDrawSurface
⑦ Stack in order: shadow / innerShadow / highlight
⑧ onDrawFront
⑨ Apply layerBlock's graphicsLayer to the whole layer
⑩ If exportedBackdrop is given, write "this node's final frame" into it
```

> Remember: **the order of ④ is determined by the writing order of the `effects` code block**. The library won't sort it for you.

---

## 2. `effects` — The Shading Pipeline (`BackdropEffectScope`)

`effects`' receiver is `BackdropEffectScope`. It is also:

- a **`Density`** (so you can directly write `12f.dp.toPx()` inside);
- a scope that can read **`size` / `shape` / `layoutDirection`**;
- an **appendable RenderEffect chain** (each call appends a segment to the chain tail via `chain(...)`).

### 2.1 `vibrancy()`

```kotlin
vibrancy()
```

Takes no arguments. It builds a "boost saturation + slight brightening" filter based on `colorMatrix` (called `VibrantColorFilter` in the library).
**Purpose: counteract the graying caused by blur.** Used together with `blur()`, almost without exception.

- Where to use: almost all "light glass" recipes.
- When not needed (e.g., large dark dialogs), switch to `colorControls` for explicit control.

### 2.2 `colorControls(brightness, contrast, saturation)`

```kotlin
colorControls(
    brightness = 0.2f,     // brightness +/- (light theme commonly 0.2; dark usually 0)
    contrast   = 1f,       // contrast (upstream default unchanged)
    saturation = 1.5f      // saturation (upstream dialogs fixed at 1.5)
)
```

More controllable than `vibrancy()`. **The upstream dialog uses exactly this**, because it needs different brightness per light/dark theme.

### 2.3 `colorFilter(colorFilter)` / `opacity(alpha)`

```kotlin
colorFilter(ColorFilter.tint(...))
opacity(0.8f)
```

`colorFilter` is the general Compose `ColorFilter` entry point (grayscale, tinting, matrices all work);
`opacity` is pure alpha multiplication. Both belong to the first stage of the pipeline (the color stage), same as `vibrancy/colorControls`.

> Note the distinction: `drawRect(color)` inside `onDrawSurface` is **a coating painted on the glass** (affects "what color the glass is"),
> whereas `opacity()` is **the transparency of the entire sampled result** (fades the background along with it). Don't mix them up.

### 2.4 `blur(radius, edgeTreatment)`

```kotlin
blur(16f.dp.toPx())
```

| Parameter | Meaning |
|---|---|
| `radius` | Gaussian/box blur radius. On API 31+ it goes through `RenderEffect.createBlurEffect` (`isRenderEffectSupported()`) |
| `edgeTreatment` | Edge handling strategy (prevents out-of-bounds sampling from producing black/transparent edges). Usually **no need to change**, the library has a default |

**The larger `blur` is, the more you need `vibrancy()` as a fallback**, see §2.1.

### 2.5 `lens(refractionHeight, refractionAmount, depthEffect, chromaticAberration)`

The heart of the whole effect. **Only it has "geometric refraction" capability**; everything else is just color/blur.

```kotlin
lens(
    refractionHeight    = 24f.dp.toPx(),   // toPx() required
    refractionAmount    = 48f.dp.toPx(),   // toPx() required
    depthEffect         = false,
    chromaticAberration = false
)
```

| Parameter | Type | Purpose | When to enable |
|---|---|---|---|
| `refractionHeight` | `Float` (px) | lens "bulge height" → refraction band width | always |
| `refractionAmount` | `Float` (px) | max pixel displacement → refraction strength | always |
| `depthEffect` | `Boolean` | spherical depth mapping, glass bulges more in the middle | only for large-area strongly-hinted materials: **dialogs, control center** |
| `chromaticAberration` | `Boolean` | enable chromatic aberration (multi-band sampling) | **only on press/drag**, zeroed together with progress |

**Implementation details of chromatic aberration (verified from shader source)**:

The library has two AGSL shaders — `RoundedRectRefractionShaderString` and
`RoundedRectRefractionWithDispersionShaderString`.
The latter samples the background **7 times**, with displacement amounts being the dispersion-offset
`+1, +2/3, +1/3, 0, -1/3, -2/3, -1` multiples, corresponding to
the **red / orange / yellow / green / cyan / blue / violet** seven spectral bands, then composited channel by channel.

So "chromatic aberration" is not a simple RGB three-channel thing, but **seven-band spectral offset** — this is why it looks like real glass when animated.
It also explains why it can't be always-on when static: with seven spectral bands resident, a visible rainbow edge appears.

**The uniforms shared by both shaders** (equivalent to telling you "what the refraction is determined by"):

```
uniform shader content;              // ← the recorded background texture, passed in as a shader
uniform float2 size;                 // glass size
uniform float2 offset;               // glass position in root coordinates
uniform float4 cornerRadii;          // radii of the four corners (G2 continuous curvature)
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
uniform float chromaticAberration;
```

> Note the first line `uniform shader content` — **your background texture is evaluated as a "sub-shader"** (`content.eval(coord)`).
> This is the underlying reason "liquid glass must have a real background texture": it's not texture overlay, it's **geometric resampling**.

### 2.6 `renderEffect { … }` / `runtimeShaderEffect(…)`

```kotlin
runtimeShaderEffect(
    shaderString = "half4 main(float2 coord) { … }",   // AGSL
    uniformShaderName = "shader",
    block = { setFloatUniform("amount", 0.5f) }
)
```

Escape hatch: custom AGSL. `chain` appends your custom effect to the existing chain tail.
This repo **does not use** it — wherever an upstream recipe works, we use the recipe.

### 2.7 Context Readable Inside `effects`

| Property | Description |
|---|---|
| `size` | glass's current size (px, `Size`) |
| `shape` | current `Shape` |
| `layoutDirection` | LTR / RTL |
| `padding` | sampling outward expansion. **`lens()` auto-expands it** (to avoid refraction sampling outside the texture), usually leave it alone |
| `renderEffect` | the current effect chain, readable and writable |

---

## 3. `shape` — Must Be G2 Continuous Curvature

```kotlin
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.Capsule

shape = { RoundedRectangle(24f.dp) }   // Apple squircle (superellipse / G2) — for clip() / NON-LENS glass ONLY
shape = { Capsule() }                  // fully rounded ends (buttons / bottom bar / search box) — clip() / NON-LENS ONLY
```

**Do not** use `androidx.compose.foundation.shape.RoundedCornerShape` for a *non-refracting* outline —
kyant shapes give the G2-continuous Apple corner. **BUT (verified on device, see `docs/10` §1):** if
`lens()` is in the `effects` block, the `shape` passed to `drawBackdrop` **must** be a Compose
`CornerBasedShape` (`RoundedCornerShape` / `CutCornerShape` / `RoundedRectangularShape`). A
`com.kyant.shapes.RoundedRectangle` is a custom `Shape`, not a `CornerBasedShape`; `lens()`
cannot build its refraction SDF from it and throws
`UnsupportedOperationException: Only RoundedRectangularShape or CornerBasedShape is supported in lens effects`
**at attach time → the app crashes on open.** (Note: `com.kyant.shapes.Capsule` was *observed to work*
with `lens()` in our build — the lock screen used it and opened fine — so `Capsule` appears to implement
`CornerBasedShape`. `RoundedRectangle` does not.) So for any glass that refracts, prefer
`RoundedCornerShape(...)` of the same radius; `Capsule` is acceptable if you only need a pill.

The difference between the two (for non-lens outlines) is in **curvature continuity**:

| | Plain rounded corner (RoundedCornerShape) | G2 continuous curvature (kyant shapes) |
|---|---|---|
| Straight-to-rounded connection | curvature jumps (C1 continuous) | curvature transitions continuously (C2 continuous) |
| Look & feel | obviously an "Android card" at a glance | the "full" rounded corner like Apple icons |
| Refraction band distribution | refraction abruptly stops at the corners | refraction transitions smoothly along the outline |

`lens()`'s shader receives **`cornerRadii: float4`**, indicating it computes the refraction band as a "rounded-rect SDF" —
the closer the shape is to Apple's superellipse, the more natural the refraction band's width distribution.

> `Capsule` also comes from `com.kyant.shapes`; it's equivalent to "a rounded rect with radius = half the short side", not `CircleShape`. Use it for `clip()` and non-lens glass; switch to `RoundedCornerShape` whenever `lens()` is present.

---

## 4. `highlight` — Edge Highlight

### 4.1 Data Structure

```kotlin
data class Highlight(
    val style: HighlightStyle = …,
    val width: Dp,
    val blurRadius: Dp,
    val alpha: Float
)
```

### 4.2 Three Presets

| Preset | Implementation | Feature | Where used |
|---|---|---|---|
| `Highlight.Default` | `DefaultHighlightShaderString` | a **directional** diagonal highlight (`uniform angle` / `uniform falloff`), with `layout(color) uniform half4 color` on the light-facing side | the vast majority of components (auto-used by the library when omitted) |
| `Highlight.Ambient` | `AmbientHighlightShaderString` | **colorless**, more uniform ambient light (shader directly returns `half4(t,t,t,1)*intensity`) | large flat surfaces; highlight when pressed |
| `Highlight.Plain` | minimal | almost invisible | large glass like dialogs that "don't want a directional bright edge" |

### 4.3 Common Usage

```kotlin
highlight = { Highlight.Default }                                  // always-on default highlight
highlight = { Highlight.Default.copy(alpha = progress) }           // lights up only when pressed
highlight = { Highlight.Plain }                                    // dialogs
highlight = {
    Highlight.Ambient.copy(                                          // finer, softer ambient light
        width = Highlight.Ambient.width / 1.5f,
        blurRadius = Highlight.Ambient.blurRadius / 1.5f,
        alpha = progress
    )
}
```

### 4.4 Gravity Highlight (The Key to the Apple Feel)

```kotlin
val uiSensor = rememberUISensor()

highlight = {
    Highlight(
        style = HighlightStyle.Default(
            angle = uiSensor.gravityAngle,   // highlight direction follows the phone's tilt
            falloff = 2f                     // falloff curve; larger value converges faster
        )
    )
}
```

`gravityAngle` comes from the accelerometer's `atan2(y, x)`, smoothed with a first-order low-pass filter at `alpha = 0.5f`, initial value `45f`.
Implementation in [UISensor.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/core/utils/UISensor.kt).
**When you tilt the phone, the highlight "slides" over** — this is the most easily overlooked yet most eye-deceiving detail.

### 4.5 Never Use `border()`

```kotlin
// ❌ uniform hard edge = plastic frame
Modifier.border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(24.dp))

// ✅ present on the light-facing side, absent on the back side, and fading inward
highlight = { Highlight.Default }
```

---

## 5. `shadow` / `innerShadow`

```kotlin
data class Shadow(val radius: Dp, val offset: DpOffset, val color: Color, val alpha: Float)
data class InnerShadow(val radius: Dp, val offset: DpOffset, val color: Color, val alpha: Float)
```

| Usage | Code | Effect |
|---|---|---|
| Always-on soft outer shadow | `shadow = { Shadow.Default }` | glass "floats up" |
| Float up when pressed | `shadow = { Shadow(alpha = progress) }` | press feedback |
| Explicit small shadow | `shadow = { Shadow(radius = 4f.dp, color = Color.Black.copy(alpha = 0.05f)) }` | toggle knob, slider knob |
| Inner dark edge (thickness) | `innerShadow = { InnerShadow(radius = 8f.dp * progress, alpha = progress) }` | **bright edge + inner dark edge = a solid with thickness** |
| Turn off | `shadow = null` | used by control center (to avoid double shadows) |

`innerShadow` is added upstream **only when pressed** (`progress` from 0→1), staying clean when at rest.

---

## 6. Remaining Parameters

### 6.1 `layerBlock: GraphicsLayerScope.() -> Unit`

Apply a `graphicsLayer` to "the glass's layer". **This is the standard place to apply velocity feel / deformation**:

```kotlin
layerBlock = {
    val p = pressProgress
    // slight horizontal stretch when pressed, like water being squeezed
    val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, p)
    scaleX = scale
    scaleY = scale
}
```

Note that the scope can read `size` (this layer's size) and `toPx()` (`GraphicsLayerScope` is a `Density`).

Buttons/bottom bars also do "velocity deformation" (velocity squash & stretch) here:

```kotlin
val velocity = dampedDragAnimation.velocity / 10f
scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
```

### 6.2 `onDrawBehind` / `onDrawSurface` / `onDrawFront`

All three are `DrawScope.() -> Unit`; the only difference is the **drawing layer**:

```
        ┌─────────────── onDrawFront        ← topmost (outlines, badges)
        ├─────────────── highlight / shadow
        ├─────────────── onDrawSurface      ← the glass's own tint ("what color is this glass")
        ├─────────────── onDrawBehind       ← the underlay beneath the glass
        └─────────────── refracted background
```

**`onDrawSurface` is the glass's tint, not a background color, and must be extremely restrained:**

| Scenario | Value (actually used in this repo) |
|---|---|
| Control center small tile | `Color.Black @ 5%` |
| Dock / bottom bar | `Color.White @ 22% ~ 34%` |
| Notification card (on lock screen, directly over wallpaper) | `Color.White @ 14%` |
| Notification card (in notification center) | `Color.White @ 22%` |
| Nav bar / search box (light) | `Color.White @ 35% ~ 50%` |
| Dialog | `dialogContainer` (light `#FAFAFA @ 60%` / dark `#121212 @ 40%`) |
| Segmented control / bottom-bar indicator | `Black @ 10%` (light) / `White @ 10%` (dark), and fades out with press |

> **Once it exceeds ~50%, the refraction is completely covered and the glass degenerates into a solid rounded rectangle.**

`onDrawSurface` can also do **overlay drawing** (e.g., a button's tint + overlay):

```kotlin
onDrawSurface = {
    drawRect(tint, blendMode = BlendMode.Hue)   // first tint by hue
    drawRect(tint.copy(alpha = 0.75f))          // then stack another layer
}
```

### 6.3 `onDrawBackdrop`

Fully takes over the drawing of the "refraction result". The default implementation is `DefaultOnDrawBackdrop` (runs the chain above and draws it out).
Unless you want fully custom sampling compositing, **don't pass it**.

### 6.4 `exportedBackdrop: LayerBackdrop?`

Export this node's final frame as a sampleable texture, for child nodes' "glass-on-glass". See `02-Backdrop-Layered-Architecture.md §5`.

---

## 7. Standalone APIs That Pair With `drawBackdrop`

### 7.1 `rememberLayerBackdrop()`

```kotlin
@Composable fun rememberLayerBackdrop(): LayerBackdrop
```

Creates a new empty recording texture. Use together with `Modifier.layerBackdrop(it)`.

### 7.2 `Modifier.layerBackdrop(backdrop: LayerBackdrop)`

Records the host node's (including subtree) per-frame frame into `backdrop`. **One backdrop attaches exactly one recording node.**

### 7.3 `rememberCombinedBackdrop(vararg backdrop: Backdrop)`

```kotlin
rememberCombinedBackdrop(wallpaperBackdrop, surfaceBackdrop)   // 2 blocks → Combined2Backdrops
rememberCombinedBackdrop(a, b, c)                              // 3 blocks → Combined3Backdrops
```

Stacks bottom-to-top by painter's algorithm. See Chapter `02` §3.

### 7.4 `rememberBackdrop(backdrop) { drawBackdrop -> … }`

```kotlin
rememberBackdrop(source) { drawBackdrop ->
    scale(sx, sy) { drawBackdrop() }   // transform source then use it as a new texture
}
```

See Chapter `02` §4.

### 7.5 `CanvasBackdrop` / `EmptyBackdrop`

- `EmptyBackdrop`: a placeholder that draws nothing (`Backdrop.Empty`). For debugging — swap `backdrop` to it,
  and the glass should be "empty but correctly shaped", quickly distinguishing "backdrop wasn't recorded" from "effects written wrong".
- `CanvasBackdrop`: directly provides a `DrawScope`-custom-drawn backdrop. Use when you want to draw the background with code (instead of recording a node).

### 7.6 RuntimeShader Utilities

```kotlin
isRuntimeShaderSupported()          // API 31+ only returns true
RuntimeShader(agslString)           // compile an AGSL snippet
shader.asComposeShader()            // convert to a Compose Shader
shader.setFloatUniform(name, x, y)
shader.setColorUniform(name, color)
```

`InteractiveHighlight` uses this to write its own "circular radial highlight" AGSL, implementing "local glow where the finger presses"
([InteractiveHighlight.kt L63-L115](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/core/utils/InteractiveHighlight.kt#L63-L115)).

> Low-version compatibility: all AGSL calls inside the library are preceded by an `isRuntimeShaderSupported()` check, and **auto-degrade on Android 12**
> (refraction/chromatic aberration disappear, blur remains). So `minSdk 31` is safe; it's just that Android 12 won't see refraction.

---

## 8. Signature Cheat Sheet (All Verified from the Artifact)

```kotlin
// ── main entry ───────────────────────────────────────────────────────────
fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shape: (Density.() -> Shape)? = null,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    highlight: (Density.() -> Highlight)? = null,
    shadow: (Density.() -> Shadow)? = null,
    innerShadow: (Density.() -> InnerShadow)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: (DrawScope.() -> Unit)? = DefaultOnDrawBackdrop,
    exportedBackdrop: LayerBackdrop? = null,
): Modifier

// ── effects ─────────────────────────────────────────────────────────────
fun BackdropEffectScope.vibrancy()
fun BackdropEffectScope.colorControls(brightness: Float = 0f, contrast: Float = 1f, saturation: Float = 1f)
fun BackdropEffectScope.colorFilter(colorFilter: ColorFilter)
fun BackdropEffectScope.opacity(alpha: Float)
fun BackdropEffectScope.blur(radius: Float, edgeTreatment: … = default)
fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false,
)
fun BackdropEffectScope.runtimeShaderEffect(shaderString: String, uniformShaderName: String, block: …)

// ── backdrop sources ─────────────────────────────────────────────────────
@Composable fun rememberLayerBackdrop(): LayerBackdrop
fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier
@Composable fun rememberCombinedBackdrop(vararg backdrop: Backdrop): Backdrop
@Composable fun rememberBackdrop(backdrop: Backdrop, onDraw: DrawScope.(drawBackdrop: () -> Unit) -> Unit): Backdrop
val Backdrop.Companion.Empty: Backdrop

// ── value objects ────────────────────────────────────────────────────────
data class Highlight(style: HighlightStyle, width: Dp, blurRadius: Dp, alpha: Float) {
    companion object { val Default; val Ambient; val Plain }
}
data class HighlightStyle(…) {
    companion object { fun Default(angle: Float, falloff: Float): HighlightStyle }
}
data class Shadow(radius: Dp, offset: DpOffset, color: Color, alpha: Float) { companion object { val Default } }
data class InnerShadow(radius: Dp, offset: DpOffset, color: Color, alpha: Float) { companion object { val Default } }
```

> ⚠️ **Always call `drawBackdrop` with named arguments.** Parameter order may change between versions; named calls are always safe.

---

## 9. The 7 Most Common API Misuses in This Chapter

| ❌ | Consequence | ✅ |
|---|---|---|
| `lens(24f, 48f)` missing `toPx()` | on a 3x screen refraction is only 1/3 | `lens(24f.dp.toPx(), 48f.dp.toPx())` |
| `lens` written before `blur` | post-refraction out-of-bounds pixels smeared by blur → dirty edges | `lens` always the last line |
| missing `vibrancy()` in `effects` | glass turns gray | pair `vibrancy` with `blur` |
| `chromaticAberration = true` always on | static red/blue color fringe | bind to `progress`, zero at rest |
| `border()` instead of `highlight` | plastic frame | `highlight = { Highlight.Default }` |
| `RoundedCornerShape` used for a *non-refracting* outline | plain Android corner, not Apple squircle | `com.kyant.shapes.RoundedRectangle` (clip / non-lens glass only) |
| `com.kyant.shapes.RoundedRectangle` passed to `drawBackdrop` **with `lens()`** | app crashes on open (`UnsupportedOperationException`) | `androidx.compose.foundation.shape.RoundedCornerShape` — **required** whenever `lens()` is present (docs/10 §1) |
| `onDrawSurface` using 60% white | refraction fully covered | keep to 5%~35% |

---

**Next step**: `04-Material-Recipe-Table.md` — 12 already-tuned recipes, just copy them.
