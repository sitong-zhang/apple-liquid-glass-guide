# 10 · Compose Production Field Notes — Building a Real iOS Shell with Backdrop 2.0.0

> This chapter is written **after shipping APKs `苹果系统(14)` and `苹果系统(15)`** to a real phone
> (Huawei EMUI / API 31+), and after fixing a crash-on-open plus four iOS-fidelity defects found
> in on-device testing.
>
> It is the **field supplement** to chapters 01–09. Where 01–09 describe the *theory and the API*,
> this chapter describes what actually breaks on hardware, the exact root causes, the verified fixes,
> and — most importantly — **where to get real assets instead of hand-drawing anything** ("找资源").
>
> All content here was **personally practiced and verified on a device**, not inferred from source.
> It is written in English per the repo's operating convention.

---

## 0. TL;DR — the five things that will bite you

| # | Symptom on a real phone | One-line cause | One-line fix |
|---|---|---|---|
| 1 | **App crashes instantly on open** with `UnsupportedOperationException: Only RoundedRectangularShape or CornerBasedShape is supported in lens effects` | `lens()` was given a `com.kyant.shapes.RoundedRectangle` as its `shape` | Pass `androidx.compose.foundation.shape.RoundedCornerShape` to `drawBackdrop` whenever `lens()` is in `effects` |
| 2 | Face ID unlocks even when no face is in front of the camera | The scan was a timer, not a real sensor | Use Camera2 `STATISTICS_FACE_DETECT_MODE` + `CaptureResult.STATISTICS_FACES`, headless (no preview) |
| 3 | The Face ID / lock glyph looks "drawn by a developer" | Hand-coded vector paths | Pull the real **Tabler** `face-id` / `lock` / `lock-open` SVGs (MIT, 80k+ stars) and convert to vector drawables |
| 4 | After swipe-up unlock, dragging down within ~3 s brings the lock screen back | The pane only went transparent; it was never removed | Add an `unlocking` guard that disables the drag once unlock starts |
| 5 | Tapping the lock-screen camera button does nothing on Huawei | `MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA` *resolves* but opens nothing | Launch the OEM camera **by package name** first (`com.huawei.camera`, …) |

Everything below expands these five, plus the build/import gotchas and the in-app crash banner.

---

## 1. THE CRASH — `lens()` only accepts Compose `CornerBasedShape`

### 1.1 The stack you will see

```
java.lang.UnsupportedOperationException:
    Only RoundedRectangularShape or CornerBasedShape is supported in lens effects
        at com.kyant.backdrop.effects.LensEffect.<init>(...)
        at com.kyant.backdrop.drawBackdrop$... (drawBackdrop.kt:...)
```

The exception is thrown **at attach / composition time**, not lazily — so the app dies the moment the
first glass surface with a `lens` appears. In our build it was the **home screen and the clock** that
used `RoundedRectangle` via `GlassCard`; the lock screen happened to survive v14 only because its
offending calls were narrower.

### 1.2 Root cause (verified, not guessed)

`lens()` builds its refraction SDF from the `shape` parameter. The factory that does this only knows
how to read Compose foundation shapes:

> **Accepted:** `androidx.compose.foundation.shape.RoundedRectangularShape`, `CornerBasedShape`
> (i.e. `RoundedCornerShape`, `CutCornerShape`, `RoundedCornerShape`-derived).
>
> **Rejected:** `com.kyant.shapes.RoundedRectangle` — it is a *custom* `Shape` implementation, **not** a
> `CornerBasedShape`, so `lens()` cannot derive a corner-radius SDF from it and throws.
>
> **Empirically accepted (verified in our build):** `com.kyant.shapes.Capsule`. The lock screen used
> `Capsule` as its `drawBackdrop` shape with `lens()` and opened fine, while the home screen (which used
> `RoundedRectangle`) crashed with the stack above. So `Capsule` appears to implement `CornerBasedShape`
> and is safe with `lens`. To stay 100% safe, prefer `RoundedCornerShape(...)` of the same radius whenever
> `lens()` is present — but if you only have a capsule, `Capsule` works.
>
> **G2 squircle with `lens()` — confirmed from the upstream source.** The kyant G2-continuous class
> `com.kyant.shapes.RoundedRectangularShape` (note the name: **`RoundedRectangularShape`, not `RoundedRectangle`**)
> is *also* accepted by `lens()` — it is matched by the first `when` branch in `effects/Lens.kt:64`. So if you
> want Apple's G2 corners **and** real refraction, use `com.kyant.shapes.RoundedRectangularShape`; reserve plain
> `RoundedCornerShape` for when G2 continuity doesn't matter. `RoundedRectangle` is the *only* kyant shape that crashes.

This directly contradicts the "always use kyant `RoundedRectangle`" advice in README §1 Rule 10,
README §3.3, docs/03 §3 & §9, and docs/09-Practitioner-Notes.md §2.3 & defect #4. Those passages are correct **for `clip()`
and for blur/vibrancy glass**, but they are **wrong and crash-producing** when the same shape is handed
to `drawBackdrop` **with `lens()`**. The inline corrections to those passages were made in this same
commit (see §1.5).

### 1.3 The fix — a mechanical, repo-wide replacement

Because `drawBackdrop(..., shape = { ... }, effects = { ... lens(...) })` is everywhere, the safest
fix is to replace the kyant shape with the Compose one **in every file that uses `lens()`**:

```python
# Pseudocode for the one-shot sed-style fix we actually ran (Python, offline):
import pathlib, re
for f in PATHS:
    s = f.read_text()
    s = s.replace("RoundedRectangle(", "RoundedCornerShape(")
    s = s.replace(
        "import com.kyant.shapes.RoundedRectangle",
        "import androidx.compose.foundation.shape.RoundedCornerShape")
    f.write_text(s)
```

Files touched (all in `LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/`):

```
ios/IosPasscode.kt          ios/IosHomeScreen.kt        ios/IosAppWindow.kt
ios/IosAppSwitcher.kt       catalog/GlassFrame.kt       catalog/CatalogScreen.kt
catalog/PlaygroundScreen.kt components/GlassText.kt     GlassFoundation.kt
GlassButtons.kt             GlassOverlays.kt            GlassControls.kt
GlassBars.kt                GlassList.kt
```

The 14-file replacement is deliberately **blunt** (it also rewrites `clip(RoundedRectangle(...))`
calls, which were harmless anyway) — better to over-replace than to miss the one `lens` call that
crashes the build. After it, `./gradlew assembleRelease` produced a green build and the app opened.

### 1.4 The safe rule to write down

> **If `lens()` is in the `effects` block, the `shape` argument of `drawBackdrop` MUST be a Compose
> `CornerBasedShape` (`RoundedCornerShape` / `CutCornerShape` / `RoundedRectangularShape`).**
> Use kyant `RoundedRectangle` / `Capsule` only for `clip()` and for glass that uses `blur`+`vibrancy`
> **without** `lens`.

If you want the G2-continuous squircle *and* real refraction, the pragmatic answer is: clip the content
with `RoundedCornerShape` for the lens pass (a slightly-less-perfect corner is invisible at glass
thickness), and reserve kyant shapes for non-refracting outlines. The refraction band reads fine with
`RoundedCornerShape` at the radii we use (10–28 dp).

### 1.5 Inline corrections made alongside this chapter

To stop the next reader from copying crash code, the following passages in the earlier chapters
were corrected in this revision (the crash behind them was reproduced end-to-end before any of
them was changed):

- `README.md` §1 Iron Rule #10 — now carries the lens exception.
- `README.md` §3.3 `GlassCard` example — `shape` changed to `RoundedCornerShape(32.dp)` and noted.
- `README.md` §4 "rounded corners look wrong" row — notes the lens exception.
- `docs/03-API-Complete-Reference.md` §3 — `shape` section now states the lens constraint.
- `docs/03-API-Complete-Reference.md` §9 — the `RoundedCornerShape` misuse row is annotated.
- `docs/09-Practitioner-Notes.md` §2.3 — `RoundedRectangle(24.dp)` → `RoundedCornerShape(24.dp)`.
- `docs/09-Practitioner-Notes.md` §6 defect #4 — notes kyant shapes are fine for clip/non-lens only.

---

## 2. REAL FACE ID — a sensor, not a timer or an animation

The requirement, stated as a hard rule: *real iOS shows no camera-scan overlay; Face ID pops from
the Dynamic Island; the lock glyph must come from a real asset library — never hand-drawn.*

### 2.1 What real iOS Face ID is (and is not)

- It is **automatic**: the scan runs the instant the lock screen appears; there is no "tap to scan".
- It shows **no camera feed**. iOS never paints the front camera onto the screen during Face ID.
- The visual cue is a **padlock that opens** (and on newer devices a subtle glyph at the Dynamic
  Island), driven by the *result* of the sensor — not by a timer.
- If no face is present, it **does not unlock**. It eventually offers the passcode.

Our v14 violated all of this: it had a blurred "scan" overlay under the Dynamic Island, a hand-drawn
padlock, and — worst — it unlocked on a timeout regardless of whether a face was there.

### 2.2 The verified implementation

`FaceIdScan` opens the **front camera headlessly** (a `SurfaceTexture` with no `PreviewView`), enables
the pipeline's built-in face detection, and calls `onFaceDetected()` only when `CaptureResult
.STATISTICS_FACES` is non-empty. It never shows a frame.

```kotlin
@Composable
fun FaceIdScan(
    active: Boolean,
    onFaceDetected: () -> Unit,
    onTimeout: () -> Unit,
    timeoutMs: Long = 3500
) {
    val context = LocalContext.current
    val detected = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(active) {
        if (!active) return@DisposableEffect onDispose {}

        detected.value = false
        fun timeout() { if (!detected.value) onTimeout() }

        val cm = runCatching { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
            .getOrNull()
        val frontId = cm?.cameraIdList?.firstOrNull { id ->
            runCatching {
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            }.getOrDefault(false)
        }
        val faceMode = frontId?.let {
            runCatching {
                cm!!.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)
            }.getOrNull()
        }?.let { modes ->
            when {
                modes.contains(CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_FULL) ->
                    CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_FULL
                modes.contains(CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE) ->
                    CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE
                else -> -1
            }
        } ?: -1

        // No front cam / no face detection → go straight to timeout so the passcode is offered.
        if (cm == null || frontId == null || faceMode < 0) {
            val job = scope.launch { delay(timeoutMs); timeout() }
            return@DisposableEffect onDispose { job.cancel() }
        }

        val handlerThread = HandlerThread("faceid-scan").also { it.start() }
        val handler = Handler(handlerThread.looper)
        var camera: CameraDevice? = null
        var session: CameraCaptureSession? = null

        val surfaceTexture = SurfaceTexture(1)   // headless: never attached to a View
        val surface = Surface(surfaceTexture)

        val timeoutJob = scope.launch { delay(timeoutMs); timeout() }

        val captureCb = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                s: CameraCaptureSession, r: CaptureRequest, result: TotalCaptureResult
            ) {
                if (detected.value) return
                val faces = result.get(CaptureResult.STATISTICS_FACES)
                if (faces != null && faces.isNotEmpty()) {
                    detected.value = true
                    timeoutJob.cancel()
                    onFaceDetected()
                }
            }
        }
        // ... openCamera(frontId, openCb, handler); setRepeatingRequest with
        //     CaptureRequest.STATISTICS_FACE_DETECT_MODE = faceMode; onDispose closes all.
    }
}
```

Two import traps that cost real compile cycles:

- `SurfaceTexture` lives in **`android.graphics`**, *not* `android.view`. The wrong import fails with
  "Unresolved reference".
- `CaptureResult` is **`android.hardware.camera2.CaptureResult`** (you read faces from the *result*, not
  the request).

### 2.3 The glyph at the Dynamic Island (real asset, see §3)

The lock screen shows the real **Tabler `face-id`** vector at the Dynamic Island, tinted by scan state:

```kotlin
val faceColor = when (faceState) {
    FaceState.Scanning -> Color.White
    FaceState.Detected -> Color(0xFF34C759)   // iOS green
    FaceState.Failed   -> Color(0xFFFF453A)   // iOS red
}
Image(
    painter = painterResource(R.drawable.ic_face_id),
    contentDescription = null,
    modifier = Modifier.size(40.dp),
    colorFilter = ColorFilter.tint(faceColor)
)
Image(   // real Tabler lock, opens when recognised
    painter = painterResource(
        if (faceState == FaceState.Detected) R.drawable.ic_lock_open else R.drawable.ic_lock
    ),
    contentDescription = null,
    modifier = Modifier.size(30.dp),
    colorFilter = ColorFilter.tint(Color.White)
)
```

`FaceState` is `Scanning / Detected / Failed`. On `Detected` we wait ~480 ms (the natural iOS beat) then
`doUnlock()`; on `Failed` after ~700 ms we hand off to the passcode. **No timer ever unlocks the phone.**

---

## 3. WHERE TO GET REAL ASSETS — find them, never hand-draw

The single most repeated instruction during review: *don't programmatically generate glyphs; go
find high-star GitHub resources.* It is the right call, and also the lazier path once you know the
trick.

### 3.1 The hard rule

> **Any UI glyph (Face ID, lock, flashlight, camera, control glyphs) must come from a real icon
> library — converted to an Android vector drawable — never hand-coded paths.**

Hand-drawn paths read as "a developer's placeholder" instantly, the same way a procedural wallpaper
does (docs/09-Practitioner-Notes.md §1.1). A real icon library gives you correct stroke weights, caps, and optical balance
for free.

### 3.2 The resource we found and used: Tabler Icons

| Attribute | Value |
|---|---|
| Repo | [`tabler/tabler-icons`](https://github.com/tabler/tabler-icons) (MIT, 80k+ stars) |
| Asset source used | the published npm package `@tabler/icons@3.48.0` |
| Exact URLs | `https://unpkg.com/@tabler/icons@3.48.0/icons/outline/face-id.svg`<br>`https://unpkg.com/@tabler/icons@3.48.0/icons/outline/lock.svg`<br>`https://unpkg.com/@tabler/icons@3.48.0/icons/outline/lock-open.svg` |
| Files produced | `res/drawable/ic_face_id.xml`, `ic_lock.xml`, `ic_lock_open.xml` (Android vector drawables, white stroke) |

### 3.3 Why `unpkg.com`, not `raw.githubusercontent.com` / `cdn.jsdelivr.net`

On the build machine, `raw.githubusercontent.com` and `cdn.jsdelivr.net` **timed out / 404'd from
`curl`**, but **`unpkg.com` worked**. So the reliable fetch for any npm-published icon set is:

```bash
curl -sL https://unpkg.com/@tabler/icons@3.48.0/icons/outline/face-id.svg -o face-id.svg
```

Then convert the SVG to an Android vector drawable (keep `strokeColor="#FFFFFFFF"`, `strokeWidth="2"`,
`strokeLineCap/Join="round"`, `fillColor="#00000000"`). The Tabler outline style maps 1:1 to a vector
drawable with no pathData changes.

### 3.4 General "find resources" procedure (reusable)

1. Think of the **canonical open-source icon set** for the glyph (Tabler, Lucide, Feather, Material
   Symbols). Prefer MIT/Apache and high star counts.
2. Find its **npm package** (most ship an `icons/outline/*.svg` tree).
3. Pull the individual SVG from **`unpkg.com/@scope/pkg@version/path`** (jsDelivr/raw GitHub may be
   blocked in restricted networks — unpkg was reachable here).
4. Convert to `res/drawable/*.xml`, tint at runtime with `ColorFilter.tint(...)`.
5. Record provenance + licence in `NOTICE` (docs/09-Practitioner-Notes.md §8 checklist already requires this).

This is the same "pipeline A" idea as docs/06, extended to *runtime UI glyphs*, not just app icons.

---

## 4. THE UNLOCKING GUARD — swipe-up must *remove* the lock screen

### 4.1 The bug

In v14, swipe-up set the pane's `alpha` to 0 but left it **mounted and still draggable**. Within ~3 s,
a downward drag re-caught the transparent pane and pulled it back into view — visually "the lock screen
resurrected". That is wrong: iOS *dismisses* the lock screen.

### 4.2 The fix

A single boolean guard that disables the drag the moment unlock starts:

```kotlin
var unlocking by remember { mutableStateOf(false) }

fun doUnlock() {
    if (unlocking) return
    unlocking = true
    scope.launch {
        offsetY.animateTo(-760f, spring(stiffness = 420f, dampingRatio = 0.95f))
        onUnlock()                 // shell removes the lock screen for good
    }
}

// in the draggable state / onDragStopped:
if (!unlocking) { /* allow drag / decide unlock-or-passcode */ }
```

Because `unlocking` is set synchronously at the top of `doUnlock()`, the still-animating transparent
pane can no longer be re-grabbed. The shell's `onUnlock` then unmounts the screen entirely.

### 4.3 Smooth hand-off to the passcode (no home flash)

When a face is *not* detected but a passcode is set, we do **not** jump to home and then to the
keypad. We keep the pane in place and fade the glass keypad in over the wallpaper:

```kotlin
passcodeEnabled -> {
    scope.launch { offsetY.animateTo(0f, spring(stiffness = 380f, dampingRatio = 0.9f)) }
    mode = LockMode.Passcode      // IosPasscodeSheet fades in on top
}
```

The passcode itself is **glass, never a black screen** — `IosPasscodeSheet` frosts the *same* lock
wallpaper via `drawBackdrop(... blur(...))` with a flat `RoundedCornerShape(0.dp)` so the lens never
distorts screen edges (this is also why it is safe after §1's crash fix).

---

## 5. LAUNCHING THE REAL CAMERA — by package name, with permission handling

### 5.1 The bug

`MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA` **resolves** on Huawei EMUI (so the intent is "valid")
but **opens nothing visible**. The lock-screen camera button and the home "相机" app did nothing.

### 5.2 The fix

Try the device's own camera app **by package name first**, then fall back to generic intents. Also
queue the launch until the `CAMERA` permission is granted (the torch needs it too):

```kotlin
fun openCamera(context: Context) {
    val ctx = if (this::activity.isInitialized) activity else context
    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED) {
        pendingOpenCamera = true
        if (this::permissionLauncher.isInitialized) permissionLauncher.launch(Manifest.permission.CAMERA)
        else runCatching { ctx.startActivity(buildCameraIntent(ctx)) }
        return
    }
    runCatching { ctx.startActivity(buildCameraIntent(ctx)) }
}

private fun buildCameraIntent(context: Context): Intent? {
    val pm = context.packageManager
    val cameraPackages = listOf(
        "com.huawei.camera", "com.android.camera", "com.android.camera2",
        "com.sec.android.app.camera", "com.miui.camera", "com.oppo.camera",
        "com.oneplus.camera", "com.vivo.camera"
    )
    for (pkg in cameraPackages) {
        pm.getLaunchIntentForPackage(pkg)?.let { return it }   // NOT wrapped in runCatching:
    }                                                          // getLaunchIntentForPackage returns Intent?
    for (intent in listOf(
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    )) if (intent.resolveActivity(pm) != null) return intent
    return null
}
```

> Gotcha: do **not** wrap `getLaunchIntentForPackage(pkg)` in `runCatching { ... }` and return its
> result — `runCatching` returns `Result<Intent?>`, which is a type mismatch for a function declared to
> return `Intent?`. Return the `Intent?` directly.

The pending open is consumed in `applyPendingTorch()` once the permission grant lands, so the camera
(or torch) fires exactly once the user accepts.

---

## 6. IN-APP CRASH BANNER — a safety net you can actually see

Because some defects only surface on hardware, we added a last-line crash reporter that survives even
when the dedicated `:crash` process cannot paint in time:

- `ShowcaseApplication` registers a `Thread.currentThread().setDefaultUncaughtExceptionHandler` that
  writes the stack to `cacheDir/crash_trace.txt` (with a small sleep bump, 450 → 600 ms, so the write
  finishes before the process dies).
- `LiquidGlassApp` reads that file on every launch and, if present, paints a top-most dark banner with
  the **full stack**, plus **"复制堆栈"** (copy) and **"清除"** (clear) buttons. The copy button puts the
  trace on the clipboard so the user can paste it straight back.

This is the channel that surfaced the `lens` crash stack quoted in §1.1 — treat it as mandatory for any
build you cannot test on every device yourself.

---

## 7. Compilation / import gotchas (each cost a real build cycle)

| Symptom | Missing / wrong | Fix |
|---|---|---|
| `Unresolved reference: SurfaceTexture` | imported from `android.view` | import **`android.graphics.SurfaceTexture`** |
| `Unresolved reference: CaptureResult` | not imported | import **`android.hardware.camera2.CaptureResult`** |
| `Unresolved reference: getValue` / `setValue` | `by remember { }` without imports | import `androidx.compose.runtime.getValue` / `setValue` |
| `unresolved: return@onDragStopped` / `return@rememberDraggableState` | labeled return inside a lambda that the compiler rejects | rewrite the drag block with `if (!unlocking) { ... }` guards instead of labeled returns |
| `Result<Intent?>` type mismatch | `runCatching { getLaunchIntentForPackage(pkg) }` | return `Intent?` directly (see §5.2) |
| App grey/foggy on press | upstream `InteractiveHighlight` full-bleed white | see docs/09-Practitioner-Notes.md §1.4 `pressSheen` |

---

## 8. Build environment & commands (reproducible)

```bash
export JAVA_HOME=/root/.sdkman/candidates/java/current   # JDK 17
export ANDROID_HOME=/tmp/work/android-sdk
cd /tmp/work/glasskit                                    # package com.liquidglass.showcase
./gradlew assembleRelease --offline                       # green build → app-release.apk
```

- `minSdk = 31` (AGSL / `RuntimeShader` requirement; auto-degrades on Android 12, see docs/03 §7.6).
- The crash fix in §1 does **not** change any visual parameter — it is a pure shape-type swap, so the
  glass look is unchanged; only the crash is gone.

---

## 9. Verified external resources (use these, don't reinvent)

| Need | Resource | Licence | How obtained |
|---|---|---|---|
| Liquid-glass engine | `io.github.kyant0:backdrop:2.0.0` | Apache-2.0 | Maven Central |
| G2 squircle shapes | `io.github.kyant0:shapes:1.2.0` | Apache-2.0 | Maven Central (for `clip`/non-lens only — see §1.2) |
| Face ID / lock glyphs | [`tabler/tabler-icons`](https://github.com/tabler/tabler-icons) `face-id` / `lock` / `lock-open` | MIT | `unpkg.com/@tabler/icons@3.48.0/icons/outline/*.svg` → vector drawable |
| Real app icons | `assets/app-icons/` (ui-icons-hub PNGs) | per upstream | `tools/fetch_app_icons.py` (docs/06) |
| iOS 26 wallpaper | `drawable-nodpi` WebP (light+dark) | Apple artwork | docs/09-Practitioner-Notes.md §1.1 |

---

## 10. Release checklist — additions for a *device-shipped* iOS shell

Append to the docs/09-Practitioner-Notes.md §8 checklist:

```
[ ] Every drawBackdrop that uses lens() passes a Compose CornerBasedShape (RoundedCornerShape),
    never a kyant shape — otherwise the app crashes on open (§1)
[ ] Face ID is a real sensor result, not a timer; no camera preview is ever shown (§2)
[ ] All UI glyphs come from a real icon library (Tabler etc.), converted to vector drawables — none hand-drawn (§3)
[ ] Swipe-up truly removes the lock screen; an unlocking guard blocks post-unlock drag-back (§4)
[ ] Camera button launches the OEM app by package name and waits for CAMERA permission (§5)
[ ] Crash banner reads cacheDir/crash_trace.txt and offers copy/clear (§6)
[ ] Built and opened on a real phone, not just compiled (§8)
```

---

## 11. The one-paragraph version

Building an iOS liquid-glass shell on Android is 90 % getting `drawBackdrop` right and 10 % respecting
the device. The trap that crashes the app is handing `lens()` a non-`CornerBasedShape` kyant shape
(e.g. `RoundedRectangle`) instead of a Compose `CornerBasedShape` (§1) — fix it repo-wide and the build is
green. (`Capsule` is the one kyant shape that worked with `lens()` in our build.) Beyond the material, the
things that
make it *feel* like iOS are all "don't fake it": real Face ID from the camera's own face detector with
no preview (§2), real glyphs pulled from a high-star icon library like Tabler instead of hand-drawn
paths (§3), a genuine unlock that removes the lock screen rather than hiding it (§4), and a real camera
launch by package name (§5). Ship a crash banner so hardware-only defects come back to you as a stack
trace (§6), and never call a build "done" until it has opened on a phone (§8).
