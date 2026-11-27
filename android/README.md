# Fakarni for Android

Reminders by **place and time**. Tap a spot on a Google Map, size the circle, write what to remember,
and optionally limit it to certain hours or a certain day. When you walk into the circle (and the time
rules match), your phone shows a notification, even if the app is closed.

## What's in this version
- Google Maps (Maps Compose + Maps SDK for Android).
- Tap the map to place a circle. Resize with the slider, or long-press the light-blue pin on the edge and drag.
- Size is shown as the **diameter** ("300 m across").
- Optional rules: only between certain hours, only on a specific day, once or every time.
- Works with the app closed (Google Play services geofencing), and comes back after a phone restart.
- List of reminders: switch on/off, tap to edit, delete from the editor. Tap a circle on the map to edit it.
- Everything stays on the phone.

## Known limits (from Android)
- Up to **100 places** at once.
- Circles under ~100 m across can be late or missed.
- You must choose **"Allow all the time"** for location, or reminders only work while the app is open.
- A reminder fires when you *enter* the circle. If you're already inside when its hours start, it waits until you next enter.

## Install on your phone (no PC needed)
1. One time: add your Google Maps key to GitHub (below). Without it the map is blank.
2. On your phone, open https://github.com/jihedjridi-hub/fakarni/releases/tag/latest and tap **fakarni.apk**.
3. Android asks to allow installing from your browser; allow it, then install.
4. Open Fakarni, allow location, then choose **"Allow all the time"** when asked, and allow notifications.

## Google Maps key (free tier)
1. https://console.cloud.google.com → create a project → enable **"Maps SDK for Android"**.
2. *Credentials → Create credentials → API key.* (Google asks for a billing card even on the free tier.)
3. Recommended: restrict the key to Android apps with package `com.fakarni.app`. The SHA-1 fingerprint
   is printed in the build log; ask Claude to fetch it.
4. In GitHub: repo *Settings → Secrets and variables → Actions → New repository secret*,
   name `MAPS_API_KEY`, paste the key. Then re-run the *Android build* (Actions tab → Run workflow).

## Building on your Windows PC (optional)
Install Android Studio, open the `android` folder, copy `local.properties.example` to `local.properties`
and paste your key, then press ▶︎ with your phone plugged in (USB debugging on).

## Guesses made (easy to change)
- App name "Fakarni", package `com.fakarni.app`.
- Android 8.0 or newer.
- Map starts on Tunis until GPS finds you.
- "Remind me only once" is on by default.
- The APK is signed with a test key: fine for installing yourself, not for Google Play.
