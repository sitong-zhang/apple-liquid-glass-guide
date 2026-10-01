# RuntimeShader (AGSL) uniforms are resolved by name at runtime, keep the JNI-facing
# members of the Backdrop library intact.
-keep class com.kyant.backdrop.** { *; }
-keep class com.kyant.shapes.** { *; }

-dontwarn org.jetbrains.annotations.**