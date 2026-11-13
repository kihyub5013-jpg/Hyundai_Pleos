#!/usr/bin/env bash
# 빌드 → 에뮬레이터 설치(권한 자동 허용) → 실행.  사전: Pleos 에뮬레이터가 adb 에 보여야 한다.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f ~/android-sdk/env.sh ] && source ~/android-sdk/env.sh
./gradlew -q assembleDebug
adb install -r -g app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb shell "am start --user current -n com.yikihyub.drivingpromise/.ui.MainActivity" >/dev/null
echo "실행했어요. 로그: adb logcat -s Companion VehicleGateway SpeedGateway NaviGateway SpeechGateway"
