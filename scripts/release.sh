#!/usr/bin/env bash
# 제출용 release APK 를 만들어 apk/ 에 둔다.  필요: keystore.properties (서명), .env (Pleos 키)
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f ~/android-sdk/env.sh ] && source ~/android-sdk/env.sh
[ -f keystore.properties ] || { echo "keystore.properties 가 없어요 (README 7장 참고)." >&2; exit 1; }
./gradlew -q clean assembleRelease
mkdir -p apk
OUT="apk/이기협_내운전약속_release.apk"
cp app/build/outputs/apk/release/app-release.apk "$OUT"
"$ANDROID_HOME"/build-tools/*/apksigner verify --print-certs "$OUT" | head -1
ls -l "$OUT"
