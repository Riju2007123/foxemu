# FoxEmu

Android app: Store (itch.io) / Games / Downloads / Settings, with a
one-tap "Start Setup" that pulls:

- **Turnip 710/720/722** — resolved live from `The412Banner/Banners-Turnip`
  releases (auto-built, no fixed version pinned — `GithubReleaseResolver`
  picks the right asset out of whatever the latest release is)
- **Proton ARM64EC** (bundled FEXCore unixlib, covers Windows games) —
  resolved live from `The412Banner/proton-wine`
- **FEX interpreter + x86_64 Ubuntu rootfs** — for native Linux (non-Wine)
  games, standalone FEX-Emu + official Ubuntu base rootfs

Launch routing: Windows → `wine64` from proton-wine directly (its unixlib
loader handles FEX translation internally). Linux → `FEXInterpreter`
directly, no Wine. Android → system package installer.

## What's real vs. what needs your hands on it

Everything here compiles as written. What I could NOT verify without a
physical A710/A720 device and a PC to run it on, and what you need to
check on first run:

1. **The Turnip `.so` filename.** `SetupManager` globs for any `.so`
   containing "vulkan" after extraction and renames it to
   `libvulkan_freedreno.so`. If Banners-Turnip changes their zip layout,
   that glob needs adjusting — check Logcat for `DriverLoader` tags.
2. **`TU_DEBUG=sysmem` / `WRAPPER_BLIT=1`** — these are the flags the
   Banners-Turnip release notes recommend for the 710/720 build. They're
   wired into both `TurnipLoader` and `LaunchRouter`, but the driver is
   explicitly marked "UNVERIFIED — needs hardware testing" by its own
   author. Expect to fight rendering bugs.
3. **`wine64` entry point for Windows games.** proton-wine's arm64ec
   build self-loads FEXCore's unixlib, so no separate FEX wrapper call —
   confirm this against whatever release tag actually gets resolved at
   setup time, since behavior has shifted across their recent builds
   (`build-p10-...` vs `build-p11-...`).
4. **The itch.io store module is wired for the owned-games API only**
   (`my-owned-keys`). Browsing/purchasing unowned titles needs the
   broader itch API surface (`itch.io/api/1/key/me`, OAuth) which this
   doesn't implement — ask if you want that layered in.
5. **DXVK/VKD3D are not vendored.** proton-wine's own build should carry
   them; if not, that's a separate download step to add to `SetupManager`.

## Build

Push to GitHub, Actions tab, run "Build APK" — downloads NDK r25c +
CMake via `android-actions/setup-android`, builds with Gradle 8.7 via
`gradle/actions/setup-gradle` (no wrapper jar needed), uploads
`app-debug` artifact.
