package org.psilynx.psikit.ftc

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.LynxModule.BulkCachingMode.MANUAL
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.core.rlog.RLOGServer
import org.psilynx.psikit.core.rlog.RLOGWriter
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Composition-based PsiKit logging helper for FTC [LinearOpMode]s.
 *
 */
class FtcLoggingSession {

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
    fun start(opMode: LinearOpMode, rlogPort: Int, filename: String = defaultLogFilename(opMode)) {
        // If the prior OpMode was force-stopped, PsiKit may still be "running".
        try {
            Logger.end()
        } catch (_: Exception) {
            // ignore
        }
        Logger.reset()

        // Wrap hardwareMap for /HardwareMap/... inputs and replay manifest.
        opMode.hardwareMap = HardwareMapWrapper(opMode.hardwareMap)
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

        // Record basic OpMode metadata like PsiKit's base classes do.
        recordOpModeMetadata(opMode)

        Logger.addDataReceiver(RLOGServer(rlogPort))
        Logger.addDataReceiver(RLOGWriter(filename))

        if (Logger.isReplay()) {
            // Best-effort: avoid blocking forever in waitForStart()/opModeInInit().
            forceOpModeStarted(opMode)
        }

        Logger.start()
    }

    fun end() {
        try {
            Logger.end()
        } catch (_: Exception) {
            // ignore
        }
    }

    /** Call once per loop, after [Logger.periodicBeforeUser]. */
    fun logOncePerLoop(opMode: LinearOpMode) {
        clearBulkCaches()

        OpModeControls.started = opMode.isStarted
        OpModeControls.stopped = opMode.isStopRequested
        Logger.processInputs("OpModeControls", OpModeControls)

        // DriverStation inputs (AdvantageScope Joysticks schema).
        driverStationLogger.log(opMode.gamepad1, opMode.gamepad2)

        if (enablePinpointOdometryLogging) {
            // Pinpoint odometry (AdvantageScope Pose2d/Pose3d structs under /Odometry).
            pinpointOdometryLogger.logAll(opMode.hardwareMap)
        }

        // Log all accessed hardware devices.
        for ((key, value) in HardwareMapWrapper.devicesToProcess) {
            val startNs = System.nanoTime()
            Logger.processInputs("HardwareMap/$key", value)
            val endNs = System.nanoTime()
            Logger.recordOutput("PsiKit/logTimes (us)/$key", (endNs - startNs) / 1_000.0)
        }
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

    private fun defaultLogFilename(opMode: LinearOpMode): String {
        return opMode::class.java.simpleName +
            "_log_" +
            SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(Date()) +
            ".rlog"
    }

    private fun recordOpModeMetadata(opMode: LinearOpMode) {
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

    private fun forceOpModeStarted(opMode: LinearOpMode) {
        try {
            val startedField = OpMode::class.java.getDeclaredField("isStarted")
            startedField.isAccessible = true
            startedField.setBoolean(opMode, true)
        } catch (_: Throwable) {
            // ignore
        }
    }
}
