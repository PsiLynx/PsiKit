package org.psilynx.psikit.ftc

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.LynxModule.BulkCachingMode.MANUAL
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.core.LogReplaySource
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.core.rlog.RLOGServer
import org.psilynx.psikit.core.rlog.RLOGReplay
import org.psilynx.psikit.core.rlog.RLOGWriter
import org.psilynx.psikit.ftc.wrappers.AnalogInputWrapper
import org.psilynx.psikit.ftc.wrappers.ColorDistanceSensorWrapper
import org.psilynx.psikit.ftc.wrappers.CrServoWrapper
import org.psilynx.psikit.ftc.wrappers.DigitalChannelWrapper
import org.psilynx.psikit.ftc.wrappers.ImuWrapper
import org.psilynx.psikit.ftc.wrappers.Limelight3AWrapper
import org.psilynx.psikit.ftc.wrappers.MotorWrapper
import org.psilynx.psikit.ftc.wrappers.PinpointWrapper
import org.psilynx.psikit.ftc.wrappers.ServoWrapper
import org.psilynx.psikit.ftc.wrappers.SparkFunOTOSWrapper
import org.psilynx.psikit.ftc.wrappers.VoltageSensorWrapper
import java.text.SimpleDateFormat
import java.util.Date
import java.io.File

/**
 * Composition-based PsiKit logging helper for FTC [OpMode]s.
 */
class FtcLoggingSession {

    companion object {
        private const val REPLAY_LOG_PROPERTY = "psikitReplayLog"
        private const val REPLAY_LOG_ENV = "PSIKIT_REPLAY_LOG"
        private const val REPLAY_ENABLE_SERVER_PROPERTY = "psikitReplayEnableServer"
        private const val REPLAY_WRITE_OUTPUT_PROPERTY = "psikitReplayWriteOutput"
        private const val REPLAY_OUTPUT_DIR_PROPERTY = "psikitReplayOutputDir"
        private const val REPLAY_MOCK_HARDWAREMAP_PROPERTY = "psikitReplayMockHardwareMap"

        private const val REPLAY_ENABLE_SERVER_ENV = "PSIKIT_REPLAY_ENABLE_SERVER"
        private const val REPLAY_WRITE_OUTPUT_ENV = "PSIKIT_REPLAY_WRITE_OUTPUT"
        private const val REPLAY_OUTPUT_DIR_ENV = "PSIKIT_REPLAY_OUTPUT_DIR"
        private const val REPLAY_MOCK_HARDWAREMAP_ENV = "PSIKIT_REPLAY_MOCK_HARDWAREMAP"
    }

    /**
     * If true, logs Pinpoint odometry (when present) each loop via [PinpointOdometryLogger].
     *
     * Behavior when no Pinpoint is configured:
     * - Performs a one-time scan of the [com.qualcomm.robotcore.hardware.HardwareMap]
     * - Then becomes a no-op (no outputs are produced)
     *
     * Set to false to opt out even if a Pinpoint is present.
     */
    @JvmField
    var enablePinpointOdometryLogging: Boolean = true

    private val driverStationLogger = DriverStationLogger()
    private val pinpointOdometryLogger = PinpointOdometryLogger()

    private var wrappedHardwareMap: com.qualcomm.robotcore.hardware.HardwareMap? = null
    private var allHubs: List<LynxModule>? = null

    @JvmOverloads
    fun start(
        opMode: OpMode,
        rlogPort: Int,
        filename: String = defaultLogFilename(opMode),
        folder: String = "/sdcard/FIRST/PsiKit/",
        replaySource: LogReplaySource? = null,
        metadataOpMode: OpMode = opMode,
        configure: (() -> Unit)? = null,
    ) {
        // If the prior OpMode was force-stopped, PsiKit may still be "running".
        try {
            Logger.end()
        } catch (_: Exception) {
            // ignore
        }
        Logger.reset()

        // Reset the per-session list of hardware devices to log.
        // HardwareMapWrapper.devicesToProcess is a static map and would otherwise retain
        // devices from prior OpModes (which can lead to unexpected logging and extra I/O).
        HardwareMapWrapper.devicesToProcess.clear()

        // Optional: configure replay before Logger.start().
        // - Explicit replaySource wins
        // - Otherwise allow CLI-driven replay via -DpsikitReplayLog=... / PSIKIT_REPLAY_LOG=...
        val effectiveReplaySource = replaySource ?: resolveReplaySourceFromSystem()
        val isReplay = effectiveReplaySource != null
        if (isReplay) {
            Logger.setReplay(true)
            Logger.setReplaySource(effectiveReplaySource)

            // Many FTC SDK OpModes call telemetry.update() during init/loop.
            // In replay (Robolectric/CLI), internal FTC services are often not wired,
            // which can cause TelemetryImpl.update() to throw.
            installReplaySafeTelemetry(opMode)
        }

        // Wrap hardwareMap for /HardwareMap/... inputs and replay manifest.
        val existingHardwareMap = opMode.hardwareMap
        if (existingHardwareMap != null) {
            opMode.hardwareMap = HardwareMapWrapper(existingHardwareMap)
            wrappedHardwareMap = opMode.hardwareMap

            // Configure Lynx bulk caching like PsiKitLinearOpMode.
            allHubs = try {
                val hubs = opMode.hardwareMap.getAll(LynxModule::class.java)
                for (hub in hubs) {
                    hub.bulkCachingMode = MANUAL
                }
                hubs
            } catch (_: Throwable) {
                null
            }
        } else if (isReplay) {
            // In replay/CLI scenarios we may not have a real FTC HardwareMap available.
            // Optionally (opt-in), install a wrapper around null so hardwareMap.get(...) can still
            // return PsiKit device wrappers for supported device types.
            //
            // NOTE: Some wrapper implementations extend FTC SDK concrete classes (e.g. DcMotorImplEx)
            // which depend on android.util.Log at runtime. That works on-robot and under Robolectric,
            // but will crash a plain JVM runner using android.jar stubs. Therefore this is opt-in.
            val enableMockHardwareMap =
                (System.getProperty(REPLAY_MOCK_HARDWAREMAP_PROPERTY)?.toBoolean() == true)
                    || (System.getenv(REPLAY_MOCK_HARDWAREMAP_ENV)?.toBoolean() == true)

            if (enableMockHardwareMap) {
                try {
                    opMode.hardwareMap = HardwareMapWrapper(null)
                    wrappedHardwareMap = opMode.hardwareMap
                } catch (t: Throwable) {
                    // Keep replay usable even if the mock hardware map can't be created on this JVM.
                    wrappedHardwareMap = null
                }
            } else {
                wrappedHardwareMap = null
            }
            allHubs = null
        } else {
            wrappedHardwareMap = null
            allHubs = null
        }

        // Record basic OpMode metadata like PsiKit's base classes do.
        // Some integrations run as a wrapper OpMode; allow metadata to come from the underlying user OpMode.
        recordOpModeMetadata(metadataOpMode)

        // Port 0 (or negative) disables the server. Useful in tests and competitions.
        // In replay, keep server disabled unless explicitly enabled.
        val enableServer = if (isReplay) {
            // Support both JVM properties (-D...) and environment variables for CLI/Gradle reliability.
            // Properties are convenient for IDE run configs; env vars are a dependable fallback when
            // Gradle/test workers/Windows quoting make -D propagation flaky.
            (System.getProperty(REPLAY_ENABLE_SERVER_PROPERTY)?.toBoolean() == true)
                || (System.getenv(REPLAY_ENABLE_SERVER_ENV)?.toBoolean() == true)
        } else {
            true
        }
        if (enableServer && rlogPort > 0) {
            Logger.addDataReceiver(RLOGServer(rlogPort))
        }

        // Blank filename disables file output.
        // In replay, keep file output disabled unless explicitly enabled.
        val enableWriter = if (isReplay) {
            // See note above: accept both -D... and env vars so this can be controlled reliably from
            // IDE runs, Gradle CLI, and CI.
            (System.getProperty(REPLAY_WRITE_OUTPUT_PROPERTY)?.toBoolean() == true)
                || (System.getenv(REPLAY_WRITE_OUTPUT_ENV)?.toBoolean() == true)
        } else {
            true
        }
        if (enableWriter && filename.isNotBlank()) {
            val effectiveFolderRaw = if (isReplay) {
                System.getProperty(REPLAY_OUTPUT_DIR_PROPERTY)?.takeIf { it.isNotBlank() }
                    ?: System.getenv(REPLAY_OUTPUT_DIR_ENV)?.takeIf { it.isNotBlank() }
                    ?: folder
            } else {
                folder
            }

            // RLOGWriter expects forward slashes and a trailing slash.
            val effectiveFolder = effectiveFolderRaw
                .replace('\\', '/')
                .let { if (it.endsWith('/')) it else "$it/" }

            Logger.addDataReceiver(RLOGWriter(effectiveFolder, filename))
        }

        // Allow user code (or a base class) to add additional receivers / metadata
        // after Logger.reset() but before the first cycle begins.
        configure?.invoke()

        Logger.start()
    }

    private fun installReplaySafeTelemetry(opMode: OpMode) {
        try {
            val existing = opMode.telemetry
            if (existing != null && existing !is ReplaySafeTelemetry) {
                opMode.telemetry = ReplaySafeTelemetry(existing)
            }
        } catch (_: Throwable) {
            // Best-effort: if the FTC SDK changes or telemetry is inaccessible,
            // do not break replay startup.
        }
    }

    /**
     * Java-friendly overload that keeps the default filename behavior while still allowing a
     * pre-start [configure] hook.
     */
    @JvmOverloads
    fun startWithConfigure(
        opMode: OpMode,
        rlogPort: Int,
        folder: String = "/sdcard/FIRST/PsiKit/",
        replaySource: LogReplaySource? = null,
        configure: (() -> Unit)? = null,
    ) {
        start(
            opMode = opMode,
            rlogPort = rlogPort,
            filename = defaultLogFilename(opMode),
            folder = folder,
            replaySource = replaySource,
            metadataOpMode = opMode,
            configure = configure,
        )
    }

    private fun resolveReplaySourceFromSystem(): LogReplaySource? {
        // Prefer a JVM property for per-run overrides, but also accept an env var because in some
        // Gradle/Robolectric setups system properties can fail to reach the forked test JVM.
        val path = System.getProperty(REPLAY_LOG_PROPERTY)?.takeIf { it.isNotBlank() }
            ?: System.getenv(REPLAY_LOG_ENV)?.takeIf { it.isNotBlank() }
            ?: return null

        val file = File(path)
        require(file.exists()) { "Replay log does not exist: $path" }
        require(file.isFile) { "Replay log is not a file: $path" }

        return RLOGReplay(path)
    }

    fun end() {
        try {
            Logger.end()
        } catch (_: Exception) {
            // ignore
        }
    }

    /** Call once per loop, after [Logger.periodicBeforeUser]. */
    fun logOncePerLoop(opMode: OpMode) {
        val loopStartNs = System.nanoTime()

        val clearStartNs = System.nanoTime()
        clearBulkCaches()
        val clearEndNs = System.nanoTime()
        Logger.recordOutput(
            "PsiKit/sessionTimes (us)/ClearBulkCaches",
            (clearEndNs - clearStartNs) / 1_000.0
        )

        // Optional: prefetch bulk data so per-device logTimes don't "blame" the first device on
        // each hub for the full bulk transaction. This is useful for tuning.
        if (FtcLogTuning.prefetchBulkDataEachLoop) {
            val hubs = allHubs
            if (hubs != null) {
                var prefetchTotalNs = 0L
                for (hub in hubs) {
                    val startNs = System.nanoTime()
                    try {
                        hub.bulkData
                    } catch (_: Throwable) {
                        // ignore
                    }
                    val endNs = System.nanoTime()
                    prefetchTotalNs += (endNs - startNs)

                    val hubId = try {
                        "addr${hub.moduleAddress}"
                    } catch (_: Throwable) {
                        try {
                            hub.deviceName
                        } catch (_: Throwable) {
                            "hub"
                        }
                    }

                    Logger.recordOutput(
                        "PsiKit/logTimes (us)/BulkPrefetch/$hubId",
                        (endNs - startNs) / 1_000.0
                    )
                }

                Logger.recordOutput(
                    "PsiKit/sessionTimes (us)/BulkPrefetchTotal",
                    prefetchTotalNs / 1_000.0
                )
            }
        }

        val opModeControlsStartNs = System.nanoTime()
        if (!Logger.isReplay()) {
            // OpMode does not expose isStarted/isStopRequested publicly, but the FTC SDK stores
            // these as internal fields on OpModeInternal (superclass of OpMode).
            // Read via reflection so this works for both iterative OpMode and LinearOpMode.
            OpModeControls.started = getBooleanFieldIfPresent(opMode, "isStarted") ?: false
            OpModeControls.stopped = getBooleanFieldIfPresent(opMode, "stopRequested") ?: false
        }
        Logger.processInputs("OpModeControls", OpModeControls)

        // In replay, drive the OpMode's state from the log so init/start/stop loops can be
        // reproduced faithfully.
        if (Logger.isReplay() && Logger.isRunning()) {
            applyOpModeControls(opMode, OpModeControls.started, OpModeControls.stopped)
        }

        val opModeControlsEndNs = System.nanoTime()
        Logger.recordOutput(
            "PsiKit/sessionTimes (us)/OpModeControls",
            (opModeControlsEndNs - opModeControlsStartNs) / 1_000.0
        )

        // DriverStation inputs (AdvantageScope Joysticks schema).
        val dsStartNs = System.nanoTime()
        driverStationLogger.log(opMode.gamepad1, opMode.gamepad2)
        val dsEndNs = System.nanoTime()
        Logger.recordOutput(
            "PsiKit/sessionTimes (us)/DriverStation",
            (dsEndNs - dsStartNs) / 1_000.0
        )

        if (enablePinpointOdometryLogging) {
            // Pinpoint odometry (AdvantageScope Pose2d/Pose3d structs under /Odometry).
            val pinpointStartNs = System.nanoTime()
            pinpointOdometryLogger.logAll(opMode.hardwareMap)
            val pinpointEndNs = System.nanoTime()
            Logger.recordOutput(
                "PsiKit/sessionTimes (us)/PinpointOdometry",
                (pinpointEndNs - pinpointStartNs) / 1_000.0
            )
        }

        // Log all accessed hardware devices.
        var hardwareTotalNs = 0L

        var motorNs = 0L
        var servoNs = 0L
        var crServoNs = 0L
        var analogInputNs = 0L
        var digitalChannelNs = 0L
        var imuNs = 0L
        var colorDistanceNs = 0L
        var voltageSensorNs = 0L
        var pinpointWrapperNs = 0L
        var limelightNs = 0L
        var otosNs = 0L
        var otherNs = 0L

        var maxDeviceNs = 0L
        var maxDeviceKey: String? = null

        for ((key, value) in HardwareMapWrapper.devicesToProcess) {
            val startNs = System.nanoTime()
            Logger.processInputs("HardwareMap/$key", value)
            val endNs = System.nanoTime()
            val dtNs = endNs - startNs
            hardwareTotalNs += dtNs
            Logger.recordOutput("PsiKit/logTimes (us)/$key", dtNs / 1_000.0)

            when (value) {
                is MotorWrapper -> motorNs += dtNs
                is ServoWrapper -> servoNs += dtNs
                is CrServoWrapper -> crServoNs += dtNs
                is AnalogInputWrapper -> analogInputNs += dtNs
                is DigitalChannelWrapper -> digitalChannelNs += dtNs
                is ImuWrapper -> imuNs += dtNs
                is ColorDistanceSensorWrapper -> colorDistanceNs += dtNs
                is VoltageSensorWrapper -> voltageSensorNs += dtNs
                is PinpointWrapper -> pinpointWrapperNs += dtNs
                is Limelight3AWrapper -> limelightNs += dtNs
                is SparkFunOTOSWrapper -> otosNs += dtNs
                else -> otherNs += dtNs
            }

            if (dtNs > maxDeviceNs) {
                maxDeviceNs = dtNs
                maxDeviceKey = key
            }
        }

        val loopEndNs = System.nanoTime()
        Logger.recordOutput(
            "PsiKit/sessionTimes (us)/HardwareMapTotal",
            hardwareTotalNs / 1_000.0
        )

        // Higher-level breakdown to identify where HardwareMapTotal spikes come from.
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/Motor", motorNs / 1_000.0)
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/Servo", servoNs / 1_000.0)
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/CrServo", crServoNs / 1_000.0)
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/AnalogInput", analogInputNs / 1_000.0)
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/DigitalChannel", digitalChannelNs / 1_000.0)
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/Imu", imuNs / 1_000.0)
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/ColorDistance", colorDistanceNs / 1_000.0)
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/VoltageSensor", voltageSensorNs / 1_000.0)
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/Pinpoint", pinpointWrapperNs / 1_000.0)
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/Limelight", limelightNs / 1_000.0)
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/OTOS", otosNs / 1_000.0)
        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapByType/Other", otherNs / 1_000.0)

        Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapMaxDeviceUs", maxDeviceNs / 1_000.0)
        if (maxDeviceKey != null) {
            Logger.recordOutput("PsiKit/sessionTimes (us)/HardwareMapMaxDeviceKey", maxDeviceKey!!)
        }

        Logger.recordOutput(
            "PsiKit/sessionTimes (us)/LogOncePerLoopTotal",
            (loopEndNs - loopStartNs) / 1_000.0
        )
    }

    private fun clearBulkCaches() {
        val hubs = allHubs ?: return
        for (hub in hubs) {
            try {
                hub.clearBulkCache()
            } catch (_: Throwable) {
                // ignore
            }
        }
    }

    private fun applyOpModeControls(opMode: OpMode, started: Boolean, stopped: Boolean) {
        // FTC SDK (RobotCore 11.0.0): LinearOpMode's opModeInInit()/opModeIsActive() ultimately
        // depend on internal fields on OpModeInternal (superclass of OpMode).
        // Those fields are not declared on OpMode itself, so we must search the class hierarchy.
        setBooleanFieldIfPresent(opMode, "isStarted", started)
        setBooleanFieldIfPresent(opMode, "stopRequested", stopped)
    }

    private fun defaultLogFilename(opMode: OpMode): String {
        val replayTag = if (
            Logger.isReplay() ||
                System.getProperty(REPLAY_LOG_PROPERTY)?.isNotBlank() == true ||
                System.getenv(REPLAY_LOG_ENV)?.isNotBlank() == true
        ) "_Replay" else ""
        return opMode::class.java.simpleName +
            replayTag +
            "_log_" +
            SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(Date()) +
            ".rlog"
    }

    private fun recordOpModeMetadata(opMode: OpMode) {
        val teleOp = opMode::class.java.getAnnotation(TeleOp::class.java)
        if (teleOp != null) {
            Logger.recordMetadata("OpMode Name", teleOp.name)
            Logger.recordMetadata("OpMode type", "TeleOp")
            return
        }

        val auto = opMode::class.java.getAnnotation(Autonomous::class.java)
        if (auto != null) {
            Logger.recordMetadata("OpMode Name", auto.name)
            Logger.recordMetadata("OpMode type", "Autonomous")
            return
        }

        Logger.recordMetadata("OpMode Name", opMode::class.java.simpleName)
        Logger.recordMetadata("OpMode type", "Unknown")
    }

    private fun getBooleanFieldIfPresent(target: Any, fieldName: String): Boolean? {
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

    private fun setBooleanFieldIfPresent(target: Any, fieldName: String, value: Boolean): Boolean {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.setBoolean(target, value)
                return true
            } catch (_: NoSuchFieldException) {
                clazz = clazz.superclass
            } catch (_: Throwable) {
                return false
            }
        }
        return false
    }
}
