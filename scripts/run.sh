#!/usr/bin/env bash
# 빌드 → (부팅 대기) → 에뮬레이터 설치(권한 자동 허용) → 실행.  에뮬레이터: ~/android-sdk/run-pleos.sh
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f ~/android-sdk/env.sh ] && source ~/android-sdk/env.sh
./gradlew -q assembleDebug
# 에뮬레이터를 막 띄운 경우: 부팅이 끝날 때까지 기다린다 (최대 5분)
echo "에뮬레이터 부팅을 기다리는 중..."
timeout 300 adb wait-for-device
timeout 300 bash -c 'until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d "\r")" = 1 ]; do sleep 3; done' \
  || { echo "에뮬레이터가 5분 안에 부팅되지 않았어요. ~/android-sdk/run-pleos.sh 가 떠 있는지 확인하세요." >&2; exit 1; }
adb install -r -g app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb shell "am start --user current -n com.yikihyub.drivingpromise/.ui.MainActivity" >/dev/null
echo "실행했어요. 로그: adb logcat -s Companion VehicleGateway SpeedGateway NaviGateway SpeechGateway"
