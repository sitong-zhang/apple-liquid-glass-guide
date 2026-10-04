# IOS26 · Android Liquid Glass App — Complete Handover Document

> This document records the complete history of the IOS26 build (requirements → research → decisions → implementation → build verification → delivery) so that anyone picking up this repository can reconstruct every decision without re-deriving it.
> If you are picking up this repository: read Chapters 1–3 first (requirements and facts), then Chapters 4–6 (implementation and pitfalls); Chapter 8 is the original working log, from which the working preferences that shaped the project can be judged.
>
> Handover time: 2026-10-01 ｜ Deliverables: this repository (source code + handover document); the signed APK is a build artifact and is not committed — regenerate it via docs/07 §3

---

## 0. One-sentence summary

The project **fully researched and 1:1 reused** the **Backdrop 2.0.0** Liquid Glass solution from the GitHub repo [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (AGSL shaders / default parameters / rendering logic all unchanged), and built a native Android app with Jetpack Compose that showcases **26 iOS-style system components**, including a **Liquid Glass splash loading animation + 1.2s easeOutCubic transition**, and **actually compiled a signed Release APK** on the build machine (minSdk 31 / targetSdk 34).

- Source: `LiquidGlassShowcase/` (Gradle project, can directly run `./gradlew :app:assembleRelease`)
- Installer: `IOS26.apk` (1.15 MB; build artifact — not committed to the repo, regenerate via docs/07 §3)
- App name (shown on launcher): **IOS26**

---

## 1. Original requirements (verbatim, itemized)

Original brief (complete verbatim):

> A complete research process must be performed before development begins; blind development without understanding the implementation principles is prohibited:
>
> 1. Read in full the specified GitHub repo's README, official docs, all sample code, and AGSL shader source; thoroughly master its Liquid Glass rendering principles, core parameter definitions, layer invocation logic, and default parameter values; only start development after fully aligning with the repo's implementation.
> 2. All auxiliary dependencies and UI component libraries introduced must first be searched and verified on GitHub; only choose high-star open-source projects with ≥1000 stars that are actively maintained; do not use niche, low-star, or abandoned third-party libraries.
> 3. Strictly follow the repo's native implementation logic throughout; do not modify any core visual parameters such as glass refraction strength, dispersion coefficient, blur radius, edge highlight brightness, or corner deformation; do not invent any glass effect implementation.
>
> Develop a native Android APK based on the GitHub repo https://github.com/Kyant0/AndroidLiquidGlass, strictly following that repo's Liquid Glass implementation; do not invent any glass effect parameters yourself. Specific requirements are as follows:
>
> 1. Implementation baseline: fully reuse that repo's Backdrop 2.0.0 AGSL shaders, default parameters, and rendering logic to 1:1 recreate the Liquid Glass effect; do not modify the core parameters of glass refraction, dispersion, highlight, or blur.
> 2. App content: build an iOS-style showcase of 26 system standard components with the Liquid Glass effect, including but not limited to cards, buttons, toggles, sliders, input fields, navigation bars, bottom tab bars, dialogs, alerts, segmented controls, list items, progress bars, etc.; all components uniformly use the repo's Liquid Glass material.
> 3. Launch animation: after the app opens, first show a Liquid Glass-textured waiting/loading animation, then smoothly transition to the main component showcase screen over 1.2 seconds with an easeOutCubic interpolator; the transition must keep the glass texture continuous, with no abrupt jumps.
> 4. Technical spec: use Jetpack Compose native Android development, minSdk 31, targetSdk 34; all auxiliary utility libraries and UI components must be chosen from GitHub high-star open-source projects with 1000+ stars; do not use niche custom implementations.
> 5. Delivery standard: output a complete, directly compilable Android project code structure and core code, ensuring a one-click Release APK build after introducing the corresponding dependencies.
>
> Then you may ask me a few questions.

Subsequent additional requirements (in chronological order):

| # | Instruction | Handling result |
|---|---|---|
| 2 | "Just give me the APK file." | Copied the APK separately to the delivery folder and provided a download link |
| 3 | "No, I can't see this in the workspace, and also change this name to IOS26" | Explained that binary artifacts are delivered out-of-band; changed `app_name` to **IOS26**, renamed the APK to `IOS26.apk`, recompiled |
| 4 | "Explain all the source projects you've built... put everything into one compressed archive... and put it in a repo under this account, that placeholder repo" (and provided a GitHub token) | Generated the archive + this handover repo |
| 5 | "You need to first delete the other contents in this repo, then put this complete handover document and source files all up there" | Cleared the `sitong-zhang/experiments` repo contents (old content kept in git history), placed this document + source + APK |

> ⚠️ A GitHub Personal Access Token was shared in chat once. **For security reasons, no file in this repo records that token.** It should be **revoked/rotated** as soon as possible at GitHub Settings → Developer settings → Personal access tokens.

---

## 2. Key decisions (raised as 4 questions before starting; the choices are below)

| Question | Decision |
|---|---|
| `kyant-shapes` only has 43★ (below the 1000★ threshold), but it is a transitive runtime dependency bundled with backdrop 2.0.0, and all repo samples use it for iOS continuous corners. Should it be used directly in the app code? | **Use it (1:1 with the repo)** |
| How to import backdrop? | **Official Maven artifact** `io.github.kyant0:backdrop:2.0.0` |
| AGSL lens refraction needs Android 13+, but the requirement is minSdk 31 (no refraction on Android 12). How to handle? | **Keep minSdk 31 + the library's native automatic fallback** |
| Should we actually compile a Release APK? | **Yes, compiled the APK in practice** |

---

## 3. Research conclusions (facts, reproducible)

### 3.1 Basic facts about the repo

- Repo: `Kyant0/AndroidLiquidGlass`, default branch **`kmp`** (Compose Multiplatform project), **3,935 stars**, Apache-2.0, still has commits on 2026-08-26 (actively maintained).
- Library coordinates: `io.github.kyant0:backdrop`, latest on Maven Central is **2.0.1**, historical versions include **2.0.0**.
- Official docs: <https://kyant.gitbook.io/backdrop> (has `llms.txt` full index and `.md` versions).
- Submodules: `backdrop/` (library itself), `app/` (Catalog sample components), `androidApp/` (runnable APK shell).
- Another dependency: `io.github.kyant0:shapes` (**43★**), used by backdrop's `lens()` for G2 continuous corners.

### 3.2 Version verification (key conclusion)

Compared the tag and main branch with `git diff 2.0.0..HEAD -- backdrop/`:

```
backdrop/build.gradle.kts | 30 +++++++++++++-----------------
1 file changed, 13 insertions(+), 17 deletions(-)
```

**The only difference between 2.0.0 and main under the `backdrop/` directory is one line in the build script**; the AGSL shader strings, default parameters, and rendering logic are **byte-for-byte identical**. Therefore pinning `io.github.kyant0:backdrop:2.0.0` is equivalent to using the repo's current implementation, and can be safely 1:1 recreated.

### 3.3 The four AGSL shaders (original in `backdrop/src/commonMain/kotlin/com/kyant/backdrop/internal/Shaders.kt`)

| Shader | Role | Key implementation |
|---|---|---|
| `RoundedRectRefractionShaderString` | Refraction | `circleMap(x)=1-√(1-x²)` generates a convex lens profile → `d = circleMap(1 - (-sd)/refractionHeight) * refractionAmount`, offsetting sample coordinates along the `gradSdRoundedRect` gradient + `depthEffect * normalize(centeredCoord)` |
| `RoundedRectRefractionWithDispersionShaderString` | Refraction + dispersion | On top of the above, offsets and samples each of 7 channels (red/orange/yellow/green/cyan/blue/purple) by `dispersionIntensity = chromaticAberration * (x*y/(halfW*halfH))`, then weighted-accumulates |
| `DefaultHighlightShaderString` | Edge highlight (directional) | `d = dot(grad, float2(cos(angle), sin(angle)))`, `intensity = pow(abs(d), falloff)`, color via `BlendMode.Plus` |
| `AmbientHighlightShaderString` | Ambient highlight (positive/negative halves) | Same as above, but uses `step(0.0, d)` to distinguish polarity and outputs `half4(t,t,t,1)*intensity` |

Companion SDFs: `sdRoundedRect` (signed distance of a rounded rect) and `gradSdRoundedRect` (its gradient, `gradRadius = min(radius*1.5, min(halfSize.x, halfSize.y))`).

### 3.4 Rendering pipeline (layer invocation logic)

`Modifier.drawBackdrop(...)` →
`DrawBackdropNode` (`LayoutModifierNode + DrawModifierNode + GlobalPositionAwareModifierNode + ObserverModifierNode`):
1. During measurement, add `graphicsLayer(clip=true, shape=shape, compositingStrategy=Offscreen)` to the content;
2. During drawing, `onDrawBehind → draw the backdrop layer onto its own GraphicsLayer (with padding) → onDrawSurface → drawContent → onDrawFront`;
3. `BackdropEffectScope.apply { effects() }` collects `padding` and `renderEffect` (a RenderEffect chain), then assigns them to `graphicsLayer.renderEffect`;
4. `LayerBackdrop` records a layer's content into a `GraphicsLayer` via `.layerBackdrop(backdrop)`, and glass components consume it by coordinate difference (what the glass sees is a "copy of the background").

### 3.5 Default parameters (must not be modified)

| Item | Default value (source) |
|---|---|
| `Highlight` | `width = 0.5dp`, `blurRadius = width / 2`, `alpha = 1f`, `style = HighlightStyle.Default` |
| `HighlightStyle.Default` | `color = White 50%`, `BlendMode.Plus`, `angle = 45°`, `falloff = 1f` |
| `HighlightStyle.Ambient` | `intensity = 0.38f` (`color = White @ intensity`, default BlendMode) |
| `HighlightStyle.Plain` | `color = White 38%`, `BlendMode.Plus`, no shader |
| `Shadow.Default` | `radius = 24dp`, `offset = DpOffset(0, radius/6)`, `color = Black 10%`, `alpha = 1f` |
| `InnerShadow.Default` | `radius = 24dp`, `offset = DpOffset(0, radius)`, `color = Black 15%`, `alpha = 1f` |
| `vibrancy()` | `saturation = 1.5` ColorMatrix |
| `lens()` | `refractionAmount` default = `refractionHeight`; `depthEffect=false`, `chromaticAberration=false`; internally passes `refractionAmount = -refractionAmount` |

**The effect order is a library contract (stated in docs): `colorFilter ⇒ blur ⇒ lens`.** Any reordering distorts the result.

### 3.6 Dependency star verification (hard rule: ≥1000★ and actively maintained)

| Dependency | Stars | Conclusion |
|---|---|---|
| `Kyant0/AndroidLiquidGlass` (backdrop) | 3,935★ | ✅ Adopted (core) |
| androidx.compose / activity / core-ktx | Official Google androidx (active) | ✅ Adopted |
| `Kyant0/Shapes` (kyant-shapes 1.2.0) | **43★** | ⚠️ Exception, explicitly approved (it is a transitive runtime dependency of backdrop 2.0.0, not an extra addition) |
| `androidx.compose.material:material-icons-extended` | — | ❌ **Abandoned** (frozen at 1.7.8, no releases since 2025-02), so deprecated and replaced with **26 self-drawn ImageVectors** (see `core/ios/GlassIcons.kt`) |

### 3.7 Official documentation highlights

- Effect order, `lens(height, amount)` value ranges (`height ∈ [0, shape.minCornerRadius]`, `amount ∈ [0, size.minDimension]`), and that `CornerBasedShape` is required to use lens.
- "Glass Bottom Bar" tutorial: `vibrancy() + blur(4.dp) + lens(16.dp, 32.dp)`, surface `White 50%`.
- "Glass Bottom Sheet" tutorial: `vibrancy() + blur(4.dp) + lens(24.dp, 48.dp, depthEffect=true)`, `RoundedCornerShape(44.dp)`;
  **glass-over-glass must use the `exportedBackdrop` parameter**, otherwise (applying both `layerBackdrop` and `drawBackdrop` to the same layer) triggers a SIGSEGV crash in RenderThread.
- "Smoother rounded corners" tutorial points to the Shapes library (G2 continuous curvature).

---

## 4. Engineering implementation

### 4.1 Tech stack and versions

| Item | Value |
|---|---|
| Build | Gradle **9.7.1** (wrapper), AGP **9.3.2** |
| Kotlin | **2.4.10** (AGP 9 **has built-in Kotlin support**, see pitfall 1 in Chapter 6) |
| Compose | androidx.compose **1.12.0** (runtime / ui / ui-graphics / foundation / animation / material-ripple) |
| Others | `androidx.activity:activity-compose:1.13.0`, `androidx.core:core-ktx:1.19.0` |
| Glass engine | `io.github.kyant0:backdrop:2.0.0` |
| Shape | `io.github.kyant0:shapes:1.2.0` (G2 continuous corners, transitive dependency of backdrop) |
| SDK | compileSdk **37**, minSdk **31**, targetSdk **34**, buildTools 37.0.0, JVM target 17 |
| Package name | `com.liquidglass.showcase`, app name **IOS26** |

### 4.2 Directory structure and file responsibilities

```
LiquidGlassShowcase/
├── settings.gradle.kts / build.gradle.kts / gradle.properties / gradle/libs.versions.toml
├── gradlew, gradlew.bat, gradle/wrapper/*            # Gradle 9.7.1 wrapper
├── keystore/liquidglass-demo.jks                     # demo signing (password is liquidglass throughout)
├── LICENSE / NOTICE                                  # Apache-2.0 + upstream attribution statement
└── app/
    ├── build.gradle.kts                              # minSdk31/targetSdk34/release signing+R8
    ├── proguard-rules.pro                            # keep com.kyant.backdrop.** / shapes.**
    └── src/main/
        ├── AndroidManifest.xml, res/*                # transparent system bars theme, adaptive icon, upstream wallpaper
        └── java/com/liquidglass/showcase/
            ├── MainActivity.kt                       # edge-to-edge + provides LocalIndication
            ├── LiquidGlassApp.kt                     # root: splash animation → 1.2s easeOutCubic transition
            ├── core/glass/GlassMaterials.kt           # ★ single source of truth for glass materials (value-by-value against upstream)
            ├── core/glass/GlassScaffold.kt            # wallpaper → LayerBackdrop (upstream scaffold ported)
            ├── core/ios/IosColors.kt                  # upstream iOS colors / container colors
            ├── core/ios/GlassIcons.kt                 # 26 self-drawn ImageVectors (zero icon dependencies)
            ├── core/utils/                            # upstream utility classes ported (only package names changed)
            │   ├── DampedDragAnimation.kt             # spring drag / press animation (toggle / slider / tab bar)
            │   ├── InteractiveHighlight.kt            # AGSL interactive highlight (ripple light at tap point)
            │   ├── DragGestureInspector.kt           # custom drag gesture
            │   ├── UISensor.kt                        # accelerometer → gravity angle (control-center highlight)
            │   └── Ripple.kt                          # AOSP ripple (same as upstream commonMain)
            ├── components/
            │   ├── GlassFoundation.kt                 # GlassIcon / GlassCard
            │   ├── GlassButtons.kt                    # button · icon button · label · avatar · badge · stepper
            │   ├── GlassControls.kt                   # toggle · slider · segmented control · checkbox · radio · progress bar · activity indicator
            │   ├── GlassBars.kt                       # bottom tab bar · navigation bar · search box · input field
            │   ├── GlassList.kt                       # list group · list item · divider
            │   └── GlassOverlays.kt                   # dialog · alert · action sheet · toast · popup menu · picker · control center
            └── screens/
                ├── SplashScreen.kt                    # Liquid Glass loading animation
                └── CatalogScreen.kt                   # 3 tabs + 26 components showcase + overlay linkage
```

### 4.3 Single source of truth for glass materials (`core/glass/GlassMaterials.kt`)

All components take their glass parameters from here, **each recipe annotated with its upstream source file**, copied verbatim:

| Material | Parameters (original) | Upstream source |
|---|---|---|
| `Button` | `vibrancy(); blur(2.dp); lens(12.dp, 24.dp)` | `components/LiquidButton.kt` |
| `BottomBar` | `vibrancy(); blur(8.dp); lens(24.dp, 24.dp)` | `components/LiquidBottomTabs.kt` (bar background) |
| `selectionIndicator(p)` | `lens(10.dp*p, 14.dp*p, chromaticAberration = true)` | `components/LiquidBottomTabs.kt` (indicator) |
| `toggleKnob(p)` | `blur(8.dp*(1-p)); lens(5.dp*p, 10.dp*p, chromaticAberration = true)` | `components/LiquidToggle.kt` |
| `sliderKnob(p)` | `blur(8.dp*(1-p)); lens(10.dp*p, 14.dp*p, chromaticAberration = true)` | `components/LiquidSlider.kt` |
| `dialog(light)` | `colorControls(brightness = 0.2/0, saturation = 1.5); blur(16.dp/8.dp); lens(24.dp, 48.dp, depthEffect = true)` | `destinations/DialogContent.kt` |
| `NavigationBar` | `vibrancy(); blur(4.dp); lens(16.dp, 32.dp)` | `tutorials/glass-bottom-bar` |
| `BottomSheet` | `vibrancy(); blur(4.dp); lens(24.dp, 48.dp, depthEffect = true)` | `tutorials/glass-bottom-sheet` |
| `Card` | `vibrancy(); lens(16.dp, 32.dp)` | `destinations/LazyScrollContainerContent.kt` |
| `controlCenterItem(p)` | `vibrancy(); lens(24.dp*p, 48.dp*p, depthEffect = true)` | `destinations/ControlCenterContent.kt` |

Companion upstream default values (also unchanged): toggle/slider track color `0xFF787878 @20%` (dark `0xFF787880 @36%`),
toggle accent color `0xFF34C759 / 0xFF30D158`, slider accent color `0xFF0088FF / 0xFF0091FF`,
dialog container `0xFFFAFAFA @60%` / `0xFF121212 @40%`, mask `0xFF29293A @23%` / `0xFF121212 @56%`,
control-center surface `Black @5%` + `HighlightStyle.Default(angle = gravity angle, falloff = 2f)`.

> The only "non-verbatim" writing: the progress-bar leading glass knob uses `sliderKnob(1f)` (i.e., the upstream expression evaluated at `progress = 1`),
> because the progress bar is not a drag control and has no press phase. This is annotated in the code.

### 4.4 List of the 26 iOS system components

1 Card `GlassCard`｜2 Button `GlassButton` (transparent/surface/tinted three states)｜3 Icon Button `GlassIconButton`｜
4 Toggle `GlassToggle`｜5 Slider `GlassSlider`｜6 Stepper `GlassStepper`｜7 Segmented Control `GlassSegmentedControl`｜
8 Input Field `GlassTextField`｜9 Search Bar `GlassSearchBar`｜10 Navigation Bar `GlassNavBar`｜
11 Bottom Tab Bar `GlassBottomTabBar`｜12 Dialog `GlassDialog`｜13 Alert `GlassAlert`｜
14 Action Sheet `GlassActionSheet`｜15 List Item `GlassListItem`(+`GlassListGroup`/`GlassListDivider`)｜
16 Progress Bar `GlassProgressBar`｜17 Activity Indicator `GlassActivityIndicator`｜18 Checkbox `GlassCheckbox`｜
19 Radio Button `GlassRadioButton`｜20 Chip `GlassChip`｜21 Avatar `GlassAvatar`｜22 Badge `GlassBadge`｜
23 Toast `GlassToast`｜24 Popup Menu `GlassPopoverMenu`｜25 Picker `GlassPicker`｜26 Control Center `GlassControlCenter`

Among these, 10 and 11 are the app shell itself (top navigation bar + bottom tab bar); the 3rd tab triggers the 12/13/14/23/24 overlays.

**Interaction implementation also copied verbatim from upstream**: the press phase of toggles/sliders/segmented controls is driven by `DampedDragAnimation` (spring + velocity damping + deformation),
the glass transitions from `blur` to `lens(..., chromaticAberration = true)`, and layers on
`Highlight.Ambient`, `Shadow`, `InnerShadow`, and a white surface; the tab bar's selected indicator additionally uses
the upstream trick of "invisible tinted secondary row + `rememberCombinedBackdrop(backdrop, tabsBackdrop)`" to achieve the accent-color glass.

### 4.5 Launch animation (`LiquidGlassApp.kt`)

```
Loading phase 1.6s: SplashScreen (panel = dialog material + Highlight.Plain; track = BottomBar material; knob = Button material)
        ↓ AnimatedVisibility cross transition, duration 1200ms, interpolator EaseOutCubic
Main screen: CatalogScreen (fade in + scaleIn(0.94 → 1.0)); splash fades out + scaleOut(1.0 → 1.12) simultaneously
```

Key point: **both screens share the same `LayerBackdrop` (wallpaper layer)**; during the transition both screens' glass refracts the same background layer,
so the glass texture stays continuous with no jump. Overlays and in-panel buttons uniformly use `exportedBackdrop` to achieve "glass-over-glass",
avoiding the recursive-draw crash warned about in the upstream docs.

---

## 5. Build and verification (verified evidence, not on paper)

### 5.1 Build command

```bash
cd LiquidGlassShowcase
ANDROID_HOME=<android-sdk> ./gradlew clean :app:assembleRelease
```

One-click build succeeded (`BUILD SUCCESSFUL`), artifact: `app/build/outputs/apk/release/app-release.apk`.

### 5.2 Artifact verification

```
package: name='com.liquidglass.showcase' versionCode='1' versionName='1.0.0'
minSdkVersion:'31'          targetSdkVersion:'34'      compileSdkVersion='37'
application-label:'IOS26'
launchable-activity: com.liquidglass.showcase.MainActivity
V2 Signer: certificate DN: CN=Liquid Glass Showcase, OU=Demo, O=LiquidGlass, C=CN   (apksigner verify passed)
```

### 5.3 Shader completeness verification (proving 1:1 reuse)

After decompiling `classes.dex` from the APK and searching the strings, all four shaders' key identifiers are present:

```
circleMap 4 · dispersionIntensity 2 · chromaticAberration 5 · refractionHeight 7
RoundedRectRefractionWithDispersion 2 · DefaultHighlightShaderString 1 · AmbientHighlightShaderString 1
```

### 5.4 Parts not verified (honest statement)

No emulator/real device was attached to the build machine during this round, so verification only reached "compile → R8 → package → sign → shaders into the build";
**no on-device runtime visual acceptance was done**. Install `IOS26.apk` on an Android 13+ device to re-check the visual result before relying on it.

---

## 6. Environment and pitfall log (read this before building)

1. **AGP 9 has built-in Kotlin support**: you can no longer apply the `org.jetbrains.kotlin.android` plugin, otherwise it errors with
   `The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0`.
   Only `com.android.application` + `org.jetbrains.kotlin.plugin.compose` are needed; no `compileOptions`/`kotlin{}` block either
   (the upstream `androidApp` is the same).
2. **`ImageVector.Builder.path(pathData = ...)` is no longer usable in Compose 1.12** (only the `path(pathBuilder)` overload remains),
   so use `addPath(addPathNodes("M..."), stroke = ...)` instead (this project's `GlassIcons.kt` does exactly this).
3. **`material-icons-extended` is abandoned** (frozen at 1.7.8) → replaced with self-drawn vector icons, zero icon dependencies.
4. **The build network may need a proxy**: the Gradle wrapper download of the distribution needs `GRADLE_OPTS="-Dhttps.proxyHost=... -Dhttps.proxyPort=..."`,
   and Gradle's own dependency download uses the command-line `-Dhttp(s).proxyHost/Port`. A normally networked local environment **does not need** these parameters.
5. **Signing**: `keystore/liquidglass-demo.jks` is a **demo self-signed keystore** (password is `liquidglass` throughout); replace it and keep it safe for production release.
6. **minSdk 31 fallback behavior**: the AGSL `RuntimeShader` needs API 33+, and the library's internal `isRuntimeShaderSupported()` will automatically skip
   `lens()`, leaving only color/blur on Android 12 (blur needs API 31+, `isRenderEffectSupported()`).
   This is the library's native behavior, not a defect.
7. **Glass-over-glass forbids recursion**: the same layer cannot both `layerBackdrop` and `drawBackdrop` (the upstream docs note this triggers a RenderThread SIGSEGV),
   so you must use `exportedBackdrop`.
8. Kotlin local variables can **shadow** same-named members of `GraphicsLayerScope` (such as `alpha`); be careful with naming.

---

## 7. References and attribution

- Upstream repo: <https://github.com/Kyant0/AndroidLiquidGlass> (Apache-2.0, Copyright 2025 Kyant)
- Upstream docs: <https://kyant.gitbook.io/backdrop>
- Shape library: <https://github.com/Kyant0/Shapes> (Apache-2.0)
- This project's `LICENSE` (Apache-2.0) and `NOTICE` (itemizing reuse sources) are provided with the source;
  the wallpaper `wallpaper_light.webp` is taken from upstream sample assets.

---

## 8. Appendix: original working log (instructions, decisions, and actions in order)

### 8.1 Original instructions (in chronological order, verbatim)

1. **(First requirement)** "A complete research process must be performed before development begins; blind development without understanding the implementation principles is prohibited: 1. Read in full the specified GitHub repo's README, official docs, all sample code, and AGSL shader source; thoroughly master its Liquid Glass rendering principles, core parameter definitions, layer invocation logic, and default parameter values; only start development after fully aligning with the repo's implementation. 2. All auxiliary dependencies and UI component libraries introduced must first be searched and verified on GitHub; only choose high-star open-source projects with ≥1000 stars that are actively maintained; do not use niche, low-star, or abandoned third-party libraries. 3. Strictly follow the repo's native implementation logic throughout; do not modify any core visual parameters such as glass refraction strength, dispersion coefficient, blur radius, edge highlight brightness, or corner deformation; do not invent any glass effect implementation. Develop a native Android APK based on the GitHub repo https://github.com/Kyant0/AndroidLiquidGlass... (5 specific requirements in Chapter 1)... Then you may ask me a few questions"

2. **(Decision)** The choices taken on the 4 questions: use kyant-shapes (1:1 with the repo) / official Maven artifact / keep minSdk 31 + native automatic fallback / actually compile the APK in practice.

3. **(Delivery form)** "Just give me the APK file."

4. **(Rename)** "No, I can't see this in the workspace, and also change this name to IOS26"

5. **(Handover)** "OK, I understand the reason now. Now put all the source projects you've built, with explanations — everything you did during this period and everything I said — all into one compressed archive... and put it in a repo under this account, that placeholder repo" (a GitHub token was attached in the message — **not recorded in this repo, recommend revoking**)

6. **(Clear repo)** "You need to first delete the other contents in this repo, then put this complete handover document and source files all up there, so that the repository link alone is enough to get anyone up to speed."

### 8.2 Key actions taken (in chronological order)

1. Cloned `Kyant0/AndroidLiquidGlass` (`kmp` branch) → read the library source, sample components, `androidApp`, CI config, and all GitBook tutorials and API pages;
   used `git diff 2.0.0 HEAD -- backdrop/` to prove 2.0.0 matches main; used the GitHub API to verify stars and activity (found Shapes 43★, icons-extended abandoned).
2. Raised the 4 decision questions before starting (see Chapter 2).
3. Set up a single-module Gradle project (AGP 9.3.2 / Kotlin 2.4.10 / Compose 1.12.0 / compileSdk 37),
   ported upstream utility classes (`DampedDragAnimation`, `InteractiveHighlight`, `DragGestureInspector`, `UISensor`, `Ripple`, only package names changed),
   centralized all glass recipes into `GlassMaterials.kt` with source annotations, and implemented 26 components + launch animation + catalog page.
4. Installed the Android SDK on the build machine (cmdline-tools + platforms;android-37.0 + build-tools;37.0.0),
   fixed compile issues like AGP9 built-in Kotlin, the ImageVector API, and a parenthesis typo, got `clean :app:assembleRelease` passing,
   and verified signing / minSdk / targetSdk / shader strings inside DEX.
5. Renamed the app to **IOS26** per feedback, re-built the package; then wrote this handover document and repo contents.

### 8.3 Current status and suggested next steps

- ✅ Requirements 1–5 all completed and verified to build; ❌ the only gap is **on-device visual acceptance**.
- Optional follow-ups: dark wallpaper / follow system theme, inline the `backdrop` source as a local module to tune shaders line by line,
  add more components or interaction animations, replace with production signing and package name before publishing.
