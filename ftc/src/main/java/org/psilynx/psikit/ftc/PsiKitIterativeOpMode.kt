package org.psilynx.psikit.ftc

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.autolog.PsiKitNoAutoLog

/**
 * Minimal-change base class for iterative FTC [OpMode]s that want PsiKit logging.
 *
 * - Starts PsiKit in [init] (so the wrapped hardwareMap is available immediately).
 * - Runs Logger periodic hooks + [FtcLoggingSession.logOncePerLoop] in init_loop/loop.
 * - In replay, PsiKit may toggle internal "isStarted" without the FTC runtime invoking [start].
 *   This base class detects a false->true transition and invokes [onPsiKitStart] once.
 */
@PsiKitNoAutoLog
open class PsiKitIterativeOpMode : OpMode() {

    /** Port for the optional RLOG server. Set to 0 to disable. */
    protected open val rlogPort: Int = 5800

    /** Output folder for RLOGWriter. */
    protected open val rlogFolder: String = "/sdcard/FIRST/PsiKit/"

    /** Optional filename override; blank means "use default". */
    protected open val rlogFilename: String = ""

    protected val psiKitSession: FtcLoggingSession = FtcLoggingSession()

    private var sessionStarted: Boolean = false
    private var startHookRan: Boolean = false
    private var stopHookRan: Boolean = false
    private var lastObservedStarted: Boolean = false

    final override fun init() {
        ensurePsiKitStarted()
        onPsiKitInit()
    }

    final override fun init_loop() {
        val beforeUserStart = Logger.getRealTimestamp()

        Logger.periodicBeforeUser()
        psiKitSession.logOncePerLoop(this)
        maybeRunStartHookFromReplay()

        val beforeUserEnd = Logger.getRealTimestamp()
        onPsiKitInitLoop()

        val afterUserStart = Logger.getRealTimestamp()
        Logger.periodicAfterUser(
            afterUserStart - beforeUserEnd,
            beforeUserEnd - beforeUserStart
        )
    }

    final override fun start() {
        ensurePsiKitStarted()
        internalStartOnce()
    }

    final override fun loop() {
        val beforeUserStart = Logger.getRealTimestamp()

        Logger.periodicBeforeUser()
        psiKitSession.logOncePerLoop(this)
        maybeRunStartHookFromReplay()

        val beforeUserEnd = Logger.getRealTimestamp()
        onPsiKitLoop()

        val afterUserStart = Logger.getRealTimestamp()
        Logger.periodicAfterUser(
            afterUserStart - beforeUserEnd,
            beforeUserEnd - beforeUserStart
        )
    }

    final override fun stop() {
        if (stopHookRan) return
        stopHookRan = true
        try {
            onPsiKitStop()
        } finally {
            psiKitSession.end()
            sessionStarted = false
        }
    }

    /** Called from [init] after PsiKit is started. */
    protected open fun onPsiKitInit() {}

    /**
     * Called during PsiKit session startup after Logger.reset() and receiver setup,
     * but before Logger.start().
     *
     * Use this to add custom Logger receivers (e.g. extra writers/servers) or record metadata
     * that must be present in the first logged cycle.
     */
    protected open fun onPsiKitConfigureLogging() {}

    /** Called each [init_loop] after PsiKit logging. */
    protected open fun onPsiKitInitLoop() {}

    /** Called exactly once when the OpMode transitions to started (SDK start or replay start). */
    protected open fun onPsiKitStart() {}

    /** Called each [loop] after PsiKit logging. */
    protected open fun onPsiKitLoop() {}

    /** Called from [stop] before the PsiKit session is ended. */
    protected open fun onPsiKitStop() {}

    private fun ensurePsiKitStarted() {
        if (sessionStarted) return

        if (rlogFilename.isNotBlank()) {
            psiKitSession.start(
                this,
                rlogPort,
                filename = rlogFilename,
                folder = rlogFolder,
                configure = { onPsiKitConfigureLogging() }
            )
        } else {
            psiKitSession.start(
                this,
                rlogPort,
                folder = rlogFolder,
                configure = { onPsiKitConfigureLogging() }
            )
        }

        sessionStarted = true
    }

    private fun internalStartOnce() {
        if (startHookRan) return
        startHookRan = true
        // Keep the replay edge detector from firing later.
        lastObservedStarted = true
        onPsiKitStart()
    }

    private fun maybeRunStartHookFromReplay() {
        if (startHookRan) return

        val startedNow = readBooleanFieldIfPresent(this, "isStarted") ?: false
        if (!lastObservedStarted && startedNow) {
            internalStartOnce()
        }
        lastObservedStarted = startedNow
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
}
