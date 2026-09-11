# Spark

Spark is a mindful, mood-aware habit tracker for Android built with traditional **Android Views** in **Kotlin**.

## Features & Architecture
- **Single Activity**: `MainActivity` hosting a `NavHostFragment`.
- **Navigation Component**: `nav_graph.xml` managing screens and destinations.
- **Material Components (MD3-bridged)**: Soft-tactile palette, modern corner radii, and custom typography (Plus Jakarta Sans & Nunito Sans via Google Fonts).
- **ViewBinding**: Enabled for all Activities, Fragments, and DialogFragments.
- **Screen Layouts**: Responsive `ConstraintLayout` and `RecyclerView` primitives.
- **Bottom Navigation**: `BottomNavigationView` synced with top-level tabs (`dashboard`, `stats`, `settings`).
- **Dialogs**: `MoodDialogFragment` (daily emotional check-ins) and `AddHabitDialogFragment`.

## Minimum SDK
- Android 8.0 (API level 26)
