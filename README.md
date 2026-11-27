# Location Reminders (iOS, first version)

Reminders that fire by **place and time**. Tap a spot on a Google Map, size the circle,
write what to remember, and optionally limit it to certain hours or a certain day.
When you walk into the circle (and the time rules match), your iPhone shows a notification.

## What's in this version
- Google Maps (not Apple Maps) via the official Google Maps SDK for iOS.
- Tap the map to place a circle. Resize with the slider, or press-and-hold the white handle on the edge and drag.
- The size is shown as the **diameter** ("300 m across").
- Optional rules: only between certain hours (e.g. 08:00–10:00), only on a specific day, fire once or every time.
- Works with the app closed: iOS watches the circles and wakes the app up.
- List of reminders: switch on/off, edit, swipe to delete. Tap a circle on the map to edit it.
- Everything stays on the phone (no account, no server).

## Known limits (from iOS, not from this app)
- iOS watches at most **20 places** per app at once. If you have more, the 20 closest are watched.
- Circles under ~100 m across can trigger late or not at all; GPS isn't that precise in the background.
- The user must allow location **"Always"**, or reminders only work while the app is open.

## What you need to run it
1. **A Mac with Xcode** (free from the Mac App Store). Apple only allows building iPhone apps on a Mac.
   No Mac? Options: borrow one, rent a cloud Mac (e.g. MacinCloud), or use a cloud build service
   such as Codemagic or GitHub Actions to build and send it to your phone through TestFlight.
2. **A Google Maps API key** (free tier is generous):
   - Go to https://console.cloud.google.com, create a project.
   - Enable **"Maps SDK for iOS"**.
   - Credentials → Create credentials → API key. Restrict it to iOS apps with bundle id
     `com.example.locationreminders` (or your own, see below).
   - Google asks for a billing card even for the free tier.
3. **To install on your own iPhone:** a free Apple ID works for testing (the app expires after 7 days);
   the $99/year Apple Developer Program is needed for TestFlight or the App Store.

## Building from Windows (GitHub + TestFlight)
No Mac needed. GitHub's Mac machines build the app, and TestFlight puts it on your iPhone.

**Build check** (`.github/workflows/build.yml`): runs on every push and tells you if the code compiles. Needs nothing.

**TestFlight** (`.github/workflows/testflight.yml`): run it by hand from the repo's *Actions* tab. One-time setup:
1. Join the Apple Developer Program ($99/year): https://developer.apple.com/programs/
2. In App Store Connect (https://appstoreconnect.apple.com):
   - *Apps → +  → New App*, with bundle id `com.example.locationreminders` (or your own; then set a
     repository **variable** `BUNDLE_ID` to match).
   - *Users and Access → Integrations → App Store Connect API → +*, role **App Manager**. Download the `.p8` file
     (only possible once) and note the **Key ID** and **Issuer ID**.
3. Find your **Team ID** at https://developer.apple.com/account (Membership details).
4. In the GitHub repo: *Settings → Secrets and variables → Actions → New repository secret*, add:
   | Secret | Value |
   |---|---|
   | `GOOGLE_MAPS_API_KEY` | your Google Maps key |
   | `APPLE_TEAM_ID` | Team ID |
   | `ASC_KEY_ID` | Key ID |
   | `ASC_ISSUER_ID` | Issuer ID |
   | `ASC_KEY_P8_BASE64` | the .p8 file in base64. On Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("AuthKey_XXXX.p8"))` |
5. Install the **TestFlight** app on your iPhone. After a run finishes, the build appears in App Store Connect
   under TestFlight (Apple takes 5 to 30 minutes to process it). Add yourself as an internal tester and install.

## Steps on a Mac (alternative)
```bash
brew install xcodegen          # one time (needs Homebrew: https://brew.sh)
cd LocationReminders
# Paste your key into Config/Secrets.xcconfig  (GOOGLE_MAPS_API_KEY = ...)
xcodegen                       # creates LocationReminders.xcodeproj
open LocationReminders.xcodeproj
```
In Xcode: pick your team under *Signing & Capabilities*, change the bundle id if you like,
choose a simulator or your iPhone, and press ▶︎.

Testing arrival in the simulator: *Features → Location → Custom Location…* and type a point
inside one of your circles.

## Project layout
```
project.yml                 XcodeGen spec (creates the Xcode project, pulls Google Maps)
Config/Secrets.xcconfig     your API key (git-ignored)
LocationReminders/
  App/        app entry point, Google Maps setup
  Models/     Reminder + TimeWindow
  Services/   saving, location permission, region watching (CLMonitor), notifications
  Views/      map screen, Google Map wrapper, editor, list
```

## Guesses made (easy to change)
- Minimum iOS version 17 (needed for Apple's newer region monitoring API).
- Starting map position is Tunis until GPS finds you.
- Bundle id `com.example.locationreminders`.
- Circle size 100 m to 4 km across.
- After a reminder fires once it switches off ("Remind me only once" is on by default).
