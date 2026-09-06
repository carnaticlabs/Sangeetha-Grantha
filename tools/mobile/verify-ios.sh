#!/usr/bin/env bash
# TRACK-138: build Rasika on a concrete arm64 iPhone simulator.
# Prefers an iOS 26.5 iPhone when that runtime is installed.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PROJECT="$ROOT/modules/mobile/iosApp/RasikaApp.xcodeproj"
SCHEME="RasikaApp"
PREFERRED_RUNTIME="${RASIKA_IOS_RUNTIME:-iOS 26.5}"

pick_udid() {
  xcrun simctl list devices available -j | python3 -c '
import json, sys
prefer = sys.argv[1]
data = json.load(sys.stdin)
ranked = []
for runtime, devices in data.get("devices", {}).items():
    for device in devices:
        if not device.get("isAvailable") or "iPhone" not in device.get("name", ""):
            continue
        score = 0
        if prefer in runtime:
            score += 100
        if "iPhone 17" in device.get("name", "") and "Pro" not in device.get("name", "") and "Max" not in device.get("name", ""):
            score += 10
        elif "iPhone 17" in device.get("name", ""):
            score += 5
        ranked.append((score, device["udid"], device.get("name", ""), runtime))
ranked.sort(reverse=True)
if not ranked:
    raise SystemExit("No available iPhone simulator")
print(ranked[0][1])
' "$PREFERRED_RUNTIME"
}

if [[ -n "${1:-}" ]]; then
  DEST="$1"
else
  UDID="$(pick_udid)"
  DEST="platform=iOS Simulator,id=$UDID"
fi

echo "Using destination: $DEST"
xcodebuild -project "$PROJECT" -scheme "$SCHEME" -configuration Debug \
  -destination "$DEST" \
  -derivedDataPath "$ROOT/build/track-138/ios-derived" \
  build
