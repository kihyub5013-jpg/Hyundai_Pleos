#!/usr/bin/env bash
# .env 의 PLEOS_CRN 을 에뮬레이터 VHAL(propId 554696961)에 넣고 재부팅한다.
# Pleos 서버 기반 STT/TTS(registerApp)가 이 CRN 으로 기기를 식별한다. 에뮬레이터를 새로 만들 때 한 번.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f ~/android-sdk/env.sh ] && source ~/android-sdk/env.sh
set -a; source .env; set +a
[ -n "${PLEOS_CRN:-}" ] || { echo ".env 에 PLEOS_CRN 이 없어요" >&2; exit 1; }
adb root
adb wait-for-device
adb shell su 0 "echo 'propId: 554696961 areaId: 0 values: ${PLEOS_CRN}' > /data/vendor/vsomeip/vhal_fifo"
adb reboot
echo "CRN 설정 후 재부팅합니다: ${PLEOS_CRN}"
