package org.psilynx.psikit.ftc.autolog

import android.content.Context
import com.qualcomm.ftccommon.FtcEventLoop
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.util.RobotLog
import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.FtcLoggingSession

/**
 * Opt-in annotation that enables PsiKit logging without:
 * - subclassing a PsiKit base OpMode, or
 * - calling super methods.
 *
 * When present on a user OpMode class, PsiKit installs a runtime wrapper OpMode that delegates
 * to the user OpMode while running the usual PsiKit logging hooks.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PsiKitAutoLog(
    /** Port for the optional RLOG server. Set to 0 to disable. */
    val rlogPort: Int = 5800,
    /** Output folder for RLOGWriter. */
    val rlogFolder: String = "/sdcard/FIRST/PsiKit/",
    /** Optional filename override; blank means "use default". */
    val rlogFilename: String = "",
)

/**
 * Explicit opt-out for PsiKit's auto-logging wrapper.
 *
 * This is useful when [PsiKitAutoLogSettings.enabledByDefault] is true.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PsiKitNoAutoLog

data class PsiKitAutoLogOptions(
    val rlogPort: Int = 5800,
    val rlogFolder: String = "/sdcard/FIRST/PsiKit/",
    val rlogFilename: String = "",
)

/**
 * Runtime configuration for PsiKit's event-loop auto logger.
 *
 * Defaults are intentionally aggressive ("instrument everything") to satisfy the
 * "drop-in" goal; use [PsiKitNoAutoLog] to exclude specific OpModes.
 */
object PsiKitAutoLogSettings {
    const val PROPERTY_ENABLED = "psikit.autolog.enabled"
    const val PROPERTY_ENABLE_LINEAR = "psikit.autolog.linear"
    const val PROPERTY_RLOG_PORT = "psikit.autolog.rlogPort"
    const val PROPERTY_RLOG_FOLDER = "psikit.autolog.rlogFolder"
    const val PROPERTY_RLOG_FILENAME = "psikit.autolog.rlogFilename"

    @JvmField
    var enabledByDefault: Boolean = true

    /** Linear OpModes can only be instrumented with session start/end (no per-loop wrapper). */
    @JvmField
    var enableLinearByDefault: Boolean = true

    fun isEnabled(): Boolean = readBoolProperty(PROPERTY_ENABLED) ?: enabledByDefault

    fun isLinearEnabled(): Boolean = readBoolProperty(PROPERTY_ENABLE_LINEAR) ?: enableLinearByDefault

    fun defaultOptions(): PsiKitAutoLogOptions = PsiKitAutoLogOptions(
        rlogPort = readIntProperty(PROPERTY_RLOG_PORT) ?: 5800,
        rlogFolder = System.getProperty(PROPERTY_RLOG_FOLDER)?.takeIf { it.isNotBlank() }
            ?: "/sdcard/FIRST/PsiKit/",
        rlogFilename = System.getProperty(PROPERTY_RLOG_FILENAME) ?: "",
    )

    private fun readBoolProperty(name: String): Boolean? {
        val raw = System.getProperty(name)?.trim()?.lowercase() ?: return null
        return when (raw) {
            "true", "1", "yes", "y" -> true
            "false", "0", "no", "n" -> false
            else -> null
        }
    }

    private fun readIntProperty(name: String): Int? {
        val raw = System.getProperty(name)?.trim() ?: return null
        return raw.toIntOrNull()
    }
}

/**
 * Automatically wraps iterative OpModes with a PsiKit-logging OpMode.
 *
 * Activation is controlled by [PsiKitAutoLogSettings] (global enable) and/or [PsiKitAutoLog]
 * (per-opmode opt-in), with [PsiKitNoAutoLog] as an explicit opt-out.
 */
object PsiKitAutoLogger : OpModeManagerNotifier.Notifications {

    private const val TAG = "PsiKitAutoLogger"

    @Volatile
    private var opModeManager: OpModeManagerImpl? = null

    private val linearLock = Any()
    private val linearPendingOptions = java.util.IdentityHashMap<OpMode, PsiKitAutoLogOptions>()
    private val linearSessions = java.util.IdentityHashMap<OpMode, FtcLoggingSession>()

    /**
     * Optional helper for annotated [LinearOpMode]s.
     *
     * If you annotate a [LinearOpMode] directly, PsiKit starts a session at pre-start, but FTC
     * doesn't offer a safe way to wrap the user's per-loop code automatically. Calling this method
     * once per user iteration will run the same "before user" tick and bulk-cache / input logging
     * that iterative wrappers do.
     */
    @JvmStatic
    fun linearPeriodicBeforeUser(opMode: LinearOpMode) {
        Logger.periodicBeforeUser()
        val session = synchronized(linearLock) { linearSessions[opMode] }
        session?.logOncePerLoop(opMode)
    }

    /**
     * Companion to [linearPeriodicBeforeUser].
     *
     * Call this once per user iteration after your loop body to record user-vs-PsiKit timing.
     *
     * Both values are durations in seconds, consistent with [Logger.getRealTimestamp] and
     * [Logger.periodicAfterUser].
     */
    @JvmStatic
    fun linearPeriodicAfterUser(userCodeSec: Double, psiKitOverheadSec: Double) {
        Logger.periodicAfterUser(userCodeSec, psiKitOverheadSec)
    }

    /**
     * Convenience helper for annotated [LinearOpMode]s that want per-loop PsiKit logging without
     * manually writing the `while (opModeIsActive())` loop.
     *
     * This method owns the loop, calls [linearPeriodicBeforeUser] / [linearPeriodicAfterUser] once
     * per iteration, and runs [body] as the user loop body.
     */
    @JvmStatic
    fun linearLoop(opMode: LinearOpMode, body: Runnable) {
        while (opMode.opModeIsActive()) {
            val beforeStart = Logger.getRealTimestamp()
            linearPeriodicBeforeUser(opMode)
            val beforeEnd = Logger.getRealTimestamp()

            val userStart = beforeEnd
            body.run()
            val userEnd = Logger.getRealTimestamp()

            linearPeriodicAfterUser(userEnd - userStart, beforeEnd - beforeStart)
        }
    }

    /**
     * Kotlin-friendly overload of [linearLoop].
     */
    inline fun linearLoop(opMode: LinearOpMode, crossinline body: () -> Unit) {
        linearLoop(opMode, Runnable { body() })
    }

    /**
     * FTC SDK hook invoked by the Robot Controller app after the event loop is created.
     *
     * This registers [PsiKitAutoLogger] with the [OpModeManagerImpl] so we can install wrapper OpModes.
     */
    @JvmStatic
    @OnCreateEventLoop
    fun onCreateEventLoop(context: Context, ftcEventLoop: FtcEventLoop) {
        val manager = ftcEventLoop.opModeManager

        // Best-effort detach from any prior OpModeManager.
        opModeManager?.let {
            try {
                it.unregisterListener(this)
            } catch (_: Throwable) {
                // ignore
            }
        }

        opModeManager = manager

        try {
            manager.registerListener(this)
            RobotLog.vv(TAG, "Registered with OpModeManager")
        } catch (t: Throwable) {
            RobotLog.ee(TAG, t, "Failed to register with OpModeManager")
        }
    }

    override fun onOpModePreInit(opMode: OpMode) {
        val manager = opModeManager ?: return

        // Avoid double-wrapping.
        if (opMode is PsiKitWrappedIterativeOpMode) return

        // Never instrument the SDK's default safety opmode.
        if (opMode.javaClass.name == "com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl\$DefaultOpMode") return

        // Explicit opt-out.
        if (opMode.javaClass.isAnnotationPresent(PsiKitNoAutoLog::class.java)) return

        val shouldAutoLog = PsiKitAutoLogSettings.isEnabled() || opMode.javaClass.isAnnotationPresent(PsiKitAutoLog::class.java)
        if (!shouldAutoLog) return

        val options = optionsFor(opMode)

        // Linear OpModes can't be wrapped safely with per-loop instrumentation without running the
        // user code as the active OpMode instance. We instead start/end a session around start/stop.
        if (opMode is LinearOpMode) {
            if (!PsiKitAutoLogSettings.isLinearEnabled()) return
            synchronized(linearLock) {
                linearPendingOptions[opMode] = options
            }
            return
        }

        try {
            val wrapper = PsiKitWrappedIterativeOpMode(delegate = opMode, options = options)

            // Critical: OpModeInternal fields are package-private; when we construct an OpMode
            // ourselves, they are unset. Copy the runtime-provided services and initial objects.
            PsiKitWrappedIterativeOpMode.bootstrapFromDelegate(delegate = opMode, wrapper = wrapper)

            // Swap the active OpMode instance to our wrapper.
            replaceActiveOpMode(manager, wrapper)
        } catch (t: Throwable) {
            RobotLog.ee(TAG, t, "Failed to install PsiKit wrapper for ${opMode.javaClass.name}")
        }
    }

    override fun onOpModePreStart(opMode: OpMode) {
        // If this is a LinearOpMode, we start the session here (after internalInit wiring).
        if (opMode !is LinearOpMode) return

        val options = synchronized(linearLock) { linearPendingOptions.remove(opMode) } ?: return

        val session = FtcLoggingSession()
        try {
            session.start(
                opMode = opMode,
                rlogPort = options.rlogPort,
                folder = options.rlogFolder,
                filename = options.rlogFilename,
                metadataOpMode = opMode,
            )

            synchronized(linearLock) {
                linearSessions[opMode] = session
            }

            RobotLog.ww(
                TAG,
                "Auto-logging LinearOpMode '${opMode.javaClass.name}' is limited (no per-loop wrapper). " +
                    "For full PsiKit loop timing + bulk-cache management, extend com.qualcomm.robotcore.eventloop.opmode.PsiKitLinearOpMode."
            )
        } catch (t: Throwable) {
            try {
                session.end()
            } catch (_: Throwable) {
                // ignore
            }
            RobotLog.ee(TAG, t, "Failed to start PsiKit session for LinearOpMode ${opMode.javaClass.name}")
        }
    }

    override fun onOpModePostStop(opMode: OpMode) {
        // Iterative wrappers end themselves in stop(). Linear sessions are managed here.
        if (opMode is LinearOpMode) {
            val session = synchronized(linearLock) {
                linearPendingOptions.remove(opMode)
                linearSessions.remove(opMode)
            }
            try {
                session?.end()
            } catch (_: Throwable) {
                // ignore
            }
        }
    }

    private fun optionsFor(opMode: OpMode): PsiKitAutoLogOptions {
        val annotation = opMode.javaClass.getAnnotation(PsiKitAutoLog::class.java)
        if (annotation != null) {
            return PsiKitAutoLogOptions(
                rlogPort = annotation.rlogPort,
                rlogFolder = annotation.rlogFolder,
                rlogFilename = annotation.rlogFilename,
            )
        }
        return PsiKitAutoLogSettings.defaultOptions()
    }

    private fun replaceActiveOpMode(manager: OpModeManagerImpl, newActive: OpMode) {
        val field = findField(manager.javaClass, "activeOpMode")
            ?: throw NoSuchFieldException("OpModeManagerImpl.activeOpMode")
        field.isAccessible = true
        field.set(manager, newActive)
    }

    private fun findField(clazz: Class<*>, name: String): java.lang.reflect.Field? {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                return current.getDeclaredField(name)
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }
}

/**
 * Runtime-installed wrapper that provides PsiKit logging around an arbitrary user iterative [OpMode].
 */
class PsiKitWrappedIterativeOpMode(
    private val delegate: OpMode,
    private val options: PsiKitAutoLogOptions,
) : OpMode() {

    private val psiKitSession: FtcLoggingSession = FtcLoggingSession()

    private var sessionStarted: Boolean = false
    private var startHookRan: Boolean = false
    private var stopHookRan: Boolean = false
    private var lastObservedStarted: Boolean = false

    override fun init() {
        ensurePsiKitStarted()
        callUserSafely { syncDelegateState(); delegate.init() }
    }

    override fun init_loop() {
        val beforeUserStart = Logger.getRealTimestamp()

        Logger.periodicBeforeUser()
        psiKitSession.logOncePerLoop(this)
        maybeRunStartHookFromReplay()

        val beforeUserEnd = Logger.getRealTimestamp()
        callUserSafely { syncDelegateState(); delegate.init_loop() }

        val afterUserStart = Logger.getRealTimestamp()
        Logger.periodicAfterUser(
            afterUserStart - beforeUserEnd,
            beforeUserEnd - beforeUserStart
        )
    }

    override fun start() {
        ensurePsiKitStarted()
        internalStartOnce()
        callUserSafely { syncDelegateState(); delegate.start() }
    }

    override fun loop() {
        val beforeUserStart = Logger.getRealTimestamp()

        Logger.periodicBeforeUser()
        psiKitSession.logOncePerLoop(this)
        maybeRunStartHookFromReplay()

        val beforeUserEnd = Logger.getRealTimestamp()
        callUserSafely { syncDelegateState(); delegate.loop() }

        val afterUserStart = Logger.getRealTimestamp()
        Logger.periodicAfterUser(
            afterUserStart - beforeUserEnd,
            beforeUserEnd - beforeUserStart
        )
    }

    override fun stop() {
        if (stopHookRan) return
        stopHookRan = true
        try {
            callUserSafely { syncDelegateState(); delegate.stop() }
        } finally {
            try {
                psiKitSession.end()
            } catch (_: Throwable) {
                // ignore
            }
            sessionStarted = false
        }
    }

    private fun ensurePsiKitStarted() {
        if (sessionStarted) return

        psiKitSession.start(
            opMode = this,
            rlogPort = options.rlogPort,
            folder = options.rlogFolder,
            filename = options.rlogFilename,
            // Record metadata from the user's real OpMode class (annotations, name, etc)
            metadataOpMode = delegate,
        )

        sessionStarted = true
    }

    private fun internalStartOnce() {
        if (startHookRan) return
        startHookRan = true
        // Keep the replay edge detector from firing later.
        lastObservedStarted = true
    }

    private fun maybeRunStartHookFromReplay() {
        if (startHookRan) return

        val startedNow = readBooleanFieldIfPresent(this, "isStarted") ?: false
        if (!lastObservedStarted && startedNow) {
            internalStartOnce()
        }
        lastObservedStarted = startedNow
    }

    private fun callUserSafely(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            // Ensure we close the session on user exceptions (the FTC runtime won't necessarily call stop()).
            try {
                psiKitSession.end()
            } catch (_: Throwable) {
                // ignore
            }
            sessionStarted = false
            throw t
        }
    }

    private fun syncDelegateState() {
        // Ensure user code reads the same state the FTC runtime is mutating.
        delegate.hardwareMap = hardwareMap
        delegate.telemetry = telemetry
        delegate.gamepad1 = gamepad1
        delegate.gamepad2 = gamepad2
        delegate.time = time
    }

    private fun readBooleanFieldIfPresent(target: Any, fieldName: String): Boolean? {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                return field.getBoolean(target)
            } catch (_: NoSuchFieldException) {
                clazz = clazz.superclass
            } catch (_: Throwable) {
                return null
            }
        }
        return null
    }

    companion object {

        /**
         * Copies essential FTC runtime state from the SDK-constructed OpMode into the wrapper.
         *
         * This is required because the OpModeManager initializes package-private fields on the
         * original OpMode instance, and our wrapper is constructed manually.
         */
        fun bootstrapFromDelegate(delegate: OpMode, wrapper: PsiKitWrappedIterativeOpMode) {
            // Copy OpModeInternal.public-ish fields that the OpModeThread initialization expects.
            wrapper.gamepad1 = delegate.gamepad1
            wrapper.gamepad2 = delegate.gamepad2
            wrapper.hardwareMap = delegate.hardwareMap

            // Copy OpModeInternal package-private runtime services.
            copyFieldIfPresent(delegate, wrapper, "internalOpModeServices")
        }

        private fun copyFieldIfPresent(source: Any, target: Any, fieldName: String) {
            val field = findField(source.javaClass, fieldName) ?: return
            field.isAccessible = true
            val value = field.get(source)
            field.set(target, value)
        }

        private fun findField(clazz: Class<*>, name: String): java.lang.reflect.Field? {
            var current: Class<*>? = clazz
            while (current != null) {
                try {
                    return current.getDeclaredField(name)
                } catch (_: NoSuchFieldException) {
                    current = current.superclass
                }
            }
            return null
        }
    }
}
