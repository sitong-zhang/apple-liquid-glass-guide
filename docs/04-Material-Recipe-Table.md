# 04 · Material Recipe Table

> This chapter is the documented version of the **single source of truth**.
> The corresponding file in code is [GlassMaterials.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/core/glass/GlassMaterials.kt).
>
> **All values are copied verbatim from the upstream Kyant0/AndroidLiquidGlass (Backdrop 2.0.0); none were tuned by me.**
> Every recipe is annotated with its upstream source. You may change them, but please read the derivation rules in §6 first and make a note in this chapter.

---

## 1. Recipe Master Table

Unit notes: `H` = `refractionHeight`, `A` = `refractionAmount`, both are **dp-scale, actually passed as px (`.dp.toPx()`)**.
`CA` = `chromaticAberration`, `DE` = `depthEffect`.

| # | Recipe | Upstream source | effects (strict order) | H / A | CA | DE |
|---|---|---|---|---|---|---|
| 1 | **Button** | `components/LiquidButton.kt` | `vibrancy()` → `blur(2.dp)` → `lens` | 12 / 24 | ✗ | ✗ |
| 2 | **BottomBar** | `components/LiquidBottomTabs.kt` (bar background) | `vibrancy()` → `blur(8.dp)` → `lens` | 24 / 24 | ✗ | ✗ |
| 3 | **selectionIndicator(progress)** (moving indicator) | `components/LiquidBottomTabs.kt` (moving indicator) | `lens` | 10×p / 14×p | ✓ | ✗ |
| 4 | **toggleKnob(progress)** (toggle knob) | `components/LiquidToggle.kt` (toggle knob) | `blur(8.dp ×(1−p))` → `lens` | 5×p / 10×p | ✓ | ✗ |
| 5 | **sliderKnob(progress)** (slider knob) | `components/LiquidSlider.kt` (slider knob) | `blur(8.dp ×(1−p))` → `lens` | 10×p / 14×p | ✓ | ✗ |
| 6 | **dialog(isLightTheme)** | `destinations/DialogContent.kt` | `colorControls(b, 1.5)` → `blur(16/8.dp)` → `lens` | 24 / 48 | ✗ | **✓** |
| 7 | **NavigationBar** | `tutorials/glass-bottom-bar` | `vibrancy()` → `blur(4.dp)` → `lens` | 16 / 32 | ✗ | ✗ |
| 8 | **BottomSheet** | `tutorials/glass-bottom-sheet` | `vibrancy()` → `blur(4.dp)` → `lens` | 24 / 48 | ✗ | **✓** |
| 9 | **Card** | `destinations/LazyScrollContainerContent.kt` | `vibrancy()` → `lens` (no blur) | 16 / 32 | ✗ | ✗ |
| 10 | **controlCenterItem(progress)** | `destinations/ControlCenterContent.kt` | `vibrancy()` → `lens` | 24×p / 48×p | ✗ | **✓** |

Source code (verbatim):

```kotlin
object GlassMaterials {
    val Button = { vibrancy(); blur(2f.dp.toPx()); lens(12f.dp.toPx(), 24f.dp.toPx()) }
    val BottomBar = { vibrancy(); blur(8f.dp.toPx()); lens(24f.dp.toPx(), 24f.dp.toPx()) }

    fun selectionIndicator(progress: Float) = {
        lens(10f.dp.toPx() * progress, 14f.dp.toPx() * progress, chromaticAberration = true)
    }
    fun toggleKnob(progress: Float) = {
        blur(8f.dp.toPx() * (1f - progress))
        lens(5f.dp.toPx() * progress, 10f.dp.toPx() * progress, chromaticAberration = true)
    }
    fun sliderKnob(progress: Float) = {
        blur(8f.dp.toPx() * (1f - progress))
        lens(10f.dp.toPx() * progress, 14f.dp.toPx() * progress, chromaticAberration = true)
    }
    fun dialog(isLightTheme: Boolean) = {
        colorControls(brightness = if (isLightTheme) 0.2f else 0f, saturation = 1.5f)
        blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
        lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
    }
    val NavigationBar = { vibrancy(); blur(4f.dp.toPx()); lens(16f.dp.toPx(), 32f.dp.toPx()) }
    val BottomSheet = { vibrancy(); blur(4f.dp.toPx()); lens(24f.dp.toPx(), 48f.dp.toPx(), true) }
    val Card = { vibrancy(); lens(16f.dp.toPx(), 32f.dp.toPx()) }
    fun controlCenterItem(progress: Float) = {
        vibrancy(); lens(24f.dp.toPx() * progress, 48f.dp.toPx() * progress, depthEffect = true)
    }
}
```

---

## 2. Recipe + Shape + Tint = A Complete Material

`effects` is only the "optics"; you must also pair it with **shape** and **onDrawSurface** to get a usable component.
The table below is the complete triple as actually landed in this repo.

| Component | shape | effects | onDrawSurface | highlight / shadow |
|---|---|---|---|---|
| Glass button `GlassButton` | `Capsule()` | Button | none (or `tint` coloring) | default |
| Round icon button `GlassIconButton` | `Capsule()` | Button | none / `tint` | default |
| Chip `GlassChip` | `Capsule()` | Button | `accent` when selected | default |
| Card `GlassCard` | `RoundedRectangle(32.dp)` | Card | optional `White 22%` | default |
| Dock | `RoundedRectangle(36.dp)` | Card | `White 34%` (light) / `22%` (dark) | default |
| Notification card | `RoundedRectangle(26.dp)` | Card | `White 14%` (lock screen) / `22%` (notification center) | default |
| Nav bar | `Capsule()` | NavigationBar | `White 50%` (light) / `barContainer` (dark) | default |
| Search box | `Capsule()` | NavigationBar | `White 35%` | default |
| Text input | `RoundedRectangle(32.dp)` | Card | `White 30%` | default |
| Segmented control container | `Capsule()` | BottomBar | `barContainer` | default |
| Segmented control thumb | `Capsule()` | selectionIndicator(p) | `Black 10%` (light) / `White 10%` (dark), fades out with p + `Black 3%×p` | `Highlight.Default×p` / `Shadow×p` / `InnerShadow(8.dp×p)` |
| Bottom bar | `Capsule()` | BottomBar | `barContainer` | default |
| Bottom-bar indicator | `Capsule()` | selectionIndicator(p) | same as above | same as above |
| Toggle track | `Capsule()` | — (solid color `lerp(track, accent, p)`) | — | — |
| Toggle knob | `Capsule()` | toggleKnob(p) | `White ×(1−p)` | `Highlight.Ambient/1.5 @ p`, `Shadow(4.dp, Black 5%)`, `InnerShadow(4.dp×p)` |
| Slider knob | `Capsule()` | sliderKnob(p) | `White ×(1−p)` | same as above |
| Progress bar track | `Capsule()` | Button | `track` | default |
| Progress bar knob | `Capsule()` | sliderKnob(1f) | `Color.White` | `Highlight.Ambient @ 1`, `Shadow(4.dp, Black 5%)` |
| Dialog / popup | `RoundedRectangle(48.dp)` | dialog(theme) | `dialogContainer` | `Highlight.Plain` + default shadow |
| Alert | `RoundedRectangle(48.dp)` | dialog(theme) | `dialogContainer` | `Highlight.Plain` |
| Popup menu | `RoundedRectangle(48.dp)` | dialog(theme) | `dialogContainer` | `Highlight.Plain` |
| Action sheet / bottom popup | `RoundedRectangle(44.dp)` | BottomSheet | `White 50%` | default |
| Picker container | `RoundedRectangle(44.dp)` | BottomSheet | `White 50%` | default |
| Picker selected band | `Capsule()` | `vibrancy()` → `lens(10.dp, 14.dp, CA=true)` | `Black 8%` | `shadow = null` |
| Control center tile | `RoundedRectangle(34.dp)` | controlCenterItem(1f) | `Black 5%` | **gravity highlight**, `shadow = null` |
| Splash-screen panel | `RoundedRectangle(48.dp)` | dialog(theme) | `dialogContainer` | `Highlight.Plain`, with `exportedBackdrop` |
| Toast | `Capsule()` | NavigationBar | `White 50%` (light) / `barContainer` (dark) | default |
| Loading indicator base | `Capsule()` | Button | `White 25%` | default |

---

## 3. iOS System Colors (`IosColors`)

These are the **exact colors** used in upstream components, copied directly ([IosColors.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/core/ios/IosColors.kt)):

| Name | Light | Dark | Source |
|---|---|---|---|
| `AccentLight` / `AccentDark` | `#0088FF` | `#0091FF` | iOS system blue |
| `SwitchAccentLight` / `Dark` | `#34C759` | `#30D158` | `LiquidToggle.kt` |
| `track` | `#787878 @ 20%` | `#787880 @ 36%` | `LiquidToggle.kt` / `LiquidSlider.kt` |
| `barContainer` | `#FAFAFA @ 40%` | `#121212 @ 40%` | `LiquidBottomTabs.kt` / `glass-bottom-bar` |
| `dialogContainer` | `#FAFAFA @ 60%` | `#121212 @ 40%` | `DialogContent.kt` |
| `dim` | `#29293A @ 23%` | `#121212 @ 56%` | `DialogContent.kt` |
| `ControlCenterSurface` | `Black @ 5%` | same | `ControlCenterContent.kt` |
| `content` | `Black` | `White` | — |
| `secondaryContent` | `content @ 68%` | same | — |

> **Theme detection**: `isLightTheme = !isSystemInDarkTheme()` (see `isLightTheme()` in `core/ios/IosColors.kt`).

---

## 4. Three Cheat Sheets: Which Parameter to Change, and Where

### 4.1 Size → Strength (The Most Important Table)

**Refraction strength must be proportional to component size.** The same set of `lens` parameters looks like a water drop on a 56dp button but like no refraction on a 340dp panel.

| Component visual area | Recommended H / A | Examples |
|---|---|---|
| Tiny (knobs, indicators, ≈40×24) | 5 / 10 ～ 10 / 14 | toggle knob, segmented slider |
| Small (buttons, chips, height 32~48) | 12 / 24 | `Button` |
| Medium (nav bar, search box, list items) | 16 / 32 | `NavigationBar`, `Card` |
| Medium (bottom bar, 64 tall full width) | 24 / 24 | `BottomBar` |
| Large (panels, dialogs, control center) | 24 / 48 | `dialog`, `BottomSheet`, `controlCenterItem` |

### 4.2 What Feel You Want → What to Change

| Want | Change | Direction |
|---|---|---|
| Glass thicker, more like a water drop | increase H and A **proportionally** | `16/32 → 24/48` |
| Wider refraction band (whole thing twists) | only increase H | `16/32 → 32/32` |
| Foggier background | add `blur` | `4 → 8 → 16` |
| Glass more "solid" | deepen `onDrawSurface` | `White 5% → 35%` (upper limit) |
| Stronger edge highlight | `highlight.copy(alpha = 1f)` | `0.5 → 1.0` |
| More "thickness" | add `innerShadow(radius, alpha)` | only when pressed |
| More "jelly" | add `depthEffect` | only for large areas |

### 4.3 Theme → Parameter Differences

| Parameter | Light | Dark | Why |
|---|---|---|---|
| `colorControls.brightness` | `0.2` | `0` | light background is bright, needs brightening; dark would overexpose if brightened more |
| `colorControls.saturation` | `1.5` | `1.5` | both need saturation boost |
| `blur` (dialog) | `16.dp` | `8.dp` | too much blur in dark scenes just smears everything |
| `onDrawSurface` | tends white | tends dark container color | see `barContainer` / `dialogContainer` in §3 |

---

## 5. Cross-Check: Which Upstream Parameters Did I Not Copy?

This repo only copied the recipes needed by "presentational components". The upstream Catalog still has several components (e.g., more complex popup combinations,
nav/tutorial-page-specific materials) that this repo doesn't use. If you want to extend:

1. Go to the upstream repo `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/` to find the corresponding file;
2. Copy its `effects` code block **verbatim**;
3. Add a val/fun in [GlassMaterials.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/core/glass/GlassMaterials.kt), **noting the source filename in the comment**;
4. Come back to §1 of this chapter and add a row.

**Don't invent recipes by feel.** The upstream has already tuned the optical parameters to an "Apple flavor"; your intuition is probably worse than theirs.

---

## 6. Rules for Deriving New Recipes (Must Follow)

```
Step 1  Set the size tier        → find the H/A starting point from §4.1
Step 2  Set the "haziness"       → blur: small widgets 2~4, medium 8, large 16
Step 3  Set the "thickness feel" → only consider depthEffect = true for large areas
Step 4  Set "is it dynamic"      → only enable chromaticAberration when dynamic (press/drag)
Step 5  Set the "tint"           → onDrawSurface ≤ 35% (except dialogs, up to 60%)
Step 6  Set highlight/shadow     → Default for directional; Ambient or Plain for large areas
Step 7  Keep the ratio           → A ≈ 2 × H (all upstream recipes satisfy this relation)
```

**Three hard constraints:**

1. The `effects` writing order is always `color → blur → lens`;
2. Any length parameter must use `.dp.toPx()`;
3. No chromatic aberration in the static state.

**Incremental tuning method (recommended)**: change only one parameter at a time, then screenshot and compare. If you change two at once,
you'll never know which one worked — this is the biggest time sink in tuning.

---

## 7. The "Restraint" Ladder of Materials

In Apple's design, materials are used in **layers**: the higher up and more temporary something is, the more "solid" its glass; the more foundational, the more "transparent".

```
Most transparent ───────────────────────────────────────────────► Most solid

Card            BottomBar      NavigationBar     Dialog / Sheet      ControlCenter
16/32           24/24          16/32             24/48 + DE          24/48 + DE
no blur         blur 8         blur 4            blur 8~16          no blur
White 22%       container      White 50%         dialogContainer     Black 5%
                                        ↑                            ↑
                                  temporary, strongly hinted    strongly hinted but extremely restrained

Knob class (Toggle/Slider/Indicator): 5/10 ~ 10/14 — minimal refraction, emphasizes "dynamic" the most
```

**Common mistake: using a knob's small refraction parameters on a large panel, or using a panel's large refraction parameters on a button.**
Refraction strength **follows size**; that's the reason §4.1 exists.

---

## 8. Chapter Summary

- 12 recipes (including 4 dynamic recipes with `progress`) = all materials in this repo, all copied from upstream.
- One material = `shape` + `effects` + `onDrawSurface` + `highlight/shadow`; the four-piece set is indispensable.
- **H/A follows size, A ≈ 2H, order must not be messed up, lengths must be toPx, chromatic aberration only when dynamic.**
- To extend → copy upstream + note the source + register back in this chapter.

**Next step**: `05-iOS-Shell-in-Practice.md` — assemble these materials into a usable "Apple system".
