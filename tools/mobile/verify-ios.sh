#!/usr/bin/env bash
# TRACK-138: compile Rasika for the iOS Simulator.
#
# Destination order:
#   1. First argument, or RASIKA_IOS_DESTINATION
#   2. On GitHub Actions: generic/platform=iOS Simulator (compile only; no
#      CoreSimulator boot — hosted macos-15 simctl is flaky)
#   3. Else an available iPhone, preferring RASIKA_IOS_RUNTIME (default iOS 26.5)
#   4. Else generic/platform=iOS Simulator
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PROJECT="$ROOT/modules/mobile/iosApp/RasikaApp.xcodeproj"
SCHEME="RasikaApp"
PREFERRED_RUNTIME="${RASIKA_IOS_RUNTIME:-iOS 26.5}"
GENERIC_DEST="generic/platform=iOS Simulator"

pick_udid() {
  # Wake the simulator daemon before listing; a cold CoreSimulator often
  # times out the first simctl call on virtualized Macs.
  xcrun simctl list devices available >/dev/null 2>&1 || true
  xcrun simctl list devices available -j | python3 -c '
import json, re, sys

def norm(label: str) -> str:
    text = label.lower()
    if "simruntime." in text:
        text = text.split("simruntime.", 1)[1]
    return re.sub(r"[^a-z0-9]+", "-", text).strip("-")

def version_tuple(runtime: str) -> tuple[int, int]:
    match = re.search(r"(\d+)[-.](\d+)", runtime)
    if not match:
        return (0, 0)
    return (int(match.group(1)), int(match.group(2)))

prefer = norm(sys.argv[1])
data = json.load(sys.stdin)
ranked = []
for runtime, devices in data.get("devices", {}).items():
    runtime_key = norm(runtime)
    for device in devices:
        if not device.get("isAvailable") or "iPhone" not in device.get("name", ""):
            continue
        name = device.get("name", "")
        score = 0
        if prefer and prefer == runtime_key:
            score += 100
        if "iPhone 17" in name and "Pro" not in name and "Max" not in name:
            score += 10
        elif "iPhone 17" in name:
            score += 5
        ranked.append((score, version_tuple(runtime), name, device["udid"], runtime))
ranked.sort(reverse=True)
if not ranked:
    raise SystemExit("No available iPhone simulator")
chosen = ranked[0]
print("Picked", chosen[2], chosen[4], file=sys.stderr)
print(chosen[3])
' "$PREFERRED_RUNTIME"
}

if [[ -n "${1:-}" ]]; then
  DEST="$1"
elif [[ -n "${RASIKA_IOS_DESTINATION:-}" ]]; then
  DEST="$RASIKA_IOS_DESTINATION"
elif [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
  DEST="$GENERIC_DEST"
else
  if UDID="$(pick_udid)"; then
    DEST="platform=iOS Simulator,id=$UDID"
  else
    echo "No iPhone simulator available; using $GENERIC_DEST" >&2
    DEST="$GENERIC_DEST"
  fi
fi

echo "Using destination: $DEST"

SIGNING=()
if [[ "$DEST" == generic/* ]] || [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
  # Hosted runners have no developer identity; simulator Debug does not need one.
  SIGNING+=(CODE_SIGNING_ALLOWED=NO)
fi

xcodebuild -project "$PROJECT" -scheme "$SCHEME" -configuration Debug \
  -destination "$DEST" \
  -derivedDataPath "${RASIKA_IOS_DERIVED_DATA:-$ROOT/build/track-140/ios-derived}" \
  ${SIGNING[@]+"${SIGNING[@]}"} \
  build
