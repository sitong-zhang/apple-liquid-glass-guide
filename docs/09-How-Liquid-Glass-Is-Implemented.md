# How Liquid Glass Is Actually Implemented — Explained from the Upstream Source

> **This document explains, line by line, how the upstream project [**Kyant0/AndroidLiquidGlass**](https://github.com/Kyant0/AndroidLiquidGlass)
> implements the Apple iOS 26 "liquid glass" effect. It is written around the archived upstream source at
> [`upstream/AndroidLiquidGlass/`](upstream/AndroidLiquidGlass/).** If a line of code below looks surprising, go read that file —
> this document is the map, the source is the territory.
>
> It covers everything you need — including the parts people usually never tell you: the **offline recording layer, the
> coordinate-space trick, the AGSL SDF refraction shader, the 7-band dispersion, the highlight shader, and the hard platform
> requirements (API 31 / API 33)**.
>
> Last updated: 2026-10-02 · All file paths below are relative to `upstream/AndroidLiquidGlass/`.

---

## 0. The one-sentence answer

> **Liquid glass is not a material property — it is a real-time, GPU-shader-driven *geometric refraction* of the
> already-rendered image behind the glass, computed per pixel inside an AGSL shader.**

Everything else in this repository is plumbing that makes that sentence true. Let's walk it from the bottom up.

---

## 1. The three-layer mental model (recorder → sampler → warper)

The whole engine is three pieces, and once you name them the code stops being scary:

| Role | Who it is | What it does | Key code |
|---|---|---|---|
| **Recorder** | `LayerBackdrop` | Records the "behind-the-glass" scene into an **offscreen GPU texture** every frame | `backdrops/LayerBackdrop.kt` + `Modifier.layerBackdrop(...)` |
| **Sampler** | `DrawBackdropNode` | Draws that texture into the glass area, **aligned by coordinates** | `DrawBackdropModifier.kt` |
| **Warper** | `lens()` shader | Samples the texture with an **SDF-based refraction offset** so the image appears to bend | `effects/Lens.kt` + `internal/Shaders.kt` |

> **90% of failures** come from skipping the recorder. If the glass draws *nothing* or *black*, it's almost always because
> the node behind it was never attached with `Modifier.layerBackdrop(...)` — so there is no texture to sample.

---

## 2. The recorder: `LayerBackdrop` and the offscreen texture

File: `backdrop/src/commonMain/kotlin/com/kyant/backdrop/backdrops/LayerBackdrop.kt`

```kotlin
@Composable
fun rememberLayerBackdrop(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),   // an offscreen GPU canvas
    onDraw: ContentDrawScope.() -> Unit = DefaultOnDraw      // what to record
): LayerBackdrop {
    return remember(graphicsLayer, onDraw) { LayerBackdrop(graphicsLayer, onDraw) }
}
```

`LayerBackdrop` wraps a Compose **`GraphicsLayer`** — this is the offscreen texture. Two things are glued together:

1. **Recording** — some *background node* is given `Modifier.layerBackdrop(it)`. Every frame, whatever that node draws
   gets written into this texture.
2. **Sampling** — when a glass node calls `drawBackdrop(backdrop = thisLayerBackdrop)`, it draws the texture content.

The crucial property is `isCoordinatesDependent = true` and the **coordinate-space alignment**:

```kotlin
override fun DrawScope.drawBackdrop(density, coordinates, layerBlock) {
    val coordinates = coordinates ?: return
    val layerCoordinates = layerCoordinates ?: return
    withTransform({
        // ... handle layer block ...
        val offset = layerCoordinates.localPositionOf(coordinates)  // where am I relative to the recorded layer?
        translate(-offset.x, -offset.y)                            // align the texture to THIS node
    }) {
        drawLayer(graphicsLayer)                                    // draw the offscreen texture here
    }
}
```

**Why this matters (the part people forget):** the recording layer is in *root/screen coordinates*, but the glass node can
be anywhere. So before drawing the texture, the engine computes **where this glass node sits relative to the recorded
layer** (`localPositionOf`) and shifts the texture by that offset. Without this, a glass card on the right would show the
left part of the background — a misalignment that instantly looks fake.

> **Why not just blur the node itself?** Because the recorded texture is *everything behind the glass*, and the shader
> can bend it. A node that only blurs its own children can never show "the wallpaper behind a card" — it doesn't have
> access to it. This is why `layerBackdrop` is the heart of the whole approach.

---

## 3. The sampler: `DrawBackdropNode` (the render pipeline)

File: `DrawBackdropModifier.kt`

The modifier `Modifier.drawBackdrop(...)` installs a `DrawBackdropNode`. Look at its `draw()` — this is the **fixed order**
of the whole glass pass:

```kotlin
override fun ContentDrawScope.draw() {
    if (effectScope.update(this)) updateEffects()

    onDrawBehind?.invoke(this)   // ① anything drawn BEHIND the glass (rare)
    drawBackdropLayer()           // ② THE glass layer: sample the offscreen texture + apply the shader chain
    onDrawSurface?.invoke(this)   // ③ the "tint" — a flat translucent color ON TOP of the refraction
    drawContent()                 // ④ the node's own content (button label, icon, text) — stays SHARP
    onDrawFront?.invoke(this)     // ⑤ anything drawn IN FRONT
    ...
}
```

The order is deliberate and is the key to the "glass feel":

- **The background texture is warped first (②).** So the image behind the button visibly bends.
- **Then a tint is added (③).** This is `onDrawSurface` — Control Center uses ~5% black, Dock 22–34% white.
  It must be **restrained**: above ~50% the glass becomes a solid rectangle and covers all refraction.
- **Then the content draws sharp (④).** The button label is NOT blurred — only the *background* is. This contrast
  (blurred background + sharp content) is exactly what sells "glass".

### The effect chain (`updateEffects`)

```kotlin
private fun updateEffects() {
    if (!isRenderEffectSupported()) return
    effectScope.apply(effects)
    graphicsLayer?.renderEffect = effectScope.renderEffect   // attach the chain to the sampled texture
    padding = effectScope.padding
}
```

The `effects {}` block builds an Android **`RenderEffect` chain**. Effects chain in a fixed order
(`colorFilter → blur → lens`), so inside the block you must write `vibrancy()` first, `blur()` second, `lens()` last:

```kotlin
val effect = RuntimeShaderEffect(shader, "content")   // lens shader
renderEffect = renderEffect.chain(effect)             // chain: previous effects + this one
```

On Android the chain is realized as native `android.graphics.RenderEffect`:

```kotlin
android.graphics.RenderEffect.createChainEffect(other, this)  // internal/RenderEffect.kt (androidMain)
android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
```

---

## 4. The warper: the AGSL refraction shader (the REAL secret)

File: `backdrop/src/commonMain/kotlin/com/kyant/backdrop/internal/Shaders.kt`

This is the actual liquid-glass math, written in **AGSL** (Android Graphics Shading Language), fed to
`android.graphics.RuntimeShader` at runtime. Two versions: plain refraction, and refraction **with dispersion**
(chromatic aberration).

### 4.1 The distance-field helpers (SDF)

```glsl
float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;                     // signed distance to a rounded rect: negative inside, positive outside
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    // gradient (outward normal) of the rounded-rect distance field
}
```

These compute, for each pixel, **how far it is from the glass edge** (`sd`) and **which direction points outward** (`grad`).
They are the geometry that later drives the refraction.

### 4.2 The refraction shader (the core)

```glsl
uniform shader content;            // the offscreen background texture (bound by the RenderEffect)
uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;

float circleMap(float x) { return 1.0 - sqrt(1.0 - x * x); }   // circle-profile falloff → "glass lens" look

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;
    float radius = radiusAt(coord, cornerRadii);

    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {            // far outside the glass → just pass the original pixel through
        return content.eval(coord);
    }
    sd = min(sd, 0.0);                        // clamp: only the inside / rim matters

    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;  // how much to bend, stronger near the rim
    float2 grad = normalize(gradSdRoundedRect(...) + depthEffect * normalize(centeredCoord));

    float2 refractedCoord = coord + d * grad; // bend the sample coordinate
    return content.eval(refractedCoord);      // re-sample the background at the bent position
}
```

Read it as: **for each pixel, look at the background texture, but at a slightly shifted position — the shift is biggest
near the glass rim and tapers toward the center.** That's the whole optical trick. The center looks nearly flat (real glass
is thin in the middle), the edges bend — exactly how a lens behaves.

The `offset` uniform (`-padding`) compensates for the blur's edge padding, so the refraction stays correctly positioned.

### 4.3 The dispersion shader (chromatic aberration)

```glsl
uniform float chromaticAberration;
...
float dispersionIntensity = chromaticAberration * ((x * y) / (w * h));
float2 dispersedCoord = d * grad * dispersionIntensity;

half4 color = half4(0.0);
// 7 colour bands, each sampled at a slightly different offset and summed with weights:
half4 red   = content.eval(refractedCoord + dispersedCoord);                       color.r += red.r / 3.5;
half4 orange= content.eval(refractedCoord + dispersedCoord * (2.0/3.0));           ...
half4 yellow= content.eval(refractedCoord + dispersedCoord * (1.0/3.0));           ...
half4 green = content.eval(refractedCoord);                                        ...
half4 cyan  = content.eval(refractedCoord - dispersedCoord * (1.0/3.0));           ...
half4 blue  = content.eval(refractedCoord - dispersedCoord * (2.0/3.0));           ...
half4 purple= content.eval(refractedCoord - dispersedCoord);                       ...
return color;
```

The shader splits the refracted pixel into **7 spectral bands** (red→purple), samples each at a slightly different offset,
and re-composites them. This produces the subtle **red/blue fringing at the edges** — the chromatic aberration that makes
real glass look *real*.

> **The hidden rule:** aberration is only switched on during *motion* (press/drag). Static glass with aberration always on
> shows constant red/blue fringes and looks obviously fake. The demo wires `progress → 1` on interaction, and the shader's
> `chromaticAberration` uniform rises with it. (See `InteractiveHighlight` / `LiquidButton`.)

### 4.4 Where the shader runs

File: `backdrop/src/androidMain/kotlin/com/kyant/backdrop/RuntimeShader.kt`

```kotlin
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
actual fun RuntimeShader(shaderString: String): RuntimeShader {
    val shader = android.graphics.RuntimeShader(shaderString)   // compile the AGSL string at runtime
    return AndroidRuntimeShader(shader)
}
```

AGSL is compiled **at runtime** by the GPU driver, so the whole library is pure source text — no pre-built binaries.

---

## 5. Highlight: the edge light (what makes it "polished")

File: `highlight/Highlight.kt`, `highlight/HighlightStyle.kt`, `highlight/HighlightModifier.kt`

The bright rim line is **not** `border()`. It's a separate pass that draws a stroked outline and then applies an AGSL shader
that fades the highlight along the edge by angle:

```glsl
uniform float angle;
uniform float falloff;
...
float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius); // edge normal
float2 normal = float2(cos(angle), sin(angle));                       // light direction (45°)
float d = dot(grad, normal);
float intensity = pow(abs(d), falloff);                               // bright where edge faces the light
return color * intensity;
```

So the highlight is **bright on the edges that face the 45° light and fades around the rim** — exactly Apple's polished look.
Three styles exist:
- `Default` — shader-driven directional edge light (`color = White@50%`, `BlendMode.Plus`, angle 45°, falloff 1).
- `Ambient` — soft ambient sheen around the whole rim.
- `Plain` — uniform plain stroke (no shader), for when you don't need the gradient.

`HighlightNode` records the highlight into its own `GraphicsLayer`, applies the shader + blur to the paint stroke, then
draws it offset by 1px.

---

## 6. Shadow & InnerShadow: the depth

File: `shadow/Shadow.kt`, `shadow/InnerShadow.kt`

```kotlin
data class Shadow(
    val radius: Dp = 24f.dp,
    val offset: DpOffset = DpOffset(0f.dp, radius / 6f),   // mostly below
    val color: Color = Color.Black.copy(alpha = 0.1f),
    val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode
)
```

- `Shadow` = outer drop shadow (soft, black@10%, offset downward) → lifts the glass off the background.
- `InnerShadow` = shadow cast *inside* the shape → the "concave" depth feel of the glass well.

Both are drawn as separate `ModifierNode`s chained before the `DrawBackdropNode`, so the glass has a believable
**top-lit, bottom-shadowed** solidity.

---

## 7. Combined backdrops: the Control-Center trick

File: `backdrops/CombinedBackdrop.kt`

A content layer can only refract its **immediate** lower layer. To make an upper overlay (Control Center) refract "the layer
below the below" (home-screen icons *and* wallpaper), the engine merges multiple recorded textures:

```kotlin
@Composable
fun rememberCombinedBackdrop(backdrop1: Backdrop, backdrop2: Backdrop): Backdrop { ... }
// drawBackdrop() = draw backdrop1, then backdrop2, onto the same target → painter's-algorithm stacking
```

Supported up to 3 (or vararg). This is what lets "opening the Control Center blurs the home screen" be real.

---

## 8. The effects DSL (`BackdropEffectScope`)

File: `BackdropEffectScope.kt` + `effects/`

The `effects = { ... }` block is a small DSL that accumulates a `RenderEffect` chain and a `padding` value:

| Function | Effect | File |
|---|---|---|
| `vibrancy()` / `colorControls()` | color filter — restores saturation the blur kills (else glass looks grey) | `effects/ColorFilter.kt` |
| `blur(radius)` | background Gaussian blur | `effects/Blur.kt` |
| `lens(h, amount)` | **the refraction** — the whole secret | `effects/Lens.kt` |
| `effect(...)` / `runtimeShaderEffect(...)` | chain an arbitrary RenderEffect / AGSL shader | `effects/RenderEffect.kt` |

Important details from `effects/Blur.kt` and `effects/Lens.kt`:
- `blur` pushes `padding` so the sampled texture extends past the glass edges (avoids a hard clamp border).
- `lens` reads the corner radii from the shape and **reduces `padding` by `refractionHeight`** — the two cooperate.
- Both are **no-ops on unsupported hardware** (guarded by `isRenderEffectSupported()` / `isRuntimeShaderSupported()`).

---

## 9. The hard platform requirements (what everyone forgets)

File: `backdrop/src/androidMain/kotlin/com/kyant/backdrop/Platform.kt`

```kotlin
actual fun isRenderEffectSupported(): Boolean  = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S        // API 31+
actual fun isRuntimeShaderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU // API 33+
```

| Capability | Android API | Needed for |
|---|---|---|
| `RenderEffect` (blur / colorFilter / chain) | **API 31+** (Android 12) | every blur & tint |
| `RuntimeShader` (AGSL) | **API 33+** (Android 13) | `lens()` refraction + highlight shaders |

**So `minSdk` must be ≥ 31 for any effect, and ≥ 33 for true refraction.** Below that the shader functions silently no-op —
you get a plain translucent card, not liquid glass. `RenderEffect.createRuntimeShaderEffect` (API 33) and
`createChainEffect` / `createColorFilterEffect` (API 31) are called in `internal/RenderEffect.kt (androidMain)`.

Also note the Compose dependency: the whole thing runs on **Compose Multiplatform** — on Android it's Jetpack Compose;
on desktop/iOS it uses the **Skiko** backend (`backdrop/src/skikoMain/...`), with a separate
`RuntimeShader`/`RenderEffect` implementation per platform.

---

## 10. The demo catalog (`app/`) — how real glass is assembled

File: `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/`

The `app` module is the **Backdrop Catalog** — a live showcase. The five reusable components teach you the exact call pattern:

| Component | File | Signature worth noting |
|---|---|---|
| `LiquidButton` | `components/LiquidButton.kt` | `drawBackdrop(backdrop, shape = Capsule(), effects = { vibrancy(); blur(2dp); lens(12dp, 24dp) }, layerBlock = { ...scale on press... })` |
| `LiquidToggle` | `components/LiquidToggle.kt` | capsule glass + interactive highlight |
| `LiquidSlider` | `components/LiquidSlider.kt` | glass track + thumb |
| `LiquidBottomTabs` / `LiquidBottomTab` | `components/LiquidBottomTabs.kt` | glass tab bar |
| Backdrop Scaffold | `BackdropDemoScaffold.kt` | records the wallpaper once, hands the backdrop to all children |

The destinations (`destinations/`) show each effect in isolation:
`HomeContent`, `ButtonsContent`, `ControlCenterContent`, `LockScreenContent`, `DialogContent`, `ProgressiveBlurContent`,
`MagnifierContent`, `ScrollContainerContent`, `LazyScrollContainerContent`, `ToggleContent`, `SliderContent`,
`BottomTabsContent`, `AdaptiveLuminanceGlassContent`, `GlassPlaygroundContent`.

Study **`LiquidButton.kt`** first — it is the cleanest complete example (backdrop + effects + interactive highlight +
press-driven dispersion).

---

## 11. How the archived repo connects to your showcase

Your `LiquidGlassShowcase/` is an **Android-native re-implementation** of the same engine ideas (Jetpack Compose, not
Compose Multiplatform), using the published artifacts `io.github.kyant0:backdrop` and `io.github.kyant0:shapes` from the
same author. The mapping is:

| Your `docs/` | Archived upstream source it describes |
|---|---|
| `01-Optical-Model` | `effects/Lens.kt`, `effects/Blur.kt`, `internal/Shaders.kt` |
| `02-Backdrop-Layered-Architecture` | `backdrops/LayerBackdrop.kt`, `CombinedBackdrop.kt`, `DrawBackdropModifier.kt`, `internal/LayerRecorder.kt` |
| `03-API-Complete-Reference` | `BackdropEffectScope.kt`, `highlight/*`, `shadow/*`, `effects/*` |
| `04-Material-Recipe-Table` | `effects/*` + your `core/glass/GlassMaterials.kt` |
| `05-iOS-Shell-in-Practice` | `app/.../destinations/*` (ControlCenter / LockScreen / Dialog …) |
| `07-Build-and-Troubleshooting` | `backdrop/build.gradle.kts`, `androidApp/build.gradle.kts`, `gradle/libs.versions.toml` |

---

## 12. Summary: the pipeline in one diagram

```
 background scene
      │  Modifier.layerBackdrop(backdrop)  ← RECORD every frame into GraphicsLayer (offscreen GPU texture)
      ▼
 LayerBackdrop (texture)  ── sampled by ──►  DrawBackdropNode.draw()
                                                │  order (fixed):
                                                │    drawBackdropLayer()   → sample texture, aligned by coordinates
                                                │    onDrawSurface()       → translucent tint (5%–35%!)
                                                │    drawContent()         → SHARP content
                                                ▼
                                            RenderEffect chain:  vibrancy() → blur() → lens()
                                                │  lens() = AGSL RuntimeShader:
                                                │    per-pixel SDF distance to rim → circleMap bend →
                                                │    re-sample texture at bent coord → (optional) 7-band dispersion
                                                ▼
                                            Highlight (rim light shader) + Shadow / InnerShadow
                                                ▼
                                        liquid glass that refracts, bends, fringes, and catches light
```

---

## 13. The three knobs that control "how glassy" (recap from code)

From `effects/Lens.kt` and the recipes:

| You want… | Change | Direction |
|---|---|---|
| Thicker / more "water-drop" glass | `lens(refractionHeight, refractionAmount)` | raise **both** together (`12,24` → `24,48`) |
| More fog | `blur(radius)` | `2 → 8 → 16` |
| More solid / obscuring | `onDrawSurface = { drawRect(color) }` | white `0.05 → 0.34` (stay under 50%!) |
| Stronger edge light | `highlight = { Highlight.Default.copy(alpha = 1f) }` | `0.5 → 1.0` |
| Motion fringing | chromatic aberration via `progress` | only on press/drag |

Everything else is detail. **Change parameters proportionally**, never one knob wildly out of line.
