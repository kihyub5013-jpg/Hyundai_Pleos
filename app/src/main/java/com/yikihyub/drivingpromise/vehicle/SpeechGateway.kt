package com.yikihyub.drivingpromise.vehicle

import android.content.Context
import android.util.Log
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.OnServerConnectionListener
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode

/**
 * Pleos SpeechToText / TextToSpeech 래퍼.
 *
 * 모든 안내는 음성이 먼저다. "한 마디 듣고 텍스트로 돌려주는" [listenOnce] 와 "한 줄 읽어주는" [say] 두 개만 노출한다.
 *
 * - TTS 는 ON_DEVICE: 페달 확인 안내는 네트워크를 기다리면 안 된다.
 * - STT 는 .env 의 Client ID/Secret 이 있으면 HYBRID + registerApp (목적지 이름 인식률), 없으면 ON_DEVICE.
 *   에뮬레이터에서 서버 인식을 쓰려면 scripts/set-devbox-crn.sh 로 CRN 을 먼저 넣어야 한다.
 */
class SpeechGateway(context: Context, private val clientId: String = "", private val clientSecret: String = "") {

    private val tag = "SpeechGateway"

    companion object {
        /** 한 번 듣기 최대 길이. "집에 가자" 같은 한 마디면 충분하다. */
        const val DEFAULT_LISTEN_TIMEOUT_MS = 8_000L
    }

    private val useServer = clientId.isNotBlank() && clientSecret.isNotBlank()
    private val stt = SpeechToText(context.applicationContext, if (useServer) SttMode.HYBRID else SttMode.ON_DEVICE)

    /** 서버 인식 연결 여부 (HYBRID 일 때만 의미) */
    private val _sttServer = MutableStateFlow(false)
    val sttServer: StateFlow<Boolean> = _sttServer.asStateFlow()
    private val tts = TextToSpeech(context.applicationContext, TtsMode.ON_DEVICE)

    private val _sttReady = MutableStateFlow(false)
    val sttReady: StateFlow<Boolean> = _sttReady.asStateFlow()

    private val _ttsReady = MutableStateFlow(false)
    val ttsReady: StateFlow<Boolean> = _ttsReady.asStateFlow()

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    /** 인식 중간 결과(화면에 실시간 표시용) */
    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial.asStateFlow()

    fun start() {
        runCatching {
            stt.initialize(object : ai.pleos.playground.stt.listener.OnInitializeListener {
                override fun onSuccess() {
                    _sttReady.value = true
                    runCatching { stt.setLocale(Locale.KOREA) }
                    if (useServer) registerStt()
                }
                override fun onFailure(errorType: SttError) { Log.w(tag, "stt init: $errorType") }
            })
        }.onFailure { Log.w(tag, "stt init threw: ${it.message}") }
        runCatching {
            tts.initialize(object : ai.pleos.playground.tts.listener.OnInitializeListener {
                override fun onSuccess() { _ttsReady.value = true; runCatching { tts.setLocale(Locale.KOREA) } }
                override fun onFailure(errorType: TtsError) { Log.w(tag, "tts init: $errorType") }
            })
        }.onFailure { Log.w(tag, "tts init threw: ${it.message}") }
    }

    private fun registerStt() {
        runCatching {
            stt.registerApp(clientId, clientSecret, object : OnServerConnectionListener {
                override fun onConnected() { _sttServer.value = true; Log.i(tag, "stt server connected") }
                override fun onFailed(errorType: SttError) { _sttServer.value = false; Log.w(tag, "stt registerApp: $errorType (온디바이스로 계속)") }
            })
        }.onFailure { Log.w(tag, "stt registerApp threw: ${it.message}") }
    }

    /**
     * 마이크를 켜고 한 문장을 받아 최종 텍스트를 돌려준다. 실패/미지원이면 null.
     * 호출 측이 취소하면 stop() 으로 마이크를 놓는다.
     *
     * [timeoutMs] 안에 인식이 끝나지 않으면 마이크를 놓고 그때까지 들은 부분 텍스트를 돌려준다.
     * (Pleos 에뮬레이터처럼 마이크 입력이 없는 환경에서는 온디바이스 STT가 끝점을 못 찾고
     * 무한정 듣기 때문에 반드시 필요하다.)
     */
    suspend fun listenOnce(timeoutMs: Long = DEFAULT_LISTEN_TIMEOUT_MS): String? {
        if (!_sttReady.value) return null
        _partial.value = ""
        val heard = withTimeoutOrNull(timeoutMs) { awaitRecognition() }
        if (heard == null && _listening.value) {
            Log.i(tag, "stt timeout after ${timeoutMs}ms, releasing mic")
            _listening.value = false
        }
        return heard ?: _partial.value.ifBlank { null }
    }

    private suspend fun awaitRecognition(): String? {
        return suspendCancellableCoroutine { cont ->
            var finalText: String? = null
            val listener = object : ResultListener {
                override fun onReady() = Unit
                override fun onStartedRecognition() { _listening.value = true; _partial.value = "" }
                override fun onUpdated(stt: String, completed: Boolean) {
                    _partial.value = stt
                    if (completed) finalText = stt
                }
                override fun onUpdatedEpdData(on: Long, off: Long) = Unit
                override fun onUpdatedRms(rms: Double) = Unit
                override fun onEndedRecognition() {
                    _listening.value = false
                    stt.removeListener(this)
                    if (cont.isActive) cont.resume(finalText ?: _partial.value.ifBlank { null })
                }
                override fun onError(errorType: SttError) {
                    Log.w(tag, "stt error: $errorType")
                    _listening.value = false
                    stt.removeListener(this)
                    if (cont.isActive) cont.resume(null)
                }
            }
            stt.addListener(listener)
            runCatching { stt.request() }.onFailure {
                stt.removeListener(listener)
                if (cont.isActive) cont.resume(null)
            }
            cont.invokeOnCancellation {
                runCatching { stt.stop() }
                stt.removeListener(listener)
                _listening.value = false
            }
        }
    }

    fun stopListening() = runCatching { stt.stop() }

    /** 짧은 안내 문장을 읽어준다. TTS가 없으면 조용히 무시 (화면 안내로 대체). 취소하면 즉시 멈춘다. */
    suspend fun say(text: String) {
        if (!_ttsReady.value) return
        suspendCancellableCoroutine<Unit> { cont ->
            val listener = object : EventListener {
                override fun onStart() = Unit
                override fun onReady() = Unit
                override fun onUpdatedRms(rms: Double) = Unit
                override fun onDone() { tts.removeEventListener(this); if (cont.isActive) cont.resume(Unit) }
                override fun onStop() { tts.removeEventListener(this); if (cont.isActive) cont.resume(Unit) }
                override fun onError(errorType: TtsError) {
                    Log.w(tag, "tts error: $errorType")
                    tts.removeEventListener(this); if (cont.isActive) cont.resume(Unit)
                }
            }
            tts.addEventListener(listener)
            runCatching { tts.speak(text, true) }.onFailure {
                tts.removeEventListener(listener); if (cont.isActive) cont.resume(Unit)
            }
            cont.invokeOnCancellation { runCatching { tts.stop() }; tts.removeEventListener(listener) }
        }
    }

    fun release() {
        runCatching { stt.release() }
        runCatching { tts.release() }
        _sttReady.value = false
        _ttsReady.value = false
    }
}
