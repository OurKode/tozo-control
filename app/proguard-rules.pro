# Optimization & R8 Shrinking Rules for TOZO Control

# Baseline Compose & AndroidX preservation
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Navigation and Data models
-keep class com.ourkode.tozocontrol.data.** { *; }
-keep class com.ourkode.tozocontrol.NavigationKeysKt { *; }

# Allow R8 full mode aggressive optimizations
-repackageclasses
-allowaccessmodification
