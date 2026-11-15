package ai.projectshu.musicdiary.pleosdemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInitializeListener
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.Locale

/** Public demonstration only. No server API key, vehicle writes, or remote WebView content. */
class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var tts: TextToSpeech
    private lateinit var stt: SpeechToText
    private lateinit var vehicle: Vehicle
    private lateinit var audioManager: AudioManager
    private val main = Handler(Looper.getMainLooper())
    private var pageReady = false
    private var initialized = false
    private var alive = true
    private var foreground = true
    @Volatile private var ttsReady = false
    @Volatile private var sttReady = false
    @Volatile private var vehicleConnected = false
    @Volatile private var listening = false
    @Volatile private var speaking = false
    @Volatile private var musicActive = false
    @Volatile private var musicPlaying = false
    @Volatile private var positionMs = 0
    @Volatile private var durationMs = 0
    private var pendingListen = false
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var focusOwner: String? = null
    private val musicProgress = object : Runnable {
        override fun run() {
            updateMusicProgress()
            if (alive && musicActive && player != null) main.postDelayed(this, 1000)
        }
    }
    private var vehicleRequest = 0
    private var car: Car? = null
    private var uxManager: CarUxRestrictionsManager? = null
    @Volatile private var uxRestricted: Boolean? = null
    @Volatile private var activeRestrictions: Int? = null
    @Volatile private var carApiAvailable: Boolean? = null

    private val speechAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val musicAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

    private val ttsEvents = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() {
            speaking = true
            emit("speech-start")
        }
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() = speechEnded(false)
        override fun onStop() = speechEnded(true)
        override fun onError(errorType: TtsError) {
            speaking = false
            main.post { if (focusOwner == "speech") abandonFocus() }
            error("tts", errorType.name)
        }
    }

    private val sttEvents = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() {
            listening = true
            emit("listening", "active" to true)
        }
        override fun onUpdated(stt: String, completed: Boolean) {
            // Do not log transcript content to Logcat or an analytics service.
            emit("transcript", "text" to stt, "final" to completed)
        }
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onEndedRecognition() {
            listening = false
            emit("listening", "active" to false)
        }
        override fun onError(errorType: SttError) {
            listening = false
            error("stt", errorType.name)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(AudioManager::class.java)
        WebView.setWebContentsDebuggingEnabled(
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        )
        webView = WebView(this)
        setContentView(webView)
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
        }
        webView.addJavascriptInterface(NativeBridge(), "PleosNative")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                !isLocal(request.url)
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                if (isLocal(request.url)) {
                    loader.shouldInterceptRequest(request.url)?.let {
                        // Android MimeTypeMap versions do not all recognize ES-module .mjs files.
                        if (request.url.path?.endsWith(".mjs") == true) it.mimeType = "text/javascript"
                        return it
                    }
                }
                return WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(),
                    ByteArrayInputStream("Local demo assets only".toByteArray()))
            }
            override fun onPageFinished(view: WebView, url: String) {
                if (!isLocal(Uri.parse(url))) return
                pageReady = true
                if (!initialized) initializeSdk()
                publishStatus()
            }
        }
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    private fun isLocal(uri: Uri): Boolean = uri.scheme == "https" &&
        uri.host == "appassets.androidplatform.net" && uri.path?.startsWith("/assets/") == true

    private fun initializeSdk() {
        initialized = true
        // ON_DEVICE explicitly prevents silent fallback to a paid/server mode.
        tts = TextToSpeech(applicationContext, TtsMode.ON_DEVICE)
        stt = SpeechToText(applicationContext, SttMode.ON_DEVICE)
        vehicle = Vehicle(applicationContext)
        tts.addEventListener(ttsEvents)
        stt.addListener(sttEvents)
        tts.initialize(object : TtsInitializeListener {
            override fun onSuccess() {
                if (!alive) return
                ttsReady = true
                tts.setLocale(Locale.KOREAN)
                tts.setAudioAttributes(speechAttributes)
                publishStatus()
            }
            override fun onFailure(errorType: TtsError) = error("tts-init", errorType.name)
        })
        stt.initialize(object : SttInitializeListener {
            override fun onSuccess() {
                if (!alive) return
                sttReady = true
                stt.setLocale(Locale.KOREAN)
                publishStatus()
            }
            override fun onFailure(errorType: SttError) = error("stt-init", errorType.name)
        })
        runCatching { vehicle.initialize() }.onFailure { error("vehicle-init", it.javaClass.simpleName) }
        // Some AAOS builds supply car classes through the bootclasspath, not a named shared library.
        // Keep actual UX restrictions unknown if those classes cannot be loaded.
        carApiAvailable = runCatching {
            Class.forName("android.car.Car", false, classLoader)
            Class.forName("android.car.drivingstate.CarUxRestrictionsManager", false, classLoader)
        }.isSuccess
        if (carApiAvailable == true) {
            runCatching { initializeCarUx() }.onFailure { error("car-ux", it.javaClass.simpleName) }
        } else {
            error("car-ux", "CAR_API_UNAVAILABLE")
            publishStatus()
        }
        main.postDelayed({ if (alive) queryVehicle() }, 1200)
    }

    @Suppress("DEPRECATION") // Asynchronous ServiceConnection overload also supports API 28.
    private fun initializeCarUx() {
        runCatching {
            car = Car.createCar(this, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    if (!alive) return
                    runCatching {
                        uxManager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
                        uxManager?.registerListener { updateUx(it) }
                        uxManager?.currentCarUxRestrictions?.let { updateUx(it) }
                    }.onFailure { error("car-ux", it.javaClass.simpleName) }
                }
                override fun onServiceDisconnected(name: ComponentName) {
                    uxManager = null
                    uxRestricted = null
                    activeRestrictions = null
                    emit("ux-restrictions", "restricted" to JSONObject.NULL,
                        "activeRestrictions" to JSONObject.NULL, "source" to "aaos-car-service")
                    publishStatus()
                }
            }, main)
            car?.connect()
        }.onFailure { error("car-ux", it.javaClass.simpleName) }
    }

    private fun updateUx(restrictions: CarUxRestrictions) {
        uxRestricted = restrictions.isRequiresDistractionOptimization
        activeRestrictions = restrictions.activeRestrictions
        // UI allowance comes from AAOS UX policy, never from brake pedal inference.
        emit("ux-restrictions", "restricted" to uxRestricted,
            "activeRestrictions" to activeRestrictions, "source" to "aaos-car-service")
        publishStatus()
    }

    @SuppressLint("MissingPermission") // Custom Pleos permission is declared; SDK reports permission denial asynchronously.
    private fun queryVehicle() {
        if (!alive || !initialized) return
        val request = ++vehicleRequest
        val brake = vehicle.getBrake()
        main.postDelayed({
            if (alive && request == vehicleRequest && !vehicleConnected) {
                emit("vehicle", "connected" to false, "parked" to JSONObject.NULL,
                    "source" to "pleos-sdk", "reason" to "Vehicle service has not returned a value")
            }
        }, 5000)
        brake.checkBrakePedalStatusCapabilities({ capability ->
            if (!alive) return@checkBrakePedalStatusCapabilities
            if (capability.accessType != AccessType.READ && capability.accessType != AccessType.READ_WRITE) {
                error("vehicle", "BRAKE_READ_UNAVAILABLE")
                return@checkBrakePedalStatusCapabilities
            }
            brake.getCurrentBrakePedalStatus({ pressed ->
                if (!alive) return@getCurrentBrakePedalStatus
                vehicleConnected = true
                // A brake pedal value is NOT proof of parking, so parked remains unknown.
                emit("vehicle", "connected" to true, "brakePressed" to pressed,
                    "parked" to JSONObject.NULL, "source" to "pleos-sdk")
                publishStatus()
            }, { error("vehicle-read", it.javaClass.simpleName) })
        }, { error("vehicle-capability", it.javaClass.simpleName) })
    }

    inner class NativeBridge {
        @JavascriptInterface fun getStatus(): String = status().toString()
        @JavascriptInterface fun refreshVehicle() { main.post { queryVehicle() } }
        @JavascriptInterface fun speak(text: String) {
            main.post {
                if (!foreground || !ttsReady) { error("tts", "SERVICE_NOT_READY"); return@post }
                if (text.isBlank() || text.length > 1500) { error("tts", "INVALID_TEXT"); return@post }
                stopListeningNow()
                pauseMusic()
                if (!requestFocus(speechAttributes, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, "speech")) return@post
                speaking = true
                runCatching { tts.speak(text) }.onFailure {
                    speaking = false
                    if (focusOwner == "speech") abandonFocus()
                    error("tts", it.javaClass.simpleName)
                }
            }
        }
        @JavascriptInterface fun listen() { main.post { startListening() } }
        @JavascriptInterface fun stopListening() { main.post { stopListeningNow() } }
        @JavascriptInterface fun stopSpeaking() { main.post { if (ttsReady) tts.stop() } }
        @JavascriptInterface fun playAudio(asset: String) { main.post { playMusic(asset) } }
        @JavascriptInterface fun pauseAudio() { main.post { pauseMusic() } }
        @JavascriptInterface fun stopAudio() { main.post { releasePlayer() } }
    }

    private fun startListening() {
        if (!foreground || !sttReady) { error("stt", "SERVICE_NOT_READY"); return }
        if (speaking || musicActive) { error("stt", "AUDIO_OUTPUT_ACTIVE"); return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingListen = true
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41)
            return
        }
        if (listening) return
        listening = true
        runCatching { stt.request() }.onFailure {
            listening = false
            error("stt", it.javaClass.simpleName)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 41) return
        val resume = pendingListen
        pendingListen = false
        if (resume && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
        else error("microphone", "PERMISSION_DENIED")
    }

    private fun stopListeningNow() {
        pendingListen = false
        if (sttReady) runCatching { stt.stop() }
        listening = false
        emit("listening", "active" to false)
    }

    private fun speechEnded(stopped: Boolean) {
        speaking = false
        // A late TTS onStop must not abandon the subsequent music request's focus.
        main.post { if (focusOwner == "speech") abandonFocus() }
        emit("speech-end", "stopped" to stopped)
    }

    private fun playMusic(asset: String) {
        if (!foreground || !Regex("assets/[A-Za-z0-9_.-]+\\.(mp3|m4a|ogg|wav)").matches(asset)) {
            error("music", "INVALID_LOCAL_ASSET"); return
        }
        stopListeningNow()
        if (ttsReady) tts.stop()
        releasePlayer()
        if (!requestFocus(musicAttributes, AudioManager.AUDIOFOCUS_GAIN, "music")) return
        musicActive = true
        runCatching {
            val next = MediaPlayer()
            player = next
            next.setAudioAttributes(musicAttributes)
            assets.openFd(asset).use { next.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            next.setOnPreparedListener {
                if (player === it && musicActive && foreground) {
                    it.start()
                    updateMusicProgress()
                    emit("music-start", "asset" to asset, "durationMs" to durationMs)
                    main.removeCallbacks(musicProgress)
                    main.postDelayed(musicProgress, 1000)
                }
            }
            next.setOnCompletionListener { emit("music-end"); releasePlayer() }
            next.setOnErrorListener { _, what, extra ->
                error("music", "$what:$extra"); releasePlayer(); true
            }
            next.prepareAsync()
        }.onFailure { error("music", it.javaClass.simpleName); releasePlayer() }
    }

    private fun pauseMusic() {
        main.removeCallbacks(musicProgress)
        musicActive = false
        runCatching { player?.pause() }
        updateMusicProgress()
        abandonFocus()
        emit("music-pause")
    }

    private fun releasePlayer() {
        main.removeCallbacks(musicProgress)
        musicActive = false
        runCatching { player?.release() }
        player = null
        musicPlaying = false
        positionMs = 0
        durationMs = 0
        emit("music-progress", "positionMs" to positionMs, "durationMs" to durationMs,
            "musicPlaying" to false)
        abandonFocus()
    }

    /** Called only on the main thread; JS receives cached values without touching MediaPlayer. */
    private fun updateMusicProgress() {
        val current = player
        musicPlaying = current != null && runCatching { current.isPlaying }.getOrDefault(false)
        if (current != null) {
            positionMs = runCatching { current.currentPosition }.getOrDefault(positionMs).coerceAtLeast(0)
            durationMs = runCatching { current.duration }.getOrDefault(durationMs).coerceAtLeast(0)
        }
        emit("music-progress", "positionMs" to positionMs, "durationMs" to durationMs,
            "musicPlaying" to musicPlaying)
    }

    private fun requestFocus(attributes: AudioAttributes, gain: Int, owner: String): Boolean {
        abandonFocus()
        val request = AudioFocusRequest.Builder(gain).setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener({ change ->
                if (change < 0) {
                    pauseMusic()
                    stopListeningNow()
                    if (ttsReady) tts.stop()
                    emit("audio-focus-lost")
                }
            }, main).build()
        focus = request
        focusOwner = owner
        if (audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return true
        error("audio", "FOCUS_NOT_GRANTED")
        return false
    }

    private fun abandonFocus() {
        focus?.let { audioManager.abandonAudioFocusRequest(it) }
        focus = null
        focusOwner = null
    }

    private fun status() = JSONObject().put("transport", "pleos-native")
        .put("ttsReady", ttsReady).put("sttReady", sttReady)
        .put("vehicleConnected", vehicleConnected).put("parked", JSONObject.NULL)
        .put("uxRestricted", uxRestricted ?: JSONObject.NULL)
        .put("activeRestrictions", activeRestrictions ?: JSONObject.NULL)
        .put("carApiAvailable", carApiAvailable ?: JSONObject.NULL)
        .put("musicPlaying", musicPlaying).put("positionMs", positionMs).put("durationMs", durationMs)
        .put("mode", "ON_DEVICE").put("speaking", speaking).put("listening", listening)

    private fun publishStatus() {
        emit("ready", "ttsReady" to ttsReady, "sttReady" to sttReady,
            "vehicleConnected" to vehicleConnected, "parked" to JSONObject.NULL,
            "uxRestricted" to uxRestricted, "activeRestrictions" to activeRestrictions,
            "carApiAvailable" to carApiAvailable,
            "transport" to "pleos-native", "mode" to "ON_DEVICE")
    }

    private fun error(component: String, code: String) = emit("error", "component" to component, "code" to code)

    private fun emit(type: String, vararg fields: Pair<String, Any?>) {
        val event = JSONObject().put("type", type)
        fields.forEach { event.put(it.first, it.second ?: JSONObject.NULL) }
        main.post {
            if (alive && pageReady) webView.evaluateJavascript(
                "window.onPleosEvent && window.onPleosEvent(${event});", null)
        }
    }

    override fun onPause() {
        foreground = false
        stopListeningNow()
        if (ttsReady) tts.stop()
        pauseMusic()
        emit("suspended")
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        if (initialized) publishStatus()
    }

    override fun onDestroy() {
        alive = false
        main.removeCallbacksAndMessages(null)
        if (initialized) {
            runCatching { stt.stop(); stt.removeListener(sttEvents); stt.release() }
            runCatching { tts.stop(); tts.removeEventListener(ttsEvents); tts.release() }
            runCatching { vehicle.release() }
        }
        runCatching { uxManager?.unregisterListener(); car?.disconnect() }
        releasePlayer()
        webView.removeJavascriptInterface("PleosNative")
        webView.destroy()
        super.onDestroy()
    }
}
