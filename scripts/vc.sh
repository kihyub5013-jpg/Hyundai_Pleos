#!/usr/bin/env bash
# Pleos Virtual Console HTTP API로 페달 확인 안내 시나리오를 재생한다.
#   사용: scripts/vc.sh parking   # R 기어 + 브레이크 없이 2→16km/h  → "지금 엑셀이에요"
#         scripts/vc.sh weak      # 브레이크 밟은 채 25→34km/h      → "브레이크 끝까지"
#         scripts/vc.sh launch    # 정차 후 브레이크 떼고 0→15km/h  → "발 확인하세요"
#         scripts/vc.sh normal    # 보통 출발·감속·정지                   → 안내 없음
#         scripts/vc.sh gear Drive | speed 30 | brake 100
# Virtual Console 은 1초마다 /browser/session heartbeat 가 있어야 신호를 낸다. 이 스크립트가 대신 보낸다.
set -euo pipefail
VC=${VC:-http://127.0.0.1:8088}
adb forward tcp:8088 tcp:8088 >/dev/null
heartbeat() { while :; do curl -s -m 2 -X POST "$VC/browser/session?client=promise-demo" >/dev/null 2>&1 || true; sleep 1; done; }
heartbeat & HB=$!
trap 'kill $HB 2>/dev/null || true' EXIT
sleep 2

# Virtual Console 은 실패해도 400 을 돌려줄 뿐이라 상태 코드를 확인한다 (예: 차속은 정수만 받음)
post()  { c=$(curl -s -m 5 -o /dev/null -w "%{http_code}" -X POST "$VC/$1"); [ "$c" = 200 ] || echo "요청 실패($c): $1" >&2; }
gear()  { post "vehicle/gear?gear=$1"; echo "기어 → $1"; }
ctl()   { post "vehicle/controls?speed_kph=$1&brake=$2"; }
# ramp <from> <to> <ms> <brake%>  (차속은 정수 km/h)
ramp()  { python3 -c "
f,t,ms=$1,$2,$3; n=max(1,ms//100)
for i in range(1,n+1): print(round(f+(t-f)*i/n))" | while read v; do ctl "$v" "$4"; sleep 0.1; done; }

case "${1:-parking}" in
  gear)  gear "$2" ;;
  speed) ctl "$2" 0 ;;
  brake) post "vehicle/controls?brake=$2" ;;
  parking)
    gear Drive; ctl 0 100; sleep 2
    gear Reverse; ctl 0 100; sleep 2
    ctl 2 0; sleep 1
    echo "브레이크 없이 2→16km/h (1.2초)"; ramp 2 16 1200 0
    sleep 2; echo "브레이크로 정지"; ramp 16 0 1500 100; gear Park ;;
  weak)
    gear Drive; ctl 0 0; sleep 1; echo "보통 출발 0→25km/h (6초, 안내 없어야 함)"; ramp 0 25 6000 0; sleep 3
    echo "브레이크 밟은 채 25→34km/h"; ramp 25 34 2500 100
    sleep 1; ramp 34 0 3000 100; gear Park ;;
  normal)
    gear Drive; ctl 0 100; sleep 2; ctl 0 0; sleep 0.5
    echo "보통 출발 0→30km/h (7초)"; ramp 0 30 7000 0; sleep 2
    echo "브레이크로 30→0km/h (5초)"; ramp 30 0 5000 100; gear Park ;;
  launch)
    gear Drive; ctl 0 100; sleep 2; ctl 0 0; sleep 0.3
    echo "정차 후 0→15km/h (1초)"; ramp 0 15 1000 0
    sleep 1; ramp 15 0 1500 100; gear Park ;;
esac
sleep 1
