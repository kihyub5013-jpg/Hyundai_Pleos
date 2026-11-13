package com.yikihyub.drivingpromise.vehicle

import android.content.Context
import android.util.Log
import ai.pleos.playground.navi.constants.BookmarkType
import ai.pleos.playground.navi.constants.CautionPointType
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.data.TBTInfo
import com.yikihyub.drivingpromise.domain.Caution
import com.yikihyub.drivingpromise.domain.Turn
import com.yikihyub.drivingpromise.domain.TurnInfo
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.BookmarkInfo
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RecentDestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** 내비가 안내 중인 목적지와 남은 거리·시간 */
data class NaviDestination(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val remainDistanceM: Int,
    val remainTimeS: Int,
    val tollFareKrw: Int,
    val poiId: String,
)

/** 즐겨찾기·최근 목적지 한 곳. 말로 고른 목적지를 길안내 요청할 때 그대로 넘긴다. */
data class NaviPlace(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val poiId: String = "",
    val poiSubId: String = "",
)

sealed interface NaviEvent {
    /** 경로 탐색 시작. 거리·시간·통행료는 경로 기준 예상치 */
    data class RouteStarted(val name: String, val distanceM: Int, val durationS: Int, val tollFareKrw: Int) : NaviEvent
    /** 목적지 도착. 내비가 판단한 실제 주행 거리·시간 */
    data class Arrived(val name: String, val address: String, val distanceM: Int, val durationS: Int) : NaviEvent
    data object RouteCancelled : NaviEvent
}

/** 주의구간 한 건 (앱 도메인으로 바꾼 것) */
data class CautionAhead(val id: String, val caution: Caution, val remainM: Int)

/**
 * Pleos NaviHelper SDK 래퍼. 현대차그룹 기본 내비의 데이터를 운전 약속 판단에 쓴다.
 *
 * - 목적지·남은 시간 → 해 지기 전 도착 확인, 주차장 천천히
 * - 회전 안내(TBT) → 좌회전·회전교차로 미리 알림, 방향지시등 도우미, 고속도로 진입 기록
 * - 주의구간 → 사고다발·스쿨존·철길 미리 알림
 * - 즐겨찾기·최근 목적지 → 말로 목적지 설정
 * - requestRoute(COMFORTABLE/FREE) → 약속에 맞는 편한 길로 출발
 *
 * 내비 앱이 없거나 연결이 안 되면 모든 값이 null/빈 값이고, 페달 확인 안내 같은 차량 신호 기능은 그대로 동작한다.
 */
class NaviGateway(context: Context) {

    private val tag = "NaviGateway"
    private val navi = NaviHelper(context.applicationContext)

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _status = MutableStateFlow<NavigationStatus?>(null)
    val status: StateFlow<NavigationStatus?> = _status.asStateFlow()

    private val _destination = MutableStateFlow<NaviDestination?>(null)
    val destination: StateFlow<NaviDestination?> = _destination.asStateFlow()

    private val _currentAddress = MutableStateFlow<String?>(null)
    val currentAddress: StateFlow<String?> = _currentAddress.asStateFlow()
    private val addressSeq = MutableStateFlow(0)

    private val _home = MutableStateFlow<NaviPlace?>(null)
    val home: StateFlow<NaviPlace?> = _home.asStateFlow()
    private val _office = MutableStateFlow<NaviPlace?>(null)
    val office: StateFlow<NaviPlace?> = _office.asStateFlow()

    private val _recent = MutableStateFlow<List<NaviPlace>>(emptyList())
    val recent: StateFlow<List<NaviPlace>> = _recent.asStateFlow()

    /** 다음 회전 안내들. 첫 번째가 가장 가까운 회전 */
    private val _turns = MutableStateFlow<List<TurnInfo>>(emptyList())
    val turns: StateFlow<List<TurnInfo>> = _turns.asStateFlow()

    private val _cautions = MutableStateFlow<List<CautionAhead>>(emptyList())
    val cautions: StateFlow<List<CautionAhead>> = _cautions.asStateFlow()

    private val _events = MutableSharedFlow<NaviEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<NaviEvent> = _events.asSharedFlow()

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            _events.tryEmit(NaviEvent.RouteStarted(routeStartInfo.name, routeStartInfo.distance, routeStartInfo.duration, routeStartInfo.tollFare))
            request { navi.getDestinationInfo() }
        }
        override fun onRoutingCompleted() = request { navi.getDestinationInfo() }
        override fun onDestinationChanged() = request { navi.getDestinationInfo() }
        override fun onReRouteEnded() = request { navi.getDestinationInfo() }
        override fun onRouteCancelled() { _destination.value = null; _events.tryEmit(NaviEvent.RouteCancelled) }
        override fun onRouteEnded() { _destination.value = null }
        override fun onDestinationArrived(info: DestinationArrivedInfo) {
            _events.tryEmit(NaviEvent.Arrived(info.name, info.address, info.distance, info.duration))
        }
        override fun onDestinationInfo(d: DestinationInfo) {
            Log.d(tag, "destination name='${d.name}' addr='${d.address}' remain=${d.remainDistance}m toll=${d.tollFare}")
            _destination.value = if (d.name.isBlank() && d.address.isBlank()) null else NaviDestination(
                name = d.name, address = d.address, lat = d.latitude, lng = d.longitude,
                remainDistanceM = d.remainDistance, remainTimeS = d.remainTime, tollFareKrw = d.tollFare, poiId = d.poiId,
            )
        }
        override fun onCurrentLocationInfo(c: CurrentLocationInfo) {
            Log.d(tag, "location addr='${c.address}' ${c.latitude},${c.longitude}")
            _currentAddress.value = c.address.ifBlank { null }
            addressSeq.value = addressSeq.value + 1
        }
        override fun onBookmarkInfo(list: List<BookmarkInfo>) {
            Log.d(tag, "bookmarks ${list.map { "${it.category}:${it.name}" }}")
            // 에뮬레이터는 집/회사 슬롯이 비어 있어도 좌표 0,0 으로 내려온다. 실제 값만 받는다.
            _home.value = list.firstOrNull { it.category == BookmarkType.HOME }?.toPlace()?.takeIf { it.isSet() }
            _office.value = list.firstOrNull { it.category == BookmarkType.OFFICE }?.toPlace()?.takeIf { it.isSet() }
        }
        override fun onBookMarkChanged() = request { navi.getBookmarkInfo() }
        override fun onRecentDestinationInfo(list: List<RecentDestinationInfo>) {
            Log.d(tag, "recent ${list.map { it.name }}")
            _recent.value = list.map { NaviPlace(it.name, it.address, it.latitude, it.longitude, it.poiId ?: "", it.poiSubId ?: "") }
        }
        override fun onNavigationStatus(status: NavigationStatus) {
            Log.d(tag, "status $status")
            _status.value = status
            if (status == NavigationStatus.INACTIVE) _destination.value = null
        }
        override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
            _turns.value = tbtInfoList.map { it.toTurn() }
        }
        override fun onCautionPointInfo(cautionPointList: List<CautionPoint>) {
            _cautions.value = cautionPointList.map { CautionAhead(it.id, it.type.toCaution(), it.remainDistance) }
        }
        override fun onError(errorCode: NaviErrorCode) { Log.w(tag, "navi error: $errorCode") }
    }

    fun start() {
        runCatching {
            navi.initialize(
                { _connected.value = true; navi.addListener(listener); refresh() },
                { t -> Log.w(tag, "navi connect failed: ${t.message}"); _connected.value = false },
            )
        }.onFailure { Log.w(tag, "navi init threw: ${it.message}") }
    }

    /** 목적지·현재 주소·즐겨찾기·최근 목적지를 한 번에 다시 요청한다. 결과는 리스너로 온다. */
    fun refresh() {
        if (!_connected.value) return
        request { navi.getDestinationInfo() }
        request { navi.getCurrentLocationInfo() }
        request { navi.getBookmarkInfo() }
        request { navi.getRecentDestinationInfo() }
        request { navi.getTBTInfo() }
    }

    /** 주행 중 1~2초마다: 남은 거리·회전 안내 갱신 (TBT 는 푸시가 없는 내비도 있어서 당겨온다) */
    fun poll() {
        if (!_connected.value) return
        request { navi.getDestinationInfo() }
        request { navi.getTBTInfo() }
    }

    /** 현재 위치 주소를 새로 받아 돌려준다. [timeoutMs] 안에 안 오면 마지막 값. */
    suspend fun fetchCurrentAddress(timeoutMs: Long = 1_500): String? {
        if (!_connected.value) return null
        val seq = addressSeq.value
        request { navi.getCurrentLocationInfo() }
        withTimeoutOrNull(timeoutMs) { addressSeq.first { it != seq } }
        return _currentAddress.value
    }

    /** 앱에서 내비로 길안내 요청. 약속에 따라 편한 길(COMFORTABLE)·무료도로(FREE) 옵션을 넘긴다. */
    fun navigateTo(p: NaviPlace, option: RouteOption = RouteOption.RECOMMENDED): Boolean {
        if (!_connected.value) return false
        return runCatching {
            navi.requestRoute(
                RouteInfo(
                    latitude = p.lat, longitude = p.lng,
                    displayLatitude = null, displayLongitude = null,
                    poiName = p.name, poiId = p.poiId, address = p.address, poiSubId = p.poiSubId,
                    routeOption = option,
                ),
            )
        }.onFailure { Log.w(tag, "requestRoute failed: ${it.message}") }.isSuccess
    }

    /** 이미 안내 중인 경로의 옵션만 바꾼다 (내비에서 직접 목적지를 정한 경우) */
    fun changeRouteOption(option: RouteOption) = request { navi.changeRouteOption(option) }

    fun release() {
        runCatching { navi.removeListener(listener); navi.release() }
        _connected.value = false
    }

    private fun request(block: () -> Unit) {
        runCatching(block).onFailure { Log.w(tag, "navi request failed: ${it.message}") }
    }

    private fun TBTInfo.toTurn(): TurnInfo {
        val n = type.name
        val turn = when {
            type == TurnType.TURN_LEFT || type == TurnType.TURN_SHARP_L -> Turn.LEFT
            type == TurnType.TURN_RIGHT || type == TurnType.TURN_SHARP_R -> Turn.RIGHT
            type == TurnType.TURN_UTURN -> Turn.UTURN
            n.startsWith("TURN_ENTER_ROUNDABOUT") -> Turn.ROUNDABOUT
            n.startsWith("TURN_ENTER_IC") -> Turn.HIGHWAY_IN
            type == TurnType.TURN_ARRIVE -> Turn.ARRIVE
            else -> Turn.OTHER
        }
        val clock = if (turn == Turn.ROUNDABOUT) n.takeLast(2).toIntOrNull() else null
        return TurnInfo(turn, distance, description, clock)
    }

    private fun CautionPointType.toCaution() = when (this) {
        CautionPointType.ACCIDENT_PRONE_AREA -> Caution.ACCIDENT_PRONE
        CautionPointType.SCHOOL_ZONE -> Caution.SCHOOL_ZONE
        CautionPointType.RAILROAD_CROSSING -> Caution.RAILROAD
        CautionPointType.SPEED_BUMP -> Caution.SPEED_BUMP
        CautionPointType.FOG_AREA -> Caution.FOG
        CautionPointType.DOWNHILL_CAUTION_AREA -> Caution.DOWNHILL
        CautionPointType.NARROW_ROAD -> Caution.NARROW
        CautionPointType.SLIPPERY_ROAD_ALERT -> Caution.SLIPPERY
        CautionPointType.WRONG_WAY_DRIVING_ALERT -> Caution.WRONG_WAY
        else -> Caution.OTHER
    }

    private fun NaviPlace.isSet() = (lat != 0.0 || lng != 0.0) && (name.isNotBlank() || address.isNotBlank())

    private fun BookmarkInfo.toPlace() = NaviPlace(name, address, latitude, longitude, poiId ?: "", poiSubId ?: "")
}
