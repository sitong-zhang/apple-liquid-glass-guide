# 07 · Build and Troubleshooting

> The first six chapters covered "how to make the glass look good"; this chapter covers "**how to actually get it running**".
>
> This chapter is an **engineering manual**: environment versions, build commands, where artifacts land, where to install, and how to fix errors.
> Every version number written here is a combination **verified to compile** in this project — not a suggested value.
>
> ⚠️ **This repository does not include an APK.** This is intentional: the APK is an artifact, not source code.
> To verify this Liquid Glass setup actually runs, build one yourself following this chapter — **if you can build it, the environment is correct**.

---

## 0. Start with this table: the environment verified to pass in this project

| Item | Value | Why this value |
|---|---|---|
| JDK | **17** (verified `17.0.20.1+1` Temurin) | AGP 9 requires at least 17; using 11 fails to start (see §6.1) |
| Android Gradle Plugin | `9.3.2` | `agp` in `gradle/libs.versions.toml` |
| Gradle | `9.7.1` (bundled wrapper) | `gradle/wrapper/gradle-wrapper.properties` |
| Kotlin | `2.4.10` (with Compose compiler plugin) | AGP 9 has built-in Kotlin support; no separate kotlin-android plugin needed |
| compileSdk | `37` | `app/build.gradle.kts` |
| buildToolsVersion | `37.0.0` | must match the version installed in the local SDK |
| targetSdk | `34` | — |
| **minSdk** | **`31`** | **hard floor**, see §1 |
| Compose | `1.12.0` | runtime / ui / foundation / animation, same version |
| **backdrop** | **`io.github.kyant0:backdrop:2.0.0`** | the Liquid Glass engine itself |
| **shapes** | **`io.github.kyant0:shapes:1.2.0`** | Apple G2 continuous curvature (§3.3) |

Artifact size reference: `app-release.apk` ≈ **1.25 MB** (with R8 fully enabled + resource compression).

---

## 1. The only hard gate: `minSdk ≥ 31`

**This is the make-or-break line for whether the whole approach works — confirm it first.**

The foundation of Liquid Glass is **AGSL (Android Graphics Shading Language)**, which compiles shaders at runtime via `android.graphics.RuntimeShader`. `RuntimeShader` was only introduced in **API 31 (Android 12)**.

```
minSdk = 31   ✅ has RuntimeShader, glass runs
minSdk = 30   ❌ no RuntimeShader, glass component crashes on first draw
```

What the crash looks like (on a real device):

```
java.lang.NoClassDefFoundError: Failed resolution of: Landroid/graphics/RuntimeShader;
```

Or after R8 it becomes `NoSuchMethodError` / `VerifyError`.

> **If you must support API 30 and below**, there is only one path: replace all `drawBackdrop` with a degraded implementation of `Modifier.blur()` + semi-transparent `background()`, and branch by version. **That is not Liquid Glass — it is frosted plastic.** This chapter does not cover that route.

Two other runtime prerequisites:

1. **Hardware acceleration must be enabled.** `AndroidManifest.xml` already declares `android:hardwareAccelerated="true"`; shaders run on the GPU, and software rendering fails outright or is extremely slow.
2. **Do not rely on a custom-draw fallback path other than `Modifier.graphicsLayer`**: AGSL is only executed by the GPU pipeline.

---

## 2. What is actually in the repository (must know before building)

```
experiments/
├── README.md                 ← master overview
├── docs/                     ← docs 01~08 (you are reading 07)
└── LiquidGlassShowcase/      ← the compilable project itself
    ├── gradlew / gradlew.bat           ← wrapper (bundles gradle 9.7.1 download config)
    ├── gradle/wrapper/gradle-wrapper.jar       ← ⚠️ must be committed too, otherwise the wrapper won't start
    ├── gradle/libs.versions.toml               ← the single source of all version numbers
    ├── settings.gradle.kts / build.gradle.kts
    ├── gradle.properties                       ← jvmargs / parallel / cache switches
    ├── keystore/liquidglass-demo.jks           ← demo signing (password is liquidglass throughout)
    ├── local.properties                        ← ⚠️ not in git, you must create it yourself (§3.1)
    └── app/
        ├── build.gradle.kts                    ← minSdk / signing / R8 / packaging config
        └── src/main/
            ├── AndroidManifest.xml
            ├── java/com/liquidglass/showcase/  ← all Kotlin source
            └── res/                            ← wallpaper webp / theme / icons
```

**This repository does not include**: APK, `build/`, `.gradle/`, `local.properties`, `~/.gradle` cache. These are all artifacts or local-machine environment that you **generate yourself** after cloning.

---

## 3. Build the APK in three steps

### 3.1 Step 1: Tell Gradle where the Android SDK is

`local.properties` (at the root of `LiquidGlassShowcase/`) is **not in version control** — you must create it yourself:

```properties
# Point to your local Android SDK root directory
sdk.dir=/path/to/android-sdk
```

You can also use an environment variable instead: `ANDROID_HOME=/path/to/android-sdk` (either one works, `local.properties` takes higher priority).

The SDK must actually have two things installed:

```
platforms/android-37.0          ← compileSdk = 37 needs this
build-tools/37.0.0              ← buildToolsVersion needs this
```

If missing, install with `sdkmanager`:

```bash
sdkmanager "platforms;android-37" "build-tools;37.0.0"
```

### 3.2 Step 2: Run the wrapper with JDK 17

```bash
cd LiquidGlassShowcase

# ⚠️ Key: you must explicitly specify JDK 17. The system default java is most likely 11 and will fail directly.
export JAVA_HOME=/path/to/jdk-17

# Artifact: app/build/outputs/apk/release/app-release.apk
./gradlew assembleRelease
```

To just quickly verify the code compiles:

```bash
./gradlew assembleDebug          # no obfuscation, no compression, debug-signed, fastest
./gradlew compileReleaseKotlin   # compile Kotlin only, no packaging, faster
```

**The first build downloads the Gradle 9.7.1 distribution + all dependencies**, so network is required. After that everything goes through the `~/.gradle` cache; in practice the second build onward drops from minutes to seconds.

In a persistent CI/sandbox environment, pinning `JAVA_HOME` into the wrapper is also more convenient:

```properties
# LiquidGlassShowcase/gradle.properties (optional, for convenient handover)
org.gradle.java.home=/path/to/jdk-17
```

### 3.3 Step 3: Confirm the artifact

```bash
ls -lh app/build/outputs/apk/release/
# app-release.apk        ← signed with keystore/liquidglass-demo.jks, directly installable
# output-metadata.json
```

**Success indicator** (what the verified output ends with):

```
BUILD SUCCESSFUL in 4m 1s
43 actionable tasks: 1 executed, 42 up-to-date
```

> Seeing `42 up-to-date` means it was an incremental build — only the one changed task was recompiled. That is normal, and it also confirms `org.gradle.caching=true` is in effect.

---

## 4. Install on a device

```bash
adb install -r app/build/outputs/apk/release/app-release.apk

# list installed packages (applicationId = com.liquidglass.showcase)
adb shell pm list packages | grep liquidglass
```

**The device must meet: Android 12 (API 31) or above + a GPU that supports hardware acceleration.** An emulator works too, but pick an API 31+ image and **enable hardware acceleration (GPU mode)**, otherwise the glass will not render.

After launch you should see: **splash screen (1.6 s Liquid Glass loading animation) → fade-out scale 1.2 s → iOS system shell**. If it goes black or crashes after the splash, troubleshoot in the order of §6.2.

---

## 5. Build configuration explained item by item (read before changing config)

Each block in `app/build.gradle.kts` exists for a reason:

| Config | Value | Why |
|---|---|---|
| `minSdk` | 31 | AGSL floor, see §1 |
| `targetSdk` | 34 | decoupled from compileSdk 37 to avoid too-new behavior changes |
| `isMinifyEnabled` | true | full R8 obfuscation, compresses 1.25 MB to acceptable |
| `isShrinkResources` | true | works with R8 to strip unreferenced resources |
| `signingConfigs.release` | built-in demo keystore | lets `assembleRelease` directly produce an **installable** package, no self-signing needed |
| `packaging.dex.useLegacyPackaging` | true | compatibility with older installers |
| `dependenciesInfo.includeInApk` | false | strip dependency metadata, smaller package |
| `lint.checkReleaseBuilds` | false | don't run lint on release builds, avoid unrelated warnings interrupting CI |
| `vcsInfo.include` | false | don't write VCS info into the APK |

> ⚠️ **The demo keystore is public** (password `liquidglass`, see §7 license). It is only used so "this sample can be installed directly" — **do not use it to sign your own production app**.

---

## 6. Error quick reference

### 6.1 Environment class (build won't start)

| Symptom | Root cause | Fix |
|---|---|---|
| `Android Gradle plugin requires Java 17 to run. You are currently using Java 11.` | default `java` is 11 | `export JAVA_HOME=/path/to/jdk-17`, or write `org.gradle.java.home` into `gradle.properties` |
| `Unsupported class file major version 6x` | same as above, JDK version mismatch | same as above |
| `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local.properties file.` | no `local.properties` and no `ANDROID_HOME` | see §3.1 |
| `Failed to find Build Tools revision 37.0.0` | that build-tools version is not installed in the SDK | `sdkmanager "build-tools;37.0.0"` |
| `Failed to find platform 'android-37'` / `compileSdk 37 not found` | platform 37 not installed in the SDK | `sdkmanager "platforms;android-37"` |
| `Could not resolve io.github.kyant0:backdrop:2.0.0` | no network / proxy not configured | check `HTTP_PROXY` / `HTTPS_PROXY`; confirm `mavenCentral()` is reachable (the repo is configured in `settings.gradle.kts` in §2) |
| `Could not find io.github.kyant0:shapes` | only added backdrop, not shapes | both `libs.versions.toml` + `app/build.gradle.kts` need it (§2) |
| `./gradlew: Permission denied` | wrapper script has no execute bit | `chmod +x gradlew` |
| wrapper won't start / `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` | `gradle/wrapper/gradle-wrapper.jar` was not committed | confirm this file exists in the repo |

### 6.2 Runtime class (crashes / black screen after install)

| Symptom | Root cause | Fix |
|---|---|---|
| `NoClassDefFoundError: android.graphics.RuntimeShader` | `minSdk < 31` or running on a device with API < 31 | switch to an API 31+ device; confirm `minSdk = 31` |
| glass is all-black / fully transparent square | backdrop didn't capture content | **most common**: check whether `Modifier.layerBackdrop(...)` is attached to the node that actually draws the wallpaper (`README` §1 Iron Rule 1, `docs/02-Backdrop-Layered-Architecture` §2) |
| glass shows "the previous frame" | the recording node draws later than the sampling node | the content layer must draw **before** the overlay (`docs/02-Backdrop-Layered-Architecture` §2.4, §6) |
| frame drops on any movement | unconditionally attached `layerBackdrop` | switch to conditional recording (`docs/02-Backdrop-Layered-Architecture` §7, `docs/05-iOS-Shell-in-Practice` §3.2) |
| entire UI is gray, no color | missing `vibrancy()` | add `vibrancy()` as the first line of `effects` (`docs/01-Optical-Model-of-Liquid-Glass` §2) |
| red/blue fringing on edges | chromatic aberration left on in static state | `chromaticAberration = false`, only enable on press/drag (`README` Iron Rule 4) |
| corners look "wrong", like ordinary rounded corners | used `androidx.compose.foundation.shape.RoundedCornerShape` | use `com.kyant.shapes.RoundedRectangle` (G2 continuous curvature, `docs/03-API-Complete-Reference` §3) |
| noticeable heating on real device | recording layer does a full-screen offscreen render every frame | see §8 |

### 6.3 Compile-time class (Kotlin / Compose)

| Symptom | Root cause | Fix |
|---|---|---|
| `Unresolved reference: lens` / `vibrancy` | no matching import in `effects`, or wrong backdrop version | confirm dependency is `2.0.0`; `lens` / `blur` / `vibrancy` are all members of `BackdropEffectScope` |
| `Unresolved reference: RoundedRectangle` | used the foundation package | `import com.kyant.shapes.RoundedRectangle` |
| `@Composable invocations can only happen from ...` | called `rememberLayerBackdrop()` etc. outside a Composable scope | `rememberLayerBackdrop` must be called inside `@Composable` |
| `Type mismatch: inferred type is Float but Dp was expected` (or vice versa) | forgot `.dp.toPx()` | length params for `lens` / `blur` are always `xx.dp.toPx()` (`README` Iron Rule 3) |
| Compose compiler reports version mismatch | Kotlin and Compose compiler plugin versions inconsistent | this project uses AGP 9 built-in Kotlin support + `org.jetbrains.kotlin.plugin.compose` same version (`2.4.10`); do not manually add `composeOptions.kotlinCompilerExtensionVersion` |

### 6.4 The most effective three-step localization method

When something goes wrong, **rule out in this order; 90% of glass problems surface in steps 1–2**:

```
Step 1  Replace the backdrop of some drawBackdrop with a fixed-color EmptyBackdrop / solid-color texture
        → If the glass "lights up", the problem is backdrop recording, not material parameters
Step 2  Temporarily remove the tint in onDrawSurface
        → If refraction appears, the tint was covering the glass (README Iron Rule 6)
Step 3  Temporarily turn off highlight / shadow
        → To confirm whether "wrong edges" is a highlight-parameter issue or a shape issue
```

**Always confirm "is the background captured" first, then tune parameters.** Parameters can't conjure content.

---

## 7. License and copyright (must read before publishing)

| Object | License | What you must do |
|---|---|---|
| `io.github.kyant0:backdrop` | Apache-2.0 ([Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)) | keep NOTICE, declare under Apache-2.0 |
| `io.github.kyant0:shapes` | Apache-2.0 ([Kyant0/AndroidShapes](https://github.com/Kyant0/AndroidShapes)) | same as above |
| files in this project ported from upstream | Apache-2.0 | **do not delete the `Copyright 2025 Kyant` headers in files like `core/utils/*`** |
| new code in this project (`ios/`, `GlassMaterials.kt` cleanup, etc.) | `Copyright 2026 The Liquid Glass Showcase authors` | see `LICENSE` / `NOTICE` |
| `keystore/liquidglass-demo.jks` | demo use only, public password (`liquidglass`) | **do not use for production release signing** |
| icons assets from `ui-icons-hub` | see the license summary table in `docs/06-Icons-and-Asset-Pipeline` §9 | that section has per-pipeline license notes |

---

## 8. How to verify performance (not "does it feel laggy")

The only systematic cost of Liquid Glass is **the `layerBackdrop` recording layer: a full-screen offscreen render every frame**. How to verify:

1. **Developer Options → Profile HW UI rendering (GPU rendering mode analysis)**
   Look at `Draw` / `Sync` in the bar chart. When the glass is static and no overlay is open, the bar height should be clearly lower than when a panel is open.
2. **`adb shell dumpsys gfxinfo com.liquidglass.showcase framestats`**
   Look at the `janky frames` ratio.
3. **Toggle comparison**: temporarily turn off all conditional-recording switches (let `layerBackdrop` stay attached),
   the frame rate should drop noticeably — **that difference is what "conditional recording" saved you**.

Optimization checklist (ordered by benefit, details in `docs/02-Backdrop-Layered-Architecture` §9):

- Conditional recording: **only attach `layerBackdrop` when an overlay actually needs to read it** (biggest benefit);
- keep the recording layer as small as possible, don't make it full-screen by default;
- don't leave it attached on static pages;
- reduce the number of backdrops in the same frame.

---

## 9. "Recreate from scratch" checklist for AI

If you (AI) get this repo and need to get the effect running from scratch, **follow this order strictly**:

```
① Read README.md            → build a mental model (background texture → sampling → refraction), memorize the ten iron rules
② Read docs/01-Optical-Model-of-Liquid-Glass              → understand the mapping of 6 visual cues ↔ 6 APIs
③ Read docs/02-Backdrop-Layered-Architecture              → clarify LayerBackdrop / Combined / feedback-loop prevention
④ Read docs/03-API-Complete-Reference              → go through all parameters of drawBackdrop
⑤ Copy the recipes of docs/04-Material-Recipe-Table → don't invent parameters yourself
⑥ Build a page following the minimal example in docs/03-API-Complete-Reference, confirm the glass displays
   └─ If it doesn't display → go back to §6.4 and rule out step by step, first confirm whether backdrop captured content
⑦ Need a full App shell → read docs/05-iOS-Shell-in-Practice (z-order, backdrop allocation table, conditional recording)
⑧ Need icons            → read docs/06-Icons-and-Asset-Pipeline (difference between the two pipelines + the Apple icon-zone switching scheme)
⑨ Won't compile         → this chapter
```

**Four engineering constraints that must not be violated** (violating them doesn't mean "ugly", it means "won't run"):

1. `minSdk ≥ 31`;
2. build with JDK 17;
3. `effects` order is always `color → blur → lens`;
4. all length params `.dp.toPx()`.

---

## 10. Summary

- **Building itself is simple**: write `sdk.dir` in `local.properties` → point `JAVA_HOME` at JDK 17 → `./gradlew assembleRelease`.
- **Only two kinds of problems will actually block you**: environment versions (§6.1) and backdrop recording (§6.2). The latter takes up 90% of real development time, and the fix is always "**confirm first whether the background was captured**".
- **This repo deliberately excludes the APK**: the APK is an artifact. If you can build that 1.25 MB `app-release.apk`, it means the complete Liquid Glass environment has been successfully recreated.

**At this point, `README` + `docs/01`–`docs/08` cover: optical principles → architecture → API → recipes → shell in practice → assets → build and troubleshooting.**
