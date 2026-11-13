# Pleos Connect SDK 상세 설명서

> 출처: [Pleos Playground 개발자 문서 — SDK 사용 가이드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/) 및 각 SDK의 API Reference(Dokka)
> 정리일: 2026-10-26 · 공식 문서의 가이드 페이지와 API Reference 전체(약 1,860페이지)를 기준으로 정리했습니다. 실제 개발 시에는 최신 공식 문서를 함께 확인하세요.

## 0. 개요

Pleos Connect SDK는 Pleos Connect 환경에서 동작하는 인포테인먼트 앱 개발용 통합 도구입니다. Android Automotive OS(AAOS)를 기반으로 표준화된 인터페이스를 제공하므로, 복잡한 하드웨어 연동 없이 차량 관련 앱을 만들 수 있습니다.

| SDK | 역할 | 진입점 | 아티팩트 (기준 버전) |
|---|---|---|---|
| Vehicle SDK | 차량 상태 조회 및 차량 제어 | `ai.pleos.playground.vehicle.Vehicle` | `ai.pleos.playground:Vehicle:2.3.2` |
| NaviHelper SDK | 경로 안내 제어, 내비게이션 정보 조회 | `ai.pleos.playground.navi.helper.NaviHelper` | `ai.pleos.playground:NaviHelper:3.0.5` |
| TextToSpeech SDK | 음성 합성(TTS) | `ai.pleos.playground.tts.TextToSpeech` | `ai.pleos.playground:TextToSpeech:3.3.1` |
| SpeechToText SDK | 음성 인식(STT) | `ai.pleos.playground.stt.SpeechToText` | `ai.pleos.playground:SpeechToText:3.3.1` |
| FusedLocation SDK | 차량 위치 정보 | `ai.pleos.playground.location.fused.FusedLocation` | `ai.pleos.playground:FusedLocation:1.1.3` |

기능별로 묶으면 다음과 같습니다.

- **차량 제어 및 모니터링**: Vehicle SDK, FusedLocation SDK
- **내비게이션 및 음성 서비스**: NaviHelper SDK, SpeechToText SDK, TextToSpeech SDK

### 0.1 공통 Gradle 설정

모든 SDK는 Pleos Maven 저장소에서 받습니다.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
}
```

### 0.2 API 정책

- AAOS에 같거나 비슷한 기능의 API가 있으면 플랫폼 파편화를 줄이기 위해 **Android Automotive에 맞추는 것**이 원칙입니다.
- Vehicle SDK 기능이 이후 Android Automotive에 들어가면, 해당 기능은 **최소 2개 major 버전 동안 하위 호환**이 보장됩니다. 그 뒤의 유지·변경·제거는 별도 정책과 릴리스 노트로 안내됩니다.

### 0.3 Vehicle SDK API 사용과 앱 심사

- API 지원 여부는 도메인별 `checkXxxCapabilities` API로 확인합니다.
- 사용하는 API에 필요한 퍼미션은 앱에 선언해야 합니다. 퍼미션은 **Pleos App Market과 미리 협의해 승인받은 범위 안에서만** 쓸 수 있습니다.
- 승인되지 않은 퍼미션을 선언하거나 해당 API를 호출하면 **앱 심사를 통과할 수 없습니다** (App Review Guidelines 참고).

### 목차

1. [Vehicle SDK](#1-vehicle-sdk)
2. [NaviHelper SDK](#2-navihelper-sdk)
3. [TextToSpeech SDK](#3-texttospeech-sdk)
4. [SpeechToText SDK](#4-speechtotext-sdk)
5. [FusedLocation SDK](#5-fusedlocation-sdk)

## 1. Vehicle SDK

### 1.1 개요

#### 1.1.1 Vehicle SDK란

Vehicle SDK는 **차량 정보 조회 및 제어**를 위한 API를 제공합니다. 3rd party 앱은 Vehicle SDK를 통해 Pleos Connect의 **Vehicle Service**에 연결합니다. Vehicle Service는 Android Automotive OS(AAOS) 위에서 동작하며, 앱이 Vehicle SDK API로 차량 정보 조회·제어를 요청하면 Vehicle Service가 이를 처리합니다.

| 항목 | 내용 |
|---|---|
| 기준 버전 | Vehicle SDK **v2.3.2** / Vehicle Service **v2.3.21** |
| 진입점(Entry point) | `ai.pleos.playground.vehicle.Vehicle` 클래스 (`class Vehicle(context: Context)`) |
| 도메인 API 패키지 | `ai.pleos.playground.vehicle.api` (Brake, CarInfo, Display, Door, DrivingMode, EvBattery, Fuel, HVAC, Light, Odometer, Safety, Seat, SideMirror, SteeringWheel, Tire, Toll, TurnSignal, Window, Wiper — 총 19개 인터페이스) |
| 모델 패키지 | `.api.model.area`, `.api.model.capability`, `.api.model.common`, `.api.model.telltale` |
| 상수 패키지 | `.constant`(ErrorCode, PleosPermission), `.constant.area`, `.constant.control`, `.constant.telltale`, `.constant.unit` |
| 리스너 패키지 | `ai.pleos.playground.vehicle.listener` (공통 부모 `VehicleListener`) |
| 예외 | `ai.pleos.playground.vehicle.callback.exception.VehicleException` |
| 개발/검증 환경 | Pleos Connect Emulator (AVD + Pleos Connect Virtual Console) |

#### 1.1.2 패키지 구조

| 패키지 | 내용 |
|---|---|
| `ai.pleos.playground.vehicle` | 진입점 `Vehicle` 클래스 (`initialize()`, `release()`, `getXxx()` 팩토리 메서드) |
| `ai.pleos.playground.vehicle.api` | 도메인별 인터페이스 19종 |
| `ai.pleos.playground.vehicle.api.model.area` | 영역(Area) 모델: `Area`(sealed), `DoorArea`, `MirrorArea`, `SeatArea`, `TurnSignalArea`, `WheelArea`, `WindowArea` |
| `ai.pleos.playground.vehicle.api.model.capability` | Capability 모델: `AccessType`, `CapabilityConfig`, `AreaBounds<T>`, `HvacTemperatureConfigArray`, `SmartGlassConfig` |
| `ai.pleos.playground.vehicle.api.model.common` | `TripStatistics<T>` |
| `ai.pleos.playground.vehicle.api.model.telltale` | `Telltales`, `TelltaleData` |
| `ai.pleos.playground.vehicle.constant` | `ErrorCode`, `PleosPermission` |
| `ai.pleos.playground.vehicle.constant.area` | 영역 값 enum: `AreaValue`(interface), `DoorValue`, `MirrorValue`, `SeatValue`, `TurnSignalValue`, `WheelValue`, `WindowValue` |
| `ai.pleos.playground.vehicle.constant.control` | 제어/상태 enum: `CruiseControl`, `DrivingModeType`, `FanDirectionType`, `LightState`, `LightSwitch`, `MoveControl`, `OilLevelState`, `WiperState`, `WiperSwitch`, `SmartGlassZoneCommand`, `TripStatisticType` 등 |
| `ai.pleos.playground.vehicle.constant.telltale` | `TelltaleType`, `TelltaleState`, `TelltaleColor` |
| `ai.pleos.playground.vehicle.constant.unit` | 단위 enum: `TemperatureUnit`, `SpeedDisplayUnit`, `DistanceDisplayUnit`, `EvBatteryDisplayUnit` |
| `ai.pleos.playground.vehicle.listener` | 상태 변화 구독용 리스너 인터페이스 (`VehicleListener` 상속) |
| `ai.pleos.playground.vehicle.callback.exception` | `VehicleException` |

#### 1.1.3 일반적인 사용 흐름

1. `Vehicle(context)`로 인스턴스를 생성하고 `initialize()`로 차량 서비스에 연결합니다.
2. `vehicle.getDoor()`, `vehicle.getDisplay()` 등 `getXxx()`로 **도메인 객체**를 얻습니다. (도메인 객체는 직접 생성하지 않습니다.)
3. 도메인 객체에서 **Capability → Get / Set / Callback** API를 호출합니다.
   - `checkXxxCapabilities` : 기능 지원 여부·허용 값 범위 확인
   - `getCurrentXxx` / `getXxx` / `hasXxx` : 현재 값 조회
   - `setXxx` / `moveXxx` / `selectXxx` : 값 설정·제어
   - `registerXxx` / `unregisterXxx` : 상태 변화 구독/해제
4. 사용이 끝나면 `release()`로 리소스를 해제합니다.

> 차량 통신은 **IPC**이므로 capability·get·set 결과는 즉시 반환되지 않고 `onComplete` / `onFailed` **비동기 람다**로 전달됩니다. **비동기 콜백이 끝나기 전에 `release()`를 호출하지 않도록** 주의해야 합니다.

#### 1.1.4 Gradle 설정

**(1) Maven 저장소 등록** — 앱 프로젝트의 `settings.gradle(.kts)`

```kotlin
// settings.gradle.kts (Kotlin DSL)
dependencyResolutionManagement {
    ...
    repositories {
        maven {
            url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
        }
        ...
    }
}
```

```groovy
// settings.gradle (Groovy DSL)
dependencyResolutionManagement {
    ...
    repositories {
        ...
        maven {
            url "https://nexus-playground.pleos.ai/repository/maven-releases/"
        }
        ...
    }
}
```

**(2) SDK 의존성 추가** — 앱 모듈의 `build.gradle(.kts)`

```kotlin
// Kotlin DSL
dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
}
```

```groovy
// Groovy DSL
dependencies {
    implementation "ai.pleos.playground:Vehicle:2.3.2"
}
```

| 항목 | 값 |
|---|---|
| Maven 저장소 URL | `https://nexus-playground.pleos.ai/repository/maven-releases/` |
| 의존성 좌표 | `ai.pleos.playground:Vehicle:2.3.2` |

퍼미션 선언(`AndroidManifest.xml`)은 [1.2.7 퍼미션 모델](#127-퍼미션-모델)을 참조하세요.

#### 1.1.5 라이프사이클 (`Vehicle` 클래스)

`class Vehicle(context: Context)` — Vehicle SDK의 진입점. `context`는 차량 서비스에 바인딩하는 데 사용하는 Android `Context`입니다.

| 멤버 | 설명 |
|---|---|
| `constructor(context: Context)` | Vehicle 인스턴스 생성 |
| `fun initialize()` | SDK 초기화(차량 서비스 연결). 애플리케이션이 생성될 때 호출 |
| `fun release()` | SDK 해제. 애플리케이션이 종료(destroy)될 때 호출 |
| `fun getBrake(): Brake` | Brake API 객체 획득 |
| `fun getCarInfo(): CarInfo` | CarInfo API 객체 획득 |
| `fun getDisplay(): Display` | Display API 객체 획득 |
| `fun getDoor(): Door` | Door API 객체 획득 |
| `fun getDrivingMode(): DrivingMode` | DrivingMode API 객체 획득 |
| `fun getEvBattery(): EvBattery` | EvBattery API 객체 획득 |
| `fun getFuel(): Fuel` | Fuel API 객체 획득 |
| `fun getHVAC(): HVAC` | HVAC API 객체 획득 |
| `fun getLight(): Light` | Light API 객체 획득 |
| `fun getOdometer(): Odometer` | Odometer API 객체 획득 |
| `fun getSafety(): Safety` | Safety API 객체 획득 |
| `fun getSeat(): Seat` | Seat API 객체 획득 |
| `fun getSideMirror(): SideMirror` | SideMirror API 객체 획득 |
| `fun getSteeringWheel(): SteeringWheel` | SteeringWheel API 객체 획득 |
| `fun getTire(): Tire` | Tire API 객체 획득 |
| `fun getToll(): Toll` | Toll API 객체 획득 |
| `fun getTurnSignal(): TurnSignal` | TurnSignal API 객체 획득 |
| `fun getWindow(): Window` | Window API 객체 획득 |
| `fun getWiper(): Wiper` | Wiper API 객체 획득 |

> 참고: `Vehicle` 클래스 API Reference의 Callback 예제에는 `vehicle.getGear()` / `GearSelectionListener` / `VehicleGear`가 등장하지만, v2.3.2 Reference의 `Vehicle` 멤버 목록과 `.api`·`.listener` 패키지에는 `Gear` 도메인이 존재하지 않습니다. 실제 구현 시에는 아래 도메인 목록의 API를 사용하세요.

#### 1.1.6 도메인 요약

| 도메인 | 획득 메서드 | 개요 (Reference 기준) | 대표 퍼미션 |
|---|---|---|---|
| Brake | `getBrake()` | 브레이크 페달 상태 | `CAR_DRIVE` |
| CarInfo | `getCarInfo()` | 차량 식별 정보(VIN, CRN) | `CAR_INFO` |
| Display | `getDisplay()` | 온도 표시 단위 | `CAR_INFO` |
| Door | `getDoor()` | 도어 잠금, 도어 위치(열림), 글로브박스 잠금 | `CAR_DOORS` / `CONTROL_CAR_DOORS` |
| DrivingMode | `getDrivingMode()` | 드라이브 모드, 크루즈 컨트롤 | `CAR_DRIVE` |
| EvBattery | `getEvBattery()` | EV 배터리 충전량(SoC), 전비, 충전 제어 | `CAR_ENERGY` / `CONTROL_CAR_ENERGY` |
| Fuel | `getFuel()` | 연료 잔량, 연비 | `CAR_ENERGY` |
| HVAC | `getHVAC()` | 공조(전원, A/C, 오토, 내외기, 온도, 풍량, 풍향, 디프로스터 등) | `CAR_CLIMATE` / `CONTROL_CAR_CLIMATE` |
| Light | `getLight()` | 외부등(상·하향등, 안개등, 미등), 실내등, 독서등, 앰비언트 라이트 | `CAR_LIGHTS` / `CONTROL_CAR_LIGHTS` |
| Odometer | `getOdometer()` | 누적 주행거리, 트립 주행거리·시간 | `CAR_DRIVE` |
| Safety | `getSafety()` | 에어백, 안전벨트 경고, 오일/브레이크액/워셔액, 경고등(Telltale) | `CAR_SAFETY` (Telltale: `PRIVATE_CAR_SAFETY`) |
| Seat | `getSeat()` | 시트 위치·각도 이동, 착석, 안전벨트, 시트 열선/통풍, 시트 메모리 | `CAR_SEATS` / `CONTROL_CAR_SEATS` |
| SideMirror | `getSideMirror()` | 사이드미러 열선 | `CAR_MIRRORS` / `CONTROL_CAR_MIRRORS` |
| SteeringWheel | `getSteeringWheel()` | 전륜 조향각, 스티어링휠 열선 | `CAR_DRIVE`, `CAR_CLIMATE` / `CONTROL_CAR_CLIMATE` |
| Tire | `getTire()` | 타이어 저압 임계값 | `CAR_SAFETY` |
| Toll | `getToll()` | ETCS(하이패스) 음성 안내 음량 | `CAR_TOLL` / `CONTROL_CAR_TOLL` |
| TurnSignal | `getTurnSignal()` | 방향 지시등 상태 | `CAR_LIGHTS` |
| Window | `getWindow()` | 창문 위치·잠금, 블라인드, 스마트 글라스 | `CAR_WINDOWS` / `CONTROL_CAR_WINDOWS` |
| Wiper | `getWiper()` | 와이퍼 상태·스위치 | `CAR_WINDOWS` / `CONTROL_CAR_WINDOWS` |

> 가이드(Introduction)의 도메인 표에는 CarInfo가 "외부 크기, 제조사, 연식 등", HVAC가 "습도" 등을 다룬다고 되어 있으나, v2.3.2 API Reference에 실제로 정의된 CarInfo API는 VIN/CRN 조회뿐이며 HVAC에 습도 API는 없습니다. 본 문서의 도메인별 표는 API Reference에 정의된 멤버만 기준으로 작성했습니다.

---

### 1.2 핵심 개념

#### 1.2.1 도메인 객체

- 모든 도메인 API는 `ai.pleos.playground.vehicle.api` 패키지의 **인터페이스**로 정의됩니다.
- 도메인 객체는 직접 생성하지 않고, 반드시 `Vehicle` 인스턴스의 `getXxx()`로만 얻습니다. (예: `vehicle.getDoor()`, `vehicle.getHVAC()`)
- 도메인 객체에서 호출하는 API는 네 가지 타입으로 나뉩니다.

| 타입 | 메서드 이름 패턴 | 하는 일 | 결과 전달 |
|---|---|---|---|
| Capability | `checkXxxCapabilities` | 기능 지원 여부·허용 범위 확인 | `onComplete(config, areaMap/list/bounds)` / `onFailed` |
| Get | `getCurrentXxx`, `getXxx`, `hasXxx` | 현재 값 조회 | `onComplete(값)` / `onFailed` |
| Set | `setXxx`, `moveXxx`, `selectXxx` | 값 설정·제어 | `onComplete`(선택, 기본값 있음) / `onFailed`(실패 시) |
| Callback | `registerXxx` / `unregisterXxx` | 상태 변화 구독 | 리스너의 `onXxxUpdated` / `onFailed` |

#### 1.2.2 Capability (`checkXxxCapabilities`)

`checkXxxCapabilities`는 해당 기능의 **지원 여부(접근 수준)**와 **허용 값 범위/지원 영역/지원 값 목록**을 반환합니다. 반환 형태는 API마다 다르며 공통 모델은 `api.model.capability` 패키지에 있습니다.

| 타입 | 설명 |
|---|---|
| `CapabilityConfig(accessType: AccessType = AccessType.NONE)` | 기능 단위 접근 수준. `accessType` 기본값은 `NONE` |
| `AccessType` | `NONE`, `READ`, `WRITE`, `READ_WRITE` |
| `AreaBounds<T>(minValue: T, maxValue: T)` | 영역별 허용 값 범위(최소/최대) |
| `Map<Area, AreaBounds<T>>` | 영역(예: `DoorArea`)별 `AreaBounds` 맵 |
| `List<Area>` | 기능을 지원하는 영역 목록 |
| `List<enum>` / `Map<Area, List<enum>>` | 지원되는 값 목록 (예: `List<LightSwitch>`, `Map<WindowArea, List<WiperSwitch>>`) |

`onComplete` 콜백 형태 예:

| 형태 | 예시 API |
|---|---|
| `(CapabilityConfig) -> Unit` | `checkBrakePedalStatusCapabilities`, `checkFuelPercentCapabilities` |
| `(CapabilityConfig, List<XxxArea>) -> Unit` | `checkDoorLockCapabilities`, `checkHvacAcCapabilities` |
| `(CapabilityConfig, Map<XxxArea, AreaBounds<Int>>) -> Unit` | `checkDoorPositionCapabilities`, `checkHvacFanSpeedCapabilities` |
| `(CapabilityConfig, AreaBounds<T>) -> Unit` | `checkEtcsVoiceGuidanceVolumeCapabilities`, `checkSteeringWheelAngleCapabilities` |
| `(CapabilityConfig, List<Float>) -> Unit` | `checkEvChargePercentLimitCapabilities`, `checkEvChargeCurrentDrawLimitCapabilities` |
| `(CapabilityConfig, Map<SeatArea, AreaBounds<Float>>, HvacTemperatureConfigArray?) -> Unit` | `checkHvacTemperatureCapabilities` |

**주의 사항**

- 범위 값(`minValue`/`maxValue`)은 `CapabilityConfig`가 아니라 **두 번째 콜백 인자(맵/AreaBounds)**에서 읽어야 합니다. `CapabilityConfig`는 접근/지원 여부만 알려 줍니다.
- 도어 개방 최대값, 팬 속도 범위, 설정 온도 범위 등은 **차량마다 다르며 고정값이 아니므로**, 설정 전에 반드시 capability를 먼저 조회해야 합니다.
- v2.3.2에서는 `CapabilityConfig`를 **포함하지 않는** 예전 오버로드(예: `onComplete: (List<DoorArea>) -> Unit`)가 대부분 **Deprecated**되었습니다. `CapabilityConfig`를 함께 반환하는 새 오버로드를 사용하세요.
- 미지원 호출을 줄이기 위해 Get/Set 전에 capability를 확인하는 것이 권장됩니다.

#### 1.2.3 Get / Set / Callback 패턴

**Get** — `getCurrentXxx(area?, onComplete, onFailed)`
- 영역이 필요한 속성은 첫 인자로 `area`(예: `DoorArea(DoorValue.ROW_1_LEFT)`)를 받습니다.
- `onComplete`는 nullable 값(`Int?`, `Boolean?`, `Float?`, enum`?`)을 받습니다. 값이 없으면 `null`일 수 있습니다.
- Boolean 상태 조회 중 일부는 `hasXxx` 이름을 사용합니다(예: `Safety.hasLowWasherFluidWarning`).

**Set** — `setXxx(area?, value, onComplete = {...}, onFailed = {})`
- 대부분의 Set API는 `onComplete`·`onFailed`에 **기본값(빈 람다)**이 있어 생략할 수 있습니다. 영역이 있는 Set의 `onComplete`는 `(Area, 값?) -> Unit` 형태로 설정된 영역과 값을 함께 돌려줍니다.
- 일부 API는 기본값이 없어 `onComplete`를 반드시 넘겨야 합니다(예: `Toll.setEtcsVoiceGuidanceVolume(value, onComplete: () -> Unit, onFailed)`, `Light.setAmbientLightBrightness(area, value, onComplete: (SeatArea, Int) -> Unit, onFailed)`, `Window.moveWindowBlind`).
- 시트/블라인드처럼 연속 이동하는 기능은 `moveXxx(area, value: MoveControl)`로 `FORWARD` / `BACKWARD` / `STOP` 방향을 지정합니다.

**Callback** — `registerXxx(area?, listener)` / `unregisterXxx(area?, listener)`
- 리스너는 `ai.pleos.playground.vehicle.listener` 패키지의 인터페이스이며 모두 `VehicleListener`를 상속합니다.
- 값이 변경되면 리스너의 `onXxxUpdated(...)`가, 실패 시 `onFailed(e: Exception)`가 호출됩니다.
- 영역 기반 속성은 등록·해제 시 동일한 `area`와 리스너 인스턴스를 전달해야 합니다.

> 참고: 가이드(Callback)의 예제는 `door.registerDoorPosition(doorPositionListener = callbackListener)` 형태로 영역 없이 호출하지만, v2.3.2 API Reference의 시그니처는 `registerDoorPosition(area: DoorArea, listener: DoorPositionListener)`입니다. Reference 시그니처를 따르세요.

#### 1.2.4 영역(Area) 타입

영역이 있는 속성은 `api.model.area`의 Area 클래스로 대상을 지정합니다. 각 Area는 `constant.area`의 값 enum을 감싸며, 생성자는 `vararg`이므로 **하나 이상의 값**을 지정할 수 있습니다.

| Area 클래스 | 생성자 | 값 enum (`AreaValue` 구현, `id: Int` 보유) | 값 |
|---|---|---|---|
| `DoorArea` | `DoorArea(vararg values: DoorValue)` | `DoorValue` | `ROW_1_LEFT`, `ROW_1_RIGHT`, `ROW_2_LEFT`, `ROW_2_RIGHT`, `ROW_3_LEFT`, `ROW_3_RIGHT`, `HOOD`, `REAR` |
| `SeatArea` | `SeatArea(vararg values: SeatValue)` | `SeatValue` | `ROW_1_LEFT`, `ROW_1_CENTER`, `ROW_1_RIGHT`, `ROW_2_LEFT`, `ROW_2_CENTER`, `ROW_2_RIGHT`, `ROW_3_LEFT`, `ROW_3_CENTER`, `ROW_3_RIGHT` |
| `WindowArea` | `WindowArea(vararg values: WindowValue)` | `WindowValue` | `FRONT_WINDSHIELD`, `REAR_WINDSHIELD`, `ROW_1_LEFT`, `ROW_1_RIGHT`, `ROW_2_LEFT`, `ROW_2_RIGHT`, `ROOF_TOP_1`, `ROOF_TOP_2` |
| `MirrorArea` | `MirrorArea(vararg values: MirrorValue)` | `MirrorValue` | `DRIVER_LEFT`, `DRIVER_RIGHT`, `DRIVER_CENTER` |
| `WheelArea` | `WheelArea(vararg values: WheelValue)` | `WheelValue` | `UNKNOWN`, `LEFT_FRONT`, `RIGHT_FRONT`, `LEFT_REAR`, `RIGHT_REAR` |
| `TurnSignalArea` | `TurnSignalArea(vararg values: TurnSignalValue)` | `TurnSignalValue` | `OFF`, `RIGHT`, `LEFT` |

- `Area`는 위 6개 클래스를 상속자로 갖는 sealed class입니다. 각 Area 클래스는 `values: Array<out XxxValue>` 프로퍼티와 `equals`/`hashCode`를 제공하므로 capability 맵의 키로 조회할 수 있습니다(예: `areaMap[DoorArea(DoorValue.ROW_1_LEFT)]`).
- `TurnSignalValue`는 `TurnSignal.getCurrentTurnSignalState`의 결과 값 타입으로도 사용됩니다.

#### 1.2.5 콜백(onComplete / onFailed)

- 모든 비동기 API는 `onComplete`와 `onFailed` 람다로 결과를 전달합니다.
- `onFailed`는 Get/Set API, `checkXxxCapabilities`, `VehicleListener` 모두 **Kotlin 표준 `Exception`**을 인자로 받습니다. `ErrorCode`가 콜백 인자로 직접 전달되지는 않습니다.
- Java에서 호출할 때는 람다가 `Unit`을 반환해야 하므로 `return Unit.INSTANCE;`를 사용합니다.

#### 1.2.6 예외와 오류 코드

`onFailed`로 받은 `Exception`이 `VehicleException`이면 `errorCode`로 `ErrorCode` 값을 확인합니다. **모든 `onFailed`가 `VehicleException`인 것은 아닙니다.**

`class VehicleException(error: String? = null) : Exception`

| 프로퍼티 | 타입 | 설명 |
|---|---|---|
| `errorCode` | `ErrorCode` | 오류 분류 코드 |
| `message` | `String` | 오류 메시지 |

`enum ErrorCode` (`ai.pleos.playground.vehicle.constant`, `Companion.findById(id: Int): ErrorCode?` 제공)

| ErrorCode | 설명 | 권장 처리 |
|---|---|---|
| `ERROR_NOT_SUPPORTED_PROPERTY` | 해당 속성(기능) 미지원 | UI 비활성·대체 흐름 |
| `ERROR_NOT_SUPPORTED_AREA` | 해당 영역 미지원 | 영역 선택 변경 |
| `ERROR_NOT_SUPPORTED_VALUE_TYPE` | 지원하지 않는 값 타입 | 파라미터·타입 재확인 |
| `ERROR_SECURITY_PERMISSION` | 퍼미션 부족 | 매니페스트 퍼미션·`@RequiresPermission` 확인 |
| `ERROR_SERVICE_CONNECTION_FAILURE` | Vehicle 서비스 연결 실패 | `initialize()`·에뮬레이터 상태 확인 |
| `ERROR_INTERNAL_SERVICE` | 내부 서비스 오류 | 재시도·일시 오류 안내 |
| `ERROR_DRIVING_RESTRICTED` | 주행 중 기능 제한 | 주행 제한 안내 |
| `ERROR_EMPTY_RESULT` | 결과 없음 | 빈 상태 UI 처리 |
| `ERROR_UNDEFINED` | 정의되지 않은 오류 | 로그·재시도 |

#### 1.2.7 퍼미션 모델

- Vehicle SDK API는 Android 매니페스트에 **Pleos 차량 퍼미션**을 선언해야 호출할 수 있습니다. 각 API의 `@RequiresPermission` 어노테이션과 `PleosPermission` 상수(`ai.pleos.playground.vehicle.constant.PleosPermission`)를 참조합니다.
- 매니페스트에는 `pleos.car.permission.{상수명}` 형식으로 선언합니다.
- 퍼미션은 접두사로 구분됩니다.

| 접두사 | 예시 | 용도 |
|---|---|---|
| `CAR_*` | `CAR_DOORS` → `pleos.car.permission.CAR_DOORS` | 도메인 데이터 조회(get, capability, register) |
| `CONTROL_CAR_*` | `CONTROL_CAR_DOORS` | 도메인 제어(set/move/select) |
| `PRIVATE_CAR_*` | `PRIVATE_CAR_SAFETY` | 상수로 정의됨. Reference상 Telltale API에 요구되며 보호 수준이 `signature\|privileged` |

- **보호 수준(protection level)**: API Reference의 각 멤버 문서에 `Permission: PleosPermission.XXX (protection level: ...)` 형태로 명시됩니다. 대부분은 `normal`이며, 다음 API만 `signature|privileged`로 표기되어 있습니다.
  - `Safety.checkTelltalesCapabilities`, `Safety.getTelltalesInfo`, `Safety.registerTelltales`, `Safety.unregisterTelltales` → `PRIVATE_CAR_SAFETY` (`signature|privileged`)
  - `Window.setSmartGlassState` → `CONTROL_CAR_WINDOWS` (`signature|privileged`로 표기; 다른 Window/Wiper 제어 API의 `CONTROL_CAR_WINDOWS`는 `normal`로 표기)
- `normal` 퍼미션은 매니페스트 선언만으로 사용할 수 있습니다. `signature|privileged` 퍼미션은 Android 보호 수준 정의상 플랫폼 서명 또는 사전 설치(privileged) 앱에만 부여되므로 일반 3rd party 앱에서는 사용할 수 없을 수 있습니다. 또한 SDK 개요 페이지에 따르면 퍼미션은 Pleos App Market과 사전 협의·승인된 범위 안에서만 사용할 수 있으며, 승인되지 않은 퍼미션을 선언하거나 해당 API를 호출하면 앱 심사를 통과할 수 없습니다(0.3 참고).
- 퍼미션이 없으면 API의 `onFailed`에 `Exception`이 전달되며, `VehicleException`인 경우 `ERROR_SECURITY_PERMISSION`에 해당할 수 있습니다.
- 사용하는 도메인에 필요한 퍼미션만 선언하면 됩니다.

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.CAR_DOORS" />
    <uses-permission android:name="pleos.car.permission.CAR_INFO" />
    <!-- 사용하는 도메인에 맞는 퍼미션만 선언 -->
</manifest>
```

가이드에 제시된 **도메인별 대표 조회 퍼미션(CAR_\*)**:

| 도메인 | PleosPermission 상수 | 매니페스트 `android:name` |
|---|---|---|
| Door | `CAR_DOORS` | `pleos.car.permission.CAR_DOORS` |
| Window, Wiper | `CAR_WINDOWS` | `pleos.car.permission.CAR_WINDOWS` |
| HVAC | `CAR_CLIMATE` | `pleos.car.permission.CAR_CLIMATE` |
| DrivingMode, Brake, Odometer | `CAR_DRIVE` | `pleos.car.permission.CAR_DRIVE` |
| EvBattery, Fuel | `CAR_ENERGY` | `pleos.car.permission.CAR_ENERGY` |
| CarInfo, Display | `CAR_INFO` | `pleos.car.permission.CAR_INFO` |
| Light, TurnSignal | `CAR_LIGHTS` | `pleos.car.permission.CAR_LIGHTS` |
| SideMirror | `CAR_MIRRORS` | `pleos.car.permission.CAR_MIRRORS` |
| Safety, Tire | `CAR_SAFETY` | `pleos.car.permission.CAR_SAFETY` |
| Seat | `CAR_SEATS` | `pleos.car.permission.CAR_SEATS` |
| Toll | `CAR_TOLL` | `pleos.car.permission.CAR_TOLL` |

> 세부 API마다 요구 퍼미션이 다를 수 있습니다(예: SteeringWheel의 조향각은 `CAR_DRIVE`, 열선은 `CAR_CLIMATE`. SideMirror 열선의 register/unregister는 `CAR_CLIMATE`). 호출 전 해당 함수의 `@RequiresPermission`을 확인하세요. 전체 목록은 [1.6 퍼미션 전체 목록](#16-퍼미션-전체-목록)에 있습니다.

---
### 1.3 도메인별 기능 상세

표기 규칙

- **필요 퍼미션** 열의 값은 `pleos.car.permission.` 접두사를 생략한 `PleosPermission` 상수명입니다. 별도 표기가 없으면 보호 수준은 `normal`입니다.
- 파라미터 표기에서 `onFailed: (Exception) -> Unit`은 생략했습니다. 모든 API에 `onFailed`가 있습니다.
- `register*` / `unregister*`는 같은 리스너 타입·영역을 사용하므로 한 행에 묶었습니다.
- **(Deprecated)** 는 API Reference에 Deprecated로 표시된 API 또는 오버로드입니다.

#### 1.3.1 Brake

브레이크 페달 상태를 조회하는 API를 제공합니다. (`vehicle.getBrake()`)

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkBrakePedalStatusCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 브레이크 페달 상태 기능 지원 여부 조회 | `CAR_DRIVE` |
| `getCurrentBrakePedalStatus(onComplete: (Boolean?) -> Unit)` | 브레이크 페달 상태 조회. `true`면 페달이 밟혀 있음 | `CAR_DRIVE` |
| `registerBrakePedalStatus(listener)` / `unregisterBrakePedalStatus(listener)` | 브레이크 페달 상태 변경 구독/해제 (`BrakePedalStatusListener`) | `CAR_DRIVE` |

#### 1.3.2 CarInfo

차량 식별 정보(VIN, CRN)를 조회하는 API를 제공합니다. (`vehicle.getCarInfo()`)

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkVINInfoCapabilities(onComplete: (CapabilityConfig) -> Unit)` | VIN 정보 기능 지원 여부 조회 | `CAR_INFO` |
| `getVINInfo(onComplete: (String?) -> Unit)` | 차대번호(VIN, Vehicle Identification Number) 조회 | `CAR_INFO` |
| `checkCRNInfoCapabilities(onComplete: (CapabilityConfig) -> Unit)` | CRN 정보 기능 지원 여부 조회. **(Deprecated)** → `checkVINInfoCapabilities` 사용 | `CAR_INFO` |
| `getCRNInfo(onComplete: (String?) -> Unit)` | CRN(Connected Resource Name) 정보 조회. **(Deprecated)** → `getVINInfo` 사용 | `CAR_INFO` |

#### 1.3.3 Display

인포테인먼트의 온도 표시 단위를 조회하는 API를 제공합니다. (`vehicle.getDisplay()`)

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkTemperatureDisplayUnitCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 온도 표시 단위 기능 지원 여부 조회 | `CAR_INFO` |
| `getCurrentTemperatureDisplayUnit(onComplete: (TemperatureUnit?) -> Unit)` | 현재 표시용 온도 단위(`CELSIUS`/`FAHRENHEIT`) 조회 | `CAR_INFO` |
| `registerTemperatureDisplayUnit(listener)` / `unregisterTemperatureDisplayUnit(listener)` | 온도 표시 단위 변경 구독/해제 (`TemperatureDisplayUnitListener`) | `CAR_INFO` |

> `Vehicle` 클래스 Reference 예제에는 `display.getCurrentSpeedDisplayUnit(...)`이 등장하지만, v2.3.2 `Display` 인터페이스 멤버 목록에는 해당 API가 없습니다.

#### 1.3.4 Door

도어 잠금, 도어 위치(개폐 정도), 글로브박스 잠금 API를 제공합니다. (`vehicle.getDoor()`)

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkDoorLockCapabilities(onComplete: (CapabilityConfig, List<DoorArea>) -> Unit)` | 도어 잠금 기능 지원 여부·지원 영역 조회. `(List<DoorArea>)` 오버로드는 **(Deprecated)** | `CAR_DOORS` |
| `checkDoorPositionCapabilities(onComplete: (CapabilityConfig, Map<DoorArea, AreaBounds<Int>>) -> Unit)` | 도어 위치 기능 지원 여부와 영역별 min/max 범위 조회. `(Map<DoorArea, AreaBounds<Int>>)` 오버로드는 **(Deprecated)** | `CAR_DOORS` |
| `checkGloveBoxLockCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 글로브박스 잠금 기능 지원 여부 조회 | `CAR_DOORS` |
| `getDoorLockState(area: DoorArea, onComplete: (Boolean?) -> Unit)` | 현재 도어 잠금 상태 조회 | `CAR_DOORS` |
| `getCurrentDoorPosition(area: DoorArea, onComplete: (Int?) -> Unit)` | 현재 도어 위치(열림 정도) 조회 | `CAR_DOORS` |
| `getCurrentGloveBoxLockState(onComplete: (Boolean?) -> Unit)` | 현재 글로브박스 잠금 상태 조회 | `CAR_DOORS` |
| `setDoorLock(area: DoorArea, value: Boolean, onComplete: (DoorArea, Boolean?) -> Unit = {…})` | 도어 잠금/해제. `true`=잠금, `false`=해제 | `CONTROL_CAR_DOORS` |
| `setDoorPosition(area: DoorArea, value: Int, onComplete: (DoorArea, Int?) -> Unit = {…})` | 도어 위치 설정. 최대값(완전 열림)은 차량마다 다르므로 먼저 `checkDoorPositionCapabilities`의 맵에서 영역별 `AreaBounds.minValue/maxValue`를 읽어야 함(`CapabilityConfig` 아님). 최소값은 일반적으로 0(완전 닫힘) | `CONTROL_CAR_DOORS` |
| `setGloveBoxLock(value: Boolean, onComplete: (Boolean?) -> Unit = {})` | 글로브박스 잠금/해제. `true`=잠금, `false`=해제 | `CONTROL_CAR_DOORS` |
| `registerDoorLockState(area, listener)` / `unregisterDoorLockState(area, listener)` | 도어 잠금 상태 변경 구독/해제 (`DoorLockStateListener`) | `CAR_DOORS` |
| `registerDoorPosition(area, listener)` / `unregisterDoorPosition(area, listener)` | 도어 위치 변경 구독/해제 (`DoorPositionListener`) | `CAR_DOORS` |
| `registerGloveBoxLockState(listener)` / `unregisterGloveBoxLockState(listener)` | 글로브박스 잠금 상태 변경 구독/해제 (`GloveBoxLockStateListener`) | `CAR_DOORS` |

#### 1.3.5 DrivingMode

드라이브 모드와 크루즈 컨트롤 정보를 조회하는 API를 제공합니다. (`vehicle.getDrivingMode()`)

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkDrivingModeStateCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 드라이브 모드 상태 기능 지원 여부 조회 | `CAR_DRIVE` |
| `checkCruiseControlStateCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 크루즈 컨트롤 상태 기능 지원 여부 조회 | `CAR_DRIVE` |
| `checkCruiseControlTargetSpeedCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 크루즈 컨트롤 목표 속도 기능 지원 여부 조회 | `CAR_DRIVE` |
| `getCurrentDrivingModeState(onComplete: (DrivingModeType?) -> Unit)` | 현재 드라이브 모드(`NORMAL`/`ECO`/`SPORT`) 조회 | `CAR_DRIVE` |
| `getCruiseControlState(onComplete: (CruiseControl?) -> Unit)` | 현재 크루즈 컨트롤 상태 조회 | `CAR_DRIVE` |
| `getCruiseControlTargetSpeed(onComplete: (Float?) -> Unit)` | 크루즈 컨트롤 목표 속도 조회(단위 m/s). 값이 없으면 `null` | `CAR_DRIVE` |
| `registerDrivingModeState(listener)` / `unregisterDrivingModeState(listener)` | 드라이브 모드 변경 구독/해제 (`DrivingModeStateListener`) | `CAR_DRIVE` |
| `registerCruiseControlState(listener)` / `unregisterCruiseControlState(listener)` | 크루즈 컨트롤 상태 변경 구독/해제 (`CruiseControlStateListener`) | `CAR_DRIVE` |
| `registerCruiseControlTargetSpeed(listener)` / `unregisterCruiseControlTargetSpeed(listener)` | 크루즈 컨트롤 목표 속도(m/s) 변경 구독/해제 (`CruiseControlTargetSpeedListener`) | `CAR_DRIVE` |

#### 1.3.6 EvBattery

EV 배터리 충전량(SoC), 전비(효율), 충전 관련 제어 API를 제공합니다. (`vehicle.getEvBattery()`) 전비 단위는 **km/kWh**입니다.

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkEvBatteryStateOfChargeCapabilities(onComplete: (CapabilityConfig) -> Unit)` | EV 배터리 충전량(SoC) 기능 지원 여부 조회 | `CAR_ENERGY` |
| `getEvBatteryStateOfCharge(onComplete: (Int?) -> Unit)` | EV 배터리 충전량 조회(%) | `CAR_ENERGY` |
| `registerEvBatteryStateOfCharge(listener)` / `unregisterEvBatteryStateOfCharge(listener)` | 배터리 충전량(%) 변경 구독/해제 (`EvBatteryStateOfChargeListener`) | `CAR_ENERGY` |
| `checkCurrentEvEfficiencyCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 현재 주행 전비 기능 지원 여부 조회 | `CAR_ENERGY` |
| `getCurrentEvEfficiency(onComplete: (Float?) -> Unit)` | 현재 주행 세션 전비 조회(km/kWh) | `CAR_ENERGY` |
| `registerCurrentEvEfficiency(listener)` / `unregisterCurrentEvEfficiency(listener)` | 현재 전비 변경 구독/해제 (`EvBatteryEfficiencyListener`) | `CAR_ENERGY` |
| `checkCumulativeEvEfficiencyCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 누적 전비 기능 지원 여부 조회 | `CAR_ENERGY` |
| `getCumulativeEvEfficiency(onComplete: (Float?) -> Unit)` | 운행 기간 전체 누적 전비 조회(km/kWh) | `CAR_ENERGY` |
| `registerCumulativeEvEfficiency(listener)` / `unregisterCumulativeEvEfficiency(listener)` | 누적 전비 변경 구독/해제 (`EvBatteryEfficiencyListener`) | `CAR_ENERGY` |
| `checkInstantaneousEvEfficiencyCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 순간 전비 기능 지원 여부 조회 | `CAR_ENERGY` |
| `getInstantaneousEvEfficiency(onComplete: (Float?) -> Unit)` | 순간 전비 조회(km/kWh). 최신 AAOS가 `INSTANTANEOUS_EV_EFFICIENCY` 표준 속성을 제공하므로 **향후 Deprecated 예정** | `CAR_ENERGY` |
| `registerInstantaneousEvEfficiency(listener)` / `unregisterInstantaneousEvEfficiency(listener)` | 순간 전비 변경 구독/해제 (`EvBatteryEfficiencyListener`) | `CAR_ENERGY` |
| `checkSinceChargeEvEfficiencyCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 충전 이후 전비 기능 지원 여부 조회 | `CAR_ENERGY` |
| `getSinceChargeEvEfficiency(onComplete: (Float?) -> Unit)` | 마지막 충전 이후 전비 조회(km/kWh) | `CAR_ENERGY` |
| `registerSinceChargeEvEfficiency(listener)` / `unregisterSinceChargeEvEfficiency(listener)` | 충전 이후 전비 변경 구독/해제 (`EvBatteryEfficiencyListener`) | `CAR_ENERGY` |
| `checkEvChargeCurrentDrawLimitCapabilities(onComplete: (CapabilityConfig, List<Float>) -> Unit)` | 지원되는 충전 전류 제한 값 목록(A) 조회. `(List<Float>)` 오버로드는 **(Deprecated)** | `CAR_ENERGY` |
| `getEvChargeCurrentDrawLimit(onComplete: (Float?) -> Unit)` | 현재 충전 전류 제한 값(A) 조회 | `CAR_ENERGY` |
| `setEvChargeCurrentDrawLimit(value: Float, onComplete: (Float?) -> Unit = {})` | 충전 전류 제한 값(A) 설정 | `CONTROL_CAR_ENERGY` |
| `checkEvChargePercentLimitCapabilities(onComplete: (CapabilityConfig, List<Float>) -> Unit)` | 지원되는 충전 한도(%) 값 목록 조회 | `CAR_ENERGY` |
| `setEvChargePercentLimit(value: Float, onComplete: (Float?) -> Unit = {})` | 충전 목표 SoC 한도(%) 설정. 값은 0~100% 범위이며 차량이 지원하는 값이어야 함(`checkEvChargePercentLimitCapabilities`로 확인) | `CONTROL_CAR_ENERGY` |
| `checkEvChargePortOpenCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 충전구 개폐 기능 지원 여부 조회 | `CAR_ENERGY` |
| `setEvChargePortOpen(value: Boolean, onComplete: (Boolean?) -> Unit = {})` | 충전구 열림 상태 On/Off 설정 | `CONTROL_CAR_ENERGY` |
| `checkEvChargeSwitchCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 충전 시작/중지 스위치 기능 지원 여부 조회 | `CAR_ENERGY` |
| `getEvChargeSwitchState(onComplete: (Boolean?) -> Unit)` | 현재 EV 배터리 충전 스위치 상태 조회 | `CAR_ENERGY` |
| `setEvChargeSwitch(value: Boolean, onComplete: (Boolean?) -> Unit = {})` | 충전 시작(`true`)/중지(`false`) | `CONTROL_CAR_ENERGY` |

#### 1.3.7 Fuel

연료 잔량과 연비 API를 제공합니다. (`vehicle.getFuel()`) 연비 단위는 **L/100 km**입니다.

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkFuelPercentCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 연료 잔량 기능 지원 여부 조회 | `CAR_ENERGY` |
| `getFuelPercent(onComplete: (Int?) -> Unit)` | 연료 잔량 조회(%) | `CAR_ENERGY` |
| `registerFuelPercent(listener)` / `unregisterFuelPercent(listener)` | 연료 잔량(%) 변경 구독/해제 (`FuelPercentListener`) | `CAR_ENERGY` |
| `checkCurrentFuelEconomyCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 현재 연비 기능 지원 여부 조회 | `CAR_ENERGY` |
| `getCurrentFuelEconomy(onComplete: (Float?) -> Unit)` | 현재 주행 세션 연비 조회(L/100 km) | `CAR_ENERGY` |
| `registerCurrentFuelEconomy(listener)` / `unregisterCurrentFuelEconomy(listener)` | 현재 연비 변경 구독/해제 (`FuelEconomyListener`) | `CAR_ENERGY` |
| `checkCumulativeFuelEconomyCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 누적 연비 기능 지원 여부 조회 | `CAR_ENERGY` |
| `getCumulativeFuelEconomy(onComplete: (Float?) -> Unit)` | 운행 기간 전체 누적 연비 조회(L/100 km) | `CAR_ENERGY` |
| `registerCumulativeFuelEconomy(listener)` / `unregisterCumulativeFuelEconomy(listener)` | 누적 연비 변경 구독/해제 (`FuelEconomyListener`) | `CAR_ENERGY` |
| `checkInstantaneousFuelEconomyCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 순간 연비 기능 지원 여부 조회 | `CAR_ENERGY` |
| `getInstantaneousFuelEconomy(onComplete: (Float?) -> Unit)` | 순간 연비 조회(L/100 km). 최신 AAOS가 `INSTANTANEOUS_FUEL_ECONOMY` 표준 속성을 제공하므로 **향후 Deprecated 예정** | `CAR_ENERGY` |
| `registerInstantaneousFuelEconomy(listener)` / `unregisterInstantaneousFuelEconomy(listener)` | 순간 연비 변경 구독/해제 (`FuelEconomyListener`) | `CAR_ENERGY` |
| `checkSinceChargeFuelEconomyCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 주유 이후 연비 기능 지원 여부 조회 | `CAR_ENERGY` |
| `getSinceChargeFuelEconomy(onComplete: (Float?) -> Unit)` | 마지막 주유 이후 연비 조회(L/100 km) | `CAR_ENERGY` |
| `registerSinceChargeFuelEconomy(listener)` / `unregisterSinceChargeFuelEconomy(listener)` | 주유 이후 연비 변경 구독/해제 (`FuelEconomyListener`) | `CAR_ENERGY` |

#### 1.3.8 HVAC

공조(난방·환기·냉방) 제어 및 조회 API를 제공합니다. (`vehicle.getHVAC()`) 디프로스터만 `WindowArea`를, 나머지는 `SeatArea`를 영역으로 사용합니다. 아래 `check*` API 중 `CapabilityConfig`가 없는 예전 오버로드(`(List<SeatArea>)`, `(List<WindowArea>)`, `(Map<…>)`)는 모두 **(Deprecated)** 입니다.

**전원 / 모드**

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkHvacPowerCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | HVAC 전원 기능 지원 여부·영역 조회 | `CAR_CLIMATE` |
| `getHvacPowerState(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 현재 HVAC 전체 전원 상태 조회 | `CAR_CLIMATE` |
| `setHvacPower(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = {…})` | HVAC 전체 전원 On/Off | `CONTROL_CAR_CLIMATE` |
| `registerHvacPowerState(area, listener)` / `unregisterHvacPowerState(area, listener)` | 전원 상태 변경 구독/해제 (`HvacPowerStateListener`) | `CAR_CLIMATE` |
| `checkHvacAcCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | A/C 기능 지원 여부·영역 조회 | `CAR_CLIMATE` |
| `getHvacAcState(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 현재 A/C 상태 조회 | `CAR_CLIMATE` |
| `setHvacAc(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = {…})` | 지정 영역 A/C On/Off | `CONTROL_CAR_CLIMATE` |
| `registerHvacAcState(area, listener)` / `unregisterHvacAcState(area, listener)` | A/C 상태 변경 구독/해제 (`HvacAcStateListener`) | `CAR_CLIMATE` |
| `checkHvacMaxAcCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 최대 냉방(Max A/C) 기능 지원 여부·영역 조회 | `CAR_CLIMATE` |
| `getHvacMaxAcState(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 현재 Max A/C 상태 조회 | `CAR_CLIMATE` |
| `setHvacMaxAc(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = {…})` | Max A/C On/Off | `CONTROL_CAR_CLIMATE` |
| `registerHvacMaxAcState(area, listener)` / `unregisterHvacMaxAcState(area, listener)` | Max A/C 상태 변경 구독/해제 (`HvacMaxAcStateListener`) | `CAR_CLIMATE` |
| `checkHvacAutoCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 자동(Auto) 모드 기능 지원 여부·영역 조회 | `CAR_CLIMATE` |
| `getHvacAutoState(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 현재 자동 모드 상태 조회 | `CAR_CLIMATE` |
| `setHvacAuto(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = {…})` | 자동 모드 On/Off | `CONTROL_CAR_CLIMATE` |
| `registerHvacAutoState(area, listener)` / `unregisterHvacAutoState(area, listener)` | 자동 모드 변경 구독/해제 (`HvacAutoStateListener`) | `CAR_CLIMATE` |
| `checkHvacDualCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 듀얼(영역 간 온도 연동) 기능 지원 여부·영역 조회 | `CAR_CLIMATE` |
| `getHvacDualState(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 현재 영역 간 온도 연동 상태 조회 | `CAR_CLIMATE` |
| `setHvacDual(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = {…})` | 영역 간 온도 연동 On/Off | `CONTROL_CAR_CLIMATE` |
| `registerHvacDualState(area, listener)` / `unregisterHvacDualState(area, listener)` | 듀얼 상태 변경 구독/해제 (`HvacDualStateListener`) | `CAR_CLIMATE` |
| `checkHvacAirCleanerCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 공기청정 기능 지원 여부·영역 조회 | `CAR_CLIMATE` |
| `getHvacAirCleanerState(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 현재 공기청정 상태 조회 | `CAR_CLIMATE` |
| `setHvacAirCleaner(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = {…})` | 공기청정 On/Off | `CONTROL_CAR_CLIMATE` |
| `registerHvacAirCleanerState(area, listener)` / `unregisterHvacAirCleanerState(area, listener)` | 공기청정 상태 변경 구독/해제 (`HvacAirCleanerStateListener`) | `CAR_CLIMATE` |

**내기 순환**

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkHvacRecirculationCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 내기 순환 기능 지원 여부·영역 조회 | `CAR_CLIMATE` |
| `getHvacRecirculationState(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 현재 내기 순환 상태 조회 | `CAR_CLIMATE` |
| `setHvacRecirculation(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = {…})` | 내기 순환 On/Off | `CONTROL_CAR_CLIMATE` |
| `registerHvacRecirculationState(area, listener)` / `unregisterHvacRecirculationState(area, listener)` | 내기 순환 상태 변경 구독/해제 (`HvacRecirculationStateListener`) | `CAR_CLIMATE` |
| `checkHvacAutoRecirculationCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 자동 내기 순환 기능 지원 여부·영역 조회 | `CAR_CLIMATE` |
| `getHvacAutoRecirculationState(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 현재 자동 내기 순환 상태 조회 | `CAR_CLIMATE` |
| `setHvacAutoRecirculation(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = {…})` | 자동 내기 순환 On/Off | `CONTROL_CAR_CLIMATE` |
| `registerHvacAutoRecirculationState(area, listener)` / `unregisterHvacAutoRecirculationState(area, listener)` | 자동 내기 순환 상태 변경 구독/해제 (`HvacRecirculationStateListener`) | `CAR_CLIMATE` |

**온도**

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkHvacTemperatureCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Float>>, HvacTemperatureConfigArray?) -> Unit)` | 설정 온도 기능 지원 여부, 영역별 지원 온도 범위(°C), 차량별 온도 변환 설정 배열 조회. `HvacTemperatureConfigArray`가 `null`이면 표준 변환 공식 사용. `(Map<…>)` 및 `(CapabilityConfig, Map<…>)` 오버로드는 **(Deprecated)** | `CAR_CLIMATE` |
| `getHvacTemperature(area: SeatArea, unit: TemperatureUnit, onComplete: (Float?) -> Unit)` | 지정 단위의 목표(설정) 온도 조회 | `CAR_CLIMATE` |
| `setHvacTemperature(area: SeatArea, value: Float, unit: TemperatureUnit, onComplete: (SeatArea, Float?) -> Unit = {…})` | 지정 단위로 목표 온도 설정. 지원 범위는 차량마다 다르므로 먼저 `checkHvacTemperatureCapabilities`로 영역별 `AreaBounds`를 확인 | `CONTROL_CAR_CLIMATE` |
| `registerHvacTemperature(area, listener)` / `unregisterHvacTemperature(area, listener)` | 목표 온도 변경 구독/해제. `HvacTemperatureListener`(°C만) 또는 `HvacTemperatureValueListener`(°C+°F, 차량별 변환 규칙 적용·없으면 공식) 오버로드 | `CAR_CLIMATE` |
| `checkHvacTemperatureCurrentCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 실내 현재 온도 기능 지원 여부·영역 조회 | `CAR_CLIMATE` |
| `getHvacTemperatureCurrent(area: SeatArea, onComplete: (Float?) -> Unit)` / `getHvacTemperatureCurrent(area, unit: TemperatureUnit, onComplete)` | 차량 실내 현재 온도 조회. 단위 미지정 시 °C, 단위 지정 시 해당 단위(°F는 차량별 변환 규칙 또는 공식 적용) | `CAR_CLIMATE` |
| `registerHvacTemperatureCurrent(area, listener)` / `unregisterHvacTemperatureCurrent(area, listener)` | 실내 현재 온도 변경 구독/해제 (`HvacTemperatureListener` 또는 `HvacTemperatureValueListener`) | `CAR_CLIMATE` |

**풍량 / 풍향 / 제상**

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkHvacFanSpeedCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 풍량 기능 지원 여부와 영역별 지원 풍량 범위 조회 | `CAR_CLIMATE` |
| `getHvacFanSpeed(area: SeatArea, onComplete: (Int?) -> Unit)` | 현재 풍량 조회 | `CAR_CLIMATE` |
| `setHvacFanSpeed(area: SeatArea, value: Int, onComplete: (SeatArea, Int?) -> Unit = {…})` | 풍량 설정. 범위는 차량마다 다르므로 먼저 `checkHvacFanSpeedCapabilities` 맵의 `AreaBounds`로 확인 | `CONTROL_CAR_CLIMATE` |
| `registerHvacFanSpeed(area, listener)` / `unregisterHvacFanSpeed(area, listener)` | 풍량 변경 구독/해제 (`HvacFanSpeedListener`) | `CAR_CLIMATE` |
| `checkHvacFanDirectionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, List<FanDirectionType>>) -> Unit)` | 풍향 기능 지원 여부와 영역별 지원 풍향 목록 조회 | `CAR_CLIMATE` |
| `getHvacFanDirection(area: SeatArea, onComplete: (FanDirectionType?) -> Unit)` | 현재 풍향 조회 | `CAR_CLIMATE` |
| `setHvacFanDirection(area: SeatArea, value: FanDirectionType, onComplete: (SeatArea, FanDirectionType?) -> Unit = {…})` | 풍향 설정 | `CONTROL_CAR_CLIMATE` |
| `checkHvacDefrosterCapabilities(onComplete: (CapabilityConfig, List<WindowArea>) -> Unit)` | 디프로스터(유리 제상) 기능 지원 여부·창 영역 조회 | `CAR_CLIMATE` |
| `getHvacDefrosterState(area: WindowArea, onComplete: (Boolean?) -> Unit)` | 현재 디프로스터 상태 조회 | `CAR_CLIMATE` |
| `setHvacDefroster(area: WindowArea, value: Boolean, onComplete: (WindowArea, Boolean?) -> Unit = {…})` | 디프로스터 On/Off | `CONTROL_CAR_CLIMATE` |
| `registerHvacDefrosterState(area, listener)` / `unregisterHvacDefrosterState(area, listener)` | 디프로스터 상태 변경 구독/해제 (`HvacDefrosterStateListener`) | `CAR_CLIMATE` |
| `checkHvacMaxDefrostCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 최대 제상(Max Defrost) 기능 지원 여부·영역 조회 | `CAR_CLIMATE` |
| `getHvacMaxDefrostState(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 현재 Max Defrost 상태 조회 | `CAR_CLIMATE` |
| `setHvacMaxDefrost(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = {…})` | Max Defrost On/Off | `CONTROL_CAR_CLIMATE` |
| `registerHvacMaxDefrostState(area, listener)` / `unregisterHvacMaxDefrostState(area, listener)` | Max Defrost 상태 변경 구독/해제 (`HvacMaxDefrostStateListener`) | `CAR_CLIMATE` |

> HVAC 인터페이스에는 풍향(`FanDirection`)용 register/unregister API가 없습니다. 시트 열선·통풍은 [Seat](#1312-seat), 스티어링휠 열선은 [SteeringWheel](#1314-steeringwheel), 사이드미러 열선은 [SideMirror](#1313-sidemirror) 도메인에 있습니다.

#### 1.3.9 Light

외부등, 실내등, 독서등, 앰비언트 라이트 API를 제공합니다. (`vehicle.getLight()`) 각 조명은 **State**(실제 점등 상태, `LightState`)와 **Switch**(스위치 설정, `LightSwitch`)로 나뉩니다. `CapabilityConfig`가 없는 `check*LightSwitchCapabilities` 예전 오버로드는 모두 **(Deprecated)** 입니다.

**외부등 (상향등 / 하향등 / 전·후방 안개등 / 미등)**

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkHighBeamLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit)` | 상향등 상태 기능 지원 여부·지원 상태 목록 조회 | `CAR_LIGHTS` |
| `checkHighBeamLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit)` | 상향등 스위치 기능 지원 여부·지원 스위치 값 조회 | `CAR_LIGHTS` |
| `getCurrentHighBeamLightState(onComplete: (LightState?) -> Unit)` | 현재 상향등 상태 조회 | `CAR_LIGHTS` |
| `getHighBeamLightSwitch(onComplete: (LightSwitch?) -> Unit)` | 현재 상향등 스위치 조회 | `CAR_LIGHTS` |
| `setHighBeamLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {})` | 상향등 스위치 설정 | `CONTROL_CAR_LIGHTS` |
| `registerHighBeamLightState(listener)` / `unregisterHighBeamLightState(listener)` | 상향등 상태 변경 구독/해제 (`LightStateListener`) | `CAR_LIGHTS` |
| `checkLowBeamLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit)` | 하향등 상태 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `checkLowBeamLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit)` | 하향등 스위치 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `getCurrentLowBeamLightState(onComplete: (LightState?) -> Unit)` | 현재 하향등 상태 조회 | `CAR_LIGHTS` |
| `getLowBeamLightSwitch(onComplete: (LightSwitch?) -> Unit)` | 현재 하향등 스위치 조회 | `CAR_LIGHTS` |
| `setLowBeamLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {})` | 하향등 스위치 설정 | `CONTROL_CAR_LIGHTS` |
| `registerLowBeamLightState(listener)` / `unregisterLowBeamLightState(listener)` | 하향등 상태 변경 구독/해제 (`LightStateListener`) | `CAR_LIGHTS` |
| `checkFrontFogLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit)` | 전방 안개등 상태 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `checkFrontFogLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit)` | 전방 안개등 스위치 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `getCurrentFrontFogLightState(onComplete: (LightState?) -> Unit)` | 현재 전방 안개등 상태 조회 | `CAR_LIGHTS` |
| `getFrontFogLightSwitch(onComplete: (LightSwitch?) -> Unit)` | 현재 전방 안개등 스위치 조회 | `CAR_LIGHTS` |
| `setFrontFogLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {})` | 전방 안개등 스위치 설정 | `CONTROL_CAR_LIGHTS` |
| `registerFrontFogLightState(listener)` / `unregisterFrontFogLightState(listener)` | 전방 안개등 상태 변경 구독/해제 (`LightStateListener`) | `CAR_LIGHTS` |
| `checkRearFogLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit)` | 후방 안개등 상태 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `checkRearFogLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit)` | 후방 안개등 스위치 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `getCurrentRearFogLightState(onComplete: (LightState?) -> Unit)` | 현재 후방 안개등 상태 조회 | `CAR_LIGHTS` |
| `getRearFogLightSwitch(onComplete: (LightSwitch?) -> Unit)` | 현재 후방 안개등 스위치 조회 | `CAR_LIGHTS` |
| `setRearFogLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {})` | 후방 안개등 스위치 설정 | `CONTROL_CAR_LIGHTS` |
| `registerRearFogLightState(listener)` / `unregisterRearFogLightState(listener)` | 후방 안개등 상태 변경 구독/해제 (`LightStateListener`) | `CAR_LIGHTS` |
| `checkTailLampLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit)` | 미등(테일램프) 상태 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `checkTailLampLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit)` | 미등 스위치 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `getCurrentTailLampLightState(onComplete: (LightState?) -> Unit)` | 현재 미등 상태 조회 | `CAR_LIGHTS` |
| `getTailLampLightSwitch(onComplete: (LightSwitch?) -> Unit)` | 현재 미등 스위치 조회 | `CAR_LIGHTS` |
| `setTailLampLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {})` | 미등 스위치 설정 | `CONTROL_CAR_LIGHTS` |
| `registerTailLampLightState(listener)` / `unregisterTailLampLightState(listener)` | 미등 상태 변경 구독/해제 (`LightStateListener`) | `CAR_LIGHTS` |

**실내등 / 독서등**

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkCabinLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit)` | 실내등 상태 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `checkCabinLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit)` | 실내등 스위치 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `getCurrentCabinLightState(onComplete: (LightState?) -> Unit)` | 현재 실내등 상태 조회 | `CAR_LIGHTS` |
| `getCabinLightSwitch(onComplete: (LightSwitch?) -> Unit)` | 현재 실내등 스위치 조회 | `CAR_LIGHTS` |
| `setCabinLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {})` | 실내등 스위치 설정 | `CONTROL_CAR_LIGHTS` |
| `registerCabinLightState(listener)` / `unregisterCabinLightState(listener)` | 실내등 상태 변경 구독/해제 (`LightStateListener`) | `CAR_LIGHTS` |
| `checkReadingLightStateCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, List<LightState>>) -> Unit)` | 독서등 상태 기능 지원 여부·좌석별 지원 상태 조회 | `CAR_LIGHTS` |
| `checkReadingLightSwitchCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, List<LightSwitch>>) -> Unit)` | 독서등 스위치 기능 지원 여부·좌석별 지원 값 조회 | `CAR_LIGHTS` |
| `getCurrentReadingLightState(area: SeatArea, onComplete: (LightState?) -> Unit)` | 현재 독서등 상태 조회 | `CAR_LIGHTS` |
| `getReadingLightSwitch(area: SeatArea, onComplete: (LightSwitch?) -> Unit)` | 현재 독서등 스위치 조회 | `CAR_LIGHTS` |
| `setReadingLightSwitch(area: SeatArea, value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {})` | 독서등 스위치 설정 | `CONTROL_CAR_LIGHTS` |
| `registerReadingLightState(area, listener)` / `unregisterReadingLightState(area, listener)` | 독서등 상태 변경 구독/해제 (`LightStateListener`) | `CAR_LIGHTS` |

**앰비언트 라이트**

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkAmbientLightStateCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, List<LightState>>) -> Unit)` | 앰비언트 라이트 상태 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `getAmbientLightState(area: SeatArea, onComplete: (LightState?) -> Unit)` | 좌석 영역별 현재 앰비언트 라이트 상태 조회 | `CAR_LIGHTS` |
| `registerAmbientLightStateListener(area, listener)` / `unregisterAmbientLightStateListener(area, listener)` | 앰비언트 라이트 상태 변경 구독/해제 (`LightStateListener`) | `CAR_LIGHTS` |
| `checkAmbientLightSwitchCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, List<LightSwitch>>) -> Unit)` | 앰비언트 라이트 스위치 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `getAmbientLightSwitch(area: SeatArea, onComplete: (LightSwitch?) -> Unit)` | 좌석 영역별 앰비언트 라이트 스위치 조회 | `CAR_LIGHTS` |
| `setAmbientLightSwitch(area: SeatArea, value: LightSwitch, onComplete: (SeatArea, LightSwitch?) -> Unit)` | 좌석 영역별 앰비언트 라이트 스위치 설정 | `CAR_LIGHTS` ※ |
| `checkAmbientLightBrightnessCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 앰비언트 라이트 밝기 기능 지원 여부·영역별 범위 조회 | `CAR_LIGHTS` |
| `getAmbientLightBrightness(area: SeatArea, onComplete: (Int?) -> Unit)` | 좌석 영역별 현재 앰비언트 밝기 조회 | `CAR_LIGHTS` |
| `setAmbientLightBrightness(area: SeatArea, value: Int, onComplete: (SeatArea, Int) -> Unit)` | 좌석 영역별 앰비언트 밝기 설정 | `CAR_LIGHTS` ※ |
| `registerAmbientLightBrightnessListener(area, listener)` / `unregisterAmbientLightBrightnessListener(area, listener)` | 앰비언트 밝기 변경 구독/해제 (`AmbientLightBrightnessListener`) | `CAR_LIGHTS` |
| `checkAmbientLightBrightnessColorCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 앰비언트 라이트 색상 기능 지원 여부·영역 조회 | `CAR_LIGHTS` |
| `getAmbientLightBrightnessColor(area: SeatArea, onComplete: (Int?) -> Unit)` | 앰비언트 라이트 색상 조회(32-bit ARGB, Alpha는 0xFF 고정) | `CAR_LIGHTS` |
| `setAmbientLightBrightnessColor(area: SeatArea, value: Int, onComplete: (SeatArea, Int) -> Unit)` | 앰비언트 라이트 색상 설정(32-bit ARGB, Alpha는 0xFF 고정) | `CAR_LIGHTS` ※ |
| `registerAmbientLightBrightnessColorListener(area, listener)` / `unregisterAmbientLightBrightnessColorListener(area, listener)` | 앰비언트 색상 변경 구독/해제 (`AmbientLightBrightnessColorListener`) | `CAR_LIGHTS` |
| `checkAmbientColorAutoByDriveModeEnabledCapabilities(onComplete: (CapabilityConfig) -> Unit)` | "드라이브 모드 연동 앰비언트 색상 자동 변경" 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `getAmbientColorAutoByDriveModeEnabled(onComplete: (Boolean?) -> Unit)` | 드라이브 모드 연동 색상 자동 변경 활성화 여부 조회 | `CAR_LIGHTS` |
| `setAmbientColorAutoByDriveModeEnabled(value: Boolean, onComplete: (Boolean?) -> Unit = {})` | 드라이브 모드 연동 색상 자동 변경 활성화 설정 | `CONTROL_CAR_LIGHTS` |
| `registerAmbientColorAutoByDriveModeEnabled(listener)` / `unregisterAmbientColorAutoByDriveModeEnabled(listener)` | 드라이브 모드 연동 설정 변경 구독/해제 (`AmbientColorAutoByDriveModeEnabledListener`) | `CAR_LIGHTS` |

> ※ `setAmbientLightSwitch`, `setAmbientLightBrightness`, `setAmbientLightBrightnessColor`는 Set API이지만 Reference상 `@RequiresPermission`이 `CAR_LIGHTS`로 표기되어 있습니다(다른 Light 제어 API는 `CONTROL_CAR_LIGHTS`). 이 세 API는 `onComplete`에 기본값이 없습니다.

#### 1.3.10 Odometer

총 주행거리(오도미터)와 트립 주행거리·주행시간 API를 제공합니다. (`vehicle.getOdometer()`) 오도미터는 **km**, 트립 주행거리는 **m**, 트립 주행시간은 **분(minutes)** 단위입니다.

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkCurrentDistanceDrivenCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 총 주행거리 기능 지원 여부 조회 | `CAR_DRIVE` |
| `getCurrentDistanceDriven(onComplete: (Float?) -> Unit)` | 현재 오도미터 값 조회(km). 값이 없으면 `null` | `CAR_DRIVE` |
| `registerDistanceDriven(listener)` / `unregisterDistanceDriven(listener)` | 오도미터(km) 변경 구독/해제 (`DistanceDrivenListener`) | `CAR_DRIVE` |
| `checkTripCurrentDrivingDistanceCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 현재 트립 주행거리 기능 지원 여부 조회 | `CAR_DRIVE` |
| `getTripCurrentDrivingDistance(onComplete: (Float?) -> Unit)` | 현재 주행 세션의 주행거리 조회(m) | `CAR_DRIVE` |
| `registerTripCurrentDrivingDistance(listener)` / `unregisterTripCurrentDrivingDistance(listener)` | 현재 트립 주행거리(m) 변경 구독/해제 (`DistanceDrivenListener`) | `CAR_DRIVE` |
| `checkTripCumulativeDrivingDistanceCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 누적 트립 주행거리 기능 지원 여부 조회 | `CAR_DRIVE` |
| `getTripCumulativeDrivingDistance(onComplete: (Float?) -> Unit)` | 운행 기간 전체 누적 주행거리 조회(m) | `CAR_DRIVE` |
| `registerTripCumulativeDrivingDistance(listener)` / `unregisterTripCumulativeDrivingDistance(listener)` | 누적 주행거리(m) 변경 구독/해제 (`DistanceDrivenListener`) | `CAR_DRIVE` |
| `checkTripSinceRefillDrivingDistanceCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 주유 이후 주행거리 기능 지원 여부 조회 | `CAR_DRIVE` |
| `getTripSinceRefillDrivingDistance(onComplete: (Float?) -> Unit)` | 마지막 주유 이후 주행거리 조회(m) | `CAR_DRIVE` |
| `registerTripSinceRefillDrivingDistance(listener)` / `unregisterTripSinceRefillDrivingDistance(listener)` | 주유 이후 주행거리(m) 변경 구독/해제 (`DistanceDrivenListener`) | `CAR_DRIVE` |
| `checkTripInstantaneousDrivingDistanceCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 순간 트립 주행거리 기능 지원 여부 조회. **(Deprecated)** — `TripStatisticType.INSTANTANEOUS` 사용 | `CAR_DRIVE` |
| `getTripInstantaneousDrivingDistance(onComplete: (Float?) -> Unit)` | 현재 트립의 순간 주행거리 조회(m). **(Deprecated)** | `CAR_DRIVE` |
| `registerTripInstantaneousDrivingDistance(listener)` / `unregisterTripInstantaneousDrivingDistance(listener)` | 순간 주행거리 변경 구독/해제 (`DistanceDrivenListener`). **(Deprecated)** | `CAR_DRIVE` |
| `checkTripCurrentDrivingTimeCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 현재 트립 주행시간 기능 지원 여부 조회 | `CAR_DRIVE` |
| `getTripCurrentDrivingTime(onComplete: (Int?) -> Unit)` | 현재 주행 세션의 주행시간 조회(분) | `CAR_DRIVE` |
| `registerTripCurrentDrivingTime(listener)` / `unregisterTripCurrentDrivingTime(listener)` | 현재 주행시간 변경 구독/해제 (`DrivingTimeListener`) | `CAR_DRIVE` |
| `checkTripCumulativeDrivingTimeCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 누적 주행시간 기능 지원 여부 조회 | `CAR_DRIVE` |
| `getTripCumulativeDrivingTime(onComplete: (Int?) -> Unit)` | 운행 기간 전체 누적 주행시간 조회(분) | `CAR_DRIVE` |
| `registerTripCumulativeDrivingTime(listener)` / `unregisterTripCumulativeDrivingTime(listener)` | 누적 주행시간 변경 구독/해제 (`DrivingTimeListener`) | `CAR_DRIVE` |
| `checkTripSinceRefillDrivingTimeCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 주유 이후 주행시간 기능 지원 여부 조회 | `CAR_DRIVE` |
| `getTripSinceRefillDrivingTime(onComplete: (Int?) -> Unit)` | 마지막 주유 이후 주행시간 조회(분) | `CAR_DRIVE` |
| `registerTripSinceRefillDrivingTime(listener)` / `unregisterTripSinceRefillDrivingTime(listener)` | 주유 이후 주행시간 변경 구독/해제 (`DrivingTimeListener`) | `CAR_DRIVE` |
| `checkTripDrivingDistanceCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 트립 주행거리 기능 지원 여부 조회 | `CAR_DRIVE` |
| `checkTripDrivingTimeCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 트립 주행시간 기능 지원 여부 조회 | `CAR_DRIVE` |
| `setRangeRemainingDistance(value: Float, onComplete: (Float?) -> Unit = {})` | 예상 잔여 주행가능거리(m) 설정. **(Deprecated, with error — 호출 시 컴파일 오류)** | `CONTROL_CAR_DRIVE` |

#### 1.3.11 Safety

에어백, 안전벨트 경고, 오일/브레이크액/워셔액 상태, 경고등(Telltale) API를 제공합니다. (`vehicle.getSafety()`) `CapabilityConfig`가 없는 `checkAirbagCapabilities`·`checkSeatBuckledCapabilities`의 `(List<SeatArea>)` 오버로드는 **(Deprecated)** 입니다.

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkAirbagCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 에어백 기능 지원 여부·좌석 영역 조회 | `CAR_SAFETY` |
| `hasSeatAirbagEnabled(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 좌석 에어백 활성 상태 조회. Reference 설명상 `true`면 해당 영역 에어백에 문제가 있음 | `CAR_SAFETY` |
| `registerSeatAirbagEnabled(area, listener)` / `unregisterSeatAirbagEnabled(area, listener)` | 에어백 활성 상태 변경 구독/해제 (`SeatAirbagEnabledListener`) | `CAR_SAFETY` |
| `checkAirbagDeployedCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 에어백 전개 감지 기능 지원 여부 조회 | `CAR_SAFETY` |
| `hasSeatAirbagBeenDeployed(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 에어백 전개 여부 조회. `true`면 전개됨 | `CAR_SAFETY` |
| `registerSeatAirbagDeployed(area, listener)` / `unregisterSeatAirbagDeployed(area, listener)` | 에어백 전개 상태 변경 구독/해제 (`SeatAirbagDeployedListener`) | `CAR_SAFETY` |
| `checkSeatBuckledCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 안전벨트 체결 기능 지원 여부 조회 | `CAR_SAFETY` |
| `checkSeatNotBuckledWarningCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 안전벨트 미체결 경고 기능 지원 여부 조회 | `CAR_SAFETY` |
| `hasSeatBeltNotBuckledWarning(area: SeatArea, onComplete: (Boolean?) -> Unit)` | 안전벨트 미체결 경고 상태 조회. `true`면 해당 좌석 벨트 미체결 | `CAR_SAFETY` |
| `registerSeatBeltNotBuckledWarning(area, listener)` / `unregisterSeatBeltNotBuckledWarning(area, listener)` | 미체결 경고 변경 구독/해제 (`SeatBeltNotBuckledWarningListener`) | `CAR_SAFETY` |
| `checkEngineOilLevelStateCapabilities(onComplete: (CapabilityConfig, List<OilLevelState>) -> Unit)` | 엔진 오일 레벨 기능 지원 여부·지원 상태 목록 조회 | `CAR_SAFETY` |
| `getEngineOilLevelState(onComplete: (OilLevelState?) -> Unit)` | 현재 엔진 오일 레벨 조회 | `CAR_SAFETY` |
| `registerEngineOilLevelState(listener)` / `unregisterEngineOilLevelState(listener)` | 엔진 오일 레벨 변경 구독/해제 (`OilLevelStateListener`) | `CAR_SAFETY` |
| `checkTransmissionOilLevelStateCapabilities(onComplete: (CapabilityConfig, List<OilLevelState>) -> Unit)` | 변속기 오일 레벨 기능 지원 여부 조회 | `CAR_SAFETY` |
| `getTransmissionOilLevelState(onComplete: (OilLevelState?) -> Unit)` | 현재 변속기 오일 레벨 조회 | `CAR_SAFETY` |
| `registerTransmissionOilLevelState(listener)` / `unregisterTransmissionOilLevelState(listener)` | 변속기 오일 레벨 변경 구독/해제 (`OilLevelStateListener`) | `CAR_SAFETY` |
| `checkBrakeFluidLevelStateCapabilities(onComplete: (CapabilityConfig, List<OilLevelState>) -> Unit)` | 브레이크액 레벨 기능 지원 여부 조회 | `CAR_SAFETY` |
| `getBrakeFluidLevelState(onComplete: (OilLevelState?) -> Unit)` | 현재 브레이크액 레벨 조회 | `CAR_SAFETY` |
| `registerBrakeFluidLevelState(listener)` / `unregisterBrakeFluidLevelState(listener)` | 브레이크액 레벨 변경 구독/해제 (`OilLevelStateListener`) | `CAR_SAFETY` |
| `checkLowWasherFluidWarningStateCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 워셔액 부족 경고 기능 지원 여부 조회 | `CAR_SAFETY` |
| `hasLowWasherFluidWarning(onComplete: (Boolean?) -> Unit)` | 워셔액 부족 경고 상태 조회. `true`면 워셔액 문제 있음 | `CAR_SAFETY` |
| `registerLowWasherFluidWaringState(listener)` / `unregisterLowWasherFluidWaringState(listener)` | 워셔액 경고 변경 구독/해제 (`LowWasherFluidWaringListener`, 철자 `Waring` 주의) | `CAR_SAFETY` |
| `checkPowerDownWarningIndicatorCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 출력 저하 경고등 기능 지원 여부 조회 | `CAR_SAFETY` |
| `hasPowerDownWarningIndicator(onComplete: (Boolean?) -> Unit)` | 출력 저하(Power Down) 경고 상태 조회. `true`면 엔진에 문제 있음 | `CAR_SAFETY` |
| `registerPowerDownWarningIndicator(listener)` / `unregisterPowerDownWarningIndicator(listener)` | 출력 저하 경고 변경 구독/해제 (`PowerDownWarningIndicatorListener`) | `CAR_SAFETY` |
| `checkTelltalesCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 경고등(Telltale) 기능 지원 여부 조회 | `PRIVATE_CAR_SAFETY` (`signature\|privileged`) |
| `getTelltalesInfo(onComplete: (telltales: Telltales) -> Unit)` | 현재 경고등 정보 목록 조회 | 어노테이션 `CAR_SAFETY` / 문서 표기 `PRIVATE_CAR_SAFETY` (`signature\|privileged`) |
| `registerTelltales(listener)` / `unregisterTelltales(listener)` | 경고등 정보 변경 구독/해제 (`TelltaleInfoListener`) | `PRIVATE_CAR_SAFETY` (`signature\|privileged`) |

#### 1.3.12 Seat

시트 위치·각도 이동, 착석 감지, 안전벨트, 시트 열선/통풍, 시트 메모리 API를 제공합니다. (`vehicle.getSeat()`) 모든 API는 `SeatArea`를 영역으로 사용합니다(`checkXxx` 제외).

**시트 위치 (조회·구독)** — 위치 값은 `Int?`이며, 범위는 `check…PositionCapabilities`의 영역별 `AreaBounds<Int>`로 확인합니다. 이름이 `…Position`이 없는 예전 capability API(`checkSeatForeAftCapabilities` 등, `(Map<SeatArea, AreaBounds<Int>>)` 반환)는 **(Deprecated)** 이며 대응하는 `…PositionCapabilities`로 대체됩니다.

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkSeatForeAftPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 시트 전후 위치 기능 지원 여부·범위 조회 | `CAR_SEATS` |
| `checkSeatForeAftCapabilities(…)` | **(Deprecated)** → `checkSeatForeAftPositionCapabilities` | `CAR_SEATS` |
| `getCurrentSeatForeAftPosition(area, onComplete: (Int?) -> Unit)` | 현재 시트 전후 위치 조회 | `CAR_SEATS` |
| `registerSeatForeAftPosition(area, listener)` / `unregisterSeatForeAftPosition(area, listener)` | 시트 전후 위치 변경 구독/해제 (`SeatForeAftPositionListener`) | `CAR_SEATS` |
| `checkSeatBackrestAnglePositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 등받이 각도 기능 지원 여부·범위 조회 | `CAR_SEATS` |
| `checkSeatBackrestAngleCapabilities(…)` | **(Deprecated)** → `checkSeatBackrestAnglePositionCapabilities` | `CAR_SEATS` |
| `getCurrentSeatBackrestAnglePosition(area, onComplete: (Int?) -> Unit)` | 현재 등받이 각도 위치 조회 | `CAR_SEATS` |
| `registerSeatBackrestAnglePosition(area, listener)` / `unregisterSeatBackrestAnglePosition(area, listener)` | 등받이 각도 변경 구독/해제 (`SeatBackrestAnglePositionListener`) | `CAR_SEATS` |
| `checkSeatHeightPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 시트 높이 기능 지원 여부·범위 조회 | `CAR_SEATS` |
| `checkSeatHeightCapabilities(…)` | **(Deprecated)** → `checkSeatHeightPositionCapabilities` | `CAR_SEATS` |
| `getCurrentSeatHeightPosition(area, onComplete: (Int?) -> Unit)` | 현재 시트 높이 조회 | `CAR_SEATS` |
| `registerSeatHeightPosition(area, listener)` / `unregisterSeatHeightPosition(area, listener)` | 시트 높이 변경 구독/해제 (`SeatHeightPositionListener`) | `CAR_SEATS` |
| `checkSeatDepthPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 시트 방석 깊이 기능 지원 여부·범위 조회 | `CAR_SEATS` |
| `checkSeatDepthCapabilities(…)` | **(Deprecated)** → `checkSeatDepthPositionCapabilities` | `CAR_SEATS` |
| `getCurrentSeatDepthPosition(area, onComplete: (Int?) -> Unit)` | 현재 시트 깊이 조회 | `CAR_SEATS` |
| `registerSeatDepthPosition(area, listener)` / `unregisterSeatDepthPosition(area, listener)` | 시트 깊이 변경 구독/해제 (`SeatDepthPositionListener`) | `CAR_SEATS` |
| `checkSeatTiltPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 시트 틸트 기능 지원 여부·범위 조회 | `CAR_SEATS` |
| `checkSeatTiltCapabilities(…)` | **(Deprecated)** → `checkSeatTiltPositionCapabilities` | `CAR_SEATS` |
| `getCurrentSeatTiltPosition(area, onComplete: (Int?) -> Unit)` | 현재 시트 틸트 위치 조회 | `CAR_SEATS` |
| `registerSeatTiltPosition(area, listener)` / `unregisterSeatTiltPosition(area, listener)` | 시트 틸트 변경 구독/해제 (`SeatTiltPositionListener`) | `CAR_SEATS` |
| `checkSeatLumbarForeAftPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 요추 받침 전후 기능 지원 여부·범위 조회 | `CAR_SEATS` |
| `checkSeatLumbarForeAftCapabilities(…)` | **(Deprecated)** → `checkSeatLumbarForeAftPositionCapabilities` | `CAR_SEATS` |
| `getCurrentSeatLumbarForeAftPosition(area, onComplete: (Int?) -> Unit)` | 현재 요추 받침 전후 위치 조회 | `CAR_SEATS` |
| `registerSeatLumbarForeAftPosition(area, listener)` / `unregisterSeatLumbarForeAftPosition(area, listener)` | 요추 받침 전후 위치 변경 구독/해제 (`SeatLumbarForeAftPositionListener`) | `CAR_SEATS` |
| `checkSeatLumbarSideSupportPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 요추 측면 지지 기능 지원 여부·범위 조회 | `CAR_SEATS` |
| `checkSeatLumbarSideSupportCapabilities(…)` | **(Deprecated)** → `checkSeatLumbarSideSupportPositionCapabilities` | `CAR_SEATS` |
| `getCurrentSeatLumbarSideSupportPosition(area, onComplete: (Int?) -> Unit)` | 현재 요추 측면 지지 위치 조회 | `CAR_SEATS` |
| `registerSeatLumbarSideSupportPosition(area, listener)` / `unregisterSeatLumbarSideSupportPosition(area, listener)` | 요추 측면 지지 위치 변경 구독/해제 (`SeatLumbarSideSupportPositionListener`) | `CAR_SEATS` |
| `checkSeatHeadrestHeightPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 헤드레스트 높이 기능 지원 여부·범위 조회 | `CAR_SEATS` |
| `checkSeatHeadrestHeightCapabilities(…)` | **(Deprecated)** → `checkSeatHeadrestHeightPositionCapabilities` | `CAR_SEATS` |
| `getCurrentSeatHeadrestHeightAftPosition(area, onComplete: (Int?) -> Unit)` | 현재 헤드레스트 높이 위치 조회 | `CAR_SEATS` |
| `registerSeatHeadrestHeightAftPosition(area, listener)` / `unregisterSeatHeadrestHeightAftPosition(area, listener)` | 헤드레스트 높이 위치 변경 구독/해제 (`SeatHeadrestHeightAftPositionListener`) | `CAR_SEATS` |
| `checkSeatHeadrestAnglePositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 헤드레스트 각도 기능 지원 여부·범위 조회 | `CAR_SEATS` |
| `checkSeatHeadrestAngleCapabilities(…)` | **(Deprecated)** → `checkSeatHeadrestAnglePositionCapabilities` | `CAR_SEATS` |
| `getCurrentSeatHeadrestAnglePosition(area, onComplete: (Int?) -> Unit)` | 현재 헤드레스트 각도 위치 조회 | `CAR_SEATS` |
| `registerSeatHeadrestAnglePosition(area, listener)` / `unregisterSeatHeadrestAnglePosition(area, listener)` | 헤드레스트 각도 변경 구독/해제 (`SeatHeadrestAnglePositionListener`) | `CAR_SEATS` |
| `checkSeatHeadrestForeAftPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 헤드레스트 전후 기능 지원 여부·범위 조회 | `CAR_SEATS` |
| `checkSeatHeadrestForeAftCapabilities(…)` | **(Deprecated)** → `checkSeatHeadrestForeAftPositionCapabilities` | `CAR_SEATS` |
| `getCurrentSeatHeadrestForeAftPosition(area, onComplete: (Int?) -> Unit)` | 현재 헤드레스트 전후 위치 조회 | `CAR_SEATS` |
| `registerSeatHeadrestForeAftPosition(area, listener)` / `unregisterSeatHeadrestForeAftPosition(area, listener)` | 헤드레스트 전후 위치 변경 구독/해제 (`SeatHeadrestForeAftPositionListener`) | `CAR_SEATS` |
| `checkSeatBeltHeightPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 안전벨트 높이 기능 지원 여부·범위 조회 | `CAR_SEATS` |
| `checkSeatBeltHeightCapabilities(…)` | **(Deprecated)** → `checkSeatBeltHeightPositionCapabilities` | `CAR_SEATS` |
| `getCurrentSeatBeltHeightPosition(area, onComplete: (Int?) -> Unit)` | 현재 안전벨트 높이 조회 | `CAR_SEATS` |
| `registerSeatBeltHeightPosition(area, listener)` / `unregisterSeatBeltHeightPosition(area, listener)` | 안전벨트 높이 변경 구독/해제 (`SeatBeltHeightPositionListener`) | `CAR_SEATS` |

**시트 이동 (`MoveControl`: `FORWARD` / `BACKWARD` / `STOP`)** — 모든 move API의 형태는 `moveXxx(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = {…})`이며, `checkMoveXxxCapabilities`는 `(CapabilityConfig, List<SeatArea>) -> Unit`을 반환합니다.

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkMoveSeatForeAftCapabilities` | 시트 전후 이동 기능 지원 여부·영역 조회 | `CAR_SEATS` |
| `moveSeatForeAft` | 시트 전후 이동 | `CONTROL_CAR_SEATS` |
| `checkMoveSeatBackrestAngleCapabilities` | 등받이 각도 이동 기능 지원 여부 조회 | `CAR_SEATS` |
| `moveSeatBackrestAngle` | 등받이 각도 이동 | `CONTROL_CAR_SEATS` |
| `checkMoveSeatHeightCapabilities` | 시트 높이 이동 기능 지원 여부 조회 | `CAR_SEATS` |
| `moveSeatHeight` | 시트 높이 이동 | `CONTROL_CAR_SEATS` |
| `checkMoveSeatDepthCapabilities` | 시트 깊이 이동 기능 지원 여부 조회 | `CAR_SEATS` |
| `moveSeatDepth` | 시트 깊이 이동 | `CONTROL_CAR_SEATS` |
| `checkMoveSeatTiltCapabilities` | 시트 틸트 이동 기능 지원 여부 조회 | `CAR_SEATS` |
| `moveSeatTilt` | 시트 틸트 이동 | `CONTROL_CAR_SEATS` |
| `checkMoveSeatLumbarForeAftCapabilities` | 요추 받침 전후 이동 기능 지원 여부 조회 | `CAR_SEATS` |
| `moveSeatLumbarForeAft` | 요추 받침 전후 이동 | `CONTROL_CAR_SEATS` |
| `checkMoveSeatLumbarSideSupportCapabilities` | 요추 측면 지지 이동 기능 지원 여부 조회 | `CAR_SEATS` |
| `moveSeatLumbarSideSupport` | 요추 측면 지지 이동 | `CONTROL_CAR_SEATS` |
| `checkMoveSeatHeadrestHeightCapabilities` | 헤드레스트 높이 이동 기능 지원 여부 조회 | `CAR_SEATS` |
| `moveSeatHeadrestHeight` | 헤드레스트 높이 이동 | `CONTROL_CAR_SEATS` |
| `checkMoveSeatHeadrestAngleCapabilities` | 헤드레스트 각도 이동 기능 지원 여부 조회 | `CAR_SEATS` |
| `moveSeatHeadrestAngle` | 헤드레스트 각도 이동 | `CONTROL_CAR_SEATS` |
| `checkMoveSeatHeadrestForeAftCapabilities` | 헤드레스트 전후 이동 기능 지원 여부 조회 | `CAR_SEATS` |
| `moveSeatHeadrestForeAft` | 헤드레스트 전후 이동 | `CONTROL_CAR_SEATS` |
| `checkMoveSeatBeltHeightCapabilities` | 안전벨트 높이 이동 기능 지원 여부 조회 | `CAR_SEATS` |
| `moveSeatBeltHeight` | 안전벨트 높이 이동 | `CONTROL_CAR_SEATS` |

**착석 / 안전벨트 / 열선·통풍 / 메모리**

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkSeatOccupancyCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 착석 감지 기능 지원 여부·영역 조회. `(List<SeatArea>)` 오버로드는 **(Deprecated)** | `CAR_SEATS` |
| `getCurrentSeatOccupancy(area, onComplete: (Boolean?) -> Unit)` | 현재 좌석 착석 여부 조회 | `CAR_SEATS` |
| `registerSeatOccupancy(area, listener)` / `unregisterSeatOccupancy(area, listener)` | 착석 상태 변경 구독/해제 (`SeatOccupancyListener`) | `CAR_SEATS` |
| `checkSeatBeltBuckledCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit)` | 안전벨트 체결 감지 기능 지원 여부 조회. `(List<SeatArea>)` 오버로드는 **(Deprecated)** | `CAR_SEATS` |
| `getCurrentSeatBeltBuckledState(area, onComplete: (Boolean?) -> Unit)` | 현재 안전벨트 체결 상태 조회 | `CAR_SEATS` |
| `registerSeatBeltBuckledState(area, listener)` / `unregisterSeatBeltBuckledState(area, listener)` | 안전벨트 체결 상태 변경 구독/해제 (`SeatBeltBuckledStateListener`) | `CAR_SEATS` |
| `checkSeatTemperatureCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 시트 열선/냉각 기능 지원 여부·단계 범위 조회. `(Map<…>)` 오버로드는 **(Deprecated)** | `CAR_SEATS` |
| `getHvacSeatTemperature(area, onComplete: (Int?) -> Unit)` | 현재 시트 열선/냉각 단계 조회 | `CAR_SEATS` |
| `setHvacSeatTemperature(area, value: Int, onComplete: (SeatArea, Int?) -> Unit = {…})` | 시트 열선/냉각 단계(step) 설정 | `CONTROL_CAR_SEATS` |
| `registerHvacSeatTemperature(area, listener)` / `unregisterHvacSeatTemperature(area, listener)` | 시트 열선/냉각 변경 구독/해제 (`HvacSeatTemperatureListener`) | `CAR_SEATS` |
| `checkSeatVentilationCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 시트 통풍 기능 지원 여부·단계 범위 조회. `(Map<…>)` 오버로드는 **(Deprecated)** | `CAR_SEATS` |
| `getHvacSeatVentilationState(area, onComplete: (Int?) -> Unit)` | 현재 시트 통풍 상태 조회 | `CAR_SEATS` |
| `setHvacSeatVentilation(area, value: Int, onComplete: (SeatArea, Int?) -> Unit = {…})` | 시트 통풍 단계 설정 | `CONTROL_CAR_SEATS` |
| `registerHvacSeatVentilation(area, listener)` / `unregisterHvacSeatVentilation(area, listener)` | 시트 통풍 변경 구독/해제 (`HvacSeatVentilationListener`) | `CAR_SEATS` |
| `checkSeatMemoryCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit)` | 시트 메모리 기능 지원 여부·메모리 번호 범위 조회 | `CAR_SEATS` |
| `selectSeatMemory(area: SeatArea, value: Int, onFailed)` | 시트 메모리 번호 선택(호출). `onComplete` 없음 | `CONTROL_CAR_SEATS` |

#### 1.3.13 SideMirror

사이드미러 열선 API를 제공합니다. (`vehicle.getSideMirror()`)

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkHvacSideMirrorHeatCapabilities(onComplete: (CapabilityConfig, Map<MirrorArea, AreaBounds<Int>>) -> Unit)` | 사이드미러 열선 기능 지원 여부·영역별 범위 조회. `(Map<MirrorArea, AreaBounds<Int>>)` 오버로드는 **(Deprecated)** | `CAR_MIRRORS` |
| `getHvacSideMirrorHeatState(area: MirrorArea, onComplete: (Int?) -> Unit)` | 현재 사이드미러 열선 상태 조회 | `CAR_MIRRORS` |
| `setHvacSideMirrorHeat(area: MirrorArea, value: Int, onComplete: (MirrorArea, Int?) -> Unit = {…})` | 사이드미러 열선 상태 설정 | `CONTROL_CAR_MIRRORS` |
| `registerHvacSideMirrorHeatState(area, listener)` / `unregisterHvacSideMirrorHeatState(area, listener)` | 열선 상태 변경 구독/해제 (`HvacSideMirrorHeatStateListener`) | `CAR_CLIMATE` |

#### 1.3.14 SteeringWheel

전륜 조향각과 스티어링휠 열선 API를 제공합니다. (`vehicle.getSteeringWheel()`)

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkSteeringWheelAngleCapabilities(onComplete: (CapabilityConfig, AreaBounds<Float>) -> Unit)` | 전륜 조향각 기능 지원 여부·범위 조회 | `CAR_DRIVE` |
| `getCurrentSteeringWheelAngle(onComplete: (Float?) -> Unit)` | 현재 전륜 조향각(도, degree) 조회. 좌측 조향은 음수. **스티어링휠 회전각이 아님** | `CAR_DRIVE` |
| `registerSteeringWheelAngle(listener)` / `unregisterSteeringWheelAngle(listener)` | 전륜 조향각 변경 구독/해제 (`SteeringWheelAngleListener`) | `CAR_DRIVE` |
| `checkHvacSteeringWheelHeatCapabilities(onComplete: (CapabilityConfig, AreaBounds<Int>) -> Unit)` | 스티어링휠 열선 기능 지원 여부·단계 범위 조회. `(AreaBounds<Int>)` 오버로드는 **(Deprecated)** | `CAR_CLIMATE` |
| `getHvacSteeringWheelHeatState(onComplete: (Int?) -> Unit)` | 현재 스티어링휠 열선/냉각 상태 조회 | `CAR_CLIMATE` |
| `setHvacSteeringWheelHeat(value: Int, onComplete: (Int?) -> Unit = {})` | 스티어링휠 열선/냉각 단계 설정 | `CONTROL_CAR_CLIMATE` |
| `registerHvacSteeringWheelHeatState(listener)` / `unregisterHvacSteeringWheelHeatState(listener)` | 열선 상태 변경 구독/해제 (`HvacSteeringWheelHeatStateListener`) | `CAR_CLIMATE` |

#### 1.3.15 Tire

타이어 공기압 관련 API를 제공합니다. (`vehicle.getTire()`)

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkLowTirePressureCapabilities(onComplete: (CapabilityConfig, List<WheelArea>) -> Unit)` | 타이어 저압 기능 지원 여부·바퀴 영역 조회 | `CAR_SAFETY` |
| `checkWheelAreaCapabilities(onComplete: (List<WheelArea>) -> Unit)` | 바퀴 영역 조회. **(Deprecated)** → `checkLowTirePressureCapabilities` | `CAR_SAFETY` |
| `getLowTirePressure(area: WheelArea, onComplete: (Float?) -> Unit)` | 위험 수준 저압 임계값 조회(kPa) | `CAR_SAFETY` |

#### 1.3.16 Toll

ETCS(하이패스) 음성 안내 음량 API를 제공합니다. (`vehicle.getToll()`)

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkEtcsVoiceGuidanceVolumeCapabilities(onComplete: (CapabilityConfig, AreaBounds<Int>) -> Unit)` | ETCS 음성 안내 음량 기능 지원 여부·음량 범위 조회 | `CAR_TOLL` |
| `getCurrentEtcsVoiceGuidanceVolume(onComplete: (Int?) -> Unit)` | 현재 ETCS 음성 안내 음량 조회 | `CAR_TOLL` |
| `setEtcsVoiceGuidanceVolume(value: Int, onComplete: () -> Unit, onFailed)` | ETCS 음성 안내 음량 설정 (`onComplete` 기본값 없음) | `CONTROL_CAR_TOLL` |
| `registerEtcsVoiceGuidanceVolume(listener)` / `unregisterEtcsVoiceGuidanceVolume(listener)` | 음량 변경 구독/해제 (`EtcsVoiceGuidanceVolumeListener`) | `CAR_TOLL` |

#### 1.3.17 TurnSignal

방향 지시등 상태 조회 API를 제공합니다. (`vehicle.getTurnSignal()`)

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkTurnSignalStateCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 방향 지시등 상태 기능 지원 여부 조회 | `CAR_LIGHTS` |
| `getCurrentTurnSignalState(onComplete: (TurnSignalValue?) -> Unit)` | 현재 방향 지시등 상태(`OFF`/`RIGHT`/`LEFT`) 조회 | `CAR_LIGHTS` |
| `registerTurnSignalState(listener)` / `unregisterTurnSignalState(listener)` | 방향 지시등 상태 변경 구독/해제 (`TurnSignalStateListener`) | `CAR_LIGHTS` |

#### 1.3.18 Window

창문 위치·잠금, 윈도우 블라인드, 스마트 글라스 API를 제공합니다. (`vehicle.getWindow()`) 모든 영역은 `WindowArea`입니다.

**창문 위치 / 잠금**

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkWindowPositionCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, AreaBounds<Int>>) -> Unit)` | 창문 위치 기능 지원 여부·영역별 범위 조회. `(Map<…>)` 오버로드는 **(Deprecated)** | `CAR_WINDOWS` |
| `getCurrentWindowPosition(area, onComplete: (Int?) -> Unit)` | 현재 창문 위치 조회 | `CAR_WINDOWS` |
| `setWindowPosition(area, value: Int, onComplete: (WindowArea, Int?) -> Unit = {…})` | 창문 위치 설정 | `CONTROL_CAR_WINDOWS` |
| `registerWindowPosition(area, listener)` / `unregisterWindowPosition(area, listener)` | 창문 위치 변경 구독/해제 (`WindowPositionListener`) | `CAR_WINDOWS` |
| `checkWindowLockCapabilities(onComplete: (CapabilityConfig, List<WindowArea>) -> Unit)` | 창문 잠금 기능 지원 여부·영역 조회. `(List<WindowArea>)` 오버로드는 **(Deprecated)** | `CAR_WINDOWS` |
| `getCurrentWindowLockState(area, onComplete: (Boolean?) -> Unit)` | 현재 창문 잠금 상태 조회 | `CAR_WINDOWS` |
| `setWindowLock(area, value: Boolean, onComplete: (WindowArea, Boolean?) -> Unit = {…})` | 창문 잠금 상태 설정 | `CONTROL_CAR_WINDOWS` |
| `registerWindowLockState(area, listener)` / `unregisterWindowLockState(area, listener)` | 창문 잠금 상태 변경 구독/해제 (`WindowLockStateListener`) | `CAR_WINDOWS` |

**윈도우 블라인드**

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkWindowBlindPositionCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, AreaBounds<Int>>) -> Unit)` | 블라인드 위치 기능 지원 여부·범위 조회 | `CAR_WINDOWS` |
| `getCurrentWindowBlindPosition(area, onComplete: (Int?) -> Unit)` | 지정 창 영역의 현재 블라인드 위치 조회 | `CAR_WINDOWS` |
| `registerWindowBlindPosition(area, listener)` / `unregisterWindowBlindPosition(area, listener)` | 블라인드 위치 변경 구독/해제 (`WindowPositionListener`) | `CAR_WINDOWS` |
| `checkMoveWindowBlindCapabilities(onComplete: (CapabilityConfig, List<WindowArea>) -> Unit)` | 블라인드 이동 기능 지원 여부·영역 조회 | `CAR_WINDOWS` |
| `moveWindowBlind(area, value: MoveControl, onComplete: (WindowArea, MoveControl?) -> Unit)` | 지정 창 영역의 블라인드 이동(`MoveControl`) | `CONTROL_CAR_WINDOWS` |

**스마트 글라스**

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkSmartGlassStateCapabilities(onComplete: (CapabilityConfig) -> Unit)` | 스마트 글라스 On/Off 기능 지원 여부 조회 | `CAR_WINDOWS` |
| `getCurrentSmartGlassState(onComplete: (Boolean?) -> Unit)` | 현재 스마트 글라스 상태 조회 | `CAR_WINDOWS` |
| `setSmartGlassState(value: Boolean, onComplete: (Boolean?) -> Unit = {…})` | 스마트 글라스 상태 설정 | `CONTROL_CAR_WINDOWS` (`signature\|privileged`로 표기) |
| `registerSmartGlassState(listener)` / `unregisterSmartGlassState(listener)` | 스마트 글라스 상태 변경 구독/해제 (`SmartGlassStateListener`) | `CAR_WINDOWS` |
| `checkSmartGlassLevelCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, AreaBounds<Int>>) -> Unit)` | 스마트 글라스 레벨 기능 지원 여부·영역별 범위 조회 | `CAR_WINDOWS` |
| `getCurrentSmartGlassLevel(area, onComplete: (Int?) -> Unit)` | 지정 창 영역의 현재 스마트 글라스 레벨 조회 | `CAR_WINDOWS` |
| `setSmartGlassLevel(area, value: Int, onComplete: (WindowArea, Int?) -> Unit = {…})` | 지정 창 영역의 스마트 글라스 레벨 설정. 성공 시 영역과 적용된 레벨 반환 | `CONTROL_CAR_WINDOWS` |
| `registerSmartGlassLevel(area, listener)` / `unregisterSmartGlassLevel(area, listener)` | 스마트 글라스 레벨 변경 구독/해제 (`SmartGlassLevelListener`) | `CAR_WINDOWS` |
| `getSmartGlassConfig(area, onComplete: (SmartGlassConfig?) -> Unit)` | 지정 창 영역의 스마트 글라스 구성(`zoneCount`, `levelCount`) 조회. 차량의 `[zoneCount, levelCount]` 값을 디코딩(예: `[4, 1]` → `SmartGlassConfig(zoneCount = 4, levelCount = 1)`) | `CAR_WINDOWS` |
| `checkSmartGlassZoneStateCapabilities(onComplete: (CapabilityConfig, List<WindowArea>) -> Unit)` | 스마트 글라스 존(패널)별 제어 기능 지원 여부·영역 조회 | `CAR_WINDOWS` |
| `setSmartGlassZoneState(area, value: List<SmartGlassZoneCommand>, onComplete: (WindowArea, List<SmartGlassZoneCommand>?) -> Unit = {…})` | 패널마다 명령 하나씩(`ON`/`OFF`/`SKIP`) 패널 순서대로 제출. 리스트 크기는 `SmartGlassConfig.zoneCount`와 같아야 하며 비어 있으면 안 됨 | `CONTROL_CAR_WINDOWS` |

스마트 글라스 사용 시 주의:
- 존 상태 지원이 레벨 지원을 의미하지 않으며, `SmartGlassConfig`가 있어도 레벨 기능이 없을 수 있습니다. 레벨 지원은 `checkSmartGlassLevelCapabilities`로 별도 확인해야 합니다.
- `setSmartGlassZoneState`는 `SKIP`을 포함한 모든 위치를 보존해 제출하며, `SKIP`만으로 구성된 리스트도 서비스에 제출됩니다. SDK는 제출 전에 `getSmartGlassConfig`나 capability를 확인하지 않으므로 앱에서 확인해야 합니다.

#### 1.3.19 Wiper

와이퍼 상태와 스위치 API를 제공합니다. (`vehicle.getWiper()`) 영역은 `WindowArea`입니다.

| API | 설명 | 필요 퍼미션 |
|---|---|---|
| `checkWiperStateCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, List<WiperState>>) -> Unit)` | 와이퍼 상태 기능 지원 여부·영역별 지원 상태 조회. `(Map<…>)` 오버로드는 **(Deprecated)** | `CAR_WINDOWS` |
| `getCurrentWiperState(area, onComplete: (WiperState?) -> Unit)` | 현재 와이퍼 동작 상태 조회 | `CAR_WINDOWS` |
| `registerWiperState(area, listener)` / `unregisterWiperState(area, listener)` | 와이퍼 상태 변경 구독/해제 (`WiperStateListener`) | `CAR_WINDOWS` |
| `checkWiperSwitchCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, List<WiperSwitch>>) -> Unit)` | 와이퍼 스위치 기능 지원 여부·영역별 지원 스위치 값 조회. `(Map<…>)` 오버로드는 **(Deprecated)** | `CAR_WINDOWS` |
| `getCurrentWiperSwitch(area, onComplete: (WiperSwitch?) -> Unit)` | 지정 창 영역의 현재 와이퍼 스위치 조회 | `CAR_WINDOWS` |
| `setWiperSwitch(area, value: WiperSwitch, onComplete: (WindowArea, WiperSwitch?) -> Unit = {…})` | 와이퍼 스위치(모드) 설정 | `CONTROL_CAR_WINDOWS` |

---
### 1.4 리스너 목록 (`ai.pleos.playground.vehicle.listener`)

모든 리스너는 sealed interface **`VehicleListener`**를 상속하며, 공통으로 `onFailed(e: Exception)`를 구현해야 합니다. 아래 표의 콜백 메서드는 값이 바뀔 때 호출됩니다.

| 도메인 | 리스너 | 콜백 메서드 | 사용하는 register API |
|---|---|---|---|
| 공통 | `VehicleListener` | `onFailed(e: Exception)` | (모든 리스너의 부모) |
| Brake | `BrakePedalStatusListener` | `onBrakePedalStatusUpdated(status: Boolean?)` | `registerBrakePedalStatus` |
| Display | `TemperatureDisplayUnitListener` | `onTemperatureDisplayUnitUpdated(unit: TemperatureUnit?)` | `registerTemperatureDisplayUnit` |
| Door | `DoorLockStateListener` | `onDoorLockStateUpdated(state: Boolean?)` | `registerDoorLockState` |
| Door | `DoorPositionListener` | `onPositionUpdated(pos: Int?)` | `registerDoorPosition` |
| Door | `GloveBoxLockStateListener` | `onGloveBoxLockStateUpdated(state: Boolean?)` | `registerGloveBoxLockState` |
| DrivingMode | `DrivingModeStateListener` | `onDrivingModeStateUpdated(state: DrivingModeType?)` | `registerDrivingModeState` |
| DrivingMode | `CruiseControlStateListener` | `onCruiseControlStateUpdated(state: CruiseControl?)` | `registerCruiseControlState` |
| DrivingMode | `CruiseControlTargetSpeedListener` | `onCruiseControlTargetSpeedUpdated(speed: Float?)` — m/s | `registerCruiseControlTargetSpeed` |
| EvBattery | `EvBatteryStateOfChargeListener` | `onEvBatteryStateOfChargeUpdated(value: Int?)` — % | `registerEvBatteryStateOfCharge` |
| EvBattery | `EvBatteryEfficiencyListener` | `onEvBatteryEfficiencyUpdated(efficiency: Float?)` | `registerCurrent/Cumulative/Instantaneous/SinceChargeEvEfficiency` |
| Fuel | `FuelPercentListener` | `onFuelPercentUpdated(value: Int?)` — % | `registerFuelPercent` |
| Fuel | `FuelEconomyListener` | `onFuelEconomyUpdated(efficiency: Float?)` | `registerCurrent/Cumulative/Instantaneous/SinceChargeFuelEconomy` |
| HVAC | `HvacPowerStateListener` | `onHvacPowerStateUpdated(state: Boolean?)` | `registerHvacPowerState` |
| HVAC | `HvacAcStateListener` | `onHvacAcStateUpdated(state: Boolean?)` | `registerHvacAcState` |
| HVAC | `HvacMaxAcStateListener` | `onHvacMaxAcStateUpdated(state: Boolean?)` | `registerHvacMaxAcState` |
| HVAC | `HvacAutoStateListener` | `onHvacAutoStateUpdated(state: Boolean?)` | `registerHvacAutoState` |
| HVAC | `HvacDualStateListener` | `onHvacDualStateUpdated(state: Boolean?)` | `registerHvacDualState` |
| HVAC | `HvacAirCleanerStateListener` | `onHvacAirCleanerStateUpdated(state: Boolean?)` | `registerHvacAirCleanerState` |
| HVAC | `HvacRecirculationStateListener` | `onHvacAutoRecirculationStateUpdated(state: Boolean?)` | `registerHvacRecirculationState`, `registerHvacAutoRecirculationState` |
| HVAC | `HvacDefrosterStateListener` | `onHvacDefrosterStateUpdated(state: Boolean?)` | `registerHvacDefrosterState` |
| HVAC | `HvacMaxDefrostStateListener` | `onHvacMaxDefrostStateUpdated(state: Boolean?)` | `registerHvacMaxDefrostState` |
| HVAC | `HvacFanSpeedListener` | `onHvacFanSpeedUpdated(speed: Int?)` | `registerHvacFanSpeed` |
| HVAC | `HvacTemperatureListener` | `onHvacTemperatureUpdated(temperature: Float?)` — °C | `registerHvacTemperature`, `registerHvacTemperatureCurrent` |
| HVAC | `HvacTemperatureValueListener` | `onHvacTemperatureUpdated(celsius: Float?, fahrenheit: Float?)` | `registerHvacTemperature`, `registerHvacTemperatureCurrent` |
| Light | `LightStateListener` | `onLightStateUpdated(state: LightState?)` | High/LowBeam, Front/RearFog, TailLamp, Cabin, Reading, Ambient 상태 register API |
| Light | `AmbientLightBrightnessListener` | `onBrightnessUpdated(brightness: Int?)` | `registerAmbientLightBrightnessListener` |
| Light | `AmbientLightBrightnessColorListener` | `onBrightnessColorUpdated(argbColor: Int?)` | `registerAmbientLightBrightnessColorListener` |
| Light | `AmbientColorAutoByDriveModeEnabledListener` | `onAmbientColorAutoByDriveModeEnabledUpdated(enabled: Boolean?)` | `registerAmbientColorAutoByDriveModeEnabled` |
| Odometer | `DistanceDrivenListener` | `onDistanceDrivenUpdated(value: Float?)` — 등록 API에 따라 km 또는 m | `registerDistanceDriven`(km), `registerTrip…DrivingDistance`(m) |
| Odometer | `DrivingTimeListener` | `onDrivingTimeUpdated(value: Int?)` | `registerTrip…DrivingTime` |
| Safety | `SeatAirbagEnabledListener` | `onSeatAirbagEnabledUpdated(enabled: Boolean?)` | `registerSeatAirbagEnabled` |
| Safety | `SeatAirbagDeployedListener` | `onSeatAirbagDeployUpdated(deployed: Boolean?)` | `registerSeatAirbagDeployed` |
| Safety | `SeatBeltNotBuckledWarningListener` | `onSeatBeltNotBuckledWarningUpdated(state: Boolean?)` | `registerSeatBeltNotBuckledWarning` |
| Safety | `OilLevelStateListener` | `onOilLevelUpdated(level: OilLevelState?)` | `registerEngineOilLevelState`, `registerTransmissionOilLevelState`, `registerBrakeFluidLevelState` |
| Safety | `LowWasherFluidWaringListener` | `onLowWasherFluidWaringUpdated(state: Boolean?)` | `registerLowWasherFluidWaringState` |
| Safety | `PowerDownWarningIndicatorListener` | `onPowerDownWarningIndicatorUpdated(state: Boolean?)` | `registerPowerDownWarningIndicator` |
| Safety | `TelltaleInfoListener` | `onTelltaleInfoUpdated(telltales: Telltales?)` | `registerTelltales` |
| Seat | `SeatForeAftPositionListener` | `onSeatForeAftPositionUpdated(position: Int?)` | `registerSeatForeAftPosition` |
| Seat | `SeatBackrestAnglePositionListener` | `onSeatBackrestAnglePositionUpdated(position: Int?)` | `registerSeatBackrestAnglePosition` |
| Seat | `SeatHeightPositionListener` | `onSeatHeightPositionUpdated(position: Int?)` | `registerSeatHeightPosition` |
| Seat | `SeatDepthPositionListener` | `onSeatDepthPositionUpdated(position: Int?)` | `registerSeatDepthPosition` |
| Seat | `SeatTiltPositionListener` | `onSeatTiltPositionUpdated(position: Int?)` | `registerSeatTiltPosition` |
| Seat | `SeatLumbarForeAftPositionListener` | `onSeatLumbarForeAftPositionUpdated(position: Int?)` | `registerSeatLumbarForeAftPosition` |
| Seat | `SeatLumbarSideSupportPositionListener` | `onSeatLumbarSideSupportPositionUpdated(position: Int?)` | `registerSeatLumbarSideSupportPosition` |
| Seat | `SeatHeadrestHeightAftPositionListener` | `onSeatHeadrestHeightAftPositionUpdated(position: Int?)` | `registerSeatHeadrestHeightAftPosition` |
| Seat | `SeatHeadrestAnglePositionListener` | `onSeatHeadrestAnglePositionUpdated(position: Int?)` | `registerSeatHeadrestAnglePosition` |
| Seat | `SeatHeadrestForeAftPositionListener` | `onSeatHeadrestForeAftPositionUpdated(position: Int?)` | `registerSeatHeadrestForeAftPosition` |
| Seat | `SeatBeltHeightPositionListener` | `onSeatBeltHeightPositionUpdated(position: Int?)` | `registerSeatBeltHeightPosition` |
| Seat | `SeatBeltBuckledStateListener` | `onSeatBeltBuckledStateUpdated(state: Boolean?)` | `registerSeatBeltBuckledState` |
| Seat | `SeatOccupancyListener` | `onSeatOccupancyUpdated(occupancy: Boolean?)` | `registerSeatOccupancy` |
| Seat | `HvacSeatTemperatureListener` | `onHvacSeatTemperatureUpdated(temperature: Int?)` | `registerHvacSeatTemperature` |
| Seat | `HvacSeatVentilationListener` | `onHvacSeatVentilationUpdated(ventilation: Int?)` | `registerHvacSeatVentilation` |
| SideMirror | `HvacSideMirrorHeatStateListener` | `onHvacSideMirrorHeatStateUpdated(state: Int?)` | `registerHvacSideMirrorHeatState` |
| SteeringWheel | `SteeringWheelAngleListener` | `onSteeringWheelAngleUpdated(angle: Float?)` — 도, 좌측 음수 | `registerSteeringWheelAngle` |
| SteeringWheel | `HvacSteeringWheelHeatStateListener` | `onHvacSteeringWheelHeatStateUpdated(state: Int?)` | `registerHvacSteeringWheelHeatState` |
| Toll | `EtcsVoiceGuidanceVolumeListener` | `onEtcsVoiceGuidanceVolumeUpdated(volume: Int?)` | `registerEtcsVoiceGuidanceVolume` |
| TurnSignal | `TurnSignalStateListener` | `onTurnSignalStateUpdated(state: TurnSignalValue?)` | `registerTurnSignalState` |
| Window | `WindowPositionListener` | `onWindowPositionUpdated(position: Int?)` | `registerWindowPosition`, `registerWindowBlindPosition` |
| Window | `WindowLockStateListener` | `onWindowLockStateUpdated(state: Boolean?)` | `registerWindowLockState` |
| Window | `SmartGlassStateListener` | `onSmartGlassStateUpdated(state: Boolean?)` | `registerSmartGlassState` |
| Window | `SmartGlassLevelListener` | `onSmartGlassLevelUpdated(level: Int?)` | `registerSmartGlassLevel` |
| Wiper | `WiperStateListener` | `onWiperStateUpdated(state: WiperState?)` | `registerWiperState` |
| 트립 통계 | `TripDrivingDistanceListener` | `onTripDrivingDistanceUpdated(statistics: TripStatistics<Float>?)` | — |
| 트립 통계 | `TripDrivingTimeListener` | `onTripDrivingTimeUpdated(statistics: TripStatistics<Int>?)` | — |
| 트립 통계 | `TripEvEfficiencyListener` | `onTripEvEfficiencyUpdated(statistics: TripStatistics<Float>?)` | — |
| 트립 통계 | `TripFuelEconomyListener` | `onTripFuelEconomyUpdated(statistics: TripStatistics<Float>?)` | — |

> `Trip*Listener` 4종은 `listener` 패키지에 정의되어 있지만, v2.3.2 Reference의 도메인 인터페이스에는 이 리스너를 받는 register API가 없습니다.

---

### 1.5 주요 상수 / 모델

#### 1.5.1 Capability 모델 (`api.model.capability`)

| 클래스 | 정의 | 주요 멤버 |
|---|---|---|
| `AccessType` | enum, capability 접근 수준 | `NONE`, `READ`, `WRITE`, `READ_WRITE` |
| `CapabilityConfig` | `data class CapabilityConfig(val accessType: AccessType = AccessType.NONE)` | `accessType` |
| `AreaBounds<T>` | `data class AreaBounds<T>(val minValue: T, val maxValue: T)` — 영역별 허용 범위 | `minValue`, `maxValue` |
| `HvacTemperatureConfigArray` | 차량 온도 설정 config 배열에서 만든 °C↔°F 변환 테이블 | `minCelsius`[0], `maxCelsius`[1], `incrementCelsius`[2], `minFahrenheit`[3], `maxFahrenheit`[4], `incrementFahrenheit`[5] (각 값 = VHAL 정수 ÷ 10, `Float`), `raw: List<Int>`(원본 6개 정수, ×10 값) |
| `SmartGlassConfig` | `data class SmartGlassConfig(val zoneCount: Int, val levelCount: Int)` | `zoneCount`: 창 영역의 존(패널) 수 → `setSmartGlassZoneState`에 이 개수만큼 명령 전달 / `levelCount`: 차량이 보고한 레벨 수(레벨 기능 지원 여부를 의미하지 않음) |

#### 1.5.2 공통 / Telltale 모델

| 클래스 | 정의 | 주요 멤버 |
|---|---|---|
| `TripStatistics<T>` (`api.model.common`) | 유형별 트립 통계 묶음 | `current`, `instantaneous`, `cumulative`, `sinceRefill`(모두 `T?`, 기본 `null`), `raw: List<T?>`, `get(type: TripStatisticType): T?` |
| `Telltales` (`api.model.telltale`) | `data class Telltales(val telltales: List<TelltaleData> = arrayListOf())` | `telltales` |
| `TelltaleData` (`api.model.telltale`) | 경고등 1개 항목 | `type: TelltaleType`, `state: TelltaleState`, `color: Int`, `priority: Int`, `warningCount: Int` |

#### 1.5.3 영역 상수 (`constant.area`)

[1.2.4 영역(Area) 타입](#124-영역area-타입)의 표를 참조하세요. `AreaValue` 인터페이스(`abstract val id: Int`)를 `DoorValue`, `MirrorValue`, `SeatValue`, `TurnSignalValue`, `WheelValue`, `WindowValue`가 구현합니다.

#### 1.5.4 제어/상태 상수 (`constant.control`)

| enum | 의미 | 값 |
|---|---|---|
| `CruiseControl` | 크루즈 컨트롤 상태 | `OTHER`, `ENABLED`, `ACTIVATED`, `USER_OVERRIDE`, `SUSPENDED`, `FORCED_DEACTIVATION_WARNING` |
| `DrivingModeType` | 드라이브 모드 | `NORMAL`, `ECO`, `SPORT` |
| `FanDirectionType` | HVAC 풍향 | `UNKNOWN`, `FACE`, `FLOOR`, `FACE_AND_FLOOR`, `DEFROST`, `FACE_AND_DEFROST`, `DEFROST_AND_FLOOR`, `FACE_AND_FLOOR_AND_DEFROST` |
| `LightState` | 조명 상태 | `OFF`, `ON`, `DAYTIME_RUNNING` |
| `LightSwitch` | 조명 스위치 상태 | `OFF`, `ON`, `DAYTIME_RUNNING`, `AUTOMATIC` |
| `MoveControl` | 이동 제어 방향 | `BACKWARD`, `STOP`, `FORWARD` |
| `OilLevelState` | 오일(액) 레벨 상태 | `LOW`, `HIGH`, `NORMAL`, `ERROR`, `CRITICALLY_LOW` |
| `WiperState` | 와이퍼 상태 | `OTHER`, `OFF`, `ON`, `SERVICE` |
| `WiperSwitch` | 와이퍼 스위치 | `OTHER`, `OFF`, `MIST`, `INTERMITTENT_LEVEL_1`~`INTERMITTENT_LEVEL_5`, `CONTINUOUS_LEVEL_1`~`CONTINUOUS_LEVEL_5`, `AUTO`, `SERVICE` |
| `SmartGlassZoneCommand` | 스마트 글라스 패널 1개에 대한 명령 | `SKIP`(명령 없음, 패널 유지), `OFF`(패널 끔), `ON`(패널 켬) |
| `SteeringControl` | 조향 제어 상태 | `OFF`, `READY`, `ACTIVE` |
| `TripStatisticType` | 트립 통계 유형 (`Companion.findById(id: Int?)`) | `CURRENT`, `INSTANTANEOUS`, `CUMULATIVE`, `SINCE_REFILL` |
| `EvChargeState` | EV 충전 상태 | `UNKNOWN`, `CHARGING`, `FULLY_CHARGED`, `NOT_CHARGING`, `ERROR` |
| `EvConnectorType` | EV 충전 커넥터 타입 | `UNKNOWN`, `IEC_TYPE_1_AC`, `IEC_TYPE_2_AC`, `IEC_TYPE_3_AC`, `IEC_TYPE_4_DC`, `IEC_TYPE_1_CCS_DC`, `IEC_TYPE_2_CCS_DC`, `TESLA_ROADSTER`, `TESLA_HPWC`, `TESLA_SUPERCHARGER`, `GBT_AC`, `GBT_DC`, `OTHER` |
| `EvRegenerativeBrakingState` | EV 회생제동 상태 | `UNKNOWN`, `DISABLED`, `PARTIALLY_ENABLED`, `FULLY_ENABLED` |
| `PortLocationType` | 충전구 위치 | `UNKNOWN`, `FRONT_LEFT`, `FRONT_RIGHT`, `REAR_RIGHT`, `REAR_LEFT`, `FRONT`, `REAR` |
| `FuelType` | 연료 종류 | `BIODIESEL`, `CNG`, `DIESEL_1`, `DIESEL_2`, `E85`, `ELECTRIC`, `HYDROGEN`, `LEADED`, `LNG`, `LPG`, `OTHER`, `UNKNOWN`, `UNLEADED` |
| `GsrComplianceType` | GSR 준수 유형 | `GSR_COMPLIANCE_TYPE_NOT_REQUIRED`, `GSR_COMPLIANCE_TYPE_REQUIRED_V1` |

> `EvChargeState`, `EvConnectorType`, `EvRegenerativeBrakingState`, `PortLocationType`, `FuelType`, `GsrComplianceType`, `SteeringControl`은 상수로 정의되어 있으나 v2.3.2 도메인 인터페이스의 공개 API 시그니처에서는 사용되지 않습니다.

#### 1.5.5 단위 상수 (`constant.unit`)

| enum | 값 |
|---|---|
| `TemperatureUnit` | `CELSIUS`(°C), `FAHRENHEIT`(°F) |
| `SpeedDisplayUnit` | `METER_PER_SEC`(m/s), `MILES_PER_HOUR`(mph), `KILOMETERS_PER_HOUR`(km/h) |
| `DistanceDisplayUnit` | `KILOMETER`(km), `MILE`(mi) |
| `EvBatteryDisplayUnit` | `WATT_HOUR`(Wh), `AMPERE_HOURS`(Ah), `KILOWATT_HOUR`(kWh) |

#### 1.5.6 경고등 상수 (`constant.telltale`)

| enum | 값 |
|---|---|
| `TelltaleState` | `OFF`, `ON`, `BLINK` |
| `TelltaleColor` | `WHITE`, `GREEN`, `BLUE`, `YELLOW`, `RED`, `GRAY` |
| `TelltaleType` | 아래 분류 표 참조 (총 95개) |

`TelltaleType` 값(분류는 이해를 돕기 위한 것이며 enum 선언 순서와 다를 수 있음):

| 분류 | 값 |
|---|---|
| 브레이크·주행 안정 | `PARKING_BRAKE`, `ELECTRONIC_PARKING_BRAKE`, `ABS`, `ESC`, `ESC_OFF`, `ESC_SPORT`, `AUTO_HOLD`, `MDPS`, `RBS`, `DBC`, `CREEP_OFF` |
| 탑승자 안전 | `SEAT_BELT`, `AIRBAG`, `PASSENGER_AIRBAG_OFF`, `PASSENGER_AIRBAG_ON`, `CHILD_PRESENCE_DETECTION_OFF`, `EMERGENCY_CALL`, `DRIVER_ATTENTION` |
| 차체·개폐 | `DOOR_OPEN`, `TRUNK_OPEN`, `IMMOBILIZER` |
| 조명 | `AUTO_HIGH_BEAM`, `HEADLAMP_LOW`, `HEADLAMP_HIGH`, `POSITION_LAMP`, `FRONT_FOG_LAMP`, `REAR_FOG_LAMP`, `LED_HEADLAMP`, `AFS`, `TURN_SIGNAL_LEFT`, `TURN_SIGNAL_RIGHT` |
| 엔진·파워트레인 | `ENGINE`, `ENGINE_OIL_PRESSURE`, `ENGINE_TEMPERATURE`, `POWER_DOWN`, `PREHEAT`, `PARTICLE_FILTER`, `WATER_SEPARATOR`, `SCR`, `ISG`, `LPI`, `MASTER_WARNING`, `LOW_VOLT_BATTERY`, `TPMS`, `ICY_ROAD`, `OVER_SPEED` |
| 연료 | `LOW_FUEL`, `LOW_FUEL_FCEV` |
| 전동화(EV/HEV/PHEV/FCEV/EREV) | `EV_SYSTEM`, `LOW_EV_BATTERY`, `EV_CHARGING_CONNECTOR`, `EV_BATTERY_CONDITIONING`, `HEV_SYSTEM`, `HEV_MODE`, `FCEV_SYSTEM`, `H2_SENSOR`, `H2_LEAK`, `EREV_SYSTEM`, `EREV_MODE`, `PHEV_MODE`, `PHEV_MODE_HEV`, `PHEV_MODE_AUTO`, `PHEV_MODE_EV_PLUS`, `REV`, `XEV_READY`, `I_PEDAL`, `GREEN_ZONE` |
| 구동 방식 | `AWD_LOCK`, `FORCED_ALL_WHEEL_DRIVE`, `FOUR_WHEEL_DRIVE_MODE_2H`, `FOUR_WHEEL_DRIVE_MODE_4A`, `FOUR_WHEEL_DRIVE_MODE_4H`, `FOUR_WHEEL_DRIVE_MODE_4L` |
| 드라이브 모드 | `DRIVE_MODE`, `DRIVE_MODE_SPORT`, `DRIVE_MODE_ECO`, `DRIVE_MODE_COMFORT`, `DRIVE_MODE_TERRAIN`, `TOW_MODE`, `DRIFT_MODE`, `TRACK_MODE`, `UTILITY_MODE`, `LAUNCH_MODE`, `ACCEL_RESPONSE_DASH`, `ACCEL_RESPONSE_ACCEL`, `N_BATTERY_DRAG`, `N_BATTERY_SPRINT`, `N_BATTERY_ENDURANCE` |
| 운전자 보조(ADAS) | `FCA`, `LKA`, `LFA`, `LCA_LEFT`, `LCA_RIGHT`, `ELK`, `ICC` |

#### 1.5.7 오류 코드 / 예외

`ErrorCode`(`constant`)와 `VehicleException`(`callback.exception`)은 [1.2.6 예외와 오류 코드](#126-예외와-오류-코드)를 참조하세요.

---

### 1.6 퍼미션 전체 목록

`object PleosPermission`(`ai.pleos.playground.vehicle.constant`)에 정의된 상수 전체입니다. 매니페스트 이름은 `pleos.car.permission.<상수명>`입니다. 보호 수준은 API Reference 멤버 문서의 `Permission: … (protection level: …)` 표기를 따랐으며, 어떤 API에도 사용되지 않아 보호 수준이 표기되지 않은 상수는 "(표기 없음)"으로 표시했습니다.

| 퍼미션 (`android:name`) | 보호 수준 | 관련 도메인 / 용도 |
|---|---|---|
| `pleos.car.permission.CAR_CLIMATE` | normal | HVAC 조회·구독, SteeringWheel 열선 조회·구독, SideMirror 열선 register/unregister |
| `pleos.car.permission.CAR_DOORS` | normal | Door 조회·구독 |
| `pleos.car.permission.CAR_DRIVE` | normal | Brake, DrivingMode, Odometer 조회·구독, SteeringWheel 조향각 |
| `pleos.car.permission.CAR_ENERGY` | normal | EvBattery, Fuel 조회·구독 |
| `pleos.car.permission.CAR_INFO` | normal | CarInfo, Display |
| `pleos.car.permission.CAR_LIGHTS` | normal | Light 조회·구독(앰비언트 라이트 Switch/Brightness/Color 설정 포함), TurnSignal |
| `pleos.car.permission.CAR_MIRRORS` | normal | SideMirror 열선 capability·조회 |
| `pleos.car.permission.CAR_SAFETY` | normal | Safety 조회·구독, Tire |
| `pleos.car.permission.CAR_SEATS` | normal | Seat 조회·구독·capability |
| `pleos.car.permission.CAR_TOLL` | normal | Toll 조회·구독 |
| `pleos.car.permission.CAR_WINDOWS` | normal | Window, Wiper 조회·구독 |
| `pleos.car.permission.CONTROL_CAR_CLIMATE` | normal | HVAC 설정(`setHvac*`), `setHvacSteeringWheelHeat` |
| `pleos.car.permission.CONTROL_CAR_DOORS` | normal | `setDoorLock`, `setDoorPosition`, `setGloveBoxLock` |
| `pleos.car.permission.CONTROL_CAR_DRIVE` | (표기 없음) | `setRangeRemainingDistance`(Deprecated with error) |
| `pleos.car.permission.CONTROL_CAR_ENERGY` | normal | `setEvChargeCurrentDrawLimit`, `setEvChargePercentLimit`, `setEvChargePortOpen`, `setEvChargeSwitch` |
| `pleos.car.permission.CONTROL_CAR_INFO` | (표기 없음) | 상수만 정의(사용 API 없음) |
| `pleos.car.permission.CONTROL_CAR_LIGHTS` | normal | 외부등·실내등·독서등 스위치 설정, `setAmbientColorAutoByDriveModeEnabled` |
| `pleos.car.permission.CONTROL_CAR_MIRRORS` | normal | `setHvacSideMirrorHeat` |
| `pleos.car.permission.CONTROL_CAR_SAFETY` | (표기 없음) | 상수만 정의(사용 API 없음) |
| `pleos.car.permission.CONTROL_CAR_SEATS` | normal | `moveSeat*`, `setHvacSeatTemperature`, `setHvacSeatVentilation`, `selectSeatMemory` |
| `pleos.car.permission.CONTROL_CAR_TOLL` | normal | `setEtcsVoiceGuidanceVolume` |
| `pleos.car.permission.CONTROL_CAR_WINDOWS` | normal (`setSmartGlassState`만 signature\|privileged로 표기) | `setWindowPosition`, `setWindowLock`, `moveWindowBlind`, `setSmartGlassLevel`, `setSmartGlassZoneState`, `setSmartGlassState`, `setWiperSwitch` |
| `pleos.car.permission.PRIVATE_CAR_CLIMATE` | (표기 없음) | 상수만 정의 |
| `pleos.car.permission.PRIVATE_CAR_DOORS` | (표기 없음) | 상수만 정의 |
| `pleos.car.permission.PRIVATE_CAR_DRIVE` | (표기 없음) | 상수만 정의 |
| `pleos.car.permission.PRIVATE_CAR_ENERGY` | (표기 없음) | 상수만 정의 |
| `pleos.car.permission.PRIVATE_CAR_INFO` | (표기 없음) | 상수만 정의 |
| `pleos.car.permission.PRIVATE_CAR_LIGHTS` | (표기 없음) | 상수만 정의 |
| `pleos.car.permission.PRIVATE_CAR_MIRRORS` | (표기 없음) | 상수만 정의 |
| `pleos.car.permission.PRIVATE_CAR_SAFETY` | signature\|privileged | Safety Telltale(`checkTelltalesCapabilities`, `getTelltalesInfo`, `registerTelltales`, `unregisterTelltales`) |
| `pleos.car.permission.PRIVATE_CAR_SEATS` | (표기 없음) | 상수만 정의 |
| `pleos.car.permission.PRIVATE_CAR_TOLL` | (표기 없음) | 상수만 정의 |
| `pleos.car.permission.PRIVATE_CAR_WINDOWS` | (표기 없음) | 상수만 정의 |

> `getTelltalesInfo`는 `@RequiresPermission` 어노테이션 값이 `CAR_SAFETY`이지만 문서의 Permission 표기는 `PRIVATE_CAR_SAFETY (signature|privileged)`입니다. Telltale 기능을 쓰려면 두 퍼미션을 모두 고려하세요.

---

### 1.7 Kotlin 코드 예제

아래 예제는 가이드 문서와 API Reference의 예제를 그대로 옮긴 것입니다(주석 일부 한글화).

#### 1.7.1 초기화 / 도메인 객체 획득 / 해제

```kotlin
// Vehicle 인스턴스 생성 후 초기화
val vehicle = Vehicle(context)
vehicle.initialize()

// getXxx()로 도메인 객체 획득
val door = vehicle.getDoor()

// door 객체에서 Get / Set / Callback / Capability API 호출
// ...

// 사용 종료 후 리소스 해제 (비동기 콜백이 끝난 뒤 호출)
vehicle.release()
```

#### 1.7.2 Capability 확인

```kotlin
val door = vehicle.getDoor()
door.checkDoorPositionCapabilities(
  onComplete = { config, areaMap ->
    when (config.accessType) {
      AccessType.READ, AccessType.READ_WRITE -> {
        val bounds = areaMap[DoorArea(DoorValue.ROW_1_LEFT)]
        // bounds?.minValue / bounds?.maxValue 사용 후 getCurrentDoorPosition 호출
      }
      else -> {
        // NONE 또는 WRITE 전용 — UI 비활성화 또는 대체 흐름
      }
    }
  },
  onFailed = { e: Exception ->
    if (e is VehicleException) {
      // Capability 조회 실패: e.errorCode
    }
  }
)
```

#### 1.7.3 Get (값 조회)

```kotlin
val door = vehicle.getDoor()
door.getCurrentDoorPosition(
  area = DoorArea(DoorValue.ROW_1_LEFT),
  onComplete = { pos: Int? ->
    // do something
  },
  onFailed = { e: Exception ->
    // do something
  }
)

val display = vehicle.getDisplay()
display.getCurrentTemperatureDisplayUnit(
  onComplete = { unit: TemperatureUnit? ->
    // do something
  },
  onFailed = { e: Exception ->
    // do something
  }
)
```

#### 1.7.4 Set (값 설정)

```kotlin
val door = vehicle.getDoor()
door.setDoorPosition(
  area = DoorArea(DoorValue.ROW_1_LEFT),
  value = 1, // Integer type
  onFailed = { e: Exception ->
    // do something
  }
)
```

#### 1.7.5 Callback (리스너 등록/해제)

가이드 원문 예제입니다. v2.3.2 Reference 시그니처는 `registerDoorPosition(area: DoorArea, listener: DoorPositionListener)`이므로, 실제 코드에서는 `area`를 함께 전달해야 합니다(아래 두 번째 블록).

```kotlin
// 가이드 원문
// Create callback listener
val callbackListener = object : DoorPositionListener {
  override fun onPositionUpdated(pos: Int?) {
    // do something
  }
  override fun onFailed(e: Exception) {
    // do something
  }
}

val door = vehicle.getDoor()
// register callback
door.registerDoorPosition(
  doorPositionListener = callbackListener
)
// unregister callback
door.unregisterDoorPosition(
  doorPositionListener = callbackListener
)
```

```kotlin
// Reference 시그니처(area, listener) 기준
val area = DoorArea(DoorValue.ROW_1_LEFT)
door.registerDoorPosition(area, callbackListener)
// ...
door.unregisterDoorPosition(area, callbackListener)
```

#### 1.7.6 onFailed 오류 처리

```kotlin
door.getCurrentDoorPosition(
  area = DoorArea(DoorValue.ROW_1_LEFT),
  onComplete = { pos -> },
  onFailed = { e ->
    if (e is VehicleException) {
      when (e.errorCode) {
        ErrorCode.ERROR_NOT_SUPPORTED_PROPERTY,
        ErrorCode.ERROR_NOT_SUPPORTED_AREA -> { /* UI 비활성 */ }
        ErrorCode.ERROR_SECURITY_PERMISSION -> { /* 매니페스트 퍼미션 확인 */ }
        ErrorCode.ERROR_SERVICE_CONNECTION_FAILURE,
        ErrorCode.ERROR_INTERNAL_SERVICE -> { /* initialize·재시도 */ }
        else -> Log.e("Vehicle API 오류: ${e.errorCode} — ${e.message}")
      }
    } else {
      Log.e("Vehicle API 오류: ${e.message}")
    }
  }
)
```

#### 1.7.7 전체 흐름 예제 (API Reference `Vehicle` 클래스)

```kotlin
// Create Vehicle instance
val vehicle = Vehicle(context)

// Start Vehicle
vehicle.initialize()

val door = vehicle.getDoor()
door.setDoorPosition(
  area = DoorArea(DoorValue.ROW_1_LEFT),
  value = 1, // Integer type
  onFailed = { e: Exception ->
    // do something
  }
)

// End Vehicle
vehicle.release()
```

> Java에서 호출할 때는 각 람다가 `return Unit.INSTANCE;`를 반환해야 합니다(예: `door.getCurrentDoorPosition(new DoorArea(DoorValue.ROW_1_LEFT), pos -> { ...; return Unit.INSTANCE; }, e -> { ...; return Unit.INSTANCE; });`).
## 2. NaviHelper SDK

> 기준 버전: **NaviHelper SDK v3.0.5** · 아티팩트 `ai.pleos.playground:NaviHelper:3.0.5`
> 출처: NaviHelper 가이드 페이지(Introduction, Importing, Declaring permissions, Initialization, Event listeners, Destinations, Routes, Waypoints, Route requests with waypoints, Error handling)와 NaviHelper API Reference(Dokka)

### 2.1 개요

#### 2.1.1 목적

NaviHelper SDK는 **경로 안내 제어**와 **내비게이션 정보 조회** API를 제공합니다. 3rd party 앱은 NaviHelper SDK로 Pleos Connect의 **Navi Service**와 연동합니다.

- 3rd party 앱이 목적지·경유지 설정, 경로 변경, 정보 조회, Navigation 화면 표시를 요청하면 Navi Service가 처리합니다.
- 처리 결과와 주행 상태 변화는 **이벤트**(`NaviHelperEventListener` 콜백)로 전달됩니다.
- NaviHelper 요청은 Pleos Connect Navigation 앱에 전달됩니다. 실제로 오는 이벤트는 Navigation 앱의 구현과 현재 상태에 따라 다를 수 있습니다. API Reference에는 "NaviProvider SDK로 구현된 Navi 앱이 요청을 처리하고 응답하는 방식에 따라 전달되는 이벤트가 달라질 수 있다"고 적혀 있습니다.

#### 2.1.2 제공 기능

| 기능 | 주요 API |
|---|---|
| 목적지 설정·확인·취소 | `requestRoute()`, `getDestinationInfo()`, `cancelRoute()` |
| 경로 옵션 변경, 재탐색 | `changeRouteOption()`, `requestReRoute()` |
| 경유지 추가·조회·제거 | `addWaypoint()`, `getWaypointInfo()`, `removeWaypoint()` |
| 경유지를 포함한 경로 요청 | `requestRouteWithWaypoints()` |
| 경로 탐색·주행 상태 이벤트 수신 | `addListener()` / `NaviHelperEventListener` |
| 내비게이션 정보 조회 | `getBookmarkInfo()`, `getRecentDestinationInfo()`, `getCurrentLocationInfo()`, `getRouteStateInfo()`, `getTBTInfo()`, `getChargerOperatorInfo()`, `requestTrip()` |
| Navigation 화면 요청 | `requestNavigationScreen()` |

#### 2.1.3 패키지 구성

| 패키지 | 내용 |
|---|---|
| `ai.pleos.playground.navi.helper` | 진입점 클래스 `NaviHelper` |
| `ai.pleos.playground.navi.helper.listener` | 콜백 인터페이스 `NaviHelperEventListener` ("Navi 애플리케이션에서 받는 콜백 데이터의 인터페이스 정의") |
| `ai.pleos.playground.navi.data` | 요청/응답 데이터 클래스(모두 `@Serializable`), sealed class `NavigationScreenRequest` |
| `ai.pleos.playground.navi.constants` | enum 상수(`RouteOption`, `WaypointIndex`, `NaviErrorCode` 등 15종, 모두 `@Serializable`) |

플랫폼 표기는 모두 `androidJvm`입니다.

#### 2.1.4 진입점과 사용 흐름

진입점은 `class NaviHelper(context: Context)`입니다. Android `Context`로 `NaviHelper` 인스턴스를 만든 뒤 API를 직접 호출하고 이벤트 리스너 콜백을 등록합니다.

일반적인 사용 순서:

1. `NaviHelper` 객체를 생성합니다.
2. `initialize()`로 SDK 연결을 초기화합니다.
3. `addListener()`로 이벤트 리스너를 등록합니다.
4. 경로 요청 API나 정보 조회 API를 호출합니다.
5. 콜백에서 결과와 상태 변화를 처리합니다.
6. `removeListener()`와 `release()`로 리소스를 정리합니다.

> **중요**: 요청·조회 API는 모두 반환형이 `Unit`이며 **값을 직접 돌려주지 않습니다.** 결과는 대응하는 이벤트 콜백으로 받습니다. 예를 들어 `requestRoute()`가 정상적으로 호출되었다고 해서 경로 탐색이 끝난 것은 아니므로, 최종 결과는 콜백으로 확인해야 합니다.

#### 2.1.5 개발 환경 (Emulator)

NaviHelper SDK를 쓰려면 Android Studio에 Pleos Connect Emulator를 설치하고 AVD를 설정해야 합니다. 준비 항목은 다음과 같습니다.

- 소프트웨어 개발 환경 요구사항 확인
- Pleos Connect Emulator 설치
- AVD 생성 및 실행
- Pleos Connect Virtual Console 설정
- 마이크 접근 설정
- Cold boot 설정

#### 2.1.6 Gradle 설정

**Maven 저장소 등록** — `settings.gradle.kts`(또는 `settings.gradle`)에 추가합니다.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
        }
    }
}
```

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven {
            url "https://nexus-playground.pleos.ai/repository/maven-releases/"
        }
    }
}
```

**SDK 의존성 추가** — 앱 모듈 빌드 파일에 추가한 뒤 Gradle 프로젝트를 동기화합니다. SDK 버전은 프로젝트의 배포 기준에 맞춰 지정합니다.

```kotlin
// build.gradle.kts
dependencies {
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
}
```

```groovy
// build.gradle
dependencies {
    implementation "ai.pleos.playground:NaviHelper:3.0.5"
}
```

#### 2.1.7 퍼미션 선언

NaviHelper API는 기능에 따라 Android 퍼미션이 필요합니다. 사용하는 API에 맞는 퍼미션을 `AndroidManifest.xml`에 선언합니다. API Reference에 표시된 세 퍼미션의 Protection level은 모두 **Normal**입니다. API별로 정확히 어떤 퍼미션이 필요한지는 API Reference의 `@RequiresPermission`에 나와 있습니다.

| Permission | 대상 API |
|---|---|
| `pleos.car.permission.NAVI_ROUTE` | 정보 조회 `get*Info()` 계열(`getBookmarkInfo`, `getChargerOperatorInfo`, `getCurrentLocationInfo`, `getDestinationInfo`, `getRecentDestinationInfo`, `getRouteStateInfo`, `getTBTInfo`, `getWaypointInfo`), `requestTrip()` |
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | `requestRoute()`, `requestRouteWithWaypoints()`, `cancelRoute()`, `requestReRoute()`, `addWaypoint()`, `removeWaypoint()`, `changeRouteOption()` |
| `pleos.car.permission.NAVI_CUSTOM_MAP` | `requestNavigationScreen()` |

퍼미션이 필요 없는 API: `NaviHelper(context)` 생성자, `initialize()`, `release()`, `addListener()`, `removeListener()`.

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="pleos.car.permission.NAVI_ROUTE" />
    <uses-permission android:name="pleos.car.permission.NAVI_ROUTE_SEARCH" />
    <uses-permission android:name="pleos.car.permission.NAVI_CUSTOM_MAP" />

    <application>
        <!-- ... -->
    </application>
</manifest>
```

#### 2.1.8 초기화와 해제

앱이 시작되거나 생성될 때 `NaviHelper` 객체를 만들고 `initialize()`를 호출해야 NaviHelper API를 쓸 수 있습니다. 초기화가 끝나야 다른 API가 정상 동작합니다.

| API | 설명 |
|---|---|
| `constructor(context: Context)` | `NaviHelper` 인스턴스를 생성합니다. |
| `fun initialize(onConnected: () -> Unit, onFailed: (Throwable) -> Unit)` | 연결 결과 콜백과 함께 초기화합니다. 최초 서비스 연결에 성공하면 `onConnected`가, 초기 연결 재시도가 모두 실패하면 `onFailed`가 호출됩니다. **이 오버로드를 권장합니다.** |
| `fun initialize()` | 인자 없이 초기화합니다. 앱이 시작되거나 생성될 때 호출해야 합니다. |
| `fun release()` | SDK를 해제합니다. 앱이 종료되거나 소멸될 때 호출해야 합니다. |

- SDK가 더 이상 필요 없으면 **등록한 리스너를 먼저 제거한 다음** `release()`를 호출합니다.
- `release()` 후에 SDK를 다시 쓰려면 객체를 새로 만들고 초기화를 다시 수행합니다.

```kotlin
import ai.pleos.playground.navi.helper.NaviHelper

val naviHelper = NaviHelper(applicationContext)

naviHelper.initialize(
    onConnected = {
        // 최초 서비스 연결이 성공했습니다. 리스너를 등록하고 API를 호출합니다.
    },
    onFailed = { throwable ->
        // 초기 연결 재시도가 모두 실패했습니다.
    }
)

// 인자 없는 초기화도 가능합니다.
// naviHelper.initialize()
```

Java에서는 Kotlin 람다가 `Unit`을 반환해야 하므로 `return Unit.INSTANCE;`를 씁니다.

```java
import ai.pleos.playground.navi.helper.NaviHelper;
import kotlin.Unit;

NaviHelper naviHelper = new NaviHelper(getApplicationContext());
naviHelper.initialize(
    () -> {
        // 최초 서비스 연결이 성공했습니다. 리스너를 등록하고 API를 호출합니다.
        return Unit.INSTANCE;
    },
    throwable -> {
        // 초기 연결 재시도가 모두 실패했습니다.
        return Unit.INSTANCE;
    }
);
```

해제:

```kotlin
override fun onDestroy() {
    naviHelper.removeListener(naviEventListener)
    naviHelper.release()
    super.onDestroy()
}
```

#### 2.1.9 `NaviHelper` 전체 API 요약

| API | 퍼미션 | 설명 | 관련 콜백 |
|---|---|---|---|
| `NaviHelper(context: Context)` | — | 생성자 | — |
| `initialize()` / `initialize(onConnected, onFailed)` | — | SDK 초기화 | `onConnected` / `onFailed` |
| `release()` | — | SDK 해제 | — |
| `addListener(listener: NaviHelperEventListener)` | — | 리스너 등록 | — |
| `removeListener(listener: NaviHelperEventListener)` | — | 리스너 제거 | — |
| `requestRoute(routeInfo: RouteInfo)` | `NAVI_ROUTE_SEARCH` | 경로(목적지) 요청 | `onRouteStarted` |
| `requestRouteWithWaypoints(request: RouteWithWaypointsRequest)` | `NAVI_ROUTE_SEARCH` | 경유지 포함 경로 요청 | `onRouteStarted` |
| `cancelRoute()` | `NAVI_ROUTE_SEARCH` | 경로 취소 | `onRouteCancelled` |
| `requestReRoute()` | `NAVI_ROUTE_SEARCH` | 재탐색 요청 | `onReRouteEnded` |
| `changeRouteOption(routeOption: RouteOption)` | `NAVI_ROUTE_SEARCH` | 경로 옵션 변경 | `onRouteOptionChanged` |
| `addWaypoint(requestWaypointInfo: RequestWaypointInfo)` | `NAVI_ROUTE_SEARCH` | 경유지 추가 | `onWayPointInfo` |
| `removeWaypoint(waypointIndex: WaypointIndex)` | `NAVI_ROUTE_SEARCH` | 경유지 제거 | `onWaypointChanged` |
| `getDestinationInfo()` | `NAVI_ROUTE` | 목적지 정보 조회 | `onDestinationInfo` |
| `getWaypointInfo()` | `NAVI_ROUTE` | 경유지 정보 조회 | `onWayPointInfo` |
| `getBookmarkInfo()` | `NAVI_ROUTE` | 즐겨찾기 조회 | `onBookmarkInfo` |
| `getRecentDestinationInfo()` | `NAVI_ROUTE` | 최근 목적지 조회 | `onRecentDestinationInfo` |
| `getCurrentLocationInfo()` | `NAVI_ROUTE` | 현재 위치 정보 조회 | `onCurrentLocationInfo` |
| `getRouteStateInfo()` | `NAVI_ROUTE` | 경로 상태 정보 조회 | `onRouteStateInfo` |
| `getTBTInfo()` | `NAVI_ROUTE` | TBT(Turn-By-Turn) 정보 조회 | `onTBTInfo` |
| `getChargerOperatorInfo()` | `NAVI_ROUTE` | 충전 사업자 정보 조회 | `onChargerOperatorInfo` |
| `requestTrip(tripRequest: TripRequest)` | `NAVI_ROUTE` | 트립(예상 주행) 정보 요청 | `onTripInfo` |
| `requestNavigationScreen(requestScreen: NavigationScreenRequest)` | `NAVI_CUSTOM_MAP` | Navigation 화면 전환 요청 | (명시된 콜백 없음) |

퍼미션 열의 값은 `pleos.car.permission.` 접두어를 생략한 것입니다.

---

### 2.2 기능별 상세

#### 2.2.1 목적지 설정·확인·취소

`requestRoute()`로 목적지를 설정하고, `getDestinationInfo()`로 확인하고, `cancelRoute()`로 취소합니다. 목적지를 바꿀 때도 `requestRoute()`를 다시 호출합니다. 경로 옵션은 `RouteInfo.routeOption`으로 지정하며 기본값은 `RouteOption.RECOMMENDED`입니다.

| API | 설명 | 파라미터/반환 |
|---|---|---|
| `requestRoute(routeInfo: RouteInfo)` | 목적지로 가는 경로를 요청합니다. 등록된 리스너로 `onRouteStarted` 같은 관련 이벤트가 올 수 있습니다. 퍼미션 `NAVI_ROUTE_SEARCH`. | `routeInfo`: 경로(목적지) 정보 `RouteInfo` / 반환 `Unit` |
| `getDestinationInfo()` | 현재 목적지 정보를 조회합니다. 결과는 `onDestinationInfo(DestinationInfo)`로 옵니다. 퍼미션 `NAVI_ROUTE`. | 없음 / 반환 `Unit` |
| `cancelRoute()` | 경로를 취소합니다. `onRouteCancelled` 같은 이벤트가 올 수 있습니다. 퍼미션 `NAVI_ROUTE_SEARCH`. | 없음 / 반환 `Unit` |

경로 탐색 과정은 다음 콜백으로 확인합니다.

| 콜백 | 설명 |
|---|---|
| `onRouteStarted(routeStartInfo)` | Navigation 앱이 경로 탐색을 시작하면 호출됩니다. |
| `onRoutingCompleted()` | 경로 탐색이 끝나면 호출됩니다. |
| `onDestinationChanged()` | 목적지가 설정되거나 제거되면 호출됩니다. |
| `onError(errorCode)` | 경로 요청을 처리하다 오류가 나면 호출됩니다. |

```kotlin
// 목적지 설정
naviHelper.requestRoute(
    RouteInfo(
        latitude = 37.48544722,
        longitude = 127.03636666,
        displayLongitude = null,
        displayLatitude = null,
        poiName = "42dot",
        poiId = "36khp37hh8gqcm5nqhe5",
        poiSubId = "0",
        address = "서울시 강남구 남부순환로 2621"
    )
)

// 목적지 확인
naviHelper.getDestinationInfo()

// onDestinationInfo callback으로 데이터 수신
override fun onDestinationInfo(destinationInfo: DestinationInfo) {
    Log.d(logTag, "onDestinationInfo $destinationInfo")
}

// 목적지 취소
naviHelper.cancelRoute()
```

> **참고 (Java 예제 인자 순서)**: 가이드의 Java 예제는 `new RouteInfo(127.03636666, 37.48544722, null, null, "42dot", "36khp37hh8gqcm5nqhe5", "서울시 강남구 남부순환로 2621", "0", RouteOption.RECOMMENDED)`처럼 경도를 먼저 넘깁니다. 그런데 API Reference의 생성자 시그니처는 `RouteInfo(latitude, longitude, displayLatitude, displayLongitude, poiName, poiId, address, poiSubId, routeOption, poiProvider, isMobileRequest)`로 **위도가 먼저**입니다. Java에서 위치 인자로 호출할 때는 생성자 시그니처의 순서를 확인하세요. Kotlin에서는 명명 인자를 쓰면 안전합니다.

#### 2.2.2 경로 옵션 변경과 재탐색

목적지를 처음 설정할 때 `RouteInfo.routeOption`으로 옵션을 지정하거나, 안내 중에 `changeRouteOption()`으로 바꿀 수 있습니다. 현재 위치와 목적지를 기준으로 경로를 다시 찾으려면 `requestReRoute()`를 호출합니다.

> **정보**: 경로 옵션 변경과 재탐색은 **활성 경로가 있는 상태**에서 요청합니다. 처리 결과와 실제로 오는 콜백은 Navigation 앱의 상태에 따라 다를 수 있습니다.

| API | 설명 | 파라미터/반환 |
|---|---|---|
| `changeRouteOption(routeOption: RouteOption)` | 경로 옵션을 바꿉니다. `onRouteOptionChanged` 같은 이벤트가 올 수 있습니다. 퍼미션 `NAVI_ROUTE_SEARCH`. | `routeOption`: 경로 옵션 `RouteOption` / 반환 `Unit` |
| `requestReRoute()` | 재탐색을 요청합니다. `onReRouteEnded` 같은 이벤트가 올 수 있습니다. 퍼미션 `NAVI_ROUTE_SEARCH`. | 없음 / 반환 `Unit` |

`RouteOption` 값(가이드 기준 설명):

| 값 | 설명 |
|---|---|
| `RECOMMENDED` | 추천 경로 (`RouteInfo`의 기본값) |
| `SHORT_DISTANCE` | 최단 거리 경로 |
| `SHORT_TIME` | 최단 시간 경로 |
| `FREE` | 무료 도로 우선 경로 |
| `HIGHWAY` | 고속도로 우선 경로 |
| `COMFORTABLE` | 편안한 경로 |
| `SCHOOL_ZONE` | 스쿨존 고려 경로 |

```kotlin
// 목적지 설정
naviHelper.requestRoute(
    RouteInfo(
        latitude = 37.48544722,
        longitude = 127.03636666,
        displayLongitude = null,
        displayLatitude = null,
        poiName = "42dot",
        poiId = "36khp37hh8gqcm5nqhe5",
        poiSubId = "0",
        address = "서울시 강남구 남부순환로 2621"
    )
)

// 경로옵션: 고속우선으로 변경
naviHelper.changeRouteOption(RouteOption.HIGHWAY)

// onRouteOptionChanged callback으로 변경 데이터 수신
override fun onRouteOptionChanged(routeOption: RouteOption) {
    Log.d(logTag, "onRouteOptionChanged $routeOption")
}

// 목적지 취소
naviHelper.cancelRoute()
```

```kotlin
// 목적지 설정
naviHelper.requestRoute(
    RouteInfo(
        latitude = 37.48544722,
        longitude = 127.03636666,
        displayLongitude = null,
        displayLatitude = null,
        poiName = "42dot",
        poiId = "36khp37hh8gqcm5nqhe5",
        poiSubId = "0",
        address = "서울시 강남구 남부순환로 2621"
    )
)

// 경로 재탐색 요청
naviHelper.requestReRoute()

// 목적지 취소
naviHelper.cancelRoute()
```

#### 2.2.3 경유지 추가·조회·제거

경유지는 **최대 두 개**까지 지정할 수 있고, `WaypointIndex.FIRST`와 `WaypointIndex.SECOND`로 구분합니다. `addWaypoint()`로 추가하고 `removeWaypoint()`로 삭제합니다. 모든 경유지를 지우려면 `removeWaypoint(WaypointIndex.ALL)`을 씁니다.

| API | 설명 | 파라미터/반환 |
|---|---|---|
| `addWaypoint(requestWaypointInfo: RequestWaypointInfo)` | 경유지를 추가합니다. `onWayPointInfo` 같은 이벤트가 올 수 있습니다. 퍼미션 `NAVI_ROUTE_SEARCH`. | `requestWaypointInfo`: 경유지 정보 `RequestWaypointInfo`(`waypointIndex`로 위치 지정) / 반환 `Unit` |
| `getWaypointInfo()` | 현재 경유지 목록을 요청합니다. 결과는 `onWayPointInfo(List<WaypointInfo>)`로 옵니다. 퍼미션 `NAVI_ROUTE`. | 없음 / 반환 `Unit` |
| `removeWaypoint(waypointIndex: WaypointIndex)` | 경유지를 제거합니다. `onWaypointChanged` 같은 이벤트가 올 수 있습니다. 퍼미션 `NAVI_ROUTE_SEARCH`. | `waypointIndex`: `FIRST` / `SECOND` / `ALL` / 반환 `Unit` |

관련 콜백:

| 콜백 | 언제 호출되는지 |
|---|---|
| `onWayPointInfo(waypointInfoList: List<WaypointInfo>)` | 경유지 정보가 조회되거나 바뀌었을 때 |
| `onWaypointChanged(waypointChangedInfo: WaypointChangedInfo)` | 경유지가 추가되거나 제거되었을 때. `cmdType`(`ADD`/`REMOVE`)과 변경 후 경로 정보(`routeStartInfo`)가 함께 옵니다. API Reference 원문은 "Called when arriving at or removing the waypoint"입니다. |
| `onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo)` | 경유지에 도착했을 때 |

```kotlin
// 목적지 설정
naviHelper.requestRoute(
    RouteInfo(
        latitude = 37.48544722,
        longitude = 127.03636666,
        displayLongitude = null,
        displayLatitude = null,
        poiName = "42dot",
        poiId = "36khp37hh8gqcm5nqhe5",
        poiSubId = "0",
        address = "서울시 강남구 남부순환로 2621"
    )
)

// 경유지 추가
naviHelper.addWaypoint(
    RequestWaypointInfo(
        latitude = 37.41275833,
        longitude = 127.09481944,
        waypointIndex = WaypointIndex.FIRST,
        poiId = "36khp2hdj2cfcm5nqhe3",
        poiName = "소프트웨어드림센터",
        poiSubId = "0",
        address = "경기도 성남시 수정구 창업로40번길 6"
    )
)

// onWayPointInfo callback으로 데이터 수신
override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
    Log.d(logTag, "onWayPointInfo $waypointInfoList")
}

// 경유지 삭제
naviHelper.removeWaypoint(WaypointIndex.FIRST)

// 목적지 취소
naviHelper.cancelRoute()
```

```kotlin
// 현재 경유지 목록 조회 → onWayPointInfo()로 전달
naviHelper.getWaypointInfo()

// 모든 경유지 제거
naviHelper.removeWaypoint(WaypointIndex.ALL)
```

> **참고**: 가이드의 Kotlin 예제는 콜백 이름을 `onWaypointInfo(waypointInfo: List<WaypointInfo>)`로 적었지만, `NaviHelperEventListener`에 정의된 이름은 `onWayPointInfo(waypointInfoList: List<WaypointInfo>)`(대문자 P)입니다. 위 예제는 인터페이스 정의에 맞췄습니다. 같은 가이드의 Java 예제는 `onWayPointInfo`를 씁니다. 또 Java의 `RequestWaypointInfo` 예제는 경도를 먼저 넘기지만, 생성자 시그니처는 `RequestWaypointInfo(latitude, longitude, poiName, waypointIndex, poiId, address, poiSubId, poiProvider)`로 위도가 먼저입니다.

#### 2.2.4 경유지를 포함한 경로 요청

`requestRouteWithWaypoints()`는 최종 목적지와 하나 이상의 경유지를 포함한 경로를 한 번에 요청합니다. 목적지는 `RouteInfo`, 경유지는 `RequestWaypointInfo`로 만들고 두 값을 `RouteWithWaypointsRequest`에 넣어 전달합니다. 처리 결과는 `onRouteStarted`, `onRoutingCompleted`, `onError` 같은 이벤트로 확인합니다.

| API | 설명 | 파라미터/반환 |
|---|---|---|
| `requestRouteWithWaypoints(request: RouteWithWaypointsRequest)` | 경유지를 포함한 경로를 요청합니다. `onRouteStarted` 같은 이벤트가 올 수 있습니다. 퍼미션 `NAVI_ROUTE_SEARCH`. | `request`: 목적지(`destination: RouteInfo`)와 경유지 목록(`waypoints: List<RequestWaypointInfo>`)을 담은 요청 / 반환 `Unit` |

```kotlin
naviHelper.requestRouteWithWaypoints(
    RouteWithWaypointsRequest(
        destination = RouteInfo(
            latitude = 37.48544722,
            longitude = 127.03636666,
            poiName = "42dot",
            poiId = "36khp37hh8gqcm5nqhe5",
            address = "서울시 강남구 남부순환로 2621",
            poiSubId = "0"
        ),
        waypoints = listOf(
            RequestWaypointInfo(
                latitude = 37.41275833,
                longitude = 127.09481944,
                poiName = "판교 현대백화점",
                waypointIndex = WaypointIndex.FIRST,
                poiId = "36khp2hdj2cfcm5nqhe3",
                address = "경기도 성남시 수정구 창업로 0번길 6",
                poiSubId = "0"
            )
        )
    )
)
```

```java
import java.util.Arrays;

naviHelper.requestRouteWithWaypoints(new RouteWithWaypointsRequest(
        new RouteInfo(
                37.48544722,
                127.03636666,
                null,
                null,
                "42dot",
                "36khp37hh8gqcm5nqhe5",
                "서울시 강남구 남부순환로 2621",
                "0"
        ),
        Arrays.asList(new RequestWaypointInfo(
                37.41275833,
                127.09481944,
                "판교 현대백화점",
                WaypointIndex.FIRST,
                "36khp2hdj2cfcm5nqhe3",
                "경기도 성남시 수정구 창업로 0번길 6",
                "0"
        ))
));
```

#### 2.2.5 내비게이션 정보 조회

정보 조회 API는 모두 `pleos.car.permission.NAVI_ROUTE` 퍼미션이 필요하고 반환형이 `Unit`입니다. 결과는 대응하는 콜백으로 옵니다.

| API | 설명 | 파라미터/반환 (결과 콜백 → 데이터) |
|---|---|---|
| `getDestinationInfo()` | 목적지 정보 조회. 목적지 좌표, 이름, 주소, **남은 거리(m)·남은 시간(초)·통행료(원)**, POI 정보 | 없음 / `onDestinationInfo(destinationInfo: DestinationInfo)` |
| `getWaypointInfo()` | 경유지 정보 조회. 경유지별 남은 거리·시간 포함 | 없음 / `onWayPointInfo(waypointInfoList: List<WaypointInfo>)` |
| `getBookmarkInfo()` | 즐겨찾기(집/회사/EV 충전소/기본) 조회 | 없음 / `onBookmarkInfo(bookmarkInfoList: List<BookmarkInfo>)` |
| `getRecentDestinationInfo()` | 최근 목적지 조회 | 없음 / `onRecentDestinationInfo(recentDestinationInfoList: List<RecentDestinationInfo>)` |
| `getCurrentLocationInfo()` | 현재 위치 정보 조회(위·경도, 진행 방향, 주소, 경로 ID, 현재 경로 인덱스) | 없음 / `onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo)` |
| `getRouteStateInfo()` | 경로 상태 정보 조회(약관 동의 여부, `NaviMode`, 경로 활성 여부, 경로 옵션, 목적지·경유지 도착 시 예상 잔여 배터리/연료, 교통 상황) | 없음 / `onRouteStateInfo(routeStateInfo: RouteStateInfo)` |
| `getTBTInfo()` | TBT(Turn-By-Turn) 안내 정보 조회(회전 유형, 남은 거리, 안내 문구) | 없음 / `onTBTInfo(tbtInfoList: List<TBTInfo>)` |
| `getChargerOperatorInfo()` | EV 충전 사업자 목록 조회 | 없음 / `onChargerOperatorInfo(chargerOperatorList: List<ChargerOperator>)` |
| `requestTrip(tripRequest: TripRequest)` | 지정한 지점까지의 트립 정보 요청. **총 거리, ETA(초), 도착 시 예상 SOC(%), 예상 에너지 소비량(Wh)**, 경로 요약(교통 상황·경로 지점) | `tripRequest`: `TripRequest`(위·경도, POI ID/이름/Sub ID) / `onTripInfo(tripInfo: TripInfo)` |

다음 정보는 조회 API 없이 리스너 등록 후 **푸시 이벤트**로만 받습니다.

| 콜백 | 내용 |
|---|---|
| `onDrivingInfo(drivingInfo: DrivingInfo)` | 주행 정보: 목적지와 각 경유지의 `DrivingState`(남은 시간·거리, 도착 시 잔여 배터리/연료, 충전소 여부, 충전 계획 등) |
| `onNavigationStatus(status: NavigationStatus)` | 내비게이션 상태(`INACTIVE`/`INITIALIZING`/`FREE_DRIVE`/`ROUTE_DRIVE`). 주행 상태가 바뀔 때마다 옵니다. |
| `onCurrentRoadSpeedLimit(speedLimit: Int)` | 현재 도로의 제한 속도(km). 리스너를 등록한 뒤 **매초** 옵니다. |
| `onCameraAlert(cameraAlertList: List<CameraAlert>)` | 단속 카메라 알림. 과속 카메라가 있을 때 옵니다. |
| `onCautionPointInfo(cautionPointList: List<CautionPoint>)` | 주의 구간 정보(과속방지턱, 스쿨존, 안개 지역 등) |

**ETA·남은 거리 관련 필드 정리**

| 원하는 정보 | 얻는 방법 |
|---|---|
| 목적지까지 남은 거리/시간 | `getDestinationInfo()` → `DestinationInfo.remainDistance`(m), `remainTime`(초) |
| 경유지까지 남은 거리/시간 | `getWaypointInfo()` → `WaypointInfo.remainDistance`(m), `remainTime`(초) |
| 목적지·경유지별 남은 시간/거리(주행 중 갱신) | `onDrivingInfo` → `DrivingState.duration`(초), `distance`(m) |
| 경로 탐색 결과의 총 소요 시간/거리/통행료 | `onRouteStarted` → `RouteStartInfo.duration`(초), `distance`(m), `tollFare` |
| 특정 지점까지 ETA, 도착 시 SOC | `requestTrip()` → `TripInfo.eta`(초), `estimatedSocAtDest`(%) |
| 다음 회전까지 거리 | `getTBTInfo()` → `TBTInfo.distance`(m) |
| 현재 도로 제한 속도 | `onCurrentRoadSpeedLimit(speedLimit)` |

> 현재 주행 중인 도로의 **이름**을 직접 알려 주는 API는 없습니다. 도로 이름은 교통 정체 정보(`TrafficCongestionInfo.roadName`, 정체 구간의 대표 도로명)에만 있습니다.

#### 2.2.6 Navigation 화면 요청

`requestNavigationScreen()`으로 Navigation 앱의 특정 화면을 띄우도록 요청합니다. 요청할 화면은 sealed class `NavigationScreenRequest`의 하위 타입으로 지정합니다.

| API | 설명 | 파라미터/반환 |
|---|---|---|
| `requestNavigationScreen(requestScreen: NavigationScreenRequest)` | Navigation 화면을 요청합니다. 퍼미션 `pleos.car.permission.NAVI_CUSTOM_MAP`. API Reference에 명시된 결과 콜백은 없습니다. | `requestScreen`: `NavigationScreenRequest` 하위 타입 / 반환 `Unit` |

| `NavigationScreenRequest` 하위 타입 | 종류 | 설명 |
|---|---|---|
| `Foreground` | `data object` | Navigation을 포그라운드로 표시 |
| `FavoriteMain` | `data object` | 즐겨찾기 메인 화면 |
| `AddHomeFavorite` | `data object` | 집 즐겨찾기 추가 화면 |
| `AddWorkFavorite` | `data object` | 회사 즐겨찾기 추가 화면 |
| `NearbyChargingStations` | `data object` | 주변 충전소 검색 화면 |
| `Settings` | `data object` | 내비게이션 설정 화면 |
| `MoveToCurrentLocationOnMap` | `data object` | 지도에서 현재 위치 화면 |
| `OverviewRoute(info: RouteOverviewShowInfo)` | `data class` | 전체 경로 보기. `info`는 경로 전체 보기 표시 설정(`durationMs`, 기본값 5000ms) |
| `SearchPlace(keyword: String)` | `data class` | 장소 검색 화면. `keyword`는 검색어 |
| `POIDetailPopup(poiId: String, poiProvider: String = "")` | `data class` | POI 상세 팝업. `poiId`는 POI ID, `poiProvider`는 POI 콘텐츠 제공자 이름(예: 42dot, Google) |
| `RecentDestinations` | `data object` | 최근 목적지 화면 |
| `RouteOptionList` | `data object` | 경로 옵션 목록 화면 |
| `EditRoute` | `data object` | 경로 편집 화면 |

각 하위 타입에는 `@Serializable`과 `@SerialName`이 붙어 있고, 값은 클래스 이름과 같습니다(예: `@SerialName("OverviewRoute")`).

사용 예(API 시그니처 기준 예시이며, 가이드 페이지에는 이 API의 예제가 없습니다):

```kotlin
naviHelper.requestNavigationScreen(NavigationScreenRequest.Foreground)
naviHelper.requestNavigationScreen(NavigationScreenRequest.SearchPlace(keyword = "충전소"))
naviHelper.requestNavigationScreen(
    NavigationScreenRequest.OverviewRoute(RouteOverviewShowInfo(durationMs = 5000))
)
```

---

### 2.3 이벤트 리스너 (`NaviHelperEventListener`)

`interface NaviHelperEventListener`는 Navi 애플리케이션에서 받는 콜백을 정의한 인터페이스입니다. 모든 콜백이 `open fun`(기본 구현 있음)이므로 **필요한 콜백만 재정의**하면 됩니다. SDK를 초기화한 뒤 `addListener()`로 등록하고, 더 이상 필요 없거나 SDK를 해제하기 전에는 **등록할 때 쓴 것과 같은 인스턴스**를 `removeListener()`로 제거합니다.

> Navigation 앱의 구현 시나리오에 따라 실제로 오는 이벤트가 다를 수 있습니다.

#### 2.3.1 콜백 목록

**경로 탐색**

| 콜백 | 언제 호출되는지 | 파라미터 |
|---|---|---|
| `onRouteStarted(routeStartInfo: RouteStartInfo)` | 내비게이션 경로 탐색이 시작될 때 (`requestRoute`, `requestRouteWithWaypoints`의 관련 이벤트) | `RouteStartInfo`: 소요 시간, 거리, 통행료, 이름, 도착 시 잔여 배터리/연료 |
| `onRoutingCompleted()` | 내비게이션 경로 탐색이 끝났을 때 | 없음 |
| `onRouteCancelled()` | 현재 경로가 취소되었을 때 (`cancelRoute` 관련) | 없음 |
| `onReRouteEnded()` | 재탐색이 끝났을 때 (`requestReRoute` 관련) | 없음 |
| `onRouteOptionChanged(routeOption: RouteOption)` | 경로 유형(옵션)이 바뀌었을 때 (`changeRouteOption` 관련) | 바뀐 `RouteOption` |

**목적지와 경유지**

| 콜백 | 언제 호출되는지 | 파라미터 |
|---|---|---|
| `onDestinationChanged()` | 목적지가 설정되거나 제거되었을 때 (API Reference 원문: "Called when arriving at or removing the destination.") | 없음 |
| `onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo)` | 목적지에 도착했을 때 | 총 주행 시간·거리, 이름, 주소 |
| `onWaypointChanged(waypointChangedInfo: WaypointChangedInfo)` | 경유지가 추가되거나 제거되었을 때 (API Reference 원문: "Called when arriving at or removing the waypoint.") | 인덱스, 명령 유형(`ADD`/`REMOVE`), 갱신된 `RouteStartInfo` |
| `onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo)` | 경유지에 도착했을 때 | 총 주행 시간·거리, 인덱스, 이름, 주소 |

**정보 조회 / 상태 알림**

| 콜백 | 언제 호출되는지 | 파라미터 |
|---|---|---|
| `onBookmarkInfo(bookmarkInfoList: List<BookmarkInfo>)` | 즐겨찾기 정보가 바뀌었을 때 / `getBookmarkInfo()`의 응답 | `List<BookmarkInfo>` |
| `onBookMarkChanged()` | 즐겨찾기가 추가되거나 제거되었을 때 (API Reference 원문: "Called when arriving at or removing the bookmark.") | 없음 |
| `onRecentDestinationInfo(recentDestinationInfoList: List<RecentDestinationInfo>)` | 최근 목적지 정보가 바뀌었을 때 / `getRecentDestinationInfo()`의 응답 | `List<RecentDestinationInfo>` |
| `onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo)` | 현재 위치 정보가 바뀌었을 때 / `getCurrentLocationInfo()`의 응답 | `CurrentLocationInfo` |
| `onDestinationInfo(destinationInfo: DestinationInfo)` | 목적지 정보가 바뀌었을 때 / `getDestinationInfo()`의 응답 | `DestinationInfo` |
| `onWayPointInfo(waypointInfoList: List<WaypointInfo>)` | 경유지 정보가 바뀌었을 때 / `getWaypointInfo()`, `addWaypoint()`의 응답 | `List<WaypointInfo>` |
| `onRouteStateInfo(routeStateInfo: RouteStateInfo)` | 경로 상태 정보가 바뀌었을 때 / `getRouteStateInfo()`의 응답 | `RouteStateInfo` |
| `onTBTInfo(tbtInfoList: List<TBTInfo>)` | TBT 정보가 알려질 때 / `getTBTInfo()`의 응답 | `List<TBTInfo>` |
| `onChargerOperatorInfo(chargerOperatorList: List<ChargerOperator>)` | 충전 사업자 정보가 알려질 때 / `getChargerOperatorInfo()`의 응답 | `List<ChargerOperator>` |
| `onTripInfo(tripInfo: TripInfo)` | 트립 정보가 알려질 때 / `requestTrip()`의 응답 | `TripInfo` |
| `onNavigationStatus(status: NavigationStatus)` | 내비게이션 상태가 알려질 때. **주행 상태가 바뀔 때마다** 옵니다. | `NavigationStatus` |
| `onDrivingInfo(drivingInfo: DrivingInfo)` | 주행 정보가 알려질 때 | `DrivingInfo` |
| `onCurrentRoadSpeedLimit(speedLimit: Int)` | 도로 제한 속도가 알려질 때. **리스너 등록 후 매초** 옵니다. | `speedLimit`: 제한 속도(km) |
| `onCameraAlert(cameraAlertList: List<CameraAlert>)` | 카메라 알림이 있을 때. 과속 카메라가 있으면 옵니다. | `List<CameraAlert>` |
| `onCautionPointInfo(cautionPointList: List<CautionPoint>)` | 주의 구간 정보가 알려질 때 | `List<CautionPoint>` |

**오류**

| 콜백 | 언제 호출되는지 | 파라미터 |
|---|---|---|
| `onError(errorCode: NaviErrorCode)` | 내비게이션 오류가 났을 때(경로 요청 처리 오류 등) | 발생한 `NaviErrorCode` |

#### 2.3.2 리스너 등록·제거 예제

```kotlin
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

private val naviEventListener = object : NaviHelperEventListener {
    override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
        // 경로 탐색 시작을 처리합니다.
    }

    override fun onRoutingCompleted() {
        // 경로 탐색 완료를 처리합니다.
    }

    override fun onError(errorCode: NaviErrorCode) {
        // 내비게이션 오류를 처리합니다.
    }
}

naviHelper.addListener(naviEventListener)

// 더 이상 필요하지 않을 때 (등록에 사용한 동일 인스턴스)
naviHelper.removeListener(naviEventListener)
```

```java
private final NaviHelperEventListener naviEventListener =
        new NaviHelperEventListener() {
    @Override
    public void onRouteStarted(RouteStartInfo routeStartInfo) {
        // 경로 탐색 시작을 처리합니다.
    }

    @Override
    public void onRoutingCompleted() {
        // 경로 탐색 완료를 처리합니다.
    }

    @Override
    public void onError(NaviErrorCode errorCode) {
        // 내비게이션 오류를 처리합니다.
    }
};

naviHelper.addListener(naviEventListener);
```

---

### 2.4 데이터 모델 (`ai.pleos.playground.navi.data`)

모든 데이터 클래스에는 `@Serializable`이 붙어 있습니다. 위도(`latitude`)는 −90°(남)~90°(북), 경도(`longitude`)는 −180°(서)~180°(동) 범위의 도(°) 단위입니다. 공통 필드인 `displayLatitude`/`displayLongitude`는 지도에 표시할 좌표, `poiId`는 지도상 POI의 고유 식별자, `poiSubId`는 같은 POI를 구분하는 순번, `poiProvider`는 POI 콘텐츠 제공자 이름(예: 42dot, Google)입니다.

#### 2.4.1 요청용 데이터

**`RouteInfo`** — 경로(목적지) 요청 정보. `requestRoute()`와 `RouteWithWaypointsRequest.destination`에 씁니다.

| 필드 | 타입 (기본값) | 설명 |
|---|---|---|
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `displayLatitude` | `Double?` (`null`) | 지도 표시용 위도 |
| `displayLongitude` | `Double?` (`null`) | 지도 표시용 경도 |
| `poiName` | `String` | POI 이름 |
| `poiId` | `String` | POI ID |
| `address` | `String` | 전체 주소 |
| `poiSubId` | `String` | POI Sub ID(동일 POI 구분 순번) |
| `routeOption` | `RouteOption` (`RouteOption.RECOMMENDED`) | 사용할 경로 옵션(기본: 추천 경로) |
| `poiProvider` | `String` (`""`) | POI 콘텐츠 제공자 이름 |
| `isMobileRequest` | `Boolean` (`false`) | 모바일에서 받은 요청인지 여부 |

**`RequestWaypointInfo`** — 경유지 요청 정보. `addWaypoint()`와 `RouteWithWaypointsRequest.waypoints`에 씁니다.

| 필드 | 타입 (기본값) | 설명 |
|---|---|---|
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `poiName` | `String?` | 경유지 POI 이름 |
| `waypointIndex` | `WaypointIndex` | 경로상 경유지 순서(`FIRST`/`SECOND`) |
| `poiId` | `String` | POI ID |
| `address` | `String` | 전체 주소 |
| `poiSubId` | `String` | POI Sub ID |
| `poiProvider` | `String` (`""`) | POI 콘텐츠 제공자 이름 |

**`RouteWithWaypointsRequest`** — 경유지 포함 경로 요청.

| 필드 | 타입 | 설명 |
|---|---|---|
| `destination` | `RouteInfo` | 요청 경로의 최종 목적지 |
| `waypoints` | `List<RequestWaypointInfo>` | 요청 경로의 경유지 목록 |

**`TripRequest`** — 트립 정보 요청(`requestTrip()`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `poiId` | `String` | POI ID |
| `poiName` | `String` | POI 이름 |
| `poiSubId` | `String?` | POI Sub ID |

**`RouteOverviewShowInfo`** — 전체 경로 보기 표시 설정(`NavigationScreenRequest.OverviewRoute`).

| 필드 | 타입 (기본값) | 설명 |
|---|---|---|
| `durationMs` | `Int` (`5000`) | 경로 전체 보기를 표시하는 시간(ms) |

**`NavigationScreenRequest`** — 화면 전환 요청 sealed class. 하위 타입은 [2.2.6](#226-navigation-화면-요청)을 참고하세요.

#### 2.4.2 경로·목적지·경유지 응답 데이터

**`RouteStartInfo`** — 경로 시작 정보(`onRouteStarted`, `WaypointChangedInfo.routeStartInfo`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `duration` | `Int` | 경로의 예상 소요 시간(초) |
| `distance` | `Int` | 경로 총 거리(m) |
| `tollFare` | `Int` | 경로 총 통행료 |
| `name` | `String` | 목적지 또는 출발지 이름 |
| `remainDestinationChargingValue` | `Int` | 목적지 도착 시 예상 잔여 배터리 또는 연료량 |

**`DestinationInfo`** — 목적지 정보(`onDestinationInfo`).

| 필드 | 타입 (기본값) | 설명 |
|---|---|---|
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `displayLatitude` | `Double?` (`null`) | 지도 표시용 위도 |
| `displayLongitude` | `Double?` (`null`) | 지도 표시용 경도 |
| `name` | `String` | 목적지 이름 |
| `address` | `String` | 목적지 주소 |
| `remainDistance` | `Int` | 남은 거리(m) |
| `remainTime` | `Int` | 예상 남은 주행 시간(초) |
| `tollFare` | `Int` | 목적지까지 경로의 총 통행료(원) |
| `poiId` | `String` | POI 고유 식별자 |
| `poiSubId` | `String?` | POI Sub ID |
| `poiProvider` | `String` (`""`) | POI 콘텐츠 제공자 이름 |

> API Reference는 `DestinationInfo.remainDistance`/`remainTime`을 "the waypoint"까지의 값이라고 적고 있습니다(`WaypointInfo`와 같은 문구). 클래스의 의미로 보면 목적지까지의 값으로 이해하면 됩니다.

**`WaypointInfo`** — 경유지 정보(`onWayPointInfo`).

| 필드 | 타입 (기본값) | 설명 |
|---|---|---|
| `name` | `String` | 경유지 이름 |
| `address` | `String` | 경유지 주소 |
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `displayLatitude` | `Double?` (`null`) | 지도 표시용 위도 |
| `displayLongitude` | `Double?` (`null`) | 지도 표시용 경도 |
| `remainDistance` | `Int` | 경유지까지 남은 거리(m) |
| `remainTime` | `Int` | 경유지까지 예상 남은 주행 시간(초) |
| `poiId` | `String` | POI 고유 식별자 |
| `poiSubId` | `String?` | POI Sub ID |
| `poiProvider` | `String` (`""`) | POI 콘텐츠 제공자 이름 |

**`WaypointChangedInfo`** — 경유지 변경 정보(`onWaypointChanged`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `index` | `WaypointIndex` | 경로상 경유지 인덱스 |
| `cmdType` | `WaypointCmdType` | 경유지 변경 명령 유형(`ADD`/`REMOVE`) |
| `routeStartInfo` | `RouteStartInfo` | 경유지 변경 후 갱신된 경로 정보 |

**`DestinationArrivedInfo`** — 목적지 도착 정보(`onDestinationArrived`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `duration` | `Int` | 목적지 도착까지 총 주행 시간(초) |
| `distance` | `Int` | 목적지까지 총 주행 거리(m) |
| `name` | `String` | 목적지 이름 |
| `address` | `String` | 목적지 주소 |

**`WaypointArrivedInfo`** — 경유지 도착 정보(`onWayPointArrived`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `duration` | `Int` | 경유지 도착까지 총 주행 시간(초) |
| `distance` | `Int` | 경유지까지 총 주행 거리(m) |
| `index` | `WaypointIndex` | 경유지 인덱스 (API Reference에 설명 없음) |
| `name` | `String` | 경유지 이름 |
| `address` | `String` | 경유지 주소 |

#### 2.4.3 위치·즐겨찾기·최근 목적지

**`CurrentLocationInfo`** — 현재 위치 정보(`onCurrentLocationInfo`).

| 필드 | 타입 (기본값) | 설명 |
|---|---|---|
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `heading` | `Double` (`0.0`) | 진행 방향 |
| `address` | `String` | 현재 위치 주소 |
| `routeId` | `String` | 따라가고 있는 경로의 고유 식별자 |
| `currentRouteIndex` | `Int?` | 경로 시퀀스상 현재 인덱스(nullable) |

**`BookmarkInfo`** — 특정 위치의 즐겨찾기 정보(`onBookmarkInfo`).

| 필드 | 타입 (기본값) | 설명 |
|---|---|---|
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `displayLatitude` | `Double?` (`null`) | 지도 표시용 위도 |
| `displayLongitude` | `Double?` (`null`) | 지도 표시용 경도 |
| `name` | `String` | 즐겨찾기한 장소 이름 |
| `poiId` | `String` | POI ID |
| `poiSubId` | `String?` | POI Sub ID |
| `address` | `String` | 전체 주소 |
| `category` | `BookmarkType` | 즐겨찾기 분류(`HOME`/`OFFICE`/`EV_CHARGING_STATION`/`DEFAULT`) |
| `poiProvider` | `String` (`""`) | POI 콘텐츠 제공자 이름 |

**`RecentDestinationInfo`** — 최근 방문 목적지 정보(`onRecentDestinationInfo`).

| 필드 | 타입 (기본값) | 설명 |
|---|---|---|
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `displayLatitude` | `Double?` (`null`) | 지도 표시용 위도 |
| `displayLongitude` | `Double?` (`null`) | 지도 표시용 경도 |
| `name` | `String` | 최근 목적지 이름 |
| `address` | `String` | 전체 주소 |
| `poiId` | `String` | POI ID |
| `poiSubId` | `String?` | POI Sub ID |
| `poiProvider` | `String` (`""`) | POI 콘텐츠 제공자 이름 |

#### 2.4.4 경로 상태·교통 정보

**`RouteStateInfo`** — 현재 경로 상태(`onRouteStateInfo`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `termsAgreement` | `Boolean` | 사용자가 내비게이션 약관에 동의했는지 여부 |
| `naviMode` | `NaviMode` | 현재 내비게이션 모드 |
| `isRouting` | `Boolean` | 경로가 현재 활성 상태인지 여부 |
| `routeOption` | `RouteOption` | 선택된 경로 옵션 |
| `remainDestinationChargingInt` | `Int` | 목적지 도착 시 예상 잔여 배터리 또는 연료량 |
| `remainWaypointChargingInt` | `List<Int>` | 각 경유지 도착 시 예상 잔여 배터리 또는 연료량 목록 |
| `trafficStatus` | `TrafficStatus?` | 경로상 현재 교통 상황(nullable) |

**`TrafficStatus`** — 정체와 교통 이벤트를 포함한 전체 교통 상황.

| 필드 | 타입 | 설명 |
|---|---|---|
| `summary` | `TrafficCongestionSummary` | 전체 정체 요약 |
| `congestionItemList` | `List<TrafficCongestionInfo>` | 구간별 정체 상세 목록 |
| `trafficEventList` | `List<TrafficEventInfo>` | 사고·도로 통제 등 교통 이벤트 목록 |

**`TrafficCongestionSummary`** — 정체 요약.

| 필드 | 타입 | 설명 |
|---|---|---|
| `totalCongestionDist` | `Double` | 전체 정체 구간 총 거리(m) |
| `totalCongestionTime` | `Long` | 전체 정체 총 시간(초) |
| `totalCongestionAvrSpeed` | `Double` | 전체 정체 구간 평균 속도(km/h) |

**`TrafficCongestionInfo`** — 정체 구간 정보.

| 필드 | 타입 | 설명 |
|---|---|---|
| `offset` | `Double` | 현재 위치에서 정체 구간 시작점까지 거리(m) |
| `distance` | `Double` | 정체 구간 길이(m) |
| `trafficCongestionTime` | `Long` | 정체 지속 시간(초) |
| `trafficCongestionType` | `TrafficCongestionType` | 정체 유형 |
| `avrSpeed` | `Double` | 정체 구간 평균 속도(km/h) |
| `roadName` | `String` | 정체가 발생한 주요 도로명 |
| `startCrossName` | `String` | 정체 구간 시작 지점 근처 교차로 이름 |
| `endCrossName` | `String` | 정체 구간 끝 지점 근처 교차로 이름 |

**`TrafficEventInfo`** — 교통 이벤트 정보.

| 필드 | 타입 | 설명 |
|---|---|---|
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `eventType` | `TrafficEventType` | 교통 이벤트 유형(사고, 공사, 통제 등) |
| `startTime` | `String` | 이벤트 시작 시각(문서 표기: `yyyy-MM-dd hh:mm`, 예 `"2023-11-05T22:08:28"`) |
| `endTime` | `String` | 이벤트 종료 시각(형식 동일) |
| `description` | `String` | 이벤트 간단 설명 |

**`TrafficInfo`** — 경로상 정체 정보(`TripSummary.trafficConditions`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `congestionType` | `TrafficCongestionType` | 정체 유형 |
| `startRatio` | `Int` | 경로상 시작 비율(0–100) |
| `endRatio` | `Int` | 경로상 끝 비율(0–100) |

**`TBTInfo`** — Turn-By-Turn 안내 정보(`onTBTInfo`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `type` | `TurnType` | 회전 안내 유형(좌회전, 우회전 등) |
| `distance` | `Double` | 다음 회전까지 거리(m) |
| `description` | `String` | 회전 안내 문구 |

#### 2.4.5 주행·트립·EV 충전 정보

**`DrivingInfo`** — 목적지와 경유지를 포함한 주행 정보(`onDrivingInfo`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `destination` | `DrivingState` | 주행 경로의 최종 목적지 상태 |
| `waypoints` | `List<DrivingState>` | 주행 경로의 경유지별 상태 목록 |

**`DrivingState`** — 경로상 한 지점(경유지 또는 목적지)의 주행 상태.

| 필드 | 타입 (기본값) | 설명 |
|---|---|---|
| `name` | `String` | 장소 이름 |
| `duration` | `Int` | 현재 위치에서 이 지점까지 예상 주행 시간(초) |
| `distance` | `Int` | 출발지에서 이 지점까지 거리(m) |
| `remainDestinationChargingValue` | `Int` | 이 지점 도착 시 예상 잔여 배터리 또는 연료량 |
| `isChargingStation` | `Boolean` (`false`) | 이 지점이 충전소인지 여부 |
| `isRecommendedChargingStation` | `Boolean` (`false`) | 내비 앱이 추천한 충전소인지 여부. `isChargingStation`이 `true`일 때만 의미가 있고, 아니면 무시합니다. |
| `maxChargingPower` | `Int` (`0`) | 충전소 최대 충전 출력(kW) |
| `targetSoc` | `Int` (`0`) | 이 지점 도착 시 필요한 최소 배터리 잔량(%) |
| `chargerCount` | `Int` (`0`) | 충전소의 전체 충전기 수 |
| `chargingPlan` | `ChargingPlan?` (`null`) | 이 지점에서 충전이 계획된 경우의 충전 계획. 계획이 없으면 `null` |

**`ChargingPlan`** — 경로 지점의 충전 계획.

| 필드 | 타입 (기본값) | 설명 |
|---|---|---|
| `duration` | `Int` (`0`) | 이 위치에서 필요한 예상 충전 시간(초) |
| `startSoc` | `Int` (`0`) | 충전 시작 시 예상 배터리 잔량(%) |
| `endSoc` | `Int` (`0`) | 충전 완료 후 목표 배터리 잔량(%) |

**`TripInfo`** — 트립 정보(`onTripInfo`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `distance` | `Int` | 목적지까지 총 주행 거리(m) |
| `eta` | `Int` | 도착 예정 시간(ETA, 초) |
| `estimatedSocAtDest` | `Int` | 경로 주행 후 예상 잔여 배터리(%) |
| `energyConsumption` | `Double` | 경로 주행 시 예상 배터리 소비량(Wh) |
| `summary` | `TripSummary` | 주행 경로 정보 |

**`TripSummary`** — 주행 경로 요약.

| 필드 | 타입 | 설명 |
|---|---|---|
| `trafficConditions` | `List<TrafficInfo>` | 교통 정체 정보 목록 |
| `routePoints` | `List<RoutePointInfo>` | 경유지와 목적지 정보 목록 |

**`RoutePointInfo`** — 경로 지점 정보.

| 필드 | 타입 | 설명 |
|---|---|---|
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `poiName` | `String` | POI 이름 |
| `poiId` | `String` | POI 고유 식별자 |
| `ratioOnRoute` | `Double` | 경로상 비율(0.0–100.0) |
| `poiType` | `RoutePointPoiType` | 경로 지점의 POI 유형 |
| `pointType` | `RoutePointType` | 경로 지점 유형(출발/경유/도착) |
| `settingType` | `RoutePointSettingType` | 경로 지점 설정 방식(자동/수동) |

**`ChargerOperator`** — EV 충전 사업자(`onChargerOperatorInfo`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `String` | 충전 사업자 고유 식별자 |
| `name` | `String` | 충전 사업자(서비스 제공자) 이름 |

#### 2.4.6 안전 운전 알림

**`CameraAlert`** — 단속 카메라 알림(`onCameraAlert`).

| 필드 | 타입 (기본값) | 설명 |
|---|---|---|
| `id` | `String` (`""`) | 카메라 ID |
| `type` | `CameraType` | 카메라 유형 |
| `status` | `Boolean` | 카메라 알림 활성(`true`)/비활성(`false`) |
| `speedLimit` | `Int` | 카메라 단속 제한 속도(km/h) |
| `remainDistance` | `Int` | 현재 위치에서 카메라까지 남은 거리(m) |
| `sectionAvrSpeed` | `Int` | 구간 단속 구간 내 평균 속도(km/h). 구간 단속 카메라 유형에서만 사용 |
| `isOverSpeed` | `Boolean` (`false`) | 과속 여부 |
| `isOverSectionAvrSpeed` | `Boolean` (`false`) | 구간 평균 속도 초과 여부 |

**`CautionPoint`** — 주의 구간 알림(`onCautionPointInfo`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `String` | 주의 구간 ID |
| `type` | `CautionPointType` | 주의 구간 유형(스쿨존, 안개 지역 등) |
| `remainDistance` | `Int` | 주의 구간까지 남은 거리(m) |

---

### 2.5 상수 / enum (`ai.pleos.playground.navi.constants`)

모든 enum에는 `@Serializable`이 붙어 있고 표준 멤버 `entries`, `valueOf(value: String)`, `values()`를 제공합니다. API Reference에는 각 상수의 숫자 값이 나와 있지 않습니다. `TurnType`만 `val value: Long` 프로퍼티가 있으며, 상수별 값은 문서에 없습니다.

#### 2.5.1 `RouteOption` — 경로 옵션

| 상수 | 설명 |
|---|---|
| `RECOMMENDED` | 추천 경로 |
| `SHORT_DISTANCE` | 최단 거리 |
| `SHORT_TIME` | 최단 시간 |
| `FREE` | 무료 도로 우선 |
| `HIGHWAY` | 고속도로 우선 |
| `COMFORTABLE` | 편안한 경로 |
| `SCHOOL_ZONE` | 스쿨존 고려 경로 |

#### 2.5.2 `WaypointIndex` — 경유지 인덱스

| 상수 | 설명 |
|---|---|
| `FIRST` | 첫 번째 경유지 |
| `SECOND` | 두 번째 경유지 |
| `ALL` | 전체(모든 경유지 제거 시 `removeWaypoint(WaypointIndex.ALL)`) |

#### 2.5.3 `WaypointCmdType` — 경유지 변경 명령

| 상수 | 설명 |
|---|---|
| `ADD` | 추가 |
| `REMOVE` | 제거 |

#### 2.5.4 `NaviErrorCode` — 오류 코드

| 상수 | 설명 |
|---|---|
| `CAUSE_LOW_BATTERY` | 배터리 부족 |
| `CAUSE_SEARCH_ROUTE_OVER_DISTANCE` | 경로 탐색 가능 거리 초과 |
| `CAUSE_SEARCH_ROUTE` | 경로 탐색 서버 오류 |
| `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE` | 현재 위치와 목적지 사이 거리가 너무 짧음 |
| `CAUSE_SEARCH_TRIP` | 트립 서버 오류 (API Reference에만 있고 가이드 표에는 없음) |

#### 2.5.5 `NavigationStatus` — 내비게이션 상태

| 상수 | 설명 |
|---|---|
| `INACTIVE` | 내비게이션이 동작하지 않거나 완전히 종료된 상태 |
| `INITIALIZING` | 내비게이션 서비스나 지도 UI를 준비하는 중. 설정 초기화, 지도 렌더링, GPS 센서 준비를 포함합니다. |
| `FREE_DRIVE` | 경로 안내 없이 사용자 위치를 따라 지도만 보여 주는 일반 주행 상태 |
| `ROUTE_DRIVE` | 목적지가 설정되어 경로 안내가 활성화된 상태(실시간 내비게이션 동작 중) |

#### 2.5.6 `NaviMode` — 내비게이션 모드

| 상수 | 설명 |
|---|---|
| `NOT_WORKING` | Navi 초기화 중이거나 지도 다운로드 중 등 |
| `SAFE_GUIDING` | 안전 운전 안내 |
| `ROUTE_GUIDING` | 경로 안내 |
| `SIMULATION_GUIDING` | 모의 주행 안내 |

#### 2.5.7 `BookmarkType` — 즐겨찾기 유형

| 상수 | 설명 |
|---|---|
| `HOME` | 집 |
| `OFFICE` | 회사 |
| `EV_CHARGING_STATION` | EV 충전소 |
| `DEFAULT` | 기본 |

#### 2.5.8 `CameraType` — 단속 카메라 유형

| 상수 | 설명 |
|---|---|
| `SPEED_CAMERA_FIX` | 고정식 과속 카메라 |
| `SPEED_CAMERA_MOV` | 이동식 과속 카메라 |
| `SPEED_CAMERA_SECTION_START` | 구간 단속 시작 카메라 |
| `SECTION_SPEED_ALERT` | 구간 속도 알림 |
| `SPEED_CAMERA_SECTION_END` | 구간 단속 종료 카메라 |
| `SPEED_CAMERA_TRAFFIC_LIGHT` | 신호·과속 카메라 |
| `SPEED_CAMERA_REAR` | 후면 과속 단속 카메라 |
| `VARIABLE_SPEED_CAMERA` | 가변 제한 속도 단속 카메라 |
| `SPEED_CAMERA_SCHOOL_ZONE` | 스쿨존 과속 단속 카메라 |
| `SPEED_CAMERA_SCHOOL_ZONE_TRAFFIC_LIGHT` | 스쿨존 신호·과속 단속 카메라 |

> `CameraAlert.sectionAvrSpeed` 설명에는 `CameraType.SPEED_CAMERA_SECTION`만 해당한다고 적혀 있지만, enum에는 그 이름의 상수가 없고 `SPEED_CAMERA_SECTION_START`/`SPEED_CAMERA_SECTION_END`/`SECTION_SPEED_ALERT`가 있습니다.

#### 2.5.9 `CautionPointType` — 주의 구간 유형

| 상수 | 설명 |
|---|---|
| `SPEED_BUMP` | 과속방지턱 |
| `SCHOOL_ZONE` | 어린이 보호구역 |
| `ACCIDENT_PRONE_AREA` | 사고 다발 지역 |
| `RAILROAD_CROSSING` | 철도 건널목 |
| `FOG_AREA` | 안개 지역 |
| `FALLING_ROCK_AREA` | 낙석 주의 구간 |
| `DOWNHILL_CAUTION_AREA` | 급경사(내리막) 주의 구간 |
| `WILDLIFE_CROSSING` | 야생동물 주의 구간 |
| `NARROW_ROAD` | 도로 폭 좁아짐 |
| `HIGHWAY_CONSTRUCTION` | 고속도로 공사 |
| `TRAFFIC_INFO_COLLECTION` | 교통 정보 수집 |
| `NO_PARKING_ZONE` | 주정차 금지 주의 |
| `TRAFFIC_LIGHT_VIOLATION_ENFORCEMENT` | 신호 위반 단속 주의 |
| `BUS_LANE_VIOLATION_ENFORCEMENT` | 버스전용차로 위반 단속 주의 |
| `CUT_IN_VIOLATION_ENFORCEMENT` | 끼어들기 위반 단속 주의 |
| `BLOCK_INTERSECTION_ENFORCEMENT` | 교차로 꼬리물기 단속 주의 |
| `LANE_CHANGE_CAMERA_ALERT` | 차로 변경 위반 카메라 주의 |
| `SLIPPERY_ROAD_ALERT` | 미끄러운 도로 감지 주의 |
| `ROAD_DAMAGE_ALERT` | 도로 파손 감지 주의 |
| `WRONG_WAY_DRIVING_ALERT` | 역주행 경고 |
| `BUS_LANE_UNAVAILABLE_NOTICE` | 버스전용차로 진입 제한 알림 |
| `BUS_LANE_AVAILABLE_NOTICE` | 버스전용차로 진입 허용 알림 |
| `SIGN_MALFUNCION` | 표지 시스템 오작동 (상수 철자는 원문 그대로 `MALFUNCION`) |

#### 2.5.10 `TrafficCongestionType` — 정체 유형

| 상수 | 설명 |
|---|---|
| `UNKNOWN` | 알 수 없음(수집된 교통 데이터가 없거나 SafeGuiding 상태) |
| `FAST` | 원활 |
| `MODERATE` | 서행 |
| `SLOW` | 지체 |
| `HEAVY` | 정체 |

#### 2.5.11 `TrafficEventType` — 교통 이벤트 유형

| 상수 | 설명 |
|---|---|
| `ETC` | 기타 |
| `ACCIDENT` | 사고 |
| `ROAD_DAMAGE` | 도로 파손 |
| `REPAIRING` | 도로 공사 |
| `EVENT` | 행사 |
| `DISASTER` | 재난 |

#### 2.5.12 `RoutePointType` / `RoutePointPoiType` / `RoutePointSettingType`

| enum | 상수 | 설명 |
|---|---|---|
| `RoutePointType` | `ORIGIN` | 출발지 |
| | `WAYPOINT` | 경유지 |
| | `DESTINATION` | 목적지 |
| `RoutePointPoiType` | `NORMAL` | 일반 위치 또는 POI 검색을 하지 않은 경우 |
| | `EV_CHARGING` | EV 충전소 |
| `RoutePointSettingType` | `AUTO` | 자동 |
| | `MANUAL` | 사용자 지정 |

#### 2.5.13 `TurnType` — TBT 회전 유형

`TurnType`에는 `val value: Long` 프로퍼티가 있습니다(상수별 값은 문서에 없음).

| 상수 | 설명 |
|---|---|
| `TURN_UNKNOWN` | 알 수 없음 |
| `TURN_STRAIGHT` | 직진 |
| `TURN_RIGHT` | 우회전 |
| `TURN_LEFT` | 좌회전 |
| `TURN_KEEP_R` | 오른쪽 방향 |
| `TURN_KEEP_L` | 왼쪽 방향 |
| `TURN_SHARP_R` | 급우회전 |
| `TURN_SHARP_L` | 급좌회전 |
| `TURN_UTURN` | 유턴 |
| `TURN_ENTER_IC_R` / `TURN_ENTER_IC_L` / `TURN_ENTER_IC_C` | 고속도로 진입(오른쪽/왼쪽/정면) |
| `TURN_EXIT_IC_R` / `TURN_EXIT_IC_L` / `TURN_EXIT_IC_C` | 고속도로 진출(오른쪽/왼쪽/정면) |
| `TURN_ENTER_UNDER_R` / `TURN_ENTER_UNDER_L` / `TURN_ENTER_UNDER_C` | 지하차도 진입(오른쪽/왼쪽/정면) |
| `TURN_AVOID_UNDER_R` / `TURN_AVOID_UNDER_L` / `TURN_AVOID_UNDER_C` | 지하차도 옆길(오른쪽/왼쪽/정면) — 원문 "Exit underpass" |
| `TURN_ENTER_OVER_R` / `TURN_ENTER_OVER_L` / `TURN_ENTER_OVER_C` | 고가도로 진입(오른쪽/왼쪽/정면) |
| `TURN_AVOID_OVER_R` / `TURN_AVOID_OVER_L` / `TURN_AVOID_OVER_C` | 고가도로 옆길(오른쪽/왼쪽/정면) — 원문 "Exit overpass" |
| `TURN_ENTER_ROUNDABOUT_02` | 회전교차로 진입(2시 방향) |
| `TURN_ENTER_ROUNDABOUT_03` | 회전교차로 진입(3시 방향) |
| `TURN_ENTER_ROUNDABOUT_04` | 회전교차로 진입(4시 방향) |
| `TURN_ENTER_ROUNDABOUT_06` | 회전교차로 진입(6시 방향) |
| `TURN_ENTER_ROUNDABOUT_08` | 회전교차로 진입(8시 방향) |
| `TURN_ENTER_ROUNDABOUT_09` | 회전교차로 진입(9시 방향) |
| `TURN_ENTER_ROUNDABOUT_10` | 회전교차로 진입(10시 방향) |
| `TURN_ENTER_ROUNDABOUT_12` | 회전교차로 진입(12시 방향) |
| `TURN_EXIT_ROUNDABOUT` | 회전교차로 진출 |
| `TURN_WAY` | 경유지 |
| `TURN_ARRIVE` | 도착 안내 |
| `TURN_TUNNEL` | 터널 |
| `TURN_DEVIDE_MERGE` | 분기·합류 도로 |
| `TURN_TOLL` | 톨게이트 |
| `TURN_ENTER_SERVICEAREA` | 휴게소 진입 |
| `TURN_RESTAREA` | 졸음쉼터 |

---

### 2.6 오류 처리

NaviHelper에서 **초기 서비스 연결 실패**와 **Navigation 앱의 요청 처리 오류**는 서로 다른 경로로 전달됩니다.

| 오류 종류 | 전달 경로 |
|---|---|
| 초기 서비스 연결 실패 | `initialize(onConnected, onFailed)`의 `onFailed: (Throwable) -> Unit`. 초기 연결 재시도가 모두 실패하면 호출됩니다. |
| Navigation 앱 처리 오류 | `NaviHelperEventListener.onError(errorCode: NaviErrorCode)` |

#### 2.6.1 초기화 오류

초기 연결 결과가 필요하면 `initialize()`에 콜백을 넘깁니다.

```kotlin
naviHelper.initialize(
    onConnected = {
        // 리스너를 등록하고 API를 호출합니다.
    },
    onFailed = { throwable ->
        // 연결 실패 원인을 기록하거나 사용자에게 안내합니다.
    }
)
```

#### 2.6.2 내비게이션 오류

Navigation 앱에서 난 오류는 `onError()`로 전달됩니다.

```kotlin
override fun onError(errorCode: NaviErrorCode) {
    when (errorCode) {
        NaviErrorCode.CAUSE_LOW_BATTERY -> {
            // 배터리 부족 상태를 안내합니다.
        }
        NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE -> {
            // 탐색 가능 거리를 초과했음을 안내합니다.
        }
        NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE -> {
            // 목적지가 현재 위치와 너무 가까움을 안내합니다.
        }
        NaviErrorCode.CAUSE_SEARCH_ROUTE -> {
            // 경로 탐색 서버 오류를 처리합니다.
        }
        NaviErrorCode.CAUSE_SEARCH_TRIP -> {
            // 트립 서버 오류를 처리합니다. (API Reference에 정의된 코드)
        }
    }
}
```

> 가이드 원문 예제에는 `CAUSE_SEARCH_TRIP` 분기가 없습니다. API Reference에는 이 코드가 정의되어 있으므로 위 예제에 추가했습니다. `when`이 enum의 모든 값을 다루도록 하거나 `else` 분기를 두세요.

| 오류 코드 | 설명 |
|---|---|
| `CAUSE_LOW_BATTERY` | 배터리가 부족합니다. |
| `CAUSE_SEARCH_ROUTE_OVER_DISTANCE` | 경로 탐색 가능 거리를 초과했습니다. |
| `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE` | 현재 위치와 목적지 사이의 거리가 너무 짧습니다. |
| `CAUSE_SEARCH_ROUTE` | 경로 탐색 서버에서 오류가 발생했습니다. |
| `CAUSE_SEARCH_TRIP` | 트립 서버에서 오류가 발생했습니다(API Reference: "Error: trip server side"). |

#### 2.6.3 오류가 났을 때 확인할 항목

- SDK 연결이 끝난 뒤(`onConnected` 이후)에 API를 호출했는가
- 사용하는 API에 필요한 퍼미션을 선언했는가(2.1.7 참고)
- 좌표, POI ID, 경유지 인덱스가 올바른가
- 현재 Navigation 앱 상태에서 요청을 처리할 수 있는가(예: 경로 옵션 변경·재탐색은 활성 경로가 있어야 함)

오류 코드의 최신 목록은 NaviHelper API Reference에서 확인합니다.

---

### 2.7 종합 예제 (Kotlin)

아래는 가이드 페이지의 예제를 하나의 흐름으로 합친 것입니다. 초기화 → 리스너 등록 → 목적지·경유지 설정 → 옵션 변경 → 정보 조회 → 해제 순서입니다.

```kotlin
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

class NaviActivity : AppCompatActivity() {

    private val logTag = "NaviActivity"
    private lateinit var naviHelper: NaviHelper

    private val naviEventListener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            // 경로 탐색 시작
        }

        override fun onRoutingCompleted() {
            // 경로 탐색 완료
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            Log.d(logTag, "onDestinationInfo $destinationInfo")
        }

        override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
            Log.d(logTag, "onWayPointInfo $waypointInfoList")
        }

        override fun onRouteOptionChanged(routeOption: RouteOption) {
            Log.d(logTag, "onRouteOptionChanged $routeOption")
        }

        override fun onError(errorCode: NaviErrorCode) {
            // 오류 처리 (2.6 참고)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        naviHelper = NaviHelper(applicationContext)
        naviHelper.initialize(
            onConnected = {
                naviHelper.addListener(naviEventListener)

                // 목적지 설정
                naviHelper.requestRoute(
                    RouteInfo(
                        latitude = 37.48544722,
                        longitude = 127.03636666,
                        displayLongitude = null,
                        displayLatitude = null,
                        poiName = "42dot",
                        poiId = "36khp37hh8gqcm5nqhe5",
                        poiSubId = "0",
                        address = "서울시 강남구 남부순환로 2621"
                    )
                )

                // 경유지 추가
                naviHelper.addWaypoint(
                    RequestWaypointInfo(
                        latitude = 37.41275833,
                        longitude = 127.09481944,
                        waypointIndex = WaypointIndex.FIRST,
                        poiId = "36khp2hdj2cfcm5nqhe3",
                        poiName = "소프트웨어드림센터",
                        poiSubId = "0",
                        address = "경기도 성남시 수정구 창업로40번길 6"
                    )
                )

                // 경로 옵션: 고속도로 우선
                naviHelper.changeRouteOption(RouteOption.HIGHWAY)

                // 정보 조회 (결과는 콜백으로 수신)
                naviHelper.getDestinationInfo()
                naviHelper.getWaypointInfo()
            },
            onFailed = { throwable ->
                // 초기 연결 재시도가 모두 실패
            }
        )
    }

    override fun onDestroy() {
        naviHelper.removeListener(naviEventListener)
        naviHelper.release()
        super.onDestroy()
    }
}
```

> 위 종합 예제의 클래스 구조(`NaviActivity`, `onCreate` 안에서 호출하는 순서)는 이해를 돕기 위해 구성한 것입니다. 개별 API 호출 코드는 가이드 예제와 같습니다. 실제로는 경로 탐색 완료(`onRoutingCompleted`) 같은 콜백을 받은 뒤 다음 요청을 보내는 식으로 흐름을 짜는 것이 좋습니다. 경로 옵션 변경과 재탐색은 활성 경로가 있어야 하기 때문입니다.
## 3. TextToSpeech SDK

### 3.1 개요

TextToSpeech SDK는 텍스트를 음성으로 합성하고 재생하기 위한 API를 제공합니다. 3rd party 앱은 TextToSpeech SDK를 통해 Pleos Connect의 **Speech Service**와 연동됩니다. 앱이 텍스트의 음성 합성을 요청하면 Speech Service가 이를 처리하고, 합성된 음성을 **차량의 오디오 시스템**으로 재생합니다.

| 항목 | 내용 |
|---|---|
| 기준 SDK 버전 | v3.3.1 |
| Maven 좌표 | `ai.pleos.playground:TextToSpeech:3.3.1` |
| 진입점 클래스 | `ai.pleos.playground.tts.TextToSpeech` |
| 생성자 | `TextToSpeech(context: Context, mode: Mode = Mode.HYBRID)` |
| 필요 권한 | `pleos.car.permission.TTS_SERVICE` |
| 패키지 | `ai.pleos.playground.tts`, `ai.pleos.playground.tts.constant`, `ai.pleos.playground.tts.listener` |
| 버전 조회 | `TextToSpeech.Companion.version: String` |

API Reference에 명시된 클래스 설명은 다음과 같습니다.

- TextToSpeech 서비스에 대한 접근을 제공하며, 원격 서버 또는 On-device 엔진으로 오디오를 스트리밍하여 TTS를 수행합니다.
- **중요:** 더 이상 필요하지 않거나 애플리케이션이 종료(destroy)될 때 호출자는 반드시 `release()`를 호출해야 합니다.
- 서버 기반 음성 합성기를 사용하려면 Pleos Playground에 앱을 등록해야 합니다.

#### 3.1.1 음성 합성 모드 (`Mode`)

생성자의 `mode` 인자로 지정하며, 생략하면 `HYBRID`가 자동 적용됩니다.

| 모드 | 동작 | 네트워크 | 앱 등록 | 네트워크 실패 시 |
|---|---|---|---|---|
| `HYBRID` (기본값) | 서버 기반 streaming TTS를 우선 사용 | 서버 합성 시 필요 | 필요 (서버 기반 합성을 사용하려면) | SDK가 On-device 엔진으로 **자동 전환** |
| `SERVER` | 서버 기반 streaming TTS만 사용 | 필요 | 필요 | On-device로 전환하지 않고 **오류 이벤트 전달** |
| `ON_DEVICE` | 차량의 On-device 엔진에서 Streaming TTS 실행 | 불필요 | 불필요 (ON_DEVICE만 사용하는 경우) | 해당 없음 |

> API Reference: `HYBRID`, `SERVER`는 "client identification을 얻기 위해 애플리케이션을 Pleos Playground에 **반드시** 등록해야 한다(MUST)"고 명시되어 있습니다.

#### 3.1.2 일반적인 호출 순서

1. `TextToSpeech` 객체를 생성합니다.
2. `initialize()`로 SDK를 초기화합니다.
3. `addEventListener()`로 이벤트 리스너를 등록합니다.
4. 필요한 경우 `setLocale()`로 음성 합성 언어/지역을 설정합니다.
5. 서버 기반 합성 모드(`HYBRID`, `SERVER`)를 사용하는 경우 `registerApp()`으로 앱 등록을 완료합니다.
6. 서버 연결이 필요한 모드에서는 `OnServerConnectionListener.onConnected()` 이후 `speak()`를 호출합니다.
7. 재생을 중단하려면 `stop()`을 호출합니다.
8. SDK 사용이 끝나면 `removeEventListener()` 후 `release()`로 리소스를 해제합니다.

```kotlin
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode

val textToSpeech = TextToSpeech(applicationContext, Mode.HYBRID)
```

#### 3.1.3 Gradle 설정

`settings.gradle.kts`에 Pleos Maven 저장소를 추가합니다.

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
```

앱 모듈 `app/build.gradle.kts`에 의존성을 추가합니다.

```kotlin
dependencies {
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
}
```

Groovy(`settings.gradle`, `app/build.gradle`)에서는 `maven { url "https://nexus-playground.pleos.ai/repository/maven-releases/" }`, `implementation "ai.pleos.playground:TextToSpeech:3.3.1"` 형식으로 작성합니다. Gradle 동기화 후 권한을 선언합니다.

#### 3.1.4 권한 선언

`AndroidManifest.xml`에 `TTS_SERVICE` 권한을 선언해야 합니다.

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.TTS_SERVICE" />
    <application>
        <!-- ... -->
    </application>
</manifest>
```

- 이 권한은 SDK 초기화, 이벤트 리스너 등록, 앱 등록, 음성 합성 요청 등 TextToSpeech 서비스에 접근하는 API에 필요합니다.
- 권한을 선언하지 않으면 SDK 초기화나 API 호출이 실패할 수 있습니다. 오류 콜백에서 `PERMISSION_DENIED`가 전달되면 manifest와 실행 환경의 권한 허용 여부를 확인합니다.

#### 3.1.5 앱 등록 (Client ID / Client Secret)

서버 기반 음성 합성을 사용하려면 Pleos Playground에서 **My Project**를 생성할 때 발급받은 **Client ID**와 **Client Secret**으로 앱을 등록합니다. `registerApp()`은 **SDK 초기화가 완료된 후** 호출합니다.

| 모드 | 앱 등록 필요 여부 |
|---|---|
| `SERVER` | 필요 |
| `HYBRID` | 서버 기반 합성을 사용하려면 필요 |
| `ON_DEVICE` (단독 사용) | 불필요 |

`OnServerConnectionListener.onConnected()`가 호출된 이후에 서버 기반 요청(`speak()`)을 시작합니다.

#### 3.1.6 에뮬레이터 환경

Android Studio에 Pleos Connect Emulator를 설치하고 AVD를 설정합니다. 절차: 소프트웨어 개발 환경 요구사항 확인 → Pleos Connect Emulator 설치 → AVD 생성 및 실행 → Pleos Connect Virtual Console 설정 → 마이크 접근 설정 → Cold boot 설정.

### 3.2 API 상세 (`TextToSpeech`)

모든 메서드는 별도 표기가 없으면 `@RequiresPermission(value = "pleos.car.permission.TTS_SERVICE")`가 붙어 있습니다.

| API | 설명 | 파라미터 | 비고 |
|---|---|---|---|
| `constructor(context: Context, mode: Mode = Mode.HYBRID)` | TextToSpeech 객체 생성 | `context`: 애플리케이션 Context<br>`mode`: `HYBRID` / `SERVER` / `ON_DEVICE` | `mode` 생략 시 `HYBRID` |
| `initialize()` | SDK 초기화. 앱이 시작되거나 생성될 때 호출 | 없음 | 결과 콜백이 필요 없을 때 사용 |
| `initialize(listener: OnInitializeListener)` | 초기화 결과 리스너와 함께 SDK 초기화 | `listener`: TTS 서비스 바인딩 성공 시 `onSuccess`, 실패 시 `ErrorType`과 함께 `onFailure` 수신 | 초기화 후 API 사용 가능 |
| `addEventListener(listener: EventListener)` | TTS 진행 관련 이벤트를 받을 리스너 등록 | `listener`: `EventListener` | |
| `removeEventListener(listener: EventListener)` | 등록된 리스너 제거 | `listener`: 등록 시 사용한 동일 인스턴스 | `release()` 전에 호출 권장 |
| `registerApp(clientID: String, clientSecret: String, listener: OnServerConnectionListener)` | 서버 기반 합성을 위한 앱 등록 | `clientID`, `clientSecret`: Pleos Playground 등록 후 발급<br>`listener`: 서버 연결 성공/실패 이벤트 | 초기화 완료 후 호출. `SERVER`/`HYBRID`에서 필요 |
| `setLocale(locale: Locale)` | 음성 합성 언어/지역 설정 | `locale`: `java.util.Locale` | 어노테이션은 없으나 설명에 "TTS_PERMISSION 필요" 명시 |
| `setSpeakerType(type: String)` | 음성 합성 화자(voice) 설정 | `type`: 화자 이름 (3.4.3 참조) | 어노테이션은 없으나 설명에 "TTS_PERMISSION 필요" 명시. 기본값 규칙은 3.4.3 참조 |
| `setAudioAttributes(audioAttributes: AudioAttributes)` | TTS SDK에서 사용할 오디오 속성 설정 | `audioAttributes`: 오디오 스트림에 대한 유효한 audio attributes | |
| `setAudioZoneId(zoneId: Int)` | 사용할 오디오 Zone ID 갱신 | `zoneId`: 설정할 Audio Zone ID | 설명에 "TTS_PERMISSION 필요" 명시 |
| `speak(text: String, isFinal: Boolean = true)` | 텍스트를 음성으로 변환하여 재생 | `text`: 읽을 메시지<br>`isFinal`: 여러 텍스트를 나눠 보낼 때 마지막 조각을 제외하고 `false` | 기본값 `true` (하나의 완성된 요청) |
| `stop()` | 진행 중인 음성 합성과 재생 중단 | 없음 | 완료 시 `EventListener.onStop()` 호출 |
| `release()` | SDK 해제. 앱이 종료/파괴될 때 호출 | 없음 | 이후 재사용하려면 새 객체 생성 후 재초기화 |
| `Companion.version: String` | SDK 버전 문자열 | - | 프로퍼티 |

### 3.3 리스너 / 콜백

패키지: `ai.pleos.playground.tts.listener`

#### 3.3.1 `OnInitializeListener`

TextToSpeech SDK 초기화 결과 리스너입니다(`TextToSpeech.initialize` 참조).

| 콜백 | 호출 시점 | 파라미터 |
|---|---|---|
| `onSuccess()` | SDK가 초기화되어 TTS 서비스에 바인딩(bound)되었을 때 | 없음 |
| `onFailure(errorType: ErrorType)` | SDK 초기화에 실패했을 때 | `errorType`: 실패 원인 |

#### 3.3.2 `OnServerConnectionListener`

서버 연결 결과 리스너입니다(`registerApp()`에 전달).

| 콜백 | 호출 시점 | 파라미터 |
|---|---|---|
| `onConnected()` | 서버에 연결되었을 때. 이후 서버 기반 합성 요청 가능 | 없음 |
| `onFailed(errorType: ErrorType)` | 등록 또는 서버 연결에 실패했을 때 | `errorType`: 오류 타입 |

#### 3.3.3 `EventListener`

TTS 진행과 관련된 이벤트 리스너이며 `addEventListener`/`removeEventListener`로 추가·제거합니다.

| 콜백 | 호출 시점 (가이드) | API Reference 설명 |
|---|---|---|
| `onReady()` | `speak()` 요청을 처리할 준비가 되었을 때 | `speak`가 호출되었을 때 |
| `onStart()` | 음성 재생이 시작될 때 | 호출자가 인지하는 시점에 TTS가 시작될 때 |
| `onUpdatedRms(rms: Double)` | 재생 중 RMS(Root Mean Square) 값이 갱신될 때 | RMS 값 갱신 시 |
| `onDone()` | 요청한 음성 재생이 정상적으로 끝났을 때 | TTS 처리가 성공적으로 완료되었을 때 |
| `onStop()` | 진행 중인 재생이 중지되었을 때 (`stop()` 완료 시) | 진행 중 TTS가 중지되었을 때 |
| `onError(errorType: ErrorType)` | 처리 중 오류가 발생했을 때 | 처리 중 오류 발생 시 |

### 3.4 상수 / Enum

패키지: `ai.pleos.playground.tts.constant`

#### 3.4.1 `Mode`

| 값 | API Reference 설명 |
|---|---|
| `SERVER` | 서버 지향 Streaming TTS가 실행됩니다. 네트워크 연결에 실패하면 앱은 오류 이벤트를 받습니다. Pleos Playground 등록 필수 |
| `ON_DEVICE` | On-device 지향 Streaming TTS가 실행됩니다. `ON_DEVICE` 모드만 사용하면 Pleos Playground 등록이 필요 없습니다 |
| `HYBRID` | 서버 지향 Streaming TTS가 실행되며, 네트워크 장애 시 SDK가 자동으로 On-device 엔진으로 TTS 오디오를 스트리밍합니다. Pleos Playground 등록 필수 |

표준 enum 멤버 `entries`, `valueOf(value: String)`(일치하는 상수가 없으면 `IllegalArgumentException`), `values()`를 제공합니다.

#### 3.4.2 `ErrorType`

오류는 `OnInitializeListener`, `OnServerConnectionListener`, `EventListener`에서 전달됩니다.

| ErrorType | 의미 (가이드) | API Reference 설명 | 권장 조치 |
|---|---|---|---|
| `AUDIO_ERROR` | 오디오 출력 또는 재생 처리 실패 | Audio playback/output related error | 오디오 설정과 출력 장치 상태 확인 후 재요청 |
| `INVALID_REQUEST_ERROR` | 요청 값이나 요청 상태가 올바르지 않음 | Client-side invalid request | 텍스트와 호출 순서 등 요청 조건 확인 |
| `MIC_ERROR` | 오디오 입력 장치 처리 중 오류 | Microphone input/recording related error | 실행 환경의 오디오 장치 상태 확인 |
| `NETWORK_ERROR` | 네트워크 통신 실패 | Network connectivity failure | 네트워크 연결 확인 후 재요청 |
| `NOT_SUPPORTED_API` | 현재 모드/실행 환경에서 지원하지 않는 API | Requested API is not supported | 선택한 `Mode`와 플랫폼 지원 범위 확인 |
| `ON_DEVICE_ENGINE_ERROR` | On-device 음성 합성 엔진 오류 | On-device engine (e.g. Speech SDK) failure | 엔진 상태 확인 또는 `HYBRID`/`SERVER` 모드 검토 |
| `PERMISSION_DENIED` | TTS 서비스 접근 권한 없음 | Permission denied or insufficient access rights | manifest의 `TTS_SERVICE` 권한과 실행 환경 권한 상태 확인 |
| `SERVER_INTERNAL_ERROR` | Speech Service 내부 오류 | Server-side processing failure (5xx, gRPC internal issues) | 잠시 후 재요청, 반복 시 서비스 관리자 문의 |
| `TIMEOUT` | 제한 시간 안에 요청 미완료 | Request/response timeout | 네트워크와 서버 상태 확인 후 재요청 |
| `UNAUTHENTICATED` | 앱 인증 실패 | Authentication failure (invalid/expired token) | Pleos Playground My Project 자격 증명 또는 UA 앱 정보와 접속 환경 확인 |
| `UNAVAILABLE_LANGUAGE` | 요청한 언어 사용 불가 | Requested language is not available | 지원되는 Locale로 변경 |
| `UNKNOWN` | 분류되지 않은 오류 | Unknown error type | 호출 시점과 환경 정보를 수집해 서비스 관리자 문의 |

#### 3.4.3 지원 Locale 및 화자(Speaker)

**지원 Locale**

| Locale | 언어 |
|---|---|
| `ko-KR` | 한국어 |
| `en-US` | 영어 |
| `fr-CA` | 프랑스어 |
| `es-US`, `es-MX` | 스페인어 |

**`setSpeakerType()` 화자 목록**

| 언어 | 화자 |
|---|---|
| 한국어 | `seoyoon`, `eunwoo`, `haeun`, `sanghyeok` |
| 영어 | `lyra`, `finley` |
| 스페인어 | `iris`, `arlo` |
| 프랑스어 | `sabine`, `noah` |

화자 기본값 결정 순서(`setSpeakerType()`을 호출하지 않은 경우): ① 해당 앱에 이전에 저장된 화자 → ② Gleo AI 화자 설정 → ③ 둘 다 없으면 `haeun`.

### 3.5 주의사항

- **초기화 필수:** 객체 생성 후 `initialize()`를 호출해야 API를 사용할 수 있습니다.
- **서버 연결 후 합성:** 앱 등록이 필요한 모드(`HYBRID`, `SERVER`)에서는 `OnServerConnectionListener.onConnected()` 호출 이후 `speak()`를 요청합니다.
- **`isFinal` 사용법:** `speak()`의 `isFinal` 기본값은 `true`이며, 전달한 텍스트를 하나의 완성된 요청으로 처리합니다. 여러 텍스트 조각을 하나의 발화로 보낼 때는 마지막 조각을 제외한 요청에 `isFinal = false`를 지정합니다. (Reference 예: `speak("Hello," false)` → `speak("I'm," false)` → `speak("Gleo")`가 최종 텍스트)
- **Locale 적용 시점:** `speak()` 호출 전에 설정한 locale이 해당 합성 요청의 기준으로 사용되며, `setLocale()`로 설정한 값은 다음 합성 요청의 언어/지역 기준이 됩니다.
- **Locale 기본 동작(fallback):** `setLocale()`을 호출하지 않으면 SDK는 같은 앱에서 이전에 `setLocale()`로 설정한 값이 있는지 먼저 확인하여 있으면 그 값을, 없으면 **시스템 locale**을 사용합니다.
- **지원하지 않는 언어:** `UNAVAILABLE_LANGUAGE` 오류 시 지원 Locale로 변경합니다.
- **리스너 해제:** 리스너가 더 이상 필요하지 않거나 SDK를 해제하기 전에는 등록 시 사용한 **동일한 인스턴스**를 `removeEventListener()`로 제거합니다.
- **해제와 재사용:** 객체는 사용하는 컴포넌트의 수명주기에 맞춰 재사용하고, 종료 시 `release()`를 호출합니다. `release()` 이후 재사용하려면 새 객체를 생성하고 초기화 절차를 다시 수행해야 합니다.
- **오류 분기:** `ErrorType`은 새로운 값이 추가될 수 있으므로 `when`/`switch`에 기본(`else`) 분기를 두는 것을 권장합니다.

### 3.6 Kotlin 코드 예제

#### 3.6.1 초기화

```kotlin
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.tts.constant.ErrorType

textToSpeech.initialize(object : OnInitializeListener {
    override fun onSuccess() {
        // TextToSpeech API를 사용할 수 있습니다.
    }
    override fun onFailure(errorType: ErrorType) {
        // 초기화 실패 원인을 처리합니다.
    }
})

// 결과 콜백이 필요하지 않은 경우
textToSpeech.initialize()
```

#### 3.6.2 이벤트 리스너 등록

```kotlin
private val eventListener = object : EventListener {
    override fun onReady() = Unit
    override fun onStart() = Unit
    override fun onUpdatedRms(rms: Double) = Unit
    override fun onDone() = Unit
    override fun onStop() = Unit
    override fun onError(errorType: ErrorType) {
        // 오류를 처리합니다.
    }
}

textToSpeech.addEventListener(eventListener)
```

#### 3.6.3 Locale 설정

```kotlin
import java.util.Locale

textToSpeech.setLocale(Locale.KOREA)
```

#### 3.6.4 앱 등록

```kotlin
textToSpeech.registerApp(
    clientID = clientId,
    clientSecret = clientSecret,
    listener = object : OnServerConnectionListener {
        override fun onConnected() {
            // 서버 기반 음성 합성을 요청할 수 있습니다.
        }
        override fun onFailed(errorType: ErrorType) {
            // 등록 또는 서버 연결 실패를 처리합니다.
        }
    }
)
```

#### 3.6.5 음성 합성 시작 및 중지

```kotlin
// 단일 요청 (isFinal 기본값 true)
textToSpeech.speak("안녕하세요. 무엇을 도와드릴까요?")

// 여러 조각을 하나의 발화로 전달
textToSpeech.speak("안녕하세요. ", isFinal = false)
textToSpeech.speak("TextToSpeech SDK입니다.", isFinal = true)

// 진행 중인 합성/재생 중지 → EventListener.onStop() 호출
textToSpeech.stop()
```

#### 3.6.6 오류 처리

```kotlin
override fun onError(errorType: ErrorType) {
    when (errorType) {
        ErrorType.PERMISSION_DENIED -> checkTtsPermission()
        ErrorType.UNAUTHENTICATED -> checkCredentials()
        ErrorType.NETWORK_ERROR,
        ErrorType.TIMEOUT -> retryWhenNetworkIsAvailable()
        else -> logError(errorType.name)
    }
}
```

#### 3.6.7 해제

```kotlin
override fun onDestroy() {
    textToSpeech.removeEventListener(eventListener)
    textToSpeech.release()
    super.onDestroy()
}
```
## 4. SpeechToText SDK

### 4.1 개요

SpeechToText SDK는 음성을 텍스트로 변환하기 위한 API를 제공합니다. 3rd party 앱은 SpeechToText SDK를 통해 Pleos Connect의 **Speech Service**와 연동됩니다. 앱이 음성 인식을 요청하면 Speech Service가 인식된 음성을 텍스트로 변환하고 인식 결과를 앱에 전달합니다.

> **음성 명령 기능은 제공하지 않습니다.** SpeechToText는 사용자 발화를 "음악 틀어줘"와 같은 텍스트로 변환하여 반환할 뿐, 음악 재생과 같은 실제 동작은 실행하지 않습니다. 인식한 명령에 따라 앱 기능을 실행하려면 **Gleo Interface**를 참고하세요.

| 항목 | 내용 |
|---|---|
| 기준 SDK 버전 | v3.3.1 |
| Maven 좌표 | `ai.pleos.playground:SpeechToText:3.3.1` |
| 진입점 클래스 | `ai.pleos.playground.stt.SpeechToText` |
| 생성자 | `SpeechToText(context: Context, mode: Mode = Mode.HYBRID)` |
| 필요 권한 | `pleos.car.permission.STT_SERVICE` |
| 패키지 | `ai.pleos.playground.stt`, `ai.pleos.playground.stt.constant`, `ai.pleos.playground.stt.data`, `ai.pleos.playground.stt.listener` |
| 버전 조회 | `SpeechToText.Companion.version: String` |

API Reference에 명시된 클래스 설명은 다음과 같습니다.

- SpeechToText 서비스에 대한 접근을 제공하며, 원격 서버 또는 On-device 엔진으로 오디오를 스트리밍하여 STT를 수행합니다.
- **중요:** 더 이상 필요하지 않거나 애플리케이션이 종료(destroy)될 때 호출자는 반드시 `release()`를 호출해야 합니다.
- 서버 기반 음성 인식기를 사용하려면 Pleos Playground에 앱을 등록해야 합니다.

#### 4.1.1 음성 인식 모드 (`Mode`)

생성자의 `mode` 인자로 지정하며, 생략하면 `HYBRID`가 자동 적용됩니다.

| 모드 | 동작 | 네트워크 | 앱 등록 | 네트워크 실패 시 |
|---|---|---|---|---|
| `HYBRID` (기본값) | 서버 기반 음성 인식을 우선 사용 | 서버 인식 시 필요 | 필요 (서버 기반 인식을 사용하려면) | SDK가 On-device 엔진으로 **자동 전환** |
| `SERVER` | 음성 데이터를 원격 Speech Service로 전송하여 인식 | 필요 | 필요 | On-device 엔진으로 **전환하지 않음** |
| `ON_DEVICE` | 차량의 On-device 엔진에서 음성 인식 | 불필요 | 불필요 (ON_DEVICE만 사용하는 경우) | 해당 없음 |

> `sendAudio()` / `completeAudioSend()`(외부 PCM 입력)는 **`ON_DEVICE` 모드에서만** 동작합니다.

#### 4.1.2 일반적인 호출 순서

1. `SpeechToText` 객체를 생성합니다.
2. `initialize()`로 SDK를 초기화합니다.
3. `addListener()`로 `ResultListener`를 등록합니다.
4. 필요한 경우 `setLocale()`로 인식 언어/지역을 설정합니다.
5. 서버 기반 인식 모드(`HYBRID`, `SERVER`)를 사용하는 경우 `registerApp()`으로 앱 등록을 완료합니다.
6. `request()`로 음성 인식을 시작합니다. (ON_DEVICE에서 외부 오디오를 사용하는 경우 `sendAudio()` → `completeAudioSend()`)
7. 인식이 끝나면 `stop()`을 호출합니다.
8. SDK 사용이 끝나면 `removeListener()` 후 `release()`로 리소스를 해제합니다.

```kotlin
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.Mode

val speechToText = SpeechToText(applicationContext, Mode.HYBRID)
```

#### 4.1.3 Gradle 설정

`settings.gradle.kts`에 Pleos Maven 저장소를 추가합니다.

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
```

앱 모듈 `app/build.gradle.kts`에 의존성을 추가합니다.

```kotlin
dependencies {
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
}
```

Groovy에서는 `maven { url "https://nexus-playground.pleos.ai/repository/maven-releases/" }`, `implementation "ai.pleos.playground:SpeechToText:3.3.1"` 형식으로 작성합니다. Gradle 동기화 후 권한을 선언합니다.

#### 4.1.4 권한 선언

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.STT_SERVICE" />
    <application>
        <!-- ... -->
    </application>
</manifest>
```

- 이 권한은 SDK 초기화, 결과 리스너 등록, 앱 등록, 음성 인식 요청 및 오디오 데이터 전송 등 SpeechToText 서비스에 접근하는 API에 필요합니다.
- 권한을 선언하지 않으면 SDK 초기화나 API 호출이 실패할 수 있습니다. 오류 콜백에서 `PERMISSION_DENIED`가 전달되면 manifest와 실행 환경의 권한 허용 여부를 확인합니다.

#### 4.1.5 앱 등록 (Client ID / Client Secret)

서버 기반 음성 인식을 사용하려면 Pleos Playground에서 **My Project**를 생성할 때 발급받은 **Client ID**와 **Client Secret**으로 앱을 등록합니다. `registerApp()`은 **SDK 초기화가 완료된 후** 호출합니다.

| 모드 | 앱 등록 필요 여부 |
|---|---|
| `SERVER` | 필요 |
| `HYBRID` | 서버 기반 인식을 사용하려면 필요 |
| `ON_DEVICE` (단독 사용) | 불필요 |

`OnServerConnectionListener.onConnected()`가 호출된 이후 서버 기반 요청을 시작합니다. 가이드는 서버 기반 인식이 필요한 모드에서 `request()` 전에 "앱 등록 또는 UA 인증"을 완료하도록 안내합니다.

#### 4.1.6 에뮬레이터 환경

Android Studio에 Pleos Connect Emulator를 설치하고 AVD를 설정합니다. 절차: 소프트웨어 개발 환경 요구사항 확인 → Pleos Connect Emulator 설치 → AVD 생성 및 실행 → Pleos Connect Virtual Console 설정 → **마이크 접근 설정** → Cold boot 설정.

### 4.2 API 상세 (`SpeechToText`)

모든 메서드는 별도 표기가 없으면 `@RequiresPermission(value = "pleos.car.permission.STT_SERVICE")`가 붙어 있습니다.

| API | 설명 | 파라미터 | 비고 |
|---|---|---|---|
| `constructor(context: Context, mode: Mode = Mode.HYBRID)` | SpeechToText 객체 생성 | `context`: 애플리케이션 Context<br>`mode`: `HYBRID` / `SERVER` / `ON_DEVICE` | `mode` 생략 시 `HYBRID` |
| `initialize()` | SpeechToText 사용을 위한 SDK 초기화 | 없음 | 결과 콜백이 필요 없을 때 |
| `initialize(listener: OnInitializeListener)` | 초기화 결과 리스너와 함께 SDK 초기화 | `listener`: STT 서비스 바인딩 성공 시 `onSuccess`, 실패 시 `ErrorType`과 함께 `onFailure` | |
| `addListener(listener: ResultListener)` | 인식 관련 이벤트를 받을 리스너 등록 | `listener`: `ResultListener` | `request()`/`sendAudio()` 호출 **전에** 등록 |
| `removeListener(listener: ResultListener)` | 등록된 리스너 제거 | `listener`: 등록 시 사용한 동일 인스턴스 | `release()` 전에 호출 |
| `registerApp(clientID: String, clientSecret: String, listener: OnServerConnectionListener)` | 서버 기반 또는 하이브리드 STT 사용을 위한 앱 등록 | `clientID`, `clientSecret`: Pleos Playground 등록 후 발급<br>`listener`: 서버 연결 완료 이벤트 | 초기화 완료 후 호출 |
| `setLocale(locale: Locale)` | 인식 언어/지역 설정 | `locale`: `java.util.Locale` | 어노테이션은 없으나 설명에 "STT_PERMISSION 필요" 명시 |
| `request()` | 음성 인식 시작. 호출 후 마이크가 열리고 마이크를 통해 오디오가 스트리밍됨 | 없음 | 초기화·리스너 등록(서버 모드는 앱 등록) 선행 필요 |
| `stop()` | 음성 인식 중지. 호출 후 마이크 비활성화 | 없음 | 종료 시 `onEndedRecognition()` 호출 |
| `sendAudio(bytes: ByteArray)` | 외부 오디오 버퍼(PCM)를 인식용으로 전송 | `bytes`: PCM 오디오 바이트 | **`Mode.ON_DEVICE` 전용.** 다른 모드에서는 무시되며 `onError`도 호출되지 않음. 첫 버퍼 전송 시 인식 시작(`request()` 불필요). 이 경로에서는 `onReady`, `onUpdatedRms` 미호출 |
| `completeAudioSend()` | `sendAudio`로 시작한 외부 오디오 입력을 완료하고 인식 종료 | 없음 | **`Mode.ON_DEVICE` 전용.** 다른 모드에서는 무시. 마지막 버퍼 전송 후 반드시 호출 |
| `release()` | SDK 해제 | 없음 | 이후 재사용하려면 새 객체 생성 후 재초기화 |
| `Companion.version: String` | SDK 버전 문자열 | - | 프로퍼티 |

### 4.3 리스너 / 콜백

패키지: `ai.pleos.playground.stt.listener`

#### 4.3.1 `OnInitializeListener`

| 콜백 | 호출 시점 | 파라미터 |
|---|---|---|
| `onSuccess()` | SDK가 초기화되어 STT 서비스에 바인딩되었을 때 | 없음 |
| `onFailure(errorType: ErrorType)` | SDK 초기화에 실패했을 때 | `errorType`: 실패 원인 |

#### 4.3.2 `OnServerConnectionListener`

서버 연결 성공 이벤트를 받는 리스너입니다(`registerApp()`에 전달).

| 콜백 | 호출 시점 | 파라미터 |
|---|---|---|
| `onConnected()` | 서버에 연결되었을 때. 이후 서버 기반 인식 요청 가능 | 없음 |
| `onFailed(errorType: ErrorType)` | 등록 또는 서버 연결에 실패했을 때 | `errorType`: 오류 타입 |

#### 4.3.3 `ResultListener`

인식 관련 이벤트를 받는 리스너이며 `addListener`/`removeListener`로 추가·제거합니다.

| 콜백 | 호출 시점 | 파라미터 / 비고 |
|---|---|---|
| `onReady()` | 음성 인식 요청을 처리할 준비가 되었을 때 | `sendAudio()` 경로에서는 호출되지 않음 |
| `onStartedRecognition()` | 음성 인식이 시작될 때 | |
| `onUpdated(stt: String, completed: Boolean)` | 인식 텍스트(중간·최종)가 갱신될 때 | `stt`: 인식 결과 텍스트<br>`completed`: `true`이면 해당 인식 결과 완료. Reference: 이 이벤트를 받으면 앱은 오디오 스트리밍을 중단 |
| `onUpdatedEpdData(on: Long, off: Long)` | EPD(End Point Detection)가 감지한 음성 시작/종료 위치가 갱신될 때 | `on`: 음성 시작 위치, `off`: 음성 종료 위치. 값은 입력 오디오 버퍼의 **byte offset** |
| `onUpdatedRms(rms: Double)` | 입력 음성 신호의 RMS 값이 갱신될 때 | RMS는 신호 크기를 나타내며 **dB 값이 아님**. `sendAudio()` 경로에서는 호출되지 않음 |
| `onEndedRecognition()` | 음성 인식이 종료될 때 (`stop()` 후 포함) | |
| `onError(errorType: ErrorType)` | 처리 중(인식) 오류가 발생했을 때 | `errorType`: 4.4.2 참조 |

### 4.4 상수 / Enum / 데이터

#### 4.4.1 `Mode` (`ai.pleos.playground.stt.constant`)

| 값 | API Reference 설명 |
|---|---|
| `HYBRID` | 원격 서버로 오디오를 스트리밍하여 STT 수행. 네트워크 장애 시 자동으로 On-device 엔진으로 오디오 스트리밍. Pleos Playground 등록 필수 |
| `SERVER` | 원격 서버로 오디오를 스트리밍하여 STT 수행. Pleos Playground 등록 필수 |
| `ON_DEVICE` | On-device 엔진으로 오디오를 스트리밍하여 STT 수행. `ON_DEVICE`만 사용하면 Pleos Playground 등록 불필요 |

표준 enum 멤버 `entries`, `valueOf(value: String)`(일치하는 상수가 없으면 `IllegalArgumentException`), `values()`를 제공합니다.

#### 4.4.2 `ErrorType` (`ai.pleos.playground.stt.constant`)

오류는 `OnInitializeListener`, `OnServerConnectionListener`, `ResultListener`에서 전달됩니다.

| ErrorType | 의미 (가이드) | API Reference 설명 | 권장 조치 |
|---|---|---|---|
| `AUDIO_ERROR` | 입력 오디오 데이터 처리 실패 | Audio data processing error (e.g. failed to read the audio buffer) | 입력 데이터와 오디오 형식 확인 후 재요청 |
| `INVALID_REQUEST_ERROR` | 요청 값이나 요청 상태가 올바르지 않음 | Client-side invalid request | 호출 순서, 중복 요청, 입력 값 확인 |
| `MIC_ERROR` | 마이크 입력 시작·처리 중 오류 | Microphone input/recording related error | 마이크 상태와 센서 개인정보 보호 설정 확인 |
| `NETWORK_ERROR` | 네트워크 통신 실패 | Network connectivity failure | 네트워크 연결 확인 후 재요청 |
| `NOT_SUPPORTED_API` | 현재 모드/실행 환경에서 지원하지 않는 API | Requested API is not supported | 선택한 `Mode`와 플랫폼 지원 범위 확인 |
| `ON_DEVICE_ENGINE_ERROR` | On-device 음성 인식 엔진 오류 | On-device engine (e.g. Speech SDK) failure | 엔진 상태 확인 또는 `HYBRID`/`SERVER` 모드 검토 |
| `PERMISSION_DENIED` | STT 서비스 접근 권한 없음 | Permission denied or insufficient access rights | manifest의 `STT_SERVICE` 권한과 실행 환경 권한 상태 확인 |
| `SERVER_INTERNAL_ERROR` | Speech Service 내부 오류 | Server-side processing failure (5xx, gRPC internal issues) | 잠시 후 재요청, 반복 시 서비스 관리자 문의 |
| `TIMEOUT` | 제한 시간 안에 요청 미완료 | Request/response timeout | 네트워크와 서버 상태 확인 후 재요청 |
| `UNAUTHENTICATED` | 앱 인증 실패 | Authentication failure (invalid/expired token) | Pleos Playground My Project 자격 증명 또는 UA 앱 정보와 접속 환경 확인 |
| `UNAVAILABLE_LANGUAGE` | 요청한 언어 사용 불가 | Requested language is not available | 지원되는 Locale로 변경 |
| `UNKNOWN` | 분류되지 않은 오류 | Unknown error type | 호출 시점과 환경 정보를 수집해 서비스 관리자 문의 |

#### 4.4.3 `InferenceSource` (`ai.pleos.playground.stt.data`)

| 값 | 직렬화 이름 (`@SerializedName`) |
|---|---|
| `ON_DEVICE` | `"on_device"` |
| `SERVER` | `"server"` |

`Companion.DEFAULT: InferenceSource` 프로퍼티와 표준 enum 멤버(`entries`, `valueOf`, `values`)를 제공합니다. API Reference에는 이 enum의 용도나 `DEFAULT`의 실제 값에 대한 설명이 없습니다.

#### 4.4.4 지원 Locale

| Locale | 언어 |
|---|---|
| `ko-KR` | 한국어 |
| `en-US` | 영어 |
| `fr-CA` | 프랑스어 |
| `es-US`, `es-MX` | 스페인어 |

### 4.5 주의사항

- **리스너 선등록:** 인식 이벤트를 놓치지 않도록 `request()` 또는 `sendAudio()` 호출 **전에** `addListener()`로 리스너를 등록합니다.
- **`request()` 선행 조건:** SDK 초기화와 결과 리스너 등록을 완료해야 하며, 서버 기반 인식이 필요한 모드에서는 앱 등록 또는 UA 인증도 완료해야 합니다.
- **마이크 제어:** `request()` 호출 시 SDK가 마이크를 활성화하고, `stop()` 호출 시 마이크 사용을 종료합니다.
- **외부 오디오(`sendAudio`)는 ON_DEVICE 전용:**
  - 다른 모드에서는 호출이 **무시**되며 `ResultListener.onError`도 호출되지 않으므로, 모드를 반드시 확인해야 합니다.
  - 첫 버퍼 전송 시 인식이 시작되므로 `request()`를 호출할 필요가 없습니다.
  - 이 경로에서는 `onReady()`와 `onUpdatedRms()`가 호출되지 않습니다.
  - 마지막 오디오 데이터를 보낸 후 **반드시** `completeAudioSend()`를 호출하여 입력 종료를 알립니다.
  - 전체 데이터를 한 번에 보내거나 여러 청크로 나눠 보낼 수 있습니다.
- **오디오 형식과 청크 크기:** 가이드의 **3,200-byte 청크 / 100ms 간격**은 **16 kHz, 16-bit, mono PCM**을 실시간 속도로 전송하는 예시입니다. (16,000 samples/s × 2 bytes × 1 ch × 0.1 s = 3,200 bytes) 다른 오디오 형식을 사용하면 해당 형식의 100ms 분량에 맞춰 청크 크기를 계산합니다.
- **`completed` 플래그:** `onUpdated(stt, completed)`에서 `completed == true`이면 해당 인식 결과가 완료된 것이며, Reference는 이 이벤트를 받으면 앱이 오디오 스트리밍을 중단한다고 설명합니다.
- **EPD/RMS 해석:** `onUpdatedEpdData`의 `on`/`off`는 입력 오디오 버퍼 기준 byte offset이며, `onUpdatedRms`의 값은 dB가 아닙니다.
- **Locale 적용 시점 및 fallback:** `request()` 호출 전에 설정한 locale이 해당 인식 세션 기준이 되며, `setLocale()` 값은 다음 인식 요청의 기준으로 사용됩니다. `setLocale()`을 호출하지 않으면 같은 앱에서 이전에 설정한 locale을 먼저 사용하고, 없으면 **시스템 locale**을 사용합니다.
- **음성 명령 미지원:** 인식 결과는 텍스트일 뿐이며 명령 실행은 Gleo Interface를 사용해야 합니다.
- **해제와 재사용:** 객체는 컴포넌트 수명주기에 맞춰 재사용하고, 종료 시 `removeListener()` 후 `release()`를 호출합니다. `release()` 이후에는 새 객체를 생성하고 다시 초기화해야 합니다.
- **오류 분기:** `ErrorType`은 새로운 값이 추가될 수 있으므로 `when`/`switch`에 기본 분기를 둡니다.

### 4.6 Kotlin 코드 예제

#### 4.6.1 초기화

```kotlin
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.listener.OnInitializeListener

speechToText.initialize(object : OnInitializeListener {
    override fun onSuccess() {
        // SpeechToText API를 사용할 수 있습니다.
    }
    override fun onFailure(errorType: ErrorType) {
        // 초기화 실패 원인을 처리합니다.
    }
})

// 결과 콜백이 필요하지 않은 경우
speechToText.initialize()
```

#### 4.6.2 결과 리스너 등록

```kotlin
private val resultListener = object : ResultListener {
    override fun onReady() = Unit
    override fun onStartedRecognition() = Unit
    override fun onUpdated(stt: String, completed: Boolean) {
        // 중간 또는 최종 인식 결과를 처리합니다.
    }
    override fun onUpdatedEpdData(on: Long, off: Long) = Unit
    override fun onUpdatedRms(rms: Double) = Unit
    override fun onEndedRecognition() = Unit
    override fun onError(errorType: ErrorType) {
        // 오류를 처리합니다.
    }
}

speechToText.addListener(resultListener)
```

#### 4.6.3 Locale 설정

```kotlin
import java.util.Locale

speechToText.setLocale(Locale.KOREA)
```

#### 4.6.4 앱 등록

```kotlin
speechToText.registerApp(
    clientID = clientId,
    clientSecret = clientSecret,
    listener = object : OnServerConnectionListener {
        override fun onConnected() {
            // 서버 기반 음성 인식을 요청할 수 있습니다.
        }
        override fun onFailed(errorType: ErrorType) {
            // 등록 또는 서버 연결 실패를 처리합니다.
        }
    }
)
```

#### 4.6.5 음성 인식 시작 및 중지 (차량 마이크)

```kotlin
speechToText.addListener(resultListener)
speechToText.request()

// 진행 중인 인식 중지 및 마이크 종료 → ResultListener.onEndedRecognition() 호출
speechToText.stop()
```

#### 4.6.6 오디오 데이터 전송 (ON_DEVICE 전용)

전체 버퍼를 한 번에 전송:

```kotlin
val pcmData = applicationContext.assets
    .open("test_audio.pcm")
    .use { it.readBytes() }

speechToText.sendAudio(pcmData)
speechToText.completeAudioSend()
```

청크 단위 스트리밍(16 kHz / 16-bit / mono 기준 100ms = 3,200 bytes):

```kotlin
private const val BYTES_PER_100_MS = 3_200

val pcmData = applicationContext.assets
    .open("test_audio.pcm")
    .use { it.readBytes() }

pcmData.asList()
    .chunked(BYTES_PER_100_MS)
    .forEach { chunk ->
        speechToText.sendAudio(chunk.toByteArray())
        Thread.sleep(100L)
    }

speechToText.completeAudioSend()
```

#### 4.6.7 오류 처리

```kotlin
override fun onError(errorType: ErrorType) {
    when (errorType) {
        ErrorType.PERMISSION_DENIED -> checkSttPermission()
        ErrorType.UNAUTHENTICATED -> checkCredentials()
        ErrorType.NETWORK_ERROR,
        ErrorType.TIMEOUT -> retryWhenNetworkIsAvailable()
        else -> logError(errorType.name)
    }
}
```

#### 4.6.8 해제

```kotlin
override fun onDestroy() {
    speechToText.removeListener(resultListener)
    speechToText.release()
    super.onDestroy()
}
```
## 5. FusedLocation SDK

### 5.1 개요

FusedLocation SDK는 앱이 **차량 위치 데이터**를 수신할 수 있도록 API를 제공합니다. Pleos Connect 앱은 FusedLocation SDK를 통해 Pleos Connect의 **FusedLocation Service**와 연동됩니다.

| 항목 | 내용 |
|---|---|
| 기준 SDK 버전 | v1.1.3 |
| Maven 좌표 | `ai.pleos.playground:FusedLocation:1.1.3` |
| 진입점 클래스 | `ai.pleos.playground.location.fused.FusedLocation` |
| 생성자 | `FusedLocation(context: Context)` |
| 필요 권한 | `pleos.car.permission.FUSED_LOCATION` (보호 수준: `dangerous`) |
| 패키지 | `ai.pleos.playground.location.fused`, `ai.pleos.playground.location.fused.listener`, `ai.pleos.playground.location.fused.model` |

TextToSpeech/SpeechToText와 달리 FusedLocation에는 동작 모드(`Mode`)나 앱 등록(Client ID/Secret) 절차가 없습니다.

#### 5.1.1 제공 기능

| 기능 | API |
|---|---|
| 마지막으로 확인된 위치 1회 조회 | `getLastKnownLocation()` |
| 위치 업데이트 수신 | `registerFusedLocationCallback()` |
| 위치 업데이트 중지 | `unregisterFusedLocationCallback()` |
| SDK 지원 환경 확인 | `isSupported()` |
| 위치 접근 차단 시 안내 팝업 표시 | `showLocationAccessBlockedPopup()` |
| 설정 영역 진입/이탈 알림 등록 | `addProximityAlert()` |
| 설정 영역 진입/이탈 알림 해제 | `removeProximityAlert()` |

#### 5.1.2 일반적인 호출 순서 (SDK 생명주기)

| 단계 | API | 설명 |
|---|---|---|
| 1. 객체 생성 | `FusedLocation(context)` | 인스턴스를 생성합니다 |
| 2. 지원 환경 확인 | `isSupported()` | 현재 환경이 FusedLocation SDK를 지원하는지 확인합니다 |
| 3. 초기화 | `initialize()` | SDK를 초기화합니다 |
| 4. 위치 데이터 수신 | `getLastKnownLocation()`, `registerFusedLocationCallback()` | 필요한 방식으로 위치를 조회하거나 업데이트를 수신합니다. 각 API의 요구 권한을 함께 확인합니다 |
| 5. 위치 업데이트 수신 중지 | `unregisterFusedLocationCallback()` | 더 이상 필요하지 않을 때 등록한 리스너를 해제합니다 |
| 6. 리소스 해제 | `release()` | SDK 사용이 끝나면 리소스를 해제합니다 |

#### 5.1.3 Gradle 설정

`settings.gradle.kts`에 Maven 저장소를 추가합니다.

```kotlin
dependencyResolutionManagement {
    repositories {
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
```

앱 모듈 `build.gradle.kts`에 의존성을 추가합니다.

```kotlin
dependencies {
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
}
```

Groovy에서는 `maven { url "https://nexus-playground.pleos.ai/repository/maven-releases/" }`, `implementation "ai.pleos.playground:FusedLocation:1.1.3"` 형식으로 작성합니다. 의존성 추가 후 Gradle 프로젝트를 동기화합니다.

#### 5.1.4 권한 선언

```xml
<manifest ...>
    <uses-permission android:name="pleos.car.permission.FUSED_LOCATION" />
    <application ... />
</manifest>
```

| 권한 | 보호 수준 | 사용 API |
|---|---|---|
| `pleos.car.permission.FUSED_LOCATION` | `dangerous` | `getLastKnownLocation`, `registerFusedLocationCallback`, `unregisterFusedLocationCallback`, `addProximityAlert`, `removeProximityAlert` |

> 가이드의 권한 표에는 앞의 세 API만 나열되어 있으나, API Reference에는 `addProximityAlert`, `removeProximityAlert`에도 동일 권한(`@RequiresPermission`, protection level: dangerous)이 명시되어 있으며 권한이 없으면 `SecurityException`을 던집니다.

- 권한이 허용되지 않은 상태에서 권한이 필요한 API를 호출하면 정상 동작하지 않을 수 있습니다. 보호 수준이 `dangerous`이므로 권한 요청과 결과 처리는 앱의 권한 처리 흐름에 맞게 구현합니다.
- Android의 `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` 동작과 포그라운드·백그라운드 위치 접근 정책은 이 SDK 문서에서 **별도로 보장하지 않습니다.** Google Fused Location SDK의 설명을 Pleos Connect의 동작 보장으로 해석하면 안 됩니다.

#### 5.1.5 에뮬레이터 환경

Pleos Connect Emulator 환경에서 개발 및 검증합니다. 절차: 소프트웨어 개발 환경 요구사항 확인 → Pleos Connect Emulator 설치 → AVD 생성 및 실행 → Pleos Connect Virtual Console 설정 → 마이크 접근 설정 → Cold boot 설정.

### 5.2 API 상세 (`FusedLocation`)

| API | 설명 | 파라미터 | 비고 |
|---|---|---|---|
| `constructor(context: Context)` | FusedLocation 객체 생성 | `context`: Context | 가이드 예시는 `Application`에서 `FusedLocation(this)`로 생성 |
| `isSupported(): Boolean` | Pleos Connect에 Fused Location Service 패키지가 존재하는지 반환 | 없음 | 반환: 패키지 조회 성공 시 `true`, 패키지가 없거나 조회 실패 시 `false`. SDK 초기화·서비스 연결 상태와 무관하며 서비스를 시작/바인딩하지 않음. 권한 불필요 |
| `initialize()` | SDK 초기화 | 없음 | 애플리케이션 생성 시 호출. 권한 불필요 |
| `getLastKnownLocation(callback: FusedLocationListener)` | 마지막으로 알려진 위치 조회 (1회) | `callback`: 결과를 받을 `FusedLocationListener` | `FUSED_LOCATION` 권한. 새 위치를 계산하지 않으며 데이터가 없으면 `null` 전달 가능 |
| `registerFusedLocationCallback(listener: FusedLocationListener)` | 위치 업데이트 수신 콜백 등록 | `listener`: `FusedLocationListener` | `FUSED_LOCATION` 권한. 위치 변경 시 `onChanged` 호출 |
| `unregisterFusedLocationCallback(listener: FusedLocationListener)` | 지정한 리스너로의 위치 업데이트 수신 중지 | `listener`: 등록 시 사용한 동일 리스너 | `FUSED_LOCATION` 권한. `release()`를 대체하지 않음 |
| `addProximityAlert(request: ProximityAlertRequest, listener: ProximityAlertListener)` | 지정 영역과 전이(transition)에 대한 근접 알림 등록 | `request`: 감시할 영역과 전이<br>`listener`: 알림 콜백 수신 | `FUSED_LOCATION` 권한. 알림 ID는 `onRegistered`로, 잘못된 요청은 `onError`로 전달. 권한 미허용 시 `SecurityException` |
| `removeProximityAlert(alertId: String)` | 등록된 근접 알림 해제 | `alertId`: `ProximityAlertListener.onRegistered`로 받은 ID | `FUSED_LOCATION` 권한. 권한 확인 후, 알 수 없거나 이미 제거된 ID는 무시. 권한 미허용 시 `SecurityException` |
| `showLocationAccessBlockedPopup()` | 현재 개인정보 보호 설정으로 위치 접근이 차단되었음을 알리는 팝업 표시 | 없음 | 사용자가 시스템 개인정보 설정에서 위치 공유를 제한하여 SDK가 위치를 제공할 수 없을 때 표시. 위치 접근을 **허용하는 API가 아님** |
| `release()` | SDK 해제 | 없음 | 애플리케이션 종료(destroy) 시 호출. `initialize()`를 호출했다면 반드시 호출 |

### 5.3 리스너 / 콜백

패키지: `ai.pleos.playground.location.fused.listener`

#### 5.3.1 `FusedLocationListener`

위치 업데이트를 받기 위한 리스너 인터페이스입니다. `getLastKnownLocation()`, `registerFusedLocationCallback()`, `unregisterFusedLocationCallback()`에 사용합니다.

| 콜백 | 호출 시점 | 파라미터 / 비고 |
|---|---|---|
| `onChanged(location: LocationData?)` | 위치 업데이트가 가능할 때(위치 변경 시). `getLastKnownLocation()`의 결과도 이 콜백으로 전달 | `location`: 위치 데이터. **`null`일 수 있음** |
| `onError(exception: Exception)` | 오류가 발생했을 때 | `exception`: SDK가 전달한 예외. FusedLocation SDK의 오류는 이 콜백으로 전달됨 |

#### 5.3.2 `ProximityAlertListener`

등록한 근접 알림의 이벤트와 오류를 받습니다. 등록 후 알림 식별자를 보고하고, 설정한 영역의 전이를 보고합니다.

| 콜백 | 호출 시점 | 파라미터 |
|---|---|---|
| `onRegistered(alertId: String)` | 알림이 정상 등록되었을 때 | `alertId`: 이후 `removeProximityAlert()`에 사용할 ID |
| `onTransition(event: ProximityAlertEvent)` | 설정한 근접 전이(진입/이탈)가 감지되었을 때 | `event`: 감지된 전이 정보 |
| `onError(exception: Exception)` | 알림을 등록하거나 처리할 수 없을 때 (잘못된 요청 포함) | `exception`: 오류 |

### 5.4 데이터 모델 / Enum

패키지: `ai.pleos.playground.location.fused.model`

#### 5.4.1 `LocationData`

`data class LocationData(val latitude: Double, val longitude: Double, val altitude: Double, val heading: Float, val confidence: Float, val timestampMillis: Long)`

위치 결과는 `LocationData`로 전달되며 위도, 경도, 고도, 진행 방향, 신뢰도, 타임스탬프를 포함합니다.

| 필드 | 타입 | 의미 |
|---|---|---|
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `altitude` | `Double` | 고도 |
| `heading` | `Float` | 진행 방향 |
| `confidence` | `Float` | 신뢰도 |
| `timestampMillis` | `Long` | 타임스탬프 (밀리초 단위, 필드명 기준) |

> API Reference에는 각 필드의 타입만 정의되어 있고, 단위(고도 m 여부, heading 기준 방위 등)나 `confidence`의 값 범위는 명시되어 있지 않습니다.

#### 5.4.2 `ProximityAlertRequest`

`data class ProximityAlertRequest(val latitude: Double, val longitude: Double, val radiusMeters: Float, val transitionTypes: Set<ProximityTransition>)` — 근접 알림으로 감시할 영역과 전이를 정의합니다.

| 필드 | 타입 | 설명 / 제약 |
|---|---|---|
| `latitude` | `Double` | 중심 위도(도). 유한값, **-90 ~ 90** (경계 포함) |
| `longitude` | `Double` | 중심 경도(도). 유한값, **-180 ~ 180** (경계 포함) |
| `radiusMeters` | `Float` | 반경(미터). 유한값, **0보다 커야 함** |
| `transitionTypes` | `Set<ProximityTransition>` | 알림을 발생시킬 전이 집합. **비어 있으면 안 됨** |

#### 5.4.3 `ProximityAlertEvent`

`data class ProximityAlertEvent(val alertId: String, val transition: ProximityTransition)` — 등록된 근접 알림에서 감지된 전이를 설명합니다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `alertId` | `String` | 알림 등록 성공 시 전달된 식별자 |
| `transition` | `ProximityTransition` | 감지된 전이 |

#### 5.4.4 `ProximityTransition` (enum)

근접 알림을 발생시킬 수 있는 전이를 나타냅니다.

| 값 | 설명 |
|---|---|
| `ENTER` | 기기가 설정 영역 **안으로** 이동할 때 발생 |
| `EXIT` | 기기가 설정 영역 **밖으로** 이동할 때 발생 |

표준 enum 멤버 `entries`, `name`, `ordinal`, `valueOf(value: String)`, `values()`를 제공합니다.

#### 5.4.5 오류 표현

FusedLocation SDK에는 TTS/STT와 같은 `ErrorType` enum이 없습니다. 오류는 `FusedLocationListener.onError(Exception)` / `ProximityAlertListener.onError(Exception)`의 `Exception`으로 전달되며, 권한이 없을 때 근접 알림 API는 `SecurityException`을 던집니다. 예외의 구체적인 종류나 재시도 가능 여부는 SDK가 전달한 예외와 실행 환경을 기준으로 판단해야 합니다.

### 5.5 주의사항

- **지원 환경 확인 필수:** FusedLocation API 사용 전 `isSupported()`로 지원 여부를 확인합니다. 지원되지 않는 환경에서는 API가 정상 동작하지 않을 수 있으므로 위치 관련 UI와 기능을 비활성화합니다.
- **`null` 위치 처리:** `onChanged(LocationData?)`의 인자는 `null`일 수 있습니다. 사용 전에 반드시 `null` 여부를 확인하고, `null`을 유효한 위치로 처리하지 않습니다.
- **`getLastKnownLocation()`의 의미:** 새 위치를 계산하는 API가 아니라 현재 사용할 수 있는 마지막 위치를 반환합니다. 계산된 위치 데이터가 없으면 `null`이 전달될 수 있습니다.
- **`release()`와 `unregisterFusedLocationCallback()`은 서로 대체 불가:** 전자는 SDK 자체를, 후자는 위치 업데이트 리스너를 해제합니다. 위치 업데이트를 등록했다면 `release()` **전에** `unregisterFusedLocationCallback()`으로 리스너를 해제합니다.
- **`initialize()` 후 `release()` 필수:** `initialize()`를 호출했다면 SDK 사용이 끝날 때 반드시 `release()`를 호출합니다.
- **해제 후 재사용 금지:** `release()` 이후 같은 인스턴스로 API를 호출하지 않습니다. 다시 사용하려면 새 인스턴스를 생성하고 `isSupported()` 확인 후 초기화합니다.
- **동일 리스너 인스턴스:** 업데이트 중지 시 등록에 사용한 동일한 리스너를 `unregisterFusedLocationCallback()`에 전달합니다.
- **위치 접근 차단:** 차량의 개인정보 보호 설정으로 위치 접근이 차단된 경우 `showLocationAccessBlockedPopup()`으로 안내할 수 있습니다. 이 API는 접근을 허용하지 않으므로, `null` 콜백과 `onError(Exception)`를 함께 처리하고 앱의 화면·기능을 적절히 제한해야 합니다.
- **오류 처리:** 예외를 기록하고, 위치를 사용할 수 없는 상태를 사용자에게 안내하거나 기능을 제한합니다.
- **근접 알림 요청 값 검증:** `ProximityAlertRequest`의 위도·경도 범위, 양수 반경, 비어 있지 않은 `transitionTypes` 제약을 지키지 않으면 잘못된 요청으로 `onError`가 전달됩니다.
- **Android 위치 권한 정책 비보장:** Android 표준 위치 권한 및 포그라운드/백그라운드 정책 동작은 이 SDK에서 보장하지 않습니다.

### 5.6 Kotlin 코드 예제

#### 5.6.1 초기화 및 해제 (Application 범위 단일 인스턴스)

```kotlin
class MyApplication : Application() {

    lateinit var fusedLocation: FusedLocation
        private set

    override fun onCreate() {
        super.onCreate()
        // 1. 애플리케이션 생성 단계에서 인스턴스를 생성합니다.
        fusedLocation = FusedLocation(this)

        // 2. 지원 여부를 확인한 후 SDK를 초기화합니다.
        if (fusedLocation.isSupported()) {
            fusedLocation.initialize()
        } else {
            // 위치 관련 UI와 기능을 비활성화합니다.
        }
    }

    fun shutdownFusedLocation(listener: FusedLocationListener) {
        // 3. 등록된 위치 업데이트 리스너를 먼저 해제합니다.
        fusedLocation.unregisterFusedLocationCallback(listener)
        // 4. SDK 사용이 끝나면 SDK 리소스를 해제합니다.
        fusedLocation.release()
    }
}
```

#### 5.6.2 위치 업데이트 수신 및 중지

```kotlin
val listener = object : FusedLocationListener {
    override fun onChanged(location: LocationData?) {
        location ?: return
        // Use the location data.
    }

    override fun onError(exception: Exception) {
        // Handle the error.
    }
}

fusedLocation.registerFusedLocationCallback(listener)

// 더 이상 업데이트가 필요하지 않을 때 (등록 시와 동일한 리스너)
fusedLocation.unregisterFusedLocationCallback(listener)
```

#### 5.6.3 마지막 위치 1회 조회

가이드에는 별도 코드가 없으며, 아래는 API 시그니처(`getLastKnownLocation(callback: FusedLocationListener)`)에 따른 사용 형태입니다.

```kotlin
fusedLocation.getLastKnownLocation(object : FusedLocationListener {
    override fun onChanged(location: LocationData?) {
        location ?: return  // 계산된 위치가 없으면 null일 수 있음
        // location.latitude, location.longitude, ...
    }

    override fun onError(exception: Exception) {
        // Handle the error.
    }
})
```

#### 5.6.4 근접 알림 등록 및 해제

가이드에는 별도 코드가 없으며, 아래는 API Reference의 시그니처와 모델 정의에 따른 사용 형태입니다. 좌표·반경 값은 예시입니다.

```kotlin
val request = ProximityAlertRequest(
    latitude = 37.5665,          // -90 ~ 90
    longitude = 126.9780,        // -180 ~ 180
    radiusMeters = 200f,         // 0보다 커야 함
    transitionTypes = setOf(ProximityTransition.ENTER, ProximityTransition.EXIT) // 비어 있으면 안 됨
)

var registeredAlertId: String? = null

fusedLocation.addProximityAlert(request, object : ProximityAlertListener {
    override fun onRegistered(alertId: String) {
        registeredAlertId = alertId
    }

    override fun onTransition(event: ProximityAlertEvent) {
        when (event.transition) {
            ProximityTransition.ENTER -> { /* 영역 진입 */ }
            ProximityTransition.EXIT -> { /* 영역 이탈 */ }
        }
    }

    override fun onError(exception: Exception) {
        // 등록 또는 처리 실패 (잘못된 요청 포함)
    }
})

// 해제: onRegistered로 받은 ID 사용 (알 수 없거나 이미 제거된 ID는 무시됨)
registeredAlertId?.let { fusedLocation.removeProximityAlert(it) }
```

#### 5.6.5 위치 접근 차단 안내

```kotlin
// 개인정보 보호 설정으로 위치 데이터를 받을 수 없는 경우
fusedLocation.showLocationAccessBlockedPopup()
```
