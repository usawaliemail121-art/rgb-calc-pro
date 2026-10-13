# RGB Calc Pro

A neon cyberpunk calculator for Android, built with Kotlin and standard XML
layouts. Published under the **UtilityNest** developer account.

## What it does

- Full calculator: `+ - × ÷ %`, brackets, sign flip, decimals, delete, clear.
- Live result preview as you type, with graceful "Cannot divide by 0" handling.
- Persistent history with timestamps; tap any entry to restore it.
- Tap-sound customisation (Mechanical Click, Digital Beep, Soft Bubble, Haptic Only).
- 100% offline. **Zero permissions** declared in the manifest.

## Building

The app is built automatically by GitHub Actions on every push (see
`.github/workflows/android-build.yml`). Each run produces two artifacts:

- `rgb-calc-pro-release-aab` — the signed bundle to upload to Google Play.
- `rgb-calc-pro-debug-apk` — install this on a phone to test.

### How to download the build

1. Open the **Actions** tab of this repository.
2. Click the newest green run named *Build RGB Calc Pro*.
3. Scroll to **Artifacts** and download `rgb-calc-pro-release-aab`.

## Signing

Release signing uses an upload keystore supplied through encrypted GitHub
Actions secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`). No key material is stored in this repository.

Keep your upload keystore safe — Google Play ties future updates to it.

## Tech

- Kotlin, standard Android XML layouts (no Jetpack Compose).
- `compileSdk 35`, `targetSdk 35`, `minSdk 24`.
- Only AndroidX defaults: `core-ktx`, `appcompat`, `material`.
- No third-party maths or UI libraries.
