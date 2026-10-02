# Upstream Source — AndroidLiquidGlass (Backdrop)

> **This directory is a verbatim, complete copy of the upstream repository** [**Kyant0/AndroidLiquidGlass**](https://github.com/Kyant0/AndroidLiquidGlass),
> the Compose Multiplatform "Liquid Glass (Backdrop)" engine. It is archived here so that this repository stays
> **self-contained**: any AI or developer who receives this repository can read and build against the real upstream source
> without having to fetch it from GitHub again.
>
> Upstream commit archived at: `2026-10-02` · Upstream license: **Apache-2.0**
>
> **Do not modify the code inside `upstream/AndroidLiquidGlass/`.** It is the authoritative reference. All of our own work
> lives in the sibling projects (`LiquidGlassShowcase/`) and in `docs/`.

---

## Why this directory exists

This repository (EX / "experiments") explains how to implement the Apple iOS 26 **liquid glass** effect on native Android.
It is **built entirely from this upstream**, never from memory or guesswork. Keeping the full upstream source here serves three purposes:

1. **Traceability** — every API name, every effect, every material parameter in `docs/` can be checked against the real source line by line.
2. **Reproducibility** — the archive can be built independently (`./gradlew :backdrop:...`, see below), so nothing is hand-waved.
3. **Attribution** — the upstream `LICENSE` (Apache-2.0) and copyright headers travel with the code, satisfying the license terms.

---

## What was archived (upstream layout)

```
upstream/AndroidLiquidGlass/
├── backdrop/                     ← THE engine library (the actual liquid-glass implementation)
│   └── src/
│       ├── commonMain/kotlin/com/kyant/backdrop/
│       │   ├── Backdrop.kt                 — Backdrop read interface
│       │   ├── BackdropEffectScope.kt      — the effects DSL (vibrancy / blur / lens / chromaticAberration …)
│       │   ├── DrawBackdropModifier.kt     — Modifier.drawBackdrop(…): the "sampler"
│       │   ├── RuntimeShaderCache.kt
│       │   ├── backdrops/                  — LayerBackdrop / CanvasBackdrop / EmptyBackdrop / CombinedBackdrop + layerBackdrop modifier
│       │   ├── effects/                    — Lens.kt / Blur.kt / ColorFilter.kt / RenderEffect.kt
│       │   ├── highlight/                  — Highlight / HighlightStyle / HighlightModifier
│       │   ├── shadow/                     — Shadow / InnerShadow + modifiers
│       │   └── internal/                   — Shaders.kt / LayerRecorder.kt / RenderEffect.kt / ShapeProvider.kt …
│       ├── androidMain/                    — AGSL (RuntimeShader) implementation for API 31+
│       ├── skikoMain/                      — Skiko implementation (desktop / JS / WASM)
│       └── commonMain (Platform.kt etc.)
├── app/                          ← Backdrop Catalog demo (LiquidButton / Toggle / Slider / BottomTabs / ProgressiveBlur / LockScreen …)
│   └── src/commonMain/kotlin/com/kyant/backdrop/catalog/
├── androidApp/                   ← Android host app for the Catalog
├── iosApp/                       ← iOS host app (Kotlin Multiplatform + Swift)
├── artworks/                     ← banner / screenshots / diagrams
├── gradle/                       ← version catalog (libs.versions.toml)
├── build.gradle.kts / settings.gradle.kts / gradle.properties / gradlew*
└── LICENSE / README.md           ← Apache-2.0 license + upstream README (kept verbatim)
```

> The `.git`, `.idea`, `build/` and `*.apk` build artifacts were excluded (they are not source and not reproducible-safe to commit).

---

## How this maps to the rest of the repository

| This repository's docs | The upstream code it describes |
|---|---|
| `docs/01-Optical-Model-of-Liquid-Glass.md` | `backdrop/…/effects/Lens.kt`, `Blur.kt`, `ColorFilter.kt`, `internal/Shaders.kt` |
| `docs/02-Backdrop-Layered-Architecture.md` | `backdrop/…/backdrops/LayerBackdrop.kt`, `CombinedBackdrop.kt`, `DrawBackdropModifier.kt`, `internal/LayerRecorder.kt`, `internal/InverseLayerScope.kt` |
| `docs/03-API-Complete-Reference.md` | `backdrop/…/BackdropEffectScope.kt`, `highlight/*`, `shadow/*`, `effects/*` |
| `docs/04-Material-Recipe-Table.md` | `backdrop/…/effects/*` + our `LiquidGlassShowcase/…/core/glass/GlassMaterials.kt` |
| `docs/05-iOS-Shell-in-Practice.md` | our `LiquidGlassShowcase/…/ios/*` (an Android-native re-assembly of the upstream Catalog ideas) |
| `docs/06-Icons-and-Asset-Pipeline.md` | `app/…/composeResources/drawable/*` + our `tools/` |
| `docs/07-Build-and-Troubleshooting.md` | upstream `backdrop/build.gradle.kts`, `androidApp/build.gradle.kts` |
| `docs/08-Previous-Handover-Notes.md` | historical conversation record (context only) |
| `docs/09-How-Liquid-Glass-Is-Implemented.md` | **the whole engine**: `DrawBackdropModifier.kt`, `backdrops/*`, `effects/*`, `internal/Shaders.kt`, `highlight/*`, `shadow/*` — a line-by-line walk of how the effect works |

---

## How to build the upstream engine itself

The archived project is a standard Kotlin Multiplatform / Compose Multiplatform build:

```bash
cd upstream/AndroidLiquidGlass
# Android:
./gradlew :backdrop:assembleRelease
# Catalog demo on Android:
./gradlew :androidApp:assembleRelease
```

Requirements are the same as our own project (JDK 17, Android SDK, AGSL: `minSdk ≥ 31`). See
`docs/07-Build-and-Troubleshooting.md` for the exact environment.

---

## License & attribution

- Upstream: [**Kyant0/AndroidLiquidGlass**](https://github.com/Kyant0/AndroidLiquidGlass) — **Apache-2.0**.
- The verbatim `LICENSE` and every upstream copyright header inside `upstream/AndroidLiquidGlass/` are preserved.
- The `docs/` and `LiquidGlassShowcase/` work derived from it: see the main `README.md` §6 "Upstream and License".
- **Do not remove upstream copyright notices.** Apache-2.0 requires retaining attribution when redistributing.
