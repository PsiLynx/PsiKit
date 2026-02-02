package org.psilynx.psikit.ftc

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.LynxModule.BulkCachingMode.MANUAL
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.wrappers.GamepadWrapper
import kotlin.time.measureTime


abstract class PsiKitLinearOpMode: LinearOpMode() {
    val psiKitIsStopRequested get() = OpModeControls.stopped

    val psiKitIsStarted get() = OpModeControls.started

    lateinit var allHubs: List<LynxModule>

    /*
     * updates the hardware map input. this must be called before accessing
     * any hardware every loop. It's safest to call it right after
     * Logger.periodicBeforeUser()
     * note that if you have a loop that runs before isStarted, you must
     * update this in that loop as well.
     */
    fun processHardwareInputs() {
        allHubs.forEach { it.clearBulkCache() }

        // Optional: prefetch bulk data so per-device logTimes don't "blame" the first device on
        // each hub for the full bulk transaction. This is useful for tuning.
        if (FtcLogTuning.prefetchBulkDataEachLoop) {
            allHubs.forEach { hub ->
                val timeToPrefetch = measureTime {
                    try {
                        hub.bulkData
                    } catch (_: Throwable) {
                        // ignore
                    }
                }
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
                    timeToPrefetch.inWholeMicroseconds
                )
            }
        }

        // In live mode, mirror the FTC runtime state into OpModeControls.
        // In replay, allow Logger.processInputs(...) to populate OpModeControls from the log.
        if (!Logger.isReplay()) {
            OpModeControls.started = isStarted
            OpModeControls.stopped = isStopRequested
        }
        Logger.processInputs("OpModeControls", OpModeControls)

        // In replay, drive the OpMode's internal fields so opModeInInit()/opModeIsActive() behave.
        if (Logger.isReplay() && Logger.isRunning()) {
            setBooleanFieldIfPresent(this, "isStarted", OpModeControls.started)
            setBooleanFieldIfPresent(this, "stopRequested", OpModeControls.stopped)
        }

        HardwareMapWrapper.devicesToProcess.forEach {
            val timeToLog = measureTime {
                Logger.processInputs("HardwareMap/${it.key}", it.value)
            }
            Logger.recordOutput(
                "PsiKit/logTimes (us)/${it.key}",
                timeToLog.inWholeMicroseconds
            )
        }
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

    override fun getRuntime() = Logger.getTimestamp()

    override fun waitForStart() {
        if(!Logger.isReplay()) super.waitForStart()
    }

    /*
     * Initializes the hardwaremap and gamepads to use the wrapped PsiKit ones, logs some metadata
     */
    fun psiKitSetup() {
        this.hardwareMap = HardwareMapWrapper(hardwareMap)

        allHubs = this.hardwareMap.getAll(LynxModule::class.java)

        allHubs.forEach {
            it.bulkCachingMode = MANUAL
        }
        this.gamepad1 = GamepadWrapper(this.gamepad1)
        this.gamepad2 = GamepadWrapper(this.gamepad2)
        val annotation = this::class.annotations.firstOrNull {
            it is Autonomous || it is TeleOp
        } ?: TeleOp::class
        Logger.recordMetadata(
            "OpMode Name",
            when(annotation){
                is Autonomous -> annotation.name
                is TeleOp     -> annotation.name
                else          -> error("Impossible")
            }
        )
        Logger.recordMetadata(
            "OpMode type",
            if(annotation is Autonomous) "Autonomous" else "TeleOp"
        )

        /*
        val startedField = OpMode::class.java.fields.first {
            it.name == "isStarted"
        }
        startedField.isAccessible = true
        startedField.set(this, true)
         */
    }
}
