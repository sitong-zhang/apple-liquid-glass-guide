// AGP 9 ships built-in Kotlin support, so the android application plugin and the
// Compose compiler plugin are the only plugins the build needs.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}