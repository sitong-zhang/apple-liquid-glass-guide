# How Liquid Glass Is Actually Implemented — Complete Source Walkthrough

> **This document explains, line by line, how the upstream project [**Kyant0/AndroidLiquidGlass**](https://github.com/Kyant0/AndroidLiquidGlass)
> implements the Apple iOS 26 "liquid glass" effect — and how your own `LiquidGlassShowcase/` re-assembles it on Android.**
> It is written around the archived upstream source at [`upstream/AndroidLiquidGlass/`](../upstream/AndroidLiquidGlass/).
> Every class, every shader, every platform call is covered. If a line below looks surprising, go read that file — this is the map, the source is the territory.
>
> Last updated: 2026-10-02 (full rewrite). All file paths below are relative to `upstream/AndroidLiquidGlass/` unless prefixed with `showcase/`.

---

## 0. The one-sentence answer, and why it is not "blur + alpha"

> **Liquid glass is a real-time, GPU-shader-driven *geometric refraction* of the already-rendered image behind the glass,
> computed per pixel inside an AGSL shader, then layered with a restrained tint, an angle-faded rim highlight, and a soft shadow.**

A `Box` with `alpha = 0.3f` + `Modifier.blur()` is **not** glass — it blurs its *own children*, cannot see the wallpaper
behind it, and has no refraction. The upstream engine solves this with three cooperating layers:

| Layer | Role | Key classes |
|---|---|---|
| **Recorder** | Records "everything behind the glass" into an offscreen GPU texture every frame | `LayerBackdrop`, `Modifier.layerBackdrop`, `LayerRecorder` |
| **Sampler** | Draws that texture into the glass area, aligned by screen coordinates | `DrawBackdropNode`, `Backdrop.drawBackdrop()` |
| **Warper** | Bends the sampled texture per pixel with an SDF refraction shader | `lens()`, `internal/Shaders.kt` (AGSL) |

Then three finishing passes sit on top: **tint** (`onDrawSurface`), **rim highlight** (`Highlight`), **shadow** (`Shadow` / `InnerShadow`).

---

## 1. The Recorder: how the background becomes a texture

### 1.1 `LayerBackdrop` — the offscreen canvas

File: `backdrop/src/commonMain/kotlin/com/kyant/backdrop/backdrops/LayerBackdrop.kt`

```kotlin
@Composable
fun rememberLayerBackdrop(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),   // ← the offscreen GPU texture
    onDraw: ContentDrawScope.() -> Unit = DefaultOnDraw      // ← what gets recorded
): LayerBackdrop
```

`LayerBackdrop` is a thin wrapper around a Compose **`GraphicsLayer`** — an offscreen render target. It holds:
- `graphicsLayer`: the texture itself.
- `onDraw`: the drawing lambda to record (defaults to `drawContent()`).
- `layerCoordinates`: the screen position of the node that is *recording* (set by `LayerBackdropNode.onGloballyPositioned`).

### 1.2 `Modifier.layerBackdrop` — attaching the recorder to a background node

File: `backdrop/src/commonMain/kotlin/com/kyant/backdrop/backdrops/LayerBackdropModifier.kt`

```kotlin
private class LayerBackdropNode(var backdrop: LayerBackdrop) : DrawModifierNode, GlobalPositionAwareModifierNode {
    override fun ContentDrawScope.draw() {
        drawContent()                                              // ① draw the background normally
        recordLayer(backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }  // ② also record it into the texture
    }
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) backdrop.layerCoordinates = coordinates  // ③ remember where this layer sits on screen
    }
}
```

This is the heart of "no backdrop, no glass": the background node draws **twice** — once to the real screen, once into the
offscreen `GraphicsLayer`. Without this modifier attached to something behind the glass, the texture is empty and the glass
draws black.

### 1.3 `recordLayer` — the actual offscreen recording

File: `backdrop/src/commonMain/kotlin/com/kyant/backdrop/internal/LayerRecorder.kt`

```kotlin
context(node: DelegatableNode)
internal fun DrawScope.recordLayer(layer: GraphicsLayer, size: IntSize = ..., block: DrawScope.() -> Unit) {
    val density = node.requireDensity()
    layer.record(size) {
        val prevDensity = drawContext.density
        drawContext.density = density      // record with the node's real density
        try { this.block() } finally { drawContext.density = prevDensity }
    }
}
```

`GraphicsLayer.record(size) { ... }` redirects all subsequent drawing into the layer's texture. The density is temporarily
swapped so the recorded content matches the real screen's density (otherwise the texture is blurry or mis-scaled).

### 1.4 The coordinate-space trick (the part everyone gets wrong)

Back in `LayerBackdrop.drawBackdrop()`:

```kotlin
override fun DrawScope.drawBackdrop(density, coordinates, layerBlock) {
    val coordinates = coordinates ?: return          // the glass node's position
    val layerCoordinates = layerCoordinates ?: return // the recording node's position
    withTransform({
        if (layerBlock != null) {
            with(obtainInverseLayerScope()) { inverseTransform(density, layerBlock) }  // undo the glass's own scale/rotation
        }
        val offset = layerCoordinates.localPositionOf(coordinates)  // where is the glass relative to the recorded layer?
        translate(-offset.x, -offset.y)                               // shift the texture so it aligns
    }) {
        drawLayer(graphicsLayer)   // draw the offscreen texture, now correctly positioned
    }
}
```

**Why this matters:** the recorded texture is in the *recording layer's local coordinates*, but the glass can be anywhere on
screen. Before drawing the texture, the engine computes `localPositionOf(coordinates)` — the vector from the recording layer
to the glass — and translates the texture by the negative of that vector. Without this, a glass card on the right side of the
screen would show the *left* part of the wallpaper.

The `InverseLayerScope` (file `internal/InverseLayerScope.kt`) undoes the glass node's own `scaleX/scaleY/rotationZ`
(`layerBlock`) so the background texture is not scaled along with the glass — the background stays put, only the *refraction
amount* changes with scale. It computes the inverse 2×2 affine matrix by hand (rotation + scale), with a fast path for
zero rotation.

---

## 2. The Sampler: `DrawBackdropNode` and the fixed render order

File: `backdrop/src/commonMain/kotlin/com/kyant/backdrop/DrawBackdropModifier.kt`

`Modifier.drawBackdrop(...)` installs a `DrawBackdropNode`. Its `draw()` is the **fixed, non-negotiable order** of the whole
glass pass:

```kotlin
override fun ContentDrawScope.draw() {
    if (effectScope.update(this)) updateEffects()   // rebuild the RenderEffect chain if size/density changed

    onDrawBehind?.invoke(this)    // ① anything behind the glass (rarely used)
    drawBackdropLayer()            // ② ★ the glass: sample offscreen texture + apply shader chain
    onDrawSurface?.invoke(this)    // ③ the tint — a flat translucent color ON TOP of refraction
    drawContent()                  // ④ the node's own content (label, icon, text) — stays SHARP
    onDrawFront?.invoke(this)      // ⑤ anything in front

    exportedBackdrop?.graphicsLayer?.let { layer ->   // ⑥ optional: re-record this glass into another backdrop
        recordLayer(layer) { onDrawBehind?.invoke(this); drawBackdropLayer(); onDrawSurface?.invoke(this); onDrawFront?.invoke(this) }
    }
}
```

The order is deliberate and is the key to the "glass feel":

- **② refracts the background first.** The image behind the button visibly bends.
- **③ adds a restrained tint.** `onDrawSurface` is a flat `drawRect(color)` — Control Center ~5% black, Dock 22–34% white,
  notification card 14% white. **It must stay under ~50%**: above that the glass becomes a solid rectangle and covers all
  refraction.
- **④ draws content sharp.** The button label is NOT blurred — only the *background* is. This contrast (blurred background +
  sharp content) is exactly what sells "glass".

### 2.1 `drawBackdropLayer` — recording the glass into its own layer

```kotlin
private val drawBackdropLayer: DrawScope.() -> Unit = {
    val layer = graphicsLayer
    if (layer != null) {
        recordLayer(layer, size = IntSize(width + padding*2, height + padding*2)) {
            if (padding != 0f) canvas.translate(padding, padding)
            onDrawBackdrop {
                with(backdrop) { drawBackdrop(density = effectScope, coordinates = layoutCoordinates, layerBlock = layerBlock) }
            }
            if (padding != 0f) canvas.translate(-padding, -padding)
        }
        layer.topLeft = if (padding != 0f) IntOffset(-padding, -padding) else IntOffset.Zero
        drawLayer(layer)   // draw the layer (which now carries the RenderEffect chain)
    }
}
```

The glass itself is recorded into **its own** `GraphicsLayer`, sized larger by `padding * 2` on each side. Why? Because
`blur()` and `lens()` need pixels outside the glass's bounds to avoid a hard clamped edge. The `padding` is accumulated by the
effects DSL (`blur` pushes it up, `lens` reduces it by `refractionHeight`).

### 2.2 The effect chain (`updateEffects`)

```kotlin
private fun updateEffects() {
    if (!isRenderEffectSupported()) return
    effectScope.apply(effects)                          // run the effects {} block → builds renderEffect + padding
    graphicsLayer?.renderEffect = effectScope.renderEffect  // attach the chain to the glass layer
    padding = effectScope.padding
}
```

The `effects {}` block builds an Android **`RenderEffect` chain** attached to the glass layer. Because the layer is offscreen,
the RenderEffect applies to the *sampled background texture* — exactly what we want.

---

## 3. The Effects DSL: `BackdropEffectScope` and the fixed chain order

File: `backdrop/src/commonMain/kotlin/com/kyant/backdrop/BackdropEffectScope.kt` + `effects/`

```kotlin
sealed interface BackdropEffectScope : Density, RuntimeShaderCache {
    val size: Size
    val layoutDirection: LayoutDirection
    val shape: Shape
    var padding: Float
    var renderEffect: RenderEffect?
}
```

The scope accumulates two things as you call effects: a `RenderEffect` chain and a `padding` value. The chain order is
**fixed by the library contract**:

```
colorFilter  ⇒  blur  ⇒  lens
```

So inside every recipe you must write `vibrancy()` / `colorControls()` first, `blur()` second, `lens()` last. Putting `lens()`
first means refraction happens on the pre-blur image and the edges get smeared.

### 3.1 `vibrancy()` / `colorControls()` — the color filter

File: `effects/ColorFilter.kt`

```kotlin
private val VibrantColorFilter = colorControlsColorFilter(saturation = 1.5f)
fun BackdropEffectScope.vibrancy() { colorFilter(VibrantColorFilter) }
```

`vibrancy()` is just a `ColorFilter` that boosts saturation to **1.5×**. Why? Blur desaturates colors (averaging nearby pixels
washes them out), so the glass looks grey. `vibrancy()` restores the saturation so the glass looks "luminous" rather than "grey".

`colorControls(brightness, contrast, saturation)` builds a full 4×5 color matrix:

```kotlin
val colorMatrix = ColorMatrix(floatArrayOf(
    cr + cs, cg,      cb,      0f, t,
    cr,      cg + cs, cb,      0f, t,
    cr,      cg,      cb + cs, 0f, t,
    0f,      0f,      0f,      1f, 0f
))
```

where `c = contrast`, `s = saturation`, `t = (0.5 - c*0.5 + brightness) * 255`, and `r/g/b` are the luminance weights
(0.213/0.715/0.072) scaled by `1 - saturation`. This is the standard brightness/contrast/saturation matrix.

`opacity(alpha)` is a color matrix that scales only the alpha channel.

### 3.2 `blur(radius)` — background Gaussian blur

File: `effects/Blur.kt`

```kotlin
fun BackdropEffectScope.blur(radius: Float, edgeTreatment: TileMode = TileMode.Clamp) {
    if (!isRenderEffectSupported()) return
    if (radius <= 0f) return
    if (edgeTreatment != TileMode.Clamp || renderEffect != null) {
        if (radius > padding) padding = radius   // expand the layer so blur has edge pixels
    }
    renderEffect = BlurEffect(renderEffect, radius, radius, edgeTreatment)  // chain: previous + this blur
}
```

`BlurEffect` is Compose's built-in Gaussian blur RenderEffect. It chains onto the previous effect. The `padding` expansion
ensures the blur doesn't hit a hard edge at the glass boundary.

### 3.3 `lens(refractionHeight, refractionAmount, depthEffect, chromaticAberration)` — THE refraction

File: `effects/Lens.kt`

```kotlin
fun BackdropEffectScope.lens(refractionHeight: Float, refractionAmount: Float, depthEffect: Boolean = false, chromaticAberration: Boolean = false) {
    if (!isRuntimeShaderSupported()) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return
    if (padding > 0f) padding = (padding - refractionHeight).coerceAtLeast(0f)  // lens needs less padding than blur

    val cornerRadii = cornerRadii   // extract 4 corner radii from the shape (G2 RoundedRectangle or CornerBasedShape)
    val shader = if (!chromaticAberration)
        obtainRuntimeShader("Refraction", RoundedRectRefractionShaderString)
    else
        obtainRuntimeShader("RefractionWithDispersion", RoundedRectRefractionWithDispersionShaderString)

    shader.apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("offset", -padding, -padding)
        setFloatUniform("cornerRadii", cornerRadii)
        setFloatUniform("refractionHeight", refractionHeight)
        setFloatUniform("refractionAmount", -refractionAmount)
        setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
        if (chromaticAberration) setFloatUniform("chromaticAberration", 1f)
    }
    effect(RuntimeShaderEffect(shader, "content"))   // chain: the AGSL shader as a RenderEffect
}
```

Key details:
- **Both arguments are dp, not pixels.** You must call `12f.dp.toPx()`. The first is *refraction height* (how far the lens
  "bulges" — area of effect), the second is *refraction amount* (how strong the offset is). Increasing both = thicker glass.
- **`refractionAmount` is negated** when passed to the shader (`-refractionAmount`) — the shader bends *toward* the edge, so
  the sign is flipped to get the correct visual direction.
- **Corner radii are extracted from the shape.** Only `RoundedRectangularShape` (G2 squircle from `com.kyant.shapes`) or
  `CornerBasedShape` are supported; anything else throws `UnsupportedOperationException`. This is why you must use
  `com.kyant.shapes.RoundedRectangle`, not `androidx.compose.foundation.shape.RoundedCornerShape`.
- **The shader is cached by key** (`"Refraction"` / `"RefractionWithDispersion"`) via `RuntimeShaderCache` — AGSL compilation
  is expensive, so it's compiled once and reused.

### 3.4 `effect()` / `runtimeShaderEffect()` — arbitrary chaining

File: `effects/RenderEffect.kt`

```kotlin
fun BackdropEffectScope.effect(effect: RenderEffect) { renderEffect = renderEffect.chain(effect) }
fun BackdropEffectScope.runtimeShaderEffect(key, shaderString, uniformShaderName, block) {
    renderEffect = renderEffect.chain(RuntimeShaderEffect(obtainRuntimeShader(key, shaderString).apply(block), uniformShaderName))
}
```

These let you chain any `RenderEffect` or custom AGSL shader into the pipeline.

---

## 4. The AGSL Shaders: the real optical math

File: `backdrop/src/commonMain/kotlin/com/kyant/backdrop/internal/Shaders.kt`

This is the actual liquid-glass math, written in **AGSL** (Android Graphics Shading Language), compiled at runtime by
`android.graphics.RuntimeShader`.

### 4.1 The SDF helpers — distance to a rounded rect

```glsl
float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;   // negative inside the shape, positive outside, zero on the edge
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    // gradient (outward normal) of the distance field — points away from the nearest edge
}
```

For each pixel, `sdRoundedRect` computes the **signed distance** to the rounded-rect edge, and `gradSdRoundedRect` computes
the **outward normal direction**. These two values drive everything: the distance tells you *how much* to bend (stronger near the
rim), the normal tells you *which direction* to bend.

### 4.2 The refraction shader (core)

```glsl
uniform shader content;            // the offscreen background texture (bound by the RenderEffect as "content")
uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;

float circleMap(float x) { return 1.0 - sqrt(1.0 - x * x); }   // quarter-circle falloff → "lens" profile

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;
    float radius = radiusAt(coord, cornerRadii);

    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) return content.eval(coord);  // far outside → pass through unchanged
    sd = min(sd, 0.0);                                          // clamp: only inside/rim matters

    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;  // bend amount: 0 at center, max at rim
    float2 grad = normalize(gradSdRoundedRect(...) + depthEffect * normalize(centeredCoord));

    float2 refractedCoord = coord + d * grad;   // bend the sample coordinate
    return content.eval(refractedCoord);         // re-sample the background at the bent position
}
```

Read it as: **for each pixel, look at the background texture, but at a slightly shifted position — the shift is biggest near the
glass rim and tapers toward the center (circleMap profile).** The center looks nearly flat (real glass is thin in the middle),
the edges bend — exactly how a lens behaves. `depthEffect` adds a radial component so the center also subtly refracts (for thick
dialogs / bottom sheets).

### 4.3 The dispersion shader (chromatic aberration)

```glsl
uniform float chromaticAberration;
...
float dispersionIntensity = chromaticAberration * ((x * y) / (w * h));  // stronger toward corners
float2 dispersedCoord = d * grad * dispersionIntensity;

half4 color = half4(0.0);
// 7 spectral bands, each sampled at a different offset and summed with weights:
half4 red    = content.eval(refractedCoord + dispersedCoord);              color.r += red.r / 3.5;
half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0/3.0));  ...
half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0/3.0));  ...
half4 green  = content.eval(refractedCoord);                                 ...
half4 cyan   = content.eval(refractedCoord - dispersedCoord * (1.0/3.0));  ...
half4 blue   = content.eval(refractedCoord - dispersedCoord * (2.0/3.0));  ...
half4 purple = content.eval(refractedCoord - dispersedCoord);                ...
return color;
```

The shader splits the refracted pixel into **7 spectral bands** (red → orange → yellow → green → cyan → blue → purple), samples
each at a progressively different offset, and re-composites them with per-channel weights. This produces the subtle **red/blue
fringing at the edges** — the chromatic aberration that makes real glass look *real*.

> **The hidden rule:** aberration is only switched on during *motion* (press/drag). Static glass with aberration always on shows
> constant red/blue fringes and looks obviously fake. The demo wires `progress → 1` on interaction (see `GlassMaterials.selectionIndicator`,
> `toggleKnob`, `sliderKnob` — all pass `chromaticAberration = true` and scale by `progress`).

### 4.4 The highlight shaders

```glsl
// DefaultHighlightShaderString
uniform float angle;       // light direction (45°)
uniform float falloff;     // edge fade exponent
...
float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);  // edge normal
float2 normal = float2(cos(angle), sin(angle));                          // light direction
float d = dot(grad, normal);
float intensity = pow(abs(d), falloff);    // bright where edge faces the light, fades around the rim
return color * intensity;
```

The highlight is **not** a `border()`. It's a stroked outline with an AGSL shader that fades the brightness by the dot product
of the edge normal and the light direction — bright on the edges facing the 45° light, dark on the opposite side. `Ambient`
style uses a similar shader but outputs a white mask (`step(0, d)`) for a soft ambient sheen. `Plain` style uses no shader — just
a uniform stroke.

---

## 5. Highlight: the rim light pass

Files: `highlight/Highlight.kt`, `highlight/HighlightStyle.kt`, `highlight/HighlightModifier.kt`

```kotlin
data class Highlight(
    val width: Dp = 0.5f.dp,
    val blurRadius: Dp = width / 2f,
    val alpha: Float = 1f,
    val style: HighlightStyle = HighlightStyle.Default   // Default / Ambient / Plain
)
```

`HighlightNode` draws the highlight as a separate pass:
1. Records a stroked outline (`PaintingStyle.Stroke`, `strokeWidth = width * 2`) into its own `GraphicsLayer`.
2. The paint carries the highlight shader (`DefaultHighlightShaderString` or `AmbientHighlightShaderString`) + a blur
   (`paint.blur(blurRadius)`).
3. The layer is drawn offset by 1px (`translate(1,1)` then `translate(-1,-1)`) so the highlight sits on the outer edge.
4. `blendMode = BlendMode.Plus` (additive) — the highlight *adds* light rather than covering.

Three styles:
- **`Default`** — `White @ 50%`, angle 45°, falloff 1, additive. The polished Apple rim.
- **`Ambient`** — soft white sheen around the whole rim, normal blend mode.
- **`Plain`** — uniform white stroke @ 38%, no shader. For when you don't need the gradient.

---

## 6. Shadow & InnerShadow: the depth passes

Files: `shadow/Shadow.kt`, `shadow/ShadowModifier.kt`, `shadow/InnerShadow.kt`, `shadow/InnerShadowModifier.kt`

### 6.1 Outer `Shadow`

```kotlin
data class Shadow(
    val radius: Dp = 24f.dp,
    val offset: DpOffset = DpOffset(0f.dp, radius / 6f),  // mostly below
    val color: Color = Color.Black.copy(alpha = 0.1f),
    val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode
)
```

`ShadowNode` uses a **two-stroke Clear-blend trick** to get a soft outer shadow:
1. Record into an offscreen layer sized `size + radius*4 + offset` (enough room for the blur).
2. Draw the shape outline with a blurred paint (`paint.blur(radius)`) at the offset position.
3. Draw the shape outline **again** with `ShadowMaskPaint` (`blendMode = BlendMode.Clear`) at the original position — this
   *erases* the shadow from inside the shape, leaving only the outer glow.
4. Draw the layer, then `drawContent()` on top.

This is why the shadow is soft and only appears *outside* the glass — the Clear pass punches a hole in the shape's interior.

### 6.2 `InnerShadow`

```kotlin
data class InnerShadow(
    val radius: Dp = 8f.dp,
    val offset: DpOffset = DpOffset(0f.dp, radius / 6f),
    val color: Color = Color.Black.copy(alpha = 0.2f),
    val alpha: Float = 1f
)
```

`InnerShadowNode` is the inverse — it creates a shadow *inside* the shape (the "concave well" feel):
1. Record into an offscreen layer.
2. Clip to the shape outline (`canvas.clipOutline(outline, clipPath)`).
3. Draw the shape outline with the shadow color.
4. Translate by the offset and draw the outline again with `BlendMode.Clear` — this erases the shadow from the *offset*
   position, leaving a shadow only on the side opposite the offset (the "lit from above, shadow below inside" look).
5. Apply a `BlurEffect(radius)` to the layer for softness.
6. Clip to the shape again and draw the layer.

Both shadow passes are chained as `ModifierNode`s **before** the `DrawBackdropNode`, so the glass has a believable
top-lit, bottom-shadowed solidity.

---

## 7. Platform Implementation: Android (AGSL + RenderEffect)

Files: `backdrop/src/androidMain/kotlin/com/kyant/backdrop/`

### 7.1 `RuntimeShader` — AGSL compilation at runtime

```kotlin
@RequiresApi(Build.VERSION_CODES.TIRAMISU)   // API 33+
actual fun RuntimeShader(shaderString: String): RuntimeShader {
    val shader = android.graphics.RuntimeShader(shaderString)   // compile the AGSL string
    return AndroidRuntimeShader(shader)
}
```

AGSL is compiled **at runtime** by the GPU driver. The `AndroidRuntimeShader` wrapper forwards `setFloatUniform` /
`setIntUniform` / `setColorUniform` to the native `android.graphics.RuntimeShader`.

### 7.2 `RenderEffect` chain — native Android effects

File: `backdrop/src/androidMain/kotlin/com/kyant/backdrop/internal/RenderEffect.kt`

```kotlin
@RequiresApi(Build.VERSION_CODES.S)   // API 31+
internal actual fun RenderEffect?.chain(other: RenderEffect): RenderEffect {
    return if (this != null)
        android.graphics.RenderEffect.createChainEffect(other.asAndroidRenderEffect(), this.asAndroidRenderEffect())
    else other
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)  // API 33+
internal actual fun RuntimeShaderEffect(runtimeShader, uniformShaderName): RenderEffect {
    return android.graphics.RenderEffect.createRuntimeShaderEffect(runtimeShader.asAndroidRuntimeShader(), uniformShaderName)
}
```

The effects chain is realized as native `android.graphics.RenderEffect`:
- `createChainEffect(other, this)` — chains two effects (input flows through `this`, then `other`).
- `createRuntimeShaderEffect(shader, "content")` — wraps an AGSL shader as a RenderEffect, with the input texture bound to
  the uniform named `"content"`.
- `createColorFilterEffect(filter, input)` — wraps a ColorFilter.

### 7.3 The hard platform requirements

File: `backdrop/src/androidMain/kotlin/com/kyant/backdrop/Platform.kt`

```kotlin
actual fun isRenderEffectSupported(): Boolean  = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S        // API 31+ (Android 12)
actual fun isRuntimeShaderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU // API 33+ (Android 13)
```

| Capability | Android API | Needed for |
|---|---|---|
| `RenderEffect` (blur / colorFilter / chain) | **API 31+** | every blur & tint |
| `RuntimeShader` (AGSL) | **API 33+** | `lens()` refraction + highlight shaders |

**So `minSdk` must be ≥ 31 for any effect, and ≥ 33 for true refraction.** Below that the shader functions silently no-op —
you get a plain translucent card, not liquid glass. Every effect function guards itself with `isRenderEffectSupported()` /
`isRuntimeShaderSupported()`, so the app doesn't crash on older devices — it just degrades gracefully.

### 7.4 Multiplatform: Skiko for desktop/iOS

The `backdrop/src/skikoMain/` directory contains parallel implementations for the Skiko backend (desktop / JS / WASM / iOS),
with its own `RuntimeShader`, `RenderEffect`, `Paint`, and `Platform`. The `commonMain` code is shared; only the platform
expect/actual declarations differ.

---

## 8. Caching & Performance Optimizations

### 8.1 `RuntimeShaderCache` — compile AGSL once

File: `backdrop/src/commonMain/kotlin/com/kyant/backdrop/RuntimeShaderCache.kt`

```kotlin
internal class RuntimeShaderCacheImpl : RuntimeShaderCache {
    private val runtimeShaders = mutableMapOf<String, RuntimeShader>()
    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader =
        runtimeShaders.getOrPut(key) { RuntimeShader(string) }
}
```

AGSL compilation is expensive. Shaders are cached by key (`"Refraction"`, `"RefractionWithDispersion"`, `"Default"`,
`"Ambient"`) and reused across all glass components. The cache is cleared on node detach (`effectScope.reset()`).

### 8.2 `ShapeProvider` — cache the Outline

File: `backdrop/src/commonMain/kotlin/com/kyant/backdrop/internal/ShapeProvider.kt`

```kotlin
internal class ShapeProvider(val shapeBlock: () -> Shape) {
    private var _shape: Shape? = null
    private var _outline: Outline? = null
    private var _size: Size = Size.Unspecified
    // ...
    val shape = object : Shape {
        override fun createOutline(size, layoutDirection, density): Outline {
            val shape = shapeBlock()
            if (_shape != shape) { _shape = shape; _outline = null }
            if (_outline == null || _size != size || _layoutDirection != layoutDirection || _density != density.density) {
                _size = size; _layoutDirection = layoutDirection; _density = density.density
                _outline = shape.createOutline(size, layoutDirection, density)
            }
            return _outline!!
        }
    }
}
```

Creating an `Outline` (which computes the rounded-rect path) is moderately expensive. `ShapeProvider` caches it and only
recomputes when the shape, size, layout direction, or density changes. This is used by `DrawBackdropNode`, `HighlightNode`,
`ShadowNode`, and `InnerShadowNode` — all share the same `ShapeProvider` instance.

### 8.3 Conditional recording — don't waste GPU on static pages

The recording layer (`layerBackdrop`) does one full-screen offscreen render per frame. On a static page with no overlay reading
it, this is pure waste. The upstream demo pages are simple enough that it's always on, but for a real app you should conditionally
attach `layerBackdrop` only when an upper overlay actually needs to read it (see `docs/02-Backdrop-Layered-Architecture.md` §2).

---

## 9. Combined Backdrops: the Control-Center trick

File: `backdrop/src/commonMain/kotlin/com/kyant/backdrop/backdrops/CombinedBackdrop.kt`

A content layer can only refract its **immediate** lower layer. To make an upper overlay (Control Center) refract "the layer
below the below" (home-screen icons *and* wallpaper), the engine merges multiple recorded textures:

```kotlin
@Composable
fun rememberCombinedBackdrop(backdrop1: Backdrop, backdrop2: Backdrop): Backdrop { ... }
// drawBackdrop() = draw backdrop1, then backdrop2, onto the same target → painter's-algorithm stacking
```

Supported for 2, 3, or vararg backdrops. `isCoordinatesDependent` is true if *any* constituent is coordinate-dependent. This is
what lets "opening the Control Center blurs the home screen" be real — the overlay samples a combined texture of wallpaper +
home screen.

Your `GlassBars.kt` uses this in the moving tab indicator:
```kotlin
backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)
```
The indicator refracts both the wallpaper *and* the tab bar's own content (recorded into `tabsBackdrop` by an `alpha(0f)`
shadow node — the "refract-itself without a loop" trick from `docs/02` rule 8).

---

## 10. The Demo Catalog (`app/`) — how real glass is assembled

File: `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/`

The `app` module is the **Backdrop Catalog** — a live showcase. The five reusable components teach you the exact call pattern:

| Component | File | Key recipe |
|---|---|---|
| `LiquidButton` | `components/LiquidButton.kt` | `vibrancy() + blur(2dp) + lens(12dp, 24dp)` on `Capsule()`, press-driven scale + dispersion |
| `LiquidToggle` | `components/LiquidToggle.kt` | capsule glass + draggable knob with `chromaticAberration` |
| `LiquidSlider` | `components/LiquidSlider.kt` | glass track + draggable thumb |
| `LiquidBottomTabs` / `LiquidBottomTab` | `components/LiquidBottomTabs.kt` | glass tab bar + moving indicator with `rememberCombinedBackdrop` |
| `BackdropDemoScaffold` | `BackdropDemoScaffold.kt` | records the wallpaper once, hands the backdrop to all children |

The 15 destinations show each effect in isolation: `HomeContent`, `ButtonsContent`, `ControlCenterContent`, `LockScreenContent`,
`DialogContent`, `ProgressiveBlurContent`, `MagnifierContent`, `ScrollContainerContent`, `LazyScrollContainerContent`,
`ToggleContent`, `SliderContent`, `BottomTabsContent`, `AdaptiveLuminanceGlassContent`, `GlassPlaygroundContent`.

**Study `LiquidButton.kt` first** — it is the cleanest complete example (backdrop + effects + interactive highlight +
press-driven dispersion). Then `LiquidBottomTabs.kt` for the combined-backdrop trick.

---

## 11. Your `LiquidGlassShowcase/` — exact file-by-file correspondence

Your showcase is an **Android-native re-implementation** of the same engine ideas (Jetpack Compose, not Compose Multiplatform),
using the published artifacts `io.github.kyant0:backdrop` and `io.github.kyant0:shapes` from the same author. Every numeric
value is copied verbatim from the upstream catalog.

### 11.1 `core/glass/GlassMaterials.kt` — the 12 material recipes (single source of truth)

Every recipe is annotated with its upstream source file:

| Recipe | Upstream source | Effects |
|---|---|---|
| `Button` | `components/LiquidButton.kt` | `vibrancy() + blur(2dp) + lens(12dp, 24dp)` |
| `BottomBar` | `components/LiquidBottomTabs.kt` (bar) | `vibrancy() + blur(8dp) + lens(24dp, 24dp)` |
| `selectionIndicator(progress)` | `components/LiquidBottomTabs.kt` (indicator) | `lens(10dp*p, 14dp*p, chromaticAberration=true)` |
| `toggleKnob(progress)` | `components/LiquidToggle.kt` | `blur(8dp*(1-p)) + lens(5dp*p, 10dp*p, chromaticAberration=true)` |
| `sliderKnob(progress)` | `components/LiquidSlider.kt` | `blur(8dp*(1-p)) + lens(10dp*p, 14dp*p, chromaticAberration=true)` |
| `dialog(isLightTheme)` | `destinations/DialogContent.kt` | `colorControls(brightness, sat=1.5) + blur(16/8dp) + lens(24dp, 48dp, depthEffect=true)` |
| `NavigationBar` | `tutorials/glass-bottom-bar` | `vibrancy() + blur(4dp) + lens(16dp, 32dp)` |
| `BottomSheet` | `tutorials/glass-bottom-sheet` | `vibrancy() + blur(4dp) + lens(24dp, 48dp, depthEffect=true)` |
| `Card` | `destinations/LazyScrollContainerContent.kt` | `vibrancy() + lens(16dp, 32dp)` |
| `controlCenterItem(progress)` | `destinations/ControlCenterContent.kt` | `vibrancy() + lens(24dp*p, 48dp*p, depthEffect=true)` |

Note the pattern: **interactive elements (indicator, knob, slider) scale the lens by `progress` and turn on
`chromaticAberration`** — the aberration only appears while pressing/dragging, exactly as the upstream intends.

### 11.2 `core/glass/GlassScaffold.kt` — the recorder

A 1:1 port of upstream `BackdropDemoScaffold.kt`:
```kotlin
val backdrop = rememberLayerBackdrop()
Image(painter = wallpaper, modifier = Modifier.layerBackdrop(backdrop).fillMaxSize())
content(backdrop)
```
The wallpaper is recorded into a `LayerBackdrop`; every glass component refracts that same layer, keeping the material coherent
across the whole UI.

### 11.3 `components/GlassFoundation.kt` — the minimal glass card

```kotlin
fun GlassCard(backdrop, modifier, cornerRadius = 32dp, surfaceColor, content) {
    val shape = RoundedRectangle(cornerRadius)   // G2 squircle from com.kyant.shapes
    Box(modifier.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = GlassMaterials.Card,             // vibrancy() + lens(16dp, 32dp)
        onDrawSurface = if (surfaceColor.isSpecified) { { drawRect(surfaceColor) } } else null
        // highlight + shadow default to Highlight.Default + Shadow.Default (omitted = library defaults)
    ), content = content)
}
```
This is the minimal working example — 18 lines of real glass.

### 11.4 `components/GlassBars.kt` — the most complex example (understand this and you understand it all)

`GlassBottomTabBar` demonstrates **every advanced technique at once**:
1. **Bar layer**: `drawBackdrop(backdrop, Capsule(), GlassMaterials.BottomBar, layerBlock = { scale on press }, onDrawSurface = containerColor)`.
2. **Shadow recording node**: an `alpha(0f)` Row with `.layerBackdrop(tabsBackdrop)` records the tab bar's own content into a
   second backdrop (the "refract-itself without a loop" trick).
3. **Moving indicator**: `drawBackdrop(backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop), Capsule(), lens(10dp*p, 14dp*p, chromaticAberration=true), highlight = Highlight.Default @ p, shadow = Shadow @ p, innerShadow = InnerShadow(8dp*p), layerBlock = { scale + velocity-based squash }, onDrawSurface = { two-layer tint })`.
4. **Interactive highlight**: `InteractiveHighlight` tracks the press position and animates the rim light.
5. **Damped drag**: `DampedDragAnimation` provides spring physics for the indicator movement and press scale.

This single file combines: layered backdrops, combined backdrops, chromatic aberration, dynamic highlight/shadow/innerShadow,
velocity-based deformation, and interactive rim light. **Understand `GlassBars.kt` and you understand the entire engine.**

### 11.5 `ios/` — the full iOS shell

`IosShell.kt`, `IosHomeScreen.kt`, `IosLockScreen.kt`, `IosOverlays.kt`, `IosAppWindow.kt`, `IosAppSwitcher.kt`,
`IosSystemChrome.kt`, `IosAppCatalog.kt`, `IosScaffold.kt`, `IosShellState.kt`, `IosGlyphs.kt` — a full re-assembly of the
Apple system shell (home / lock / control center / app switcher / transitions) using the glass components above. See
`docs/05-iOS-Shell-in-Practice.md` for the layered architecture.

---

## 12. The Complete Pipeline in One Diagram

```
 background scene (wallpaper + content)
      │  Modifier.layerBackdrop(backdrop)  ← RECORD every frame into GraphicsLayer (offscreen GPU texture)
      │     LayerBackdropNode.draw(): drawContent() + recordLayer(graphicsLayer) { onDraw() }
      ▼
 LayerBackdrop (texture + layerCoordinates)
      │  sampled by DrawBackdropNode.draw()
      │     coordinate alignment: offset = layerCoordinates.localPositionOf(coordinates); translate(-offset)
      │     inverse transform: undo glass's own scale/rotation (InverseLayerScope)
      ▼
 drawBackdropLayer() → record glass into its own GraphicsLayer (sized + padding*2)
      │  RenderEffect chain attached to the layer (fixed order: colorFilter → blur → lens)
      │     vibrancy()     = ColorFilter(saturation 1.5×)  → restores color blur kills
      │     blur(radius)   = BlurEffect                      → Gaussian blur of background
      │     lens(h, amount) = AGSL RuntimeShader              → per-pixel SDF refraction (+ optional 7-band dispersion)
      ▼
 onDrawSurface()  → translucent tint (5%–35%! stay under 50%)
      ▼
 drawContent()    → SHARP content (label, icon, text)
      ▼
 Highlight pass   → angle-faded rim light (AGSL shader on stroked outline, BlendMode.Plus)
 Shadow pass      → soft outer shadow (two-stroke Clear-blend trick)
 InnerShadow pass → soft inner shadow (clip + offset Clear + BlurEffect)
      ▼
 liquid glass that refracts, bends, fringes, catches light, and sits in depth
```

---

## 13. The Three Knobs That Control "How Glassy" (recap from code)

| You want… | Change | Direction |
|---|---|---|
| Thicker / more "water-drop" glass | `lens(refractionHeight, refractionAmount)` | raise **both** together (`12,24` → `24,48`) |
| More fog | `blur(radius)` | `2 → 8 → 16` |
| More solid / obscuring | `onDrawSurface = { drawRect(color) }` | white `0.05 → 0.34` (stay under 50%!) |
| Stronger edge light | `highlight = { Highlight.Default.copy(alpha = 1f) }` | `0.5 → 1.0` |
| Motion fringing | chromatic aberration via `progress` | only on press/drag, scale lens by progress |
| Deeper / more "3D" | `lens(..., depthEffect = true)` | adds radial refraction at center |

Everything else is detail. **Change parameters proportionally**, never one knob wildly out of line. All exact values are in
`showcase/core/glass/GlassMaterials.kt` and `docs/04-Material-Recipe-Table.md`.

---

## 14. Common Failures — Source-Level Diagnostics

| Symptom | Root cause (from source) | Fix |
|---|---|---|
| Glass is empty / black inside | `LayerBackdrop` recorded nothing — no node has `Modifier.layerBackdrop(backdrop)` attached, or `layerCoordinates` is null | Attach `layerBackdrop` to the background node; ensure `onGloballyPositioned` fires |
| Glass shows the wrong part of background | Coordinate alignment missing — `localPositionOf` not computed, or glass is inside a transformed parent | The engine handles this; if broken, check `layerCoordinates` is set and `InverseLayerScope` undoes the transform |
| Glass looks grey | Missing `vibrancy()` — blur desaturates, no saturation boost | Add `vibrancy()` as the first effect |
| Can't see through glass | `onDrawSurface` color too dark (>50%) — covers all refraction | Drop tint to 5%–35% |
| Red/blue fringes always visible | `chromaticAberration = true` static — should only open on motion | Scale lens by `progress`, only enable aberration during press/drag |
| Edges are hard / plastic | Used `border()` instead of `Highlight` — uniform stroke vs angle-faded rim | Use `highlight = { Highlight.Default }` |
| Corners look wrong | Used `RoundedCornerShape` (androidx) instead of `com.kyant.shapes.RoundedRectangle` (G2 squircle) — `lens()` throws on unsupported shapes | Use `RoundedRectangle` / `Capsule` from `com.kyant.shapes` |
| Glass shows previous frame | Recording node and drawing node ordered backwards — content layer must draw before overlay | Ensure z-order: background (recorder) → content → overlay (sampler) |
| Laggy / high GPU | `layerBackdrop` attached unconditionally on static page — one full-screen offscreen render per frame wasted | Conditionally attach only when an overlay actually reads it |
| Crashes on older phones | `minSdk < 31` — no `RenderEffect`; `< 33` — no AGSL `RuntimeShader` | Set `minSdk ≥ 31` (≥33 for true refraction); effects guard themselves but the app should declare the requirement |
| `lens()` throws `UnsupportedOperationException` | Shape is not `RoundedRectangularShape` or `CornerBasedShape` — `cornerRadii` extraction returns null | Use `com.kyant.shapes.RoundedRectangle` or `Capsule` |

---

## 15. File Index — Where Everything Lives

### Upstream engine (`upstream/AndroidLiquidGlass/backdrop/src/`)

| Concern | File |
|---|---|
| Backdrop interface | `commonMain/.../Backdrop.kt` |
| Effect scope (DSL) | `commonMain/.../BackdropEffectScope.kt` |
| Draw modifier (sampler) | `commonMain/.../DrawBackdropModifier.kt` |
| Runtime shader cache | `commonMain/.../RuntimeShaderCache.kt` |
| Layer backdrop (recorder) | `commonMain/.../backdrops/LayerBackdrop.kt` |
| Layer backdrop modifier | `commonMain/.../backdrops/LayerBackdropModifier.kt` |
| Combined backdrop | `commonMain/.../backdrops/CombinedBackdrop.kt` |
| Canvas / Empty backdrop | `commonMain/.../backdrops/CanvasBackdrop.kt`, `EmptyBackdrop.kt` |
| Lens (refraction) | `commonMain/.../effects/Lens.kt` |
| Blur | `commonMain/.../effects/Blur.kt` |
| Color filter / vibrancy | `commonMain/.../effects/ColorFilter.kt` |
| Effect chaining | `commonMain/.../effects/RenderEffect.kt` |
| AGSL shaders (SDF/refraction/dispersion/highlight) | `commonMain/.../internal/Shaders.kt` |
| Layer recorder | `commonMain/.../internal/LayerRecorder.kt` |
| Inverse layer scope | `commonMain/.../internal/InverseLayerScope.kt` |
| Shape provider (outline cache) | `commonMain/.../internal/ShapeProvider.kt` |
| Highlight | `commonMain/.../highlight/Highlight.kt`, `HighlightStyle.kt`, `HighlightModifier.kt` |
| Shadow / InnerShadow | `commonMain/.../shadow/Shadow.kt`, `InnerShadow.kt`, `ShadowModifier.kt`, `InnerShadowModifier.kt` |
| Android RuntimeShader (AGSL) | `androidMain/.../RuntimeShader.kt` |
| Android RenderEffect chain | `androidMain/.../internal/RenderEffect.kt` |
| Android platform checks | `androidMain/.../Platform.kt` |
| Skiko (desktop/iOS) implementations | `skikoMain/.../` |

### Your showcase (`LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/`)

| Concern | File |
|---|---|
| 12 material recipes (single source of truth) | `core/glass/GlassMaterials.kt` |
| Recorder scaffold | `core/glass/GlassScaffold.kt` |
| Minimal glass card | `components/GlassFoundation.kt` |
| Complex bars (tabs/nav/search/textfield) | `components/GlassBars.kt` |
| Buttons / controls / lists / overlays | `components/GlassButtons.kt`, `GlassControls.kt`, `GlassList.kt`, `GlassOverlays.kt` |
| Full iOS shell | `ios/IosShell.kt` + 10 related files |
| Interactive utilities | `core/utils/InteractiveHighlight.kt`, `DampedDragAnimation.kt`, `DragGestureInspector.kt`, `UISensor.kt`, `Ripple.kt` |
| iOS colors / icons | `core/ios/IosColors.kt`, `GlassIcons.kt` |

---

*End of complete walkthrough. If you read this far, you now understand the entire liquid-glass engine from the offscreen
recording layer down to the per-pixel AGSL refraction shader, and how your showcase re-assembles it on Android.*
