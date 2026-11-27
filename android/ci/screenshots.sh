#!/usr/bin/env bash
# Drives the app in an emulator and saves screenshots of each screen to android/screenshots/.
set -u
OUT=screenshots; mkdir -p "$OUT"
APK=app/build/outputs/apk/release/app-release.apk

tap_text() {   # tap the center of the first UI element whose text or description matches $1
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  python3 - "$1" "${2:-0}" <<'PY'
import sys, re, xml.etree.ElementTree as ET, subprocess
want, dx = sys.argv[1], int(sys.argv[2])
for n in ET.parse('/tmp/ui.xml').iter('node'):
    if want in (n.get('text') or '') or want in (n.get('content-desc') or ''):
        x1,y1,x2,y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        x = (x1+x2)//2 if dx == 0 else dx
        subprocess.run(['adb','shell','input','tap',str(x),str((y1+y2)//2)])
        print('tapped', want); break
else:
    print('NOT FOUND', want)
PY
}
shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "saved $1"; }

adb emu geo fix 10.1815 36.8065
adb install -g "$APK"
adb shell pm grant com.fakarni.app android.permission.ACCESS_BACKGROUND_LOCATION || true
adb shell settings put system screen_off_timeout 600000
adb shell am start -n com.fakarni.app/.MainActivity
shot 1-map 25

read W H < <(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1 | tr x ' ')
adb shell input tap $((W/2)) $((H/2 - 150))
shot 2-circle 6

tap_text "Next"
sleep 2
tap_text "What should I remind you"
adb shell input text "Buy%sbread"
adb shell input keyevent 111   # hide keyboard (ESC)
sleep 1
tap_text "Only between certain hours" $((W - 120))
shot 3-editor 2

tap_text "Save"
shot 4-saved 3

tap_text "Reminders"
shot 5-list 3

# Keep the Google Maps messages, to diagnose a blank map (bad key, API not enabled, etc.)
adb logcat -d | grep -iE "Google Maps|Authorization|API key|API_KEY|maps_api|MapsInitializer|gmm|Ensure that the" > "$OUT/maps-log.txt" || true
