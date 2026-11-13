package com.yikihyub.drivingpromise.data

import android.content.Context
import android.content.SharedPreferences
import com.yikihyub.drivingpromise.domain.Promise
import com.yikihyub.drivingpromise.domain.Sensitivity
import com.yikihyub.drivingpromise.domain.SurpriseSpot
import com.yikihyub.drivingpromise.domain.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class AppSettings(
    val promises: Set<Promise> = emptySet(),
    val sensitivity: Sensitivity = Sensitivity.NORMAL,
    /** 페달 확인 안내 켜기. 기본 켬 */
    val pedalGuard: Boolean = true,
    /** 본인이 켤 때만 가족에게 월간 요약·도착 알림 (서버 전송은 3단계) */
    val familyShare: Boolean = false,
    val familyName: String = "",
    /** 처음 실행 안내를 봤는지 */
    val onboarded: Boolean = false,
)

/** 설정은 SharedPreferences. 운행 기록·깜짝 지점은 차 안 파일(JSON)에만 저장한다. */
class Store(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("promise", Context.MODE_PRIVATE)
    private val tripsFile = File(context.filesDir, "trips.json")
    private val spotsFile = File(context.filesDir, "spots.json")
    private val visitsFile = File(context.filesDir, "visits.json")

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _trips = MutableStateFlow(loadTrips())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    private val _spots = MutableStateFlow(loadSpots())
    val spots: StateFlow<List<SurpriseSpot>> = _spots.asStateFlow()

    private val visits: MutableMap<String, Int> = loadVisits()

    fun setPromises(p: Set<Promise>) = save(_settings.value.copy(promises = p))
    fun togglePromise(p: Promise) = setPromises(_settings.value.promises.let { if (p in it) it - p else it + p })
    fun setSensitivity(s: Sensitivity) = save(_settings.value.copy(sensitivity = s))
    fun setPedalGuard(on: Boolean) = save(_settings.value.copy(pedalGuard = on))
    fun setFamilyShare(on: Boolean) = save(_settings.value.copy(familyShare = on))
    fun setFamilyName(n: String) = save(_settings.value.copy(familyName = n.trim()))
    fun setOnboarded() = save(_settings.value.copy(onboarded = true))

    @Synchronized
    fun addTrip(t: Trip) {
        _trips.value = (_trips.value + t).takeLast(1000)
        tripsFile.writeText(JSONArray(_trips.value.map { it.toJson() }).toString())
    }

    @Synchronized
    fun addSpot(s: SurpriseSpot): Boolean {
        val merged = SurpriseSpot.merge(_spots.value, s)
        if (merged.size == _spots.value.size) return false
        _spots.value = merged.takeLast(200)
        spotsFile.writeText(JSONArray(_spots.value.map { it.toJson() }).toString())
        return true
    }

    @Synchronized
    fun clearSpots() { _spots.value = emptyList(); spotsFile.delete() }

    /** 목적지 방문 횟수. 두 번째부터는 "한 번 가본 길" */
    @Synchronized
    fun visitCount(dest: String): Int = visits[dest.trim()] ?: 0

    @Synchronized
    fun addVisit(dest: String) {
        if (dest.isBlank()) return
        visits[dest.trim()] = visitCount(dest) + 1
        visitsFile.writeText(JSONObject(visits as Map<*, *>).toString())
    }

    private fun save(s: AppSettings) {
        prefs.edit()
            .putString("promises", s.promises.joinToString(",") { it.name })
            .putString("sensitivity", s.sensitivity.name)
            .putBoolean("pedalGuard", s.pedalGuard)
            .putBoolean("familyShare", s.familyShare)
            .putString("familyName", s.familyName)
            .putBoolean("onboarded", s.onboarded)
            .apply()
        _settings.value = s
    }

    private fun loadSettings() = AppSettings(
        promises = prefs.getString("promises", "").orEmpty().split(",").mapNotNull { n -> Promise.entries.firstOrNull { it.name == n } }.toSet(),
        sensitivity = Sensitivity.entries.firstOrNull { it.name == prefs.getString("sensitivity", null) } ?: Sensitivity.NORMAL,
        pedalGuard = prefs.getBoolean("pedalGuard", true),
        familyShare = prefs.getBoolean("familyShare", false),
        familyName = prefs.getString("familyName", "").orEmpty(),
        onboarded = prefs.getBoolean("onboarded", false),
    )

    private fun loadTrips(): List<Trip> = readArray(tripsFile).map { it.toTrip() }
    private fun loadSpots(): List<SurpriseSpot> = readArray(spotsFile).map { it.toSpot() }
    private fun loadVisits(): MutableMap<String, Int> = runCatching {
        val o = JSONObject(visitsFile.readText()); o.keys().asSequence().associateWith { o.getInt(it) }.toMutableMap()
    }.getOrDefault(mutableMapOf())

    private fun readArray(f: File): List<JSONObject> = runCatching {
        if (!f.exists()) return emptyList()
        val a = JSONArray(f.readText()); (0 until a.length()).map { a.getJSONObject(it) }
    }.getOrDefault(emptyList())
}

internal fun Trip.toJson() = JSONObject()
    .put("start", startMs).put("end", endMs).put("km", distanceKm).put("dest", destination)
    .put("night", night).put("rain", rain).put("highway", highway).put("stint", longestStintMin)
    .put("companion", companion ?: JSONObject.NULL).put("pedal", pedalAlerts)

internal fun JSONObject.toTrip() = Trip(
    startMs = getLong("start"), endMs = getLong("end"), distanceKm = optDouble("km", 0.0), destination = optString("dest"),
    night = optBoolean("night"), rain = optBoolean("rain"), highway = optBoolean("highway"), longestStintMin = optInt("stint"),
    companion = if (isNull("companion") || !has("companion")) null else getBoolean("companion"), pedalAlerts = optInt("pedal"),
)

internal fun SurpriseSpot.toJson() = JSONObject().put("lat", lat).put("lng", lng).put("t", timeMs).put("reason", reason).put("place", place)
internal fun JSONObject.toSpot() = SurpriseSpot(getDouble("lat"), getDouble("lng"), getLong("t"), optString("reason"), optString("place"))
