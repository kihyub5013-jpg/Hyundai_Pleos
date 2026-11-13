# 내 운전 약속

Pleos Connect 차량 앱. **고령 운전자가 스스로 정한 운전 조건(밤·비·고속도로·장거리)을 차가 함께 지켜주고, 위험한 순간에는 "지금 밟은 페달"을 음성으로 알려준다.**
기획서: [docs/proposal.md](docs/proposal.md)

## 기능과 SDK

| 기능 | 코드 | Pleos SDK / API |
|---|---|---|
| 페달 확인 안내 (엑셀·브레이크 덜 밟음·급출발) | `domain/PedalGuard.kt` | Vehicle Brake·Gear + AAOS `PERF_VEHICLE_SPEED`, TTS ON_DEVICE |
| 깜짝 지점 저장·다음 방문 때 미리 알림 | `Companion.saveSpot / watchSpots` | FusedLocation `addProximityAlert` |
| 오늘의 차 상태 한마디 | `domain/CarCheck.kt` | Vehicle Safety(엔진오일·브레이크액·경고등), Fuel, EvBattery |
| 해 지기 전 도착 확인 | `domain/Sun.kt`, `DepartureCheck` | NaviHelper DestinationInfo(남은 시간), FusedLocation(일몰 계산 위치) |
| 편한 길·무료도로 우선 출발 | `Companion.routeOption / go` | NaviHelper `requestRoute(COMFORTABLE/FREE)`, `changeRouteOption` |
| 좌회전·회전교차로 미리 알림, 방향지시등 도우미 | `domain/DriveCoach.kt` | NaviHelper `onTBTInfo`, Vehicle TurnSignal·SteeringWheel |
| 사고다발·스쿨존·철길 미리 알림 | `DriveCoach.onCaution` | NaviHelper `onCautionPointInfo` |
| 비 오면 차분 모드, 해질녘 전조등 | `DriveCoach.onWiper / checkHeadlight` | Vehicle Wiper·Light(하향등) |
| 1시간마다 쉬기 | `DriveCoach.onDrivingMinutes` | Vehicle Odometer `TripCurrentDrivingTime` |
| 주차장 천천히·주차 위치 | `Companion.onNaviEvent` | NaviHelper `onDestinationArrived`, `getCurrentLocationInfo` |
| 말로 목적지·약속 설정 | `domain/Voice.kt` | SpeechToText(HYBRID + registerApp), NaviHelper 즐겨찾기·최근 목적지 |
| 이달의 약속·가족 공유 미리보기 | `domain/Monthly.kt`, `ui/screens/MonthScreen.kt` | Vehicle Odometer (차 안 파일 저장) |

모든 안내는 음성이 먼저다. 주행 중(UX 제한)에는 "오늘" 화면만 남고, 페달 안내는 전체 화면으로 덮는다.

## 설정 (.env)

루트 `.env`(커밋 금지)를 빌드 때 읽어 `BuildConfig` 로 넣는다. 형식은 `.env.example`.

| 키 | 쓰는 곳 |
|---|---|
| `PLEOS_CLIENT_ID`, `PLEOS_CLIENT_SECRET` | SpeechToText `registerApp` → HYBRID 인식. 비우면 ON_DEVICE |
| `PLEOS_PROJECT_ID` | 설정 화면 표시 |
| `PLEOS_CRN` | `scripts/set-devbox-crn.sh` 가 에뮬레이터 VHAL 에 주입 (서버 음성 인식용 기기 식별) |

## 빌드·실행

```bash
source ~/android-sdk/env.sh
./gradlew testDebugUnitTest assembleDebug   # 도메인 테스트 30개
./scripts/set-devbox-crn.sh                 # 에뮬레이터 새로 만들었을 때 한 번 (adb root → CRN 주입 → 재부팅)
./scripts/run.sh                            # 설치·실행
```

### 시연
- **Virtual Console:** 기어 R → 브레이크 끔 → 속도를 1초 안에 2→16km/h 로 올리면 "지금 엑셀이에요! 브레이크로 바꾸세요".
- **Virtual Console 없이:** 설정·시연 탭의 시나리오 3개(주차장 후진 페달 혼동 / 브레이크 덜 밟음 / 정차 후 급출발). 시연 값은 기록·깜짝 지점에 남지 않는다.

## 기획서와 다른 점 (SDK 확인 결과)
- Tire `getLowTirePressure` 는 현재 공기압이 아니라 저압 **임계값**만 준다. 타이어는 차 상태 한마디에서 뺐다.
- 경고등(Telltale)은 `PRIVATE_CAR_SAFETY`(signature|privileged) 라 일반 앱에서는 보통 비어 있다. 읽히면 노란 경고로 표시한다.
- 차속은 Pleos Vehicle 에 없어 AAOS `PERF_VEHICLE_SPEED`(CAR_SPEED 런타임 권한). 못 쓰면 위치 변화로 계산한 속도로 대신한다.
- 가족 공유 서버는 3단계. 지금은 보낼 문장 미리보기만.
# Hyundai_Pleos
