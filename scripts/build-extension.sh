#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$ROOT/.work"
OUT="$ROOT/dist"
INPUT="$ROOT/input/Rule34Video-v14.12-debug.apk"
APKTOOL_VERSION="2.11.1"
APKTOOL="$WORK/apktool.jar"

rm -rf "$WORK" "$OUT"
mkdir -p "$WORK" "$OUT"

curl -fL --retry 3 -o "$APKTOOL" "https://github.com/iBotPeaches/Apktool/releases/download/v${APKTOOL_VERSION}/apktool_${APKTOOL_VERSION}.jar"
java -jar "$APKTOOL" d -f -o "$WORK/decoded" "$INPUT"
python3 "$ROOT/patches/patch_extension.py" "$WORK/decoded"
java -jar "$APKTOOL" b "$WORK/decoded" -o "$OUT/Rule34Video-v14.12-fixed-unsigned.apk"

SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n "$SDK_ROOT" ]] || { echo 'Android SDK not found'; exit 1; }
BUILD_TOOLS="$SDK_ROOT/build-tools"
ZIPALIGN="$(find "$BUILD_TOOLS" -type f -name zipalign | sort -V | tail -1)"
APKSIGNER="$(find "$BUILD_TOOLS" -type f -name apksigner | sort -V | tail -1)"
[[ -x "$ZIPALIGN" && -x "$APKSIGNER" ]] || { echo 'Android build tools not found'; exit 1; }

keytool -genkeypair -v -keystore "$WORK/debug.keystore" -storepass android -alias androiddebugkey \
  -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
  -dname 'CN=Android Debug,O=Android,C=US'
"$ZIPALIGN" -f 4 "$OUT/Rule34Video-v14.12-fixed-unsigned.apk" "$OUT/Rule34Video-v14.12-fixed-aligned.apk"
"$APKSIGNER" sign --ks "$WORK/debug.keystore" --ks-pass pass:android --ks-key-alias androiddebugkey \
  --key-pass pass:android --out "$OUT/Rule34Video-v14.12-fixed.apk" "$OUT/Rule34Video-v14.12-fixed-aligned.apk"
"$APKSIGNER" verify --verbose "$OUT/Rule34Video-v14.12-fixed.apk"
sha256sum "$OUT/Rule34Video-v14.12-fixed.apk"
