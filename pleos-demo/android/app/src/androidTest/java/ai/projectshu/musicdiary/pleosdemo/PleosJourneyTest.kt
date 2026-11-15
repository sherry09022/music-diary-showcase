package ai.projectshu.musicdiary.pleosdemo

import android.media.AudioManager
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real Android accessibility actions only. No DOM evaluation, JS state writes, SDK mocks, or VHAL writes. */
@RunWith(AndroidJUnit4::class)
class PleosJourneyTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var bridge: MainActivity.NativeBridge
    private lateinit var bridgeActivity: MainActivity
    @Volatile private var lifecycleSnapshot: Pair<MainActivity, Stage>? = null
    private val events = JSONArray()
    private lateinit var evidence: File

    @Test
    fun parkedNativeJourneyWithExampleAnswers() {
        assertEquals("Explicit runtime gate is required", "true",
            InstrumentationRegistry.getArguments().getString("allowPleosJourney"))
        evidence = File(instrumentation.targetContext.filesDir, "journey-evidence")
        assertTrue("Private evidence directory must be writable", evidence.isDirectory || evidence.mkdirs())
        val configuration = Configurator.getInstance()
        val previousIdleTimeout = configuration.waitForIdleTimeout
        val previousSelectorTimeout = configuration.waitForSelectorTimeout
        // Animated Canvas never becomes completely idle; observe actual UI outcomes instead.
        configuration.setWaitForIdleTimeout(1000).setWaitForSelectorTimeout(0)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val lifecycleMonitor = ActivityLifecycleMonitorRegistry.getInstance()
        val lifecycleCallback = ActivityLifecycleCallback { activity, stage ->
            if (::bridgeActivity.isInitialized && activity === bridgeActivity) {
                lifecycleSnapshot = bridgeActivity to stage
            }
        }
        try {
            // Capture the real bridge once. Its status reads only volatile cached values;
            // both onActivity and ActivityScenario.getState() wait for UI idle on every call.
            scenario.onActivity {
                bridgeActivity = it
                lifecycleMonitor.addLifecycleCallback(lifecycleCallback)
                // Lifecycle queries and callbacks run on the main thread. Never assume RESUMED.
                lifecycleSnapshot = it to lifecycleMonitor.getLifecycleStageOf(it)
                bridge = it.NativeBridge()
            }
            // Read the actual native bridge state; never replace services or vehicle restrictions.
            waitFor("real Pleos SDK and unrestricted AAOS UX", 45000) { ready(sdkStatus()) }
            record("00-native-ready")
            tap("직접 체험", "30대")
            tap("30대", "말하지 않기")
            tap("말하지 않기", "피아노 연주곡을 즐겨 들어요")
            tap("피아노 연주곡을 즐겨 들어요", "first track choice") { firstTrackExists() }
            record("01-onboarding-taste")
            tapFirstTrack()
            waitMusicOutput()
            hold(6000)
            record("02-taste-preview")
            tap("이 곡으로 감상하기", "오늘 일이 많아서 피곤해요")
            tap("오늘 일이 많아서 피곤해요", "살짝 슬픈 것 같아요")
            tap("살짝 슬픈 것 같아요", "휴식이 필요해요")
            tap("휴식이 필요해요", "등에서 따스함이 느껴져요")
            tap("등에서 따스함이 느껴져요", "personal track choices") { firstTrackExists() }
            record("03-personal-recommendation")
            tapFirstTrack()
            waitMusicOutput()
            hold(6000)
            record("04-music-preview")
            tap("이 곡으로 감상하기", "감상 마치기")
            waitFor("actual native music-start UI", 15000) {
                device.findObject(UiSelector().text("음악 잠시 멈춤")).exists() ||
                    device.findObject(UiSelector().description("음악 잠시 멈춤")).exists()
            }
            waitMusicOutput()
            hold(10000)
            record("05-listening")
            tap("감상 마치기", "포근한 이불의 느낌이 생각났어요")
            tap("포근한 이불의 느낌이 생각났어요", "이 이미지로 일기 저장")
            record("06-imagery")
            tap("이 이미지로 일기 저장", "일기 다시 보기")
            assertTrue("Saved diary action must appear", findVisible("일기 다시 보기", 10000) != null)
            record("07-diary-saved")
            tap("일기 다시 보기", "saved diary list status") {
                device.findObject(UiSelector().textContains("개의 일기가 이 브라우저에 저장")).exists()
            }
            record("08-diary-reopened")
            hold(3000)
            File(evidence, "result.json").writeText(JSONObject()
                .put("passed", true).put("interaction", "real Android accessibility button actions")
                .put("sdkState", sdkStatus()).put("steps", events)
                .put("audioStreamActiveObserved", true)
                .put("ownMediaPlayerPositionAdvanced", true)
                .put("acousticOutputVerified", false).put("microphoneRecognitionVerified", false)
                .put("image", "pre-created comforter example, not generated during this test")
                .toString(2))
        } catch (failure: Throwable) {
            runCatching { record("failure") }
            File(evidence, "result.json").writeText(JSONObject()
                .put("passed", false).put("error", failure.toString()).put("steps", events).toString(2))
            throw failure
        } finally {
            try {
                scenario.close()
            } finally {
                lifecycleMonitor.removeLifecycleCallback(lifecycleCallback)
                lifecycleSnapshot = null
                configuration.setWaitForIdleTimeout(previousIdleTimeout)
                    .setWaitForSelectorTimeout(previousSelectorTimeout)
            }
        }
    }

    private fun sdkStatus(): JSONObject {
        val observed = lifecycleSnapshot
        assertTrue("Observe the same actual Activity that owns the bridge",
            observed?.first === bridgeActivity)
        assertEquals("Read status only while this actual Activity is resumed",
            Stage.RESUMED, observed?.second)
        return JSONObject(bridge.getStatus())
    }

    private fun ready(state: JSONObject): Boolean = state.optString("transport") == "pleos-native" &&
        state.optBoolean("ttsReady") && state.optBoolean("sttReady") &&
        state.optBoolean("vehicleConnected") && state.has("uxRestricted") &&
        !state.isNull("uxRestricted") && !state.getBoolean("uxRestricted")

    private fun assertSafe() {
        assertTrue("Real SDK readiness or AAOS UX allowance was lost; stop instead of bypassing it",
            ready(sdkStatus()))
    }

    private fun accessibleTextExists(label: String): Boolean =
        device.findObject(UiSelector().text(label)).exists() ||
            device.findObject(UiSelector().description(label)).exists()

    private fun firstTrackExists(): Boolean =
        device.findObject(UiSelector().textMatches("1\\. .+")).exists() ||
            device.findObject(UiSelector().descriptionMatches("1\\. .+")).exists()

    private fun tap(label: String, nextLabel: String) =
        tap(label, nextLabel) { accessibleTextExists(nextLabel) }

    private fun tap(label: String, expected: String, nextState: () -> Boolean) {
        assertSafe()
        val target = findVisible(label, 12000)
        assertTrue("Accessible button not found: $label. Use a fresh demo install.", target != null)
        clickObservedButton(target!!, label)
        waitFor("actual next UI after $label: $expected", 20000, nextState)
        events.put(JSONObject().put("action", "tap").put("label", label)
            .put("observedNextUi", expected)
            .put("elapsedMs", SystemClock.elapsedRealtime()))
        saveSteps()
        SystemClock.sleep(700)
        waitFor("question speech completion", 25000) { !sdkStatus().optBoolean("speaking") }
        assertSafe()
    }

    private fun tapFirstTrack() {
        assertSafe()
        val text = UiSelector().textMatches("1\\. .+")
        val description = UiSelector().descriptionMatches("1\\. .+")
        val target = findVisibleSelector(text, description, 12000)
        assertTrue("First track accessibility button not found", target != null)
        clickObservedButton(target!!, "first track")
        waitFor("actual track confirmation control", 20000) { accessibleTextExists("이 곡으로 감상하기") }
        events.put(JSONObject().put("action", "tap first track").put("elapsedMs", SystemClock.elapsedRealtime()))
        saveSteps()
        SystemClock.sleep(500)
    }

    @Suppress("DEPRECATION")
    private fun clickObservedButton(target: UiObject, label: String) {
        waitFor("actual enabled button: $label", 25000) {
            assertSafe()
            target.exists() && target.isEnabled && !target.visibleBounds.isEmpty
        }
        val actualLabel = target.text?.takeIf { it.isNotBlank() }
            ?: target.contentDescription?.takeIf { it.isNotBlank() }
            ?: throw AssertionError("Observed button has no accessible label: $label")
        val node = findActualButton(actualLabel)
            ?: throw AssertionError("Actual accessibility button unavailable: $label")
        try {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            assertTrue("Accessible button has no visible bounds: $label", !bounds.isEmpty)
            // This is Android's actual accessibility action on the observed HTML button.
            // It neither evaluates JS nor invokes the app's state-changing bridge methods.
            val accepted = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            events.put(JSONObject().put("action", "accessibility-click").put("label", label)
                .put("actualLabel", actualLabel).put("actionAccepted", accepted)
                .put("bounds", bounds.toShortString()).put("elapsedMs", SystemClock.elapsedRealtime()))
            saveSteps()
            // The caller must still observe the next real UI/player state before passing.
        } finally {
            node.recycle()
        }
    }

    @Suppress("DEPRECATION") // Recycle nodes on API 28–32; harmless on newer emulators.
    private fun findActualButton(label: String): AccessibilityNodeInfo? {
        val automation = instrumentation.uiAutomation
        val roots = automation.windows.mapNotNull { it.root }
            .ifEmpty { listOfNotNull(automation.rootInActiveWindow) }
        try {
            for (root in roots) {
                findActualButtonInTree(root, label)?.let { return it }
            }
            return null
        } finally {
            roots.forEach { it.recycle() }
        }
    }

    @Suppress("DEPRECATION")
    private fun findActualButtonInTree(node: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        // Chromium 131's findAccessibilityNodeInfosByText() returns an empty list.
        // Traverse the actual accessibility children instead, as UiAutomator does.
        // https://chromium.googlesource.com/chromium/src/+/131.0.6752.0/content/public/android/java/src/org/chromium/content/browser/accessibility/WebContentsAccessibilityImpl.java
        if (node.packageName?.toString() == instrumentation.targetContext.packageName &&
            (node.text?.toString() == label || node.contentDescription?.toString() == label) &&
            node.isVisibleToUser && node.isEnabled && node.isClickable &&
            node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }) {
            // The caller owns this copy; traversal nodes are always recycled below.
            return AccessibilityNodeInfo.obtain(node)
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            try {
                findActualButtonInTree(child, label)?.let { return it }
            } finally {
                child.recycle()
            }
        }
        return null
    }

    private fun findVisible(label: String, timeout: Long): UiObject? =
        findVisibleSelector(UiSelector().text(label), UiSelector().description(label), timeout)

    private fun findVisibleSelector(text: UiSelector, description: UiSelector, timeout: Long): UiObject? {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < deadline) {
            for (selector in listOf(text, description)) {
                val candidate = device.findObject(selector)
                if (candidate.exists() && !candidate.visibleBounds.isEmpty) return candidate
                // HTML buttons can be below the automotive canvas. Scroll the actual WebView.
                runCatching {
                    UiScrollable(UiSelector().scrollable(true).instance(0))
                        .setAsVerticalList().setMaxSearchSwipes(6).scrollIntoView(selector)
                }
                val afterScroll = device.findObject(selector)
                if (afterScroll.exists() && !afterScroll.visibleBounds.isEmpty) return afterScroll
            }
            SystemClock.sleep(250)
        }
        return null
    }

    private fun waitMusicOutput() {
        val audio = instrumentation.targetContext.getSystemService(AudioManager::class.java)
        waitFor("active app MediaPlayer and media stream", 10000) {
            val state = sdkStatus()
            audio.isMusicActive && state.optBoolean("musicPlaying") && state.optInt("durationMs") > 0
        }
        val firstPosition = sdkStatus().optInt("positionMs")
        waitFor("the app's own MediaPlayer position to advance", 6000) {
            val state = sdkStatus()
            state.optBoolean("musicPlaying") && state.optInt("positionMs") > firstPosition + 250
        }
    }

    private fun hold(milliseconds: Long) {
        val deadline = SystemClock.elapsedRealtime() + milliseconds
        while (SystemClock.elapsedRealtime() < deadline) {
            assertSafe()
            SystemClock.sleep(250)
        }
    }

    private fun waitFor(label: String, timeout: Long, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(250)
        }
        throw AssertionError("Timed out waiting for $label")
    }

    private fun record(label: String) {
        Log.i("PleosJourney", label)
        device.takeScreenshot(File(evidence, "$label.png"))
        device.dumpWindowHierarchy(File(evidence, "$label.xml"))
        events.put(JSONObject().put("checkpoint", label).put("sdkState", sdkStatus())
            .put("elapsedMs", SystemClock.elapsedRealtime()))
        saveSteps()
    }

    private fun saveSteps() = File(evidence, "steps.json").writeText(events.toString(2))
}
