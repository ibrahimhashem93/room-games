# Zombie Survivor (prototype)

Top-down twin-stick-style survival shooter prototype for Android, built with **Kotlin + libGDX**.

You are a survivor on a city street at night. Zombies come in waves. You move - the rifle fires by itself at the nearest zombie in range. Survive as many waves as you can.

## Gameplay

- **Move**: touch and drag anywhere on the left half of the screen (floating virtual joystick).
- **Shoot**: fully automatic. The hero aims and fires at the nearest zombie within range. No fire button - play one-handed.
- **Waves**: each wave spawns more zombies, with more health and speed. Clear a wave to start the next.
- **Health**: the green HP bar (top left) drops when a zombie touches you. Game over at 0 - tap to restart.
- HUD shows current **WAVE** and **KILLS** (top right).

## Project contents

```
ZombieSurvivor/
  settings.gradle / build.gradle / gradle.properties   root build config (Gradle 8.10.2 wrapper included)
  gradlew / gradlew.bat / gradle/wrapper/               Gradle wrapper - no local Gradle install needed
  assets/                                               real sprite art (AI-generated, concept style):
                                                        hero.png, zombie.png, wreck.png, flash.png,
                                                        splat.png, portrait.png + map.jpg (1600x1600 dusk street)
  core/                                                 game code (shared by all platforms)
    src/.../ZombieSurvivorGame.kt                       game entry point
    src/.../GameScreen.kt                               all gameplay: joystick, auto-fire, waves, collisions, HUD
    src/.../Assets.kt                                   texture loading + bullet tracer
  lwjgl3/                                               desktop launcher (for quick testing on PC)
  android/                                              Android app module
    AndroidManifest.xml                                 landscape, fullscreen
    res/mipmap-*/ic_launcher.png                        launcher icon
```

All art is original AI-generated sprite art in the style of the approved concept image (chibi, top-down, dusk lighting) - no external or licensed assets, works fully offline.

## How to open and run in Android Studio

1. Install **Android Studio** (any recent version, e.g. Ladybug or newer) with the Android SDK (API 34) via the SDK Manager.
2. **File > Open** and select the `ZombieSurvivor` folder (the one containing `settings.gradle`).
3. Let Gradle sync finish (first sync downloads the Android Gradle Plugin, Kotlin and libGDX 1.13.1 - needs internet once).
4. To run on a phone: enable **Developer options > USB debugging** on the device, plug it in, pick it in the device dropdown, then press **Run** (green triangle). Or use **Build > Build APK(s)** and install the APK manually.
5. Minimum Android version: **7.0 (API 24)**. The game runs in landscape.

### Run on desktop instead (optional)

```
./gradlew lwjgl3:run
```

Useful for quick iteration without an emulator. On desktop, move with mouse-drag on the left half of the window.

## Tech

- Kotlin 2.0.20, libGDX 1.13.1, Android Gradle Plugin 8.5.2
- compileSdk 34, minSdk 24
- Desktop module verified building and running (LWJGL3 backend). The Android module uses the standard libGDX setup and builds with a normal Android Studio SDK install.

## Next steps (ideas)

- More enemy types, weapons and maps
- Real art + sound effects
- Player upgrades between waves
- iOS build via the libGDX RoboVM module
