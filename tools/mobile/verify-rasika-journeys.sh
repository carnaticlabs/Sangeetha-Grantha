#!/usr/bin/env bash
# TRACK-140 N01: run Rasika native journey tests against an explicit device.
#
# Usage:
#   bash tools/mobile/verify-rasika-journeys.sh android <serial>
#   bash tools/mobile/verify-rasika-journeys.sh ios <udid>
#
# Generic simulator destinations are refused. There is no silent fallback.
set -euo pipefail

usage() {
  echo "Usage: bash tools/mobile/verify-rasika-journeys.sh android <serial>" >&2
  echo "       bash tools/mobile/verify-rasika-journeys.sh ios <udid>" >&2
  echo "An explicit device id is required; generic/platform destinations are rejected." >&2
}

if [[ $# -lt 2 ]]; then
  usage
  exit 2
fi

PLATFORM="${1:-}"
DEVICE="${2:-}"

if [[ -z "$PLATFORM" || -z "$DEVICE" || "$DEVICE" == generic/* || "$DEVICE" == *"generic/platform"* ]]; then
  echo "Refusing generic or empty destination: '${DEVICE}'" >&2
  usage
  exit 2
fi

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_ROOT="${RASIKA_JOURNEY_OUTPUT:-$ROOT/build/track-140/journeys}"
mkdir -p "$OUT_ROOT"

case "$PLATFORM" in
  android)
    echo "Running Android journeys on serial $DEVICE"
    ANDROID_SERIAL="$DEVICE" ./gradlew -p "$ROOT" \
      :modules:mobile:androidApp:connectedDebugAndroidTest \
      --console=plain | tee "$OUT_ROOT/android-$DEVICE.log"
    ;;
  ios)
    PROJECT="$ROOT/modules/mobile/iosApp/RasikaApp.xcodeproj"
    DEST="platform=iOS Simulator,id=$DEVICE"
    echo "Running iOS journeys on $DEST"
    xcodebuild test \
      -project "$PROJECT" \
      -scheme RasikaApp \
      -configuration Debug \
      -destination "$DEST" \
      -derivedDataPath "${RASIKA_IOS_DERIVED_DATA:-$ROOT/build/track-140/ios-derived}" \
      CODE_SIGNING_ALLOWED=NO \
      | tee "$OUT_ROOT/ios-$DEVICE.log"
    ;;
  *)
    echo "Unknown platform '$PLATFORM'" >&2
    usage
    exit 2
    ;;
esac
