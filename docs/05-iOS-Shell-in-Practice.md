# 05 · iOS Shell in Practice

> The first three chapters covered "how to make a single piece of glass."
> **This chapter covers: what problems you run into when assembling dozens of glass pieces into a system shell that "looks like iOS" — problems that only surface when you actually build the shell.**
>
> Most of the conclusions here were not "designed" but "learned the hard way":
> the lock screen showing the home screen behind it, glass showing the previous frame when opening an app, the bottom swipe gesture stealing events from other controls...
> Each one corresponds to a spot in the code that is **easy to overlook but breaks things if removed**.
>
> Corresponding source directory: [`../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/`](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/)

---

## 1. What was done in this phase, and what was not

The user's boundary was "**only build the system shell first.**" So let's define "shell" clearly:

| Part of the shell (done) | Not part of the shell (intentionally left empty) |
|---|---|
| Lock Screen (time, notifications, swipe-up to unlock) | Real app business logic |
| Home Screen (icon grid, paging, Dock, jiggle editing) | Real app content |
| Status Bar (clock / signal / Wi-Fi / battery) | Reading real signal and battery |
| Dynamic Island (collapsed / expanded) | Real media session |
| Control Center (pull down, panel, lock, change wallpaper) | Real brightness/volume system toggles |
| Notification Center (pull down, notification cards) | Real notification listening |
| App Switcher (cards, swipe up to remove) | Real process screenshots |
| Scale transition when opening / closing an app | Real Activity navigation |

**This boundary matters**: it determines that each opened app is a [placeholder page](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosAppWindow.kt#L205-L271), not 36 fake apps. **"The shell is real, the apps are empty"** is more honest than "every app fakes being real," and easier to hand off.

File responsibilities at a glance (**start reading the code from this table**):

| File | Responsibility | ~Lines |
|---|---|---|
| `IosShell.kt` | **Top-level assembly**: z-order, backdrop assignment, gesture layer mounting | ~270 |
| `IosShellState.kt` | **Single state machine**: lock screen / home / open app / overlay / wallpaper | ~130 |
| `IosScaffold.kt` | Wallpaper + two backdrops setup | ~150 |
| `IosLockScreen.kt` | Lock screen | ~185 |
| `IosHomeScreen.kt` | Home grid / paging / Dock / jiggle | ~355 |
| `IosAppWindow.kt` | Icon → fullscreen scale transition + in-app backdrop | ~280 |
| `IosOverlays.kt` | Control Center / Notification Center / notification cards | ~330 |
| `IosAppSwitcher.kt` | App switcher | ~225 |
| `IosSystemChrome.kt` | Status bar / Dynamic Island / Home indicator / clock | ~285 |
| `IosAppCatalog.kt` | App catalog and color scheme | ~100 |
| `IosGlyphs.kt` | **Generated file**: 68 vector glyphs | generated |
| `core/ios/IosColors.kt` | iOS system colors | ~75 |
| `core/ios/GlassIcons.kt` | 26 hand-drawn component icons | ~700 |

---

## 2. Layering: the shell's z-order (the most important section of this chapter)

### 2.1 Porting the real device's z-order into Compose

The composition order of [the iOS shell](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShell.kt) = **the physical stacking order of the screen from bottom to top**. In Compose there is no real z-index; **write order IS the z-order**. So if this order is wrong, the glass will sample an empty texture or the previous frame.

```
┌─────────────────────────────────────────────────────────────┐
│  IosDynamicIsland     Dynamic Island (notch cut-out, always  │  ← drawn last
│                        topmost, pure black)                  │
├─────────────────────────────────────────────────────────────┤
│  IosHomeGesture       bottom swipe-up bar + Home Indicator   │
│  IosPullDownZones     two invisible top pull-down zones      │
├─────────────────────────────────────────────────────────────┤
│  IosNotificationCenter Notification Center ← reads overlayBa- │
│  IosControlCenter     Control Center      ← reads overlayBa- │
│  IosAppSwitcher       App Switcher (plain mask, no glass)    │
│  IosLockScreen        Lock Screen         ← reads overlayBa- │
├─────────────────────────────────────────────────────────────┤
│  IosStatusBar         Status Bar (deliberately NOT recorded  │
│                        into the content layer)               │
├─────────────────────────────────────────────────────────────┤
│  ┌── Box + layerBackdrop(surfaceBackdrop) (cond. mounted)─┐  │
│  │  IosOpenAppWindow  Open App: in-app glass reads appBack- │ │
│  │  IosHomeScreen     Home: Dock glass reads wallpaperBack-  │ │
│  └────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  IosWallpaper         procedural wallpaper ─layerBackdrop→   │  ← drawn first
│                        wallpaper                             │
└─────────────────────────────────────────────────────────────┘
```

Corresponding code: [IosShell.kt L82-L151](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShell.kt#L82-L151).

### 2.2 Three layout rules you only learn by building the shell

**Rule A: The status bar must sit above the content layer, but must NOT be inside the recorded layer.**

The comment says it clearly:

```kotlin
// ---- surfaces above the content -----------------------------------------------------
// The status bar sits above the content layer so it stays readable over an open app, whose
// window is opaque. It is not part of the recorded layer, which also means it survives the
// pull-down panels drawn later.
IosStatusBar(isLightTheme = isLightTheme, modifier = Modifier.align(Alignment.TopCenter))
```

Why not record it: when the Control Center / Notification Center is pulled down, their own large clock pushes up, **and if the status bar were recorded into `surfaceBackdrop`, it would reappear inside the glass as "part of the blurred background,"** resulting in two clocks on screen. In real iOS the status bar is covered by the panels — so it must be **outside the content layer**.

**Rule B: The Dynamic Island is always drawn last, and is never glass.**

In [IosSystemChrome.kt L170-L239](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosSystemChrome.kt#L170-L239) it is a solid `Color.Black` + `RoundedCornerShape` capsule, **with no `drawBackdrop`**.

Reason: the Dynamic Island is a **hardware cut-out**, not glass. Its black is "there is no screen here," not "the glass is very dark." Making it glass would immediately look "fake" — because glass would show the background through it, while a cut-out does not.

> **This principle generalizes**: anything in the shell that "does not physically exist" (cut-outs, the Home bar's black/white) should not get a material applied.

**Rule C: The gesture layer is only mounted in states that should respond to gestures.**

```kotlin
if (!state.isLocked && !state.appSwitcher) {
    IosPullDownZones(state)
    IosHomeGesture(state = state, isLightTheme = isLightTheme)
}
```

[IosShell.kt L138-L141](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShell.kt#L138-L141)

- When locked, you cannot pull down the Control Center (on a real iOS lock screen, pulling down opens the Notification Center, handled by the lock screen's own gestures);
- In the App Switcher, swiping up means "close the switcher," not "go home."

**Conditionally mounting the gesture layer is cleaner than writing `if` inside callbacks**: not mounted = impossible to misfire, and it won't steal drags from elsewhere.

### 2.3 Backdrop assignment table (memorize it)

| Who | Which backdrop it samples | Why |
|---|---|---|
| Home Dock cards | `wallpaperBackdrop` | Dock sits directly on the wallpaper |
| Glass inside an app window | `appBackdrop` (window creates its own) | The window is opaque, glass can only refract inside the window |
| Glass on the lock screen | `overlayBackdrop` = combination(wallpaper, content layer) | Lock screen sits on top of "wallpaper + home" |
| Control Center / Notification Center | `overlayBackdrop` | Same as above: must be able to blur the home icons (this is exactly what Apple does) |
| App Switcher | no backdrop | It is a plain 42%-black mask + procedural cards, no glass |
| Dynamic Island / Status Bar / Home bar | no backdrop | not glass |

---

## 3. Building the backdrop and "conditional recording"

### 3.1 `IosScaffold`: two canvases, 18 lines

[IosScaffold.kt L130-L148](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosScaffold.kt#L130-L148):

```kotlin
Box(Modifier.fillMaxSize()) {
    val wallpaperBackdrop = rememberLayerBackdrop()   // layer 0: wallpaper
    val surfaceBackdrop = rememberLayerBackdrop()     // layer 1: screen content (conditionally mounted by the shell)

    IosWallpaper(
        isLightTheme = isLightTheme,
        variant = wallpaperVariant,
        modifier = Modifier.fillMaxSize().layerBackdrop(wallpaperBackdrop)
    )
    content(wallpaperBackdrop, surfaceBackdrop)
}
```

It splits "wallpaper" and "screen content" into **two canvases** because they are sampled at different stages of life:

- Glass inside the home itself (Dock) **only needs the wallpaper** → samples `wallpaperBackdrop`;
- Overlays (lock screen / control center) **need to see both the wallpaper and the home icons** → sample the combination of both.

If there were only one canvas (as the upstream Demo does), both the in-home glass and the overlay glass would sample the same one, and "the overlay must blur out the home icons" could not be achieved.

### 3.2 Conditional recording: the shell's only performance switch

[IosShell.kt L77-L99](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShell.kt#L77-L99):

```kotlin
// Recording the content layer costs a full-screen offscreen pass every frame, so it only
// runs while something above actually reads it.
val surfaceIsRead = state.isLocked || state.controlCenter || state.notificationCenter

Box(
    Modifier
        .fillMaxSize()
        .then(if (surfaceIsRead) Modifier.layerBackdrop(surfaceBackdrop) else Modifier)
) {
    IosHomeScreen(backdrop = wallpaperBackdrop, state = state, isLightTheme = isLightTheme)
    IosOpenAppWindow(state = state, backdrop = wallpaperBackdrop, isLightTheme = isLightTheme)
}
```

**The three conditions of `surfaceIsRead` correspond exactly to the three states where "something is reading it":**

| State | Who is reading `surfaceBackdrop` |
|---|---|
| `isLocked` | Lock screen glass (notification cards, flashlight/camera buttons) |
| `controlCenter` | All glass tiles in the Control Center panel |
| `notificationCenter` | Notification Center glass cards |

**Why it's valuable**: recording a frame = one full-screen offscreen render. While the user flips pages on the home, taps icons, and watches animations, **nothing is reading this layer**; without conditional recording, all those operations would waste a full-screen offscreen pass the whole time. The upstream Demo has few pages and simple content, so it could leave it always on; **building a system shell requires conditional recording**.

> ⚠️ A common pitfall: leaving out one condition of `surfaceIsRead` (e.g. forgetting `notificationCenter`). The symptom is "you can open the Notification Center, but the glass is empty / shows the previous frame." **When changing this condition, keep all 3 upstream consumers in mind.**

---

## 4. State machine: `IosShellState`

[IosShellState.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShellState.kt).

**Why not the Navigation library**: this is not "page navigation," but **concurrent states within one continuous system** (an app may be running in the background while the lock screen is up; the Control Center can cover the lock screen; the App Switcher can cover the home). Navigation's destination-stack model cannot express "multiple layers existing at the same time"; forcing it only complicates a simple problem.

**Why a single holder instead of a dozen `remember { mutableStateOf }`**: `IosHomeScreen`, `IosShell`, `IosLockScreen`, `IosOverlays` all need to read and write the same batch of state. Passing down one `@Stable` class is far cleaner than threading a dozen `value/onValueChange` pairs, and avoids the sync bug of "two places each keep their own copy of `page`."

### 4.1 Field table

| Field | Type | Meaning |
|---|---|---|
| `surface` | `IosSurface` (`Lock` / `Home`) | Whether the current surface is lock screen or home |
| `page` | `Int` | Current home page (rewritten by `Pager`'s `snapshotFlow`) |
| `openApp` | `IosApp?` | App currently occupying fullscreen |
| `openAppOrigin` | `Rect` | **The rectangle of the icon that launched it on the home** → start of the transition |
| `recents` | `SnapshotStateList<IosApp>` | MRU, newest first, cap 6 |
| `controlCenter` / `notificationCenter` / `appSwitcher` | `Boolean` | The three overlays |
| `jiggle` | `Boolean` | Home jiggle-edit mode |
| `islandExpanded` | `Boolean` | Whether the Dynamic Island is expanded |
| `wallpaper` | `Int` | Wallpaper variant (cycled with `++` in Control Center) |
| `galleryOpen` | `Boolean` | Whether the current app is the "widget gallery" |

### 4.2 `homeGesture()`: one gesture, three meanings

[IosShellState.kt L118-L127](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShellState.kt#L118-L127):

```kotlin
fun homeGesture() {
    when {
        appSwitcher -> appSwitcher = false          // in the app switcher → go home
        openApp != null -> goHome()                 // in an app → go home
        else -> { jiggle = false; appSwitcher = true }  // on the home screen → open the app switcher
    }
}
```

**This is why it must live in state, not in the gesture callback**: the meaning of the gesture "swipe up from bottom" **depends on the current state**, and only state knows the current state. Writing it in the callback would require passing `appSwitcher`, `openApp`, `jiggle` around again.

### 4.3 `lock()` must "clear the scene"

```kotlin
fun lock() {
    jiggle = false
    closeApp()
    closeOverlays()
    islandExpanded = false
    surface = IosSurface.Lock
}
```

The lock screen is the **lowest-level state**; no upper overlay should survive this lock. Missing a line shows up as: "after locking and unlocking again, the Control Center is still open / the Dynamic Island is still gaping."

---

## 5. Lock Screen

[IosLockScreen.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosLockScreen.kt)

### 5.1 Structure (top to bottom)

```
Spacer(84dp)
Lock-shaped glass button (GlassIconButton, 42dp)
"October 1, Thursday" (white 90%, 17sp Medium)
Large time 88sp Light (rememberIosClock, refreshes every second)
Spacer(28dp)
Notification cards ×2 (GlassCard, corner 26dp, surfaceColor = White 14%)
Spacer(weight 1f)      ← pushes the following content to the bottom
Flashlight / Camera  glass buttons ×2 (52dp, one each side, padding 44dp)
"Swipe up to unlock" (white 75%, 14sp)
Spacer(30dp)
```

**Text is always white, independent of theme**: the lock screen always sits directly on the raw wallpaper; a real iOS lock screen is also always white. So here you should **not** use `IosColors.content(isLightTheme)`.

**The notification card's `surfaceColor` is 14% rather than the Notification Center's 22%**: lock-screen cards sit directly on the vivid original image; any higher tint would cover the wallpaper. This is the concrete value of the "restraint ladder" from Chapter 04 applied to the lock screen.

### 5.2 The three numbers of swipe-up to unlock

```kotlin
val offsetY = remember { Animatable(0f) }

.draggable(
    orientation = Orientation.Vertical,
    state = rememberDraggableState { delta ->
        scope.launch { offsetY.snapTo((offsetY.value + delta).coerceIn(-2_400f, 0f)) }
    },
    onDragStopped = { velocity ->
        if (offsetY.value < -180f || velocity < -1_100f) {
            offsetY.animateTo(-2_400f, tween(300, easing = EaseOutCubic))
            onUnlock()
        } else {
            offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
        }
    }
)
```

| Number | Meaning | Why this value |
|---|---|---|
| `-180f` | Displacement threshold (px) | Too small → unlocks on slight mis-touch; too large → feels "stuck" |
| `-1_100f` | Velocity threshold (px/s) | A fast flick unlocks even with little displacement — key to the "feel" |
| `-2_400f` | Target displacement when unlocking | Far enough to guarantee the whole screen moves off; need not equal exact screen height |
| `tween(300, EaseOutCubic)` | Unlock animation | Fast then slow, matches the physics of "throwing it away" |
| `spring(MediumBouncy, MediumLow)` | Rebound | If released below threshold → bounce back, with slight overshoot |

**Note `coerceIn(-2400f, 0f)`: only upward swiping is allowed.** Without this line, the lock screen could be dragged downward, opening a blank gap.

### 5.3 ⚠️ One line that must exist: swallow taps

```kotlin
// Swallow stray taps so they cannot reach the home screen behind the lock screen.
.clickable(interactionSource = null, indication = null) {},
```

**This line looks useless (empty lambda), but it is required.** Reason:

In `IosShell`, the lock screen and home are **two sibling nodes composed in the same `Box`**. The home's icons still exist and are still clickable underneath the lock screen. `draggable` only eats "drag" gestures, **it does not block clicks** — so if the lock screen doesn't have this empty `clickable`, a random tap on the lock screen, if it lands exactly on a home icon, would open that app behind the lock screen.

> **General rule: any full-screen layer stacked above interactive content must explicitly swallow the gestures it does not handle.**
> `interactionSource = null, indication = null` avoids showing a ripple — a lighter tier than the upstream Ripple.

### 5.4 Known issue: the lock screen vs. home relationship (see §14.2)

In the current implementation the lock screen is **a sibling node alongside the home**, expressing unlock via "the whole thing moves up + the home is fully covered." This is already usable visually, but differs from real iOS; **this is the most worthwhile place to take over and continue improving**, see §14.2.

---

## 6. Home Screen

[IosHomeScreen.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosHomeScreen.kt)

### 6.1 Why hand-write the grid instead of `LazyVerticalGrid`

The code comment says it directly:

> Icons are laid out by hand rather than with a lazy grid so the four columns keep identical widths on every page (and on the short last row), which is what makes the dock and the dots line up with the grid above them.

The key is the "**short last row**": page 2 has only 16 icons (4×4). With an auto-flowing lazy grid, the column width would drift with the content count, **and the Dock would not align left-right with the icons above it**.

The approach is `apps.chunked(4)` + each column `Modifier.weight(1f)` + filling with `Spacer(Modifier.weight(1f))` when fewer than 4:

```kotlin
rowApps.forEachIndexed { columnIndex, app ->
    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) { IosHomeIcon(...) }
}
repeat(4 - rowApps.size) { Spacer(Modifier.weight(1f)) }
```

### 6.2 Icon size and the 5 "Apple-flavor" parameters

| Parameter | Value | Notes |
|---|---|---|
| Icon edge | `62.dp` | |
| Corner radius | `RoundedRectangle(15.dp)` | **≈ 24% of edge**, and must use G2-continuous curvature from `com.kyant.shapes` |
| Base color | `Brush.linearGradient(app.top, app.bottom)` | Each app gets a pair of gradient colors (see Ch. 06) |
| Content | `GlassIcon(app.glyph, app.glyphTint, size = 31dp)` | White (or dark) monochrome vector |
| Shadow | `shadow(10.dp, RoundedRectangle(15.dp), ambient/spot = Black 40%)` | Makes the icon "float" on the wallpaper |
| Label | `12.sp` `Medium` white + `Shadow(Black 45%, offset(0,1), blur 3)` | Readable even on bright wallpapers |

`RoundedRectangle(15.dp)` appears **3 times** in this code (icon, shadow, clip) and must be consistent, otherwise the shadow edge and icon edge drift by half a pixel, producing a "ghost ring."

### 6.3 Jiggle edit mode

```kotlin
val wobble = if (jiggle) {
    val transition = rememberInfiniteTransition(label = "jiggle")
    val angle by transition.animateFloat(
        initialValue = -1.1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(130 + phase * 26, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    angle
} else 0f

Column(Modifier.graphicsLayer { rotationZ = wobble })
```

Three details; miss one and it doesn't look right:

1. **`phase = (rowIndex * 4 + columnIndex) % 4`** → periods stagger from 130ms to 208ms; **icons being out of sync is what Apple looks like**; all icons wobbling at the same frequency looks like "the whole screen breathing."
2. **±1.1°** — beyond 2° it becomes a "cocktail shaker."
3. **`rotationZ` rather than `offset`** — Apple rotates around the center, not a translation.

Exiting edit mode also copies iOS: tap empty space to exit (the `if (state.jiggle)` full-screen `clickable` at the top of `IosHomeScreen`), and disable paging in edit mode (`userScrollEnabled = !state.jiggle`).

### 6.4 Paging and dots

```kotlin
val pagerState = rememberPagerState(pageCount = { IosAppCatalog.pages.size })

LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }.collectLatest { state.page = it }
}
```

**The direction is "Pager → state," not "state → Pager"**: the Pager is the source of truth for gestures; sync it one-way into state for the Dock / dots to read. Writing it the other way causes jitter where "the gesture scrolls halfway but state jumps first."

Dots: 7dp, `Capsule()`, current page white 95% / others white 38%.

### 6.5 Dock

The Dock is the only element on this screen that uses liquid glass:

```kotlin
GlassCard(
    backdrop = backdrop,                                    // = wallpaperBackdrop
    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
    cornerRadius = 36.dp,
    surfaceColor = Color.White.copy(alpha = if (isLightTheme) 0.34f else 0.22f)
)
```

- Material = `GlassMaterials.Card` (`vibrancy()` + `lens(16.dp, 32.dp)`, **no blur**). **No blur is deliberate**: the Dock should be "transparent" and let the wallpaper's color show through; adding blur turns it into a frosted-glass brick.
- Tint 34% / 22% also stays within the "35% cap" of Chapter 04.
- Dock icons 56dp / corner 14dp / no label — slightly smaller than home icons, no text.

---

## 7. Scale transition for opening / closing an app

[IosAppWindow.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosAppWindow.kt)

This is the **hardest animation in the whole shell to get right**.

### 7.1 Requirement: "grow" from the icon into fullscreen, without text reflowing

The naive approach is "add `scaleX/scaleY` to the fullscreen content," but then **the text stretches with the scale**, looking blurry and distorted mid-transition.

**Correct approach: interpolate position and size, but lay out the content only once.**

```kotlin
val left = lerp(origin.left, screen.left, fraction)
val top = lerp(origin.top, screen.top, fraction)
val width = lerp(origin.width, screen.width, fraction)
val height = lerp(origin.height, screen.height, fraction)
val corner = lerp(with(density) { IconCornerDp.dp.toPx() }, 0f, fraction)

Box(
    Modifier
        .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
        .size(width.toDp(), height.toDp())
        .clip(RoundedRectangle(corner.toDp()))
) {
    Box(
        Modifier
            .requiredSize(screen.width.toDp(), screen.height.toDp())   // ← always lay out at real screen size
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0f)              // ← anchor at top-left
                scaleX = width / screen.width
                scaleY = height / screen.height
            }
    ) {
        IosAppSurface(...)
    }
}
```

Two key points:

1. **`requiredSize(full screen)` + `graphicsLayer` scaling**: the content is laid out at real size (text wrapping, font size are all final values), then the whole thing is squeezed into the growing box. **Throughout the transition the content is "a scaled version of its final form" and never reflows.**
2. **`transformOrigin = TransformOrigin(0f, 0f)`**: the default anchor is center; center scaling fights with the position/size changes. The anchor must be top-left so it matches the outer `offset` + `size` interpolation.

The corner radius linearly goes from `15dp` (icon corner) to `0` — the window "flattening" is the process of it becoming fullscreen.

### 7.2 Who holds the window: the rationale for `IosOpenAppWindow`

```kotlin
/**
 * Keeps the open app's window mounted across the close animation.
 *
 * [IosShellState.openApp] drops to `null` the moment the user goes home, but the window still has
 * to shrink back into its icon before it can be unmounted — so the shell holds its own copy and
 * only clears it once [IosAppWindow] reports that it has finished animating out.
 */
```

**This is the classic "animation outlives the state" problem**: user presses Home → `state.openApp = null` → if the UI immediately unmounts the window following `null`, **the animation never gets to play** and the screen snaps back to home.

The solution is for the shell to keep its own `windowApp` copy, cleared only in `onFinished()` (once the window has shrunk back into the icon):

```kotlin
LaunchedEffect(openApp) {
    if (openApp != null) { windowOrigin = state.openAppOrigin; windowApp = openApp }
}
val app = windowApp ?: return
key(app.id) {
    IosAppWindow(app = app, origin = windowOrigin, visible = openApp != null, ...,
        onFinished = { if (state.openApp == null) windowApp = null })
}
```

`key(app.id)` ensures the window is rebuilt when switching apps, rather than reusing the previous app's animation progress.

**Duration**: open `420ms` (`EaseOutCubic`), close `300ms`. Opening being slower than closing is a universal rule of phone UI — opening needs "ceremony," closing needs to be "crisp."

### 7.3 A separate backdrop inside the app

The window is an opaque system background (light `0xFFF2F2F7` / dark `Color.Black`). At this point the glass inside the window **must not** sample `wallpaperBackdrop` — light can't pass through an opaque background, which makes no physical sense, and visually you'd see "things that shouldn't be visible floating in the glass."

```kotlin
val appBackdrop = rememberLayerBackdrop()

Box(Modifier.fillMaxSize().background(systemBackground).layerBackdrop(appBackdrop)) {
    // top 320dp app-themed color fade
    Box(Modifier.fillMaxWidth().height(320.dp).background(Brush.verticalGradient(
        listOf(app.bottom.copy(alpha = 0.55f), Color.Transparent))))
}
```

Then all glass inside the app samples `appBackdrop`.

> **Rule (also emphasized in Chapter 02)**: glass should refract "what it visually sits on top of," not "the layer recorded first in the program."

---

## 8. Control Center / Notification Center

[IosOverlays.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosOverlays.kt)

### 8.1 Shared pull-down panel `IosPullDownPanel`

Two layers of `AnimatedVisibility`, **managing the mask and the panel separately**:

```kotlin
// ① mask: only fade in/out
AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))
        .clickable(interactionSource = null, indication = null) { onDismiss() })
}

// ② panel: slide in/out
AnimatedVisibility(
    visible,
    enter = slideInVertically(tween(320, easing = EaseOutCubic)) { (it * entryOffset).roundToInt() },
    exit  = slideOutVertically(tween(240, easing = EaseOutCubic)) { (it * entryOffset).roundToInt() }
) { ... }
```

`entryOffset = -1f` → slides in from above the screen. **The parameterized `entryOffset` is so that a future "panel coming from below" can reuse this directly.**

**Exit is faster than enter (320 → 240)**: entering needs presence, leaving needs to be clean. This rule recurs throughout the shell.

### 8.2 Gesture dismissal

```kotlin
private const val DismissThresholdPx = -64f

.draggable(
    orientation = Orientation.Vertical,
    state = rememberDraggableState { delta ->
        scope.launch { dragOffset.snapTo((dragOffset.value + delta).coerceAtMost(0f)) }
    },
    onDragStopped = { velocity ->
        if (dragOffset.value < DismissThresholdPx || velocity < -900f) {
            dragOffset.animateTo(-320f, tween(180, easing = EaseOutCubic))
            onDismiss()
            dragOffset.snapTo(0f)
        } else {
            dragOffset.animateTo(0f, spring(DampingRatioMediumBouncy, StiffnessMediumLow))
        }
    }
)
```

`coerceAtMost(0f)` → the panel **can only be pushed up, not dragged down** (same logic as the lock screen's `coerceIn`).

**Note `snapTo(0f)` right after `onDismiss()`**: the panel's `AnimatedVisibility` is already playing its exit animation; if you don't reset this manual `dragOffset`, the next time you open the panel it will appear with the previous offset.

### 8.3 Control Center content

```kotlin
GlassControlCenter(backdrop = backdrop, isLightTheme = isLightTheme)
```

The material comes from `GlassOverlays.kt`'s `GlassControlCenter` (upstream `destinations/ControlCenterContent.kt`):

```kotlin
val uiSensor = rememberUISensor()          // accelerometer → gravity direction

val glassHighlight = {
    Highlight(style = HighlightStyle.Default(
        angle = uiSensor.gravityAngle,     // ← highlight slides with phone tilt
        falloff = 2f
    ))
}

// each tile
.drawBackdrop(
    backdrop = backdrop,
    shape = { itemShape },
    effects = GlassMaterials.controlCenterItem(1f),   // vibrancy + lens(24, 48, depthEffect)
    highlight = glassHighlight,
    shadow = null,                                    // ← note: control center tiles have no outer shadow
    onDrawSurface = { drawRect(IosColors.ControlCenterSurface) }   // Black 5%
)
```

Three "Apple-flavor" details, all here:

| Detail | Approach | Why |
|---|---|---|
| Highlight follows gravity | `angle = uiSensor.gravityAngle` | When the phone tilts, the bright streak on the glass "slides" — the most easily overlooked but most eye-fooling detail |
| Tint only 5% black | `ControlCenterSurface` | Large panel + low tint = glass feel; high tint turns it into a solid block |
| `shadow = null` | Explicitly remove outer shadow | The Control Center is "a layer hugging the screen," not "a floating card" |

**`shadow = null` must be written explicitly** — if omitted, the library default `Shadow.Default` (24dp black 10%) applies, and a heavy black ring appears on the large panel, looking obviously fake.

### 8.4 Notification Center

Structure: large clock 68sp Light + date + three `IosNotificationCard` (default `surfaceColor = White 22%`).

**Why 14% on the lock screen but 22% here**: the Notification Center's background is "home + mask," more "solid" than the lock screen's vivid wallpaper, so it needs a stronger base to keep text readable. This is still a value from the "restraint ladder" of Chapter 04.

### 8.5 Notification cards: shared by lock screen and Notification Center

```kotlin
fun IosNotificationCard(
    backdrop: Backdrop, app: IosApp, title: String, body: String, time: String,
    modifier: Modifier = Modifier,
    surfaceColor: Color = Color.White.copy(alpha = 0.22f)
)
```

**Sharing is correct; the only difference is the single `surfaceColor` parameter** — the notification cards on both screens are the same component in real iOS. Collapsing the difference into one default parameter is far better than duplicating the code.

Card content: 38dp gradient small icon (`RoundedCornerShape(11.dp)`) + title 14sp SemiBold + time 12sp white 60% + body 14sp white 90%.

---

## 9. App Switcher

[IosAppSwitcher.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosAppSwitcher.kt)

**It does not use liquid glass**, because it is not a "material" but a "space": a `Black 42%` mask + a row of cards.

Card specs:

| Item | Value |
|---|---|
| Width | `280.dp` |
| Preview ratio | `aspectRatio(0.62f)` (≈ phone's tall proportion) |
| Corner radius | `RoundedRectangle(28.dp)` |
| List | `LazyRow` + `contentPadding(horizontal = 28.dp)` + 16dp gap |
| Order | `state.recents` (MRU, newest at left) |

The three numbers for swipe-up removal are structurally identical to the lock screen unlock (this is "consistency of feel"):

```kotlin
if (offsetY.value < -150f || velocity < -900f) { offsetY.animateTo(-800f, tween(200, ...)); onRemove() }
else { offsetY.animateTo(0f, spring(MediumBouncy, MediumLow)) }
```

**`alpha` decays with displacement; this is the only source of "premium feel" here**:

```kotlin
.graphicsLayer { alpha = (1f - (-offsetY.value) / 600f).coerceIn(0.15f, 1f) }
```

As the card is pushed up it gradually becomes transparent (floor 0.15 not 0, to avoid the feel-illusion of "invisible before reaching threshold").

**The preview is drawn procedurally** (`PreviewBar`: a few white 34% rounded bars of varying widths), not a screenshot. **Screenshotting other apps is not feasible in a sandbox / no-permission environment**, and a fake screenshot is worse than a placeholder block — the placeholder honestly says "this is a preview here," while a fake screenshot makes people think the app actually ran.

---

## 10. Status Bar / Dynamic Island / Home Indicator

[IosSystemChrome.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosSystemChrome.kt)

### 10.1 Clock: the minimal implementation that refreshes every second

```kotlin
@Composable
fun rememberIosClock(): String {
    var text by remember { mutableStateOf(iosClockText()) }
    LaunchedEffect(Unit) {
        while (true) { text = iosClockText(); delay(1_000) }
    }
    return text
}
```

A single `while(true) + delay(1s)` **is far simpler than `LaunchedEffect(tick) + Flow`, and sufficient.** `Calendar.getInstance()` formatted as `HH:mm`. Used in three places — lock screen, Notification Center, status bar — **with only one clock source**, so the three times are always consistent; no "lock screen 10:31, status bar 10:32."

### 10.2 Status Bar

`fillMaxWidth().height(52.dp).padding(horizontal = 30.dp)`:

- Left: clock `17.sp` `SemiBold`
- Right: signal (`Canvas` draws 4 increasing rounded bars) + Wi-Fi (vector glyph) + battery (`Canvas`)

**The battery is hand-drawn with `Canvas`**, not an icon: shell + 78% fill + small bump on the right. Each length is a percentage of `size.height` (`0.09f` line width, `0.34f` corner...), so it scales without distortion.

Color uniformly uses `IosColors.content(isLightTheme)`.

### 10.3 Dynamic Island

| State | Width | Height | Corner |
|---|---|---|---|
| Collapsed | `124.dp` | `36.dp` | `18.dp` |
| Expanded | `336.dp` | `142.dp` | `46.dp` |

All three values transition together with `animateDpAsState(tween(360, EaseOutCubic))` — **width, height, and corner must all move at once**; moving only width/height looks like "a stretched rubber band."

Expanded content is a "Now Playing" card: 58dp gradient cover + "Now Playing" 11sp white 60% + song name 15sp SemiBold + static waveform + pause icon.

**Emphasized again: the Dynamic Island is solid `Color.Black` + `RoundedCornerShape`, not glass.** It is a hardware cut-out; any background showing through would make it "not black," instantly exposing the fakery.

### 10.4 Home Indicator

A `140.dp × 5.dp` capsule, color `IosColors.content(isLightTheme).copy(alpha = 0.9f)`, centered in a 26dp-tall `Box`.

---

## 11. Gesture Layer

[IosShell.kt L199-L268](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShell.kt#L199-L268)

### 11.1 Two top pull-down zones

```kotlin
private val PullZoneHeight = 44.dp
private const val PullThresholdPx = 70f

Row(Modifier.fillMaxWidth().height(PullZoneHeight).align(Alignment.TopCenter)) {
    PullZone(Modifier.weight(0.4f)) { state.notificationCenter = true }   // left 40%
    Spacer(Modifier.weight(0.2f))                                          // middle 20% left empty
    PullZone(Modifier.weight(0.4f)) { state.controlCenter = true }         // right 40%
}
```

**That 20% empty `Spacer` in the middle is the single most important line of "negative space" in the whole file**: the Dynamic Island is at top-center, and tapping it must expand it. If the two side zones fill the full width, **the Dynamic Island would be covered and untappable** (the gesture layer's z-order is below the island, but hit-testing still reaches the lower full-width gesture area first... the actual result is the middle area gets stolen).

Threshold: displacement `> 70px` **or** velocity `> 800px/s` — same as the lock screen, displacement + velocity dual criterion.

### 11.2 Bottom swipe-up

```kotlin
private val HomeGestureHeight = 40.dp

onDragStopped = { velocity ->
    val triggered = travel.value < -70f || velocity < -900f
    travel.snapTo(0f)
    if (triggered) state.homeGesture()
}
```

**`travel` is only used for the decision, not for drawing** — so after triggering, immediately `snapTo(0f)`, no rebound animation needed. (Contrast with the lock screen: the lock screen's `offsetY` **must be drawn**, so it must `animateTo` rebound.)

This distinction is worth noting separately: **whether gesture state needs to be "visualized" determines whether it's an `Animatable` or a plain accumulator.**

### 11.3 Gesture destination master table

| Gesture | Position | Criterion | Result |
|---|---|---|---|
| Swipe up | Bottom 40dp | displacement < -70px or velocity < -900px/s | `state.homeGesture()` (three meanings, see §4.2) |
| Pull down | Top-left 40% | displacement > 70px or velocity > 800px/s | Open Notification Center |
| Pull down | Top-right 40% | Same | Open Control Center |
| Swipe up | Lock screen fullscreen | displacement < -180px or velocity < -1100px/s | Unlock |
| Swipe up | Panel fullscreen | displacement < -64px or velocity < -900px/s | Dismiss panel |
| Swipe up | App Switcher card | displacement < -150px or velocity < -900px/s | Remove that app |

**For all "displacement + velocity" dual criteria, the velocity threshold sits in the 800~1100px/s range.** This is an empirical value you can reuse directly.

---

## 12. Theme, Color Scheme, and Wallpaper

- `isLightTheme = !isSystemInDarkTheme()` ([IosColors.kt L74-L76](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/core/ios/IosColors.kt#L74-L76))
- All colors are centralized in `IosColors`, each annotated with its upstream source; see Chapter 04 §3.
- **Rule**: `IosColors.content(isLightTheme)` is for text "on glass / in the UI"; lock screen text is **always white**; the Dynamic Island is **always black background with white text**.

Wallpaper is **procedurally generated**, not image assets ([IosScaffold.kt L33-L118](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosScaffold.kt#L33-L118)):

```
3 palettes (deep night blue-purple / warm orange rose / teal-green)
Each = 3-color linear gradient base + 4 radial-gradient "color balls" (position/radius in 0..1 relative coords)
Light theme: each color ball color lift() — blend 34% toward white, so glass stays legible in light theme
```

**Why no images**:
1. No third-party art assets introduced, no copyright issues;
2. What glass needs to refract is exactly **high-contrast, multi-color, gradient objects**; procedural color balls happen to provide this background, and changing the wallpaper is just `state.wallpaper++` (that button in Control Center) at zero cost.

> A practical lesson: **when evaluating glass effects, the background must have high-contrast color-block boundaries.** On a solid or grayscale background, refraction/dispersion is almost invisible, making you think "the parameters don't work."

---

## 13. Performance

| Measure | Location | Benefit |
|---|---|---|
| Conditional recording (`surfaceIsRead`) | `IosShell` | Saves one full-screen offscreen pass during normal home operation |
| Separate `appBackdrop` inside app | `IosAppWindow` | Window doesn't need full-screen recording, only records its own inner layer |
| `if (!visible) return` when panel dismissed | `IosAppSwitcher` | Not entering composition = no layout, no draw |
| Conditional gesture-layer mounting | `IosShell` | Doesn't steal gestures when locked / in switcher |
| Small `LayerBackdrop` | `GlassControls`'s `trackBackdrop` (64×28) | Offscreen area ∝ cost |

**The easiest to miss**: `IosAppSwitcher`'s first line is `if (!visible) return`. Without it, the switcher's `LazyRow` would participate in layout even when you can't see it.

---

## 14. Known Issues and Handover Suggestions (honest list)

This section records **the genuine shortcomings of this shell as it currently stands.** They are **not bug reports, but a roadmap for the next AI.**

### 14.1 Home icons use vector line art, not "Apple app icon partition"

**Status**: Home icons = hand-drawn gradient squircle + **monochrome line-art glyph** from [`IosGlyphs`](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosGlyphs.kt), glyphs come from ui-icons-hub's `Lucide` set (ISC license, SVG).

**The user's exact feedback**:

> Your Apple app icons — didn't that website I gave you have a separate Apple app-icon section? Why not just use those?

**He's right.** ui-icons-hub does have a standalone `app-icons/` partition containing **797 pre-rounded iOS26-style app-icon PNGs** (only 27MB of opaque content). That is the correct source for "Apple app icons." **See [06-Icons-and-Asset-Pipeline.md](06-Icons-and-Asset-Pipeline.md) §8 for the full integration plan.**

### 14.2 Lock screen and home are "two sibling nodes stacked together"

**Status**: [IosShell.kt L110-L115](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/ios/IosShell.kt#L110-L115)

```kotlin
if (state.isLocked) {
    IosLockScreen(backdrop = overlayBackdrop, onUnlock = { state.unlock() })
}
```

The lock screen and home are composed **in the same `Box` at the same time.** The lock screen's `Column` is all `Spacer` and a little content, **and the home sits one layer below, fully visible through the lock screen's empty areas**; when swiping up, the lock screen moves as a whole while the home stays put — the user's words:

> I found a problem: the lock screen and the home screen are stacked directly together — you can see the home screen right through the lock screen, and only when you swipe up does the lock screen disappear.

**Already fixed**: added an empty `clickable` to swallow taps (§5.3), so "visible" doesn't become "tappable." **But it's still visually wrong.**

**Correct approach (suggested for the successor to implement)**:

Change "lock screen / home" from "two parallel nodes" to **two states of the same continuous space**, driven by a shared `unlockProgress: 0f..1f`:

```kotlin
// unlock progress 0 = full lock screen, 1 = full home
val p = unlockProgress

// home: always present, but transitions from "slightly small + slightly dim" to normal
IosHomeScreen(
    modifier = Modifier
        .graphicsLayer {
            val s = lerp(0.94f, 1f, p)
            scaleX = s; scaleY = s
            alpha = p            // or lerp(0f, 1f, p), depending on desired look
        }
)

// lock screen: covers on top, moves up as a whole + fades out
IosLockScreen(
    modifier = Modifier
        .graphicsLayer {
            translationY = -size.height * EaseOutCubic.transform(p)
            alpha = 1f - p
        }
)
```

Supporting changes:

1. In `IosShellState`, replace `surface: IosSurface` with an `unlockProgress: Float` (or keep the enum + add a progress); `unlock()` becomes "animate progress to 1," switching `surface = Home` only when the animation ends;
2. The lock screen's `draggable` no longer maintains its own `offsetY`, but **directly drives `unlockProgress`** — so wherever the finger drags, the lock screen is there and the home brightens in sync (**follows the finger**, rather than "plays the animation only after release");
3. When the swipe-up doesn't pass the threshold, `unlockProgress` springs back to 0 with `spring`.

**This way "seeing the home through the lock screen" is no longer a problem — because that is the design intent**: on a real iOS unlock, the home goes from dark to bright, from slightly shrunk to normal.

> By the way: this change also makes the "boot → lock screen" of §14.3 more natural — because the lock screen is no longer "a page you can casually stack on top."

### 14.3 No "boot → lock screen" boot sequence

**Status**: [LiquidGlassApp.kt](../LiquidGlassShowcase/app/src/main/java/com/liquidglass/showcase/LiquidGlassApp.kt) is `liquid-glass launch animation (1.6s) → 1.2s easeOutCubic cross transition → IosShell`, and `IosShell`'s initial state is the lock screen. **So "splash fades out" and "lock screen appears" are the same thing — there is no boot process.**

**User's exact feedback**:

> The lock screen should be like a normal phone — boot into the lock screen the way a normal phone does.

**Suggested real boot composition** (three stages, can be placed after the splash):

| Stage | Scene | Duration |
|---|---|---|
| 1. Black screen | Fully black, no content | 300~500ms |
| 2. Boot logo | A logo fades in at screen center, then fades out | 800~1200ms |
| 3. Lock screen | Lock screen goes from "dark" to "bright" (`alpha` or `brightness` 0.3 → 1), while wallpaper goes from slightly enlarged to normal | 400~600ms |

Implementation suggestion: add a `bootStage` state machine in `LiquidGlassApp`; `IosShell` is only composed in the final stage; that boot stage **needs no backdrop at all** (screen is black), so **no extra offscreen render cost is added**.

**Note**: do not use Apple's logo (trademark). Use a geometric shape (ring / self-drawn abstract apple-shaped vector).

### 14.4 No on-device visual acceptance

There is no emulator / real device in the sandbox, **only verification up to "compile → R8 → package → sign → AGSL shader string into the bundle"**, without manual confirmation of frame rate and visuals. The successor should review on an **Android 13+ real device** (AGSL needs API 33+ for refraction), focusing on: does refraction follow the finger, does the Control Center highlight change with tilt, does the scale transition drop frames.

---

## 15. Summary

```
①  Write order = z-order.
    Wallpaper → content layer (conditional recording) → status bar → lock screen
    → app switcher → two centers → gesture → Dynamic Island

②  Every glass must answer "what do I visually sit on top of":
    Dock → wallpaper    In-app → appBackdrop    Overlay → combined(wallpaper, surface)

③  A full-screen layer covering interactive content must explicitly swallow
    gestures it doesn't handle (empty clickable).

④  Animation outlives state → use the onFinished callback to decide when to unmount,
    don't unmount the instant the state becomes null.

⑤  Only scale graphicsLayer, lay out at real size with requiredSize → text never reflows in transition.

⑥  Things that don't physically exist (cut-out, Home bar) should not get glass.

⑦  For all "displacement + velocity" gestures, the velocity threshold is 800~1100px/s.
```

**Next steps**: to replace home icons (§14.1), read [06-Icons-and-Asset-Pipeline.md](06-Icons-and-Asset-Pipeline.md); to fix build errors or rebuild in a new environment, read [07-Build-and-Troubleshooting.md](07-Build-and-Troubleshooting.md).
