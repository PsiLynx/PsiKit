package org.psilynx.psikit.ftc

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.wrappers.GamepadWrapper
import kotlin.time.measureTime


abstract class PsiKitOpMode: LinearOpMode() {
    val psiKit_isStopRequested get() = OpModeControls.stopped

    val psiKit_isStarted get() = OpModeControls.started

    /*
     * updates the hardware map input. this must be called before accessing
     * any hardware every loop. It's safest to call it right after
     * Logger.periodicBeforeUser()
     * note that if you have a loop that runs before isStarted, you must
     * update this in that loop as well.
     */
    fun processHardwareInputs() {
        Logger.processInputs("OpModeControls", OpModeControls)

        (this.hardwareMap as HardwareMapWrapper).devicesToProcess.forEach {
            val timeToLog = measureTime {
                Logger.processInputs("HardwareMap/I2c/${it.key}", it.value)
            }
            Logger.recordOutput(
                "PsiKit/logTimes (us)/${it.key}",
                timeToLog.inWholeMicroseconds
            )
        }
    }

    override fun getRuntime() = Logger.getTimestamp()

    override fun waitForStart() {
        if(!Logger.isReplay()) super.waitForStart()
    }

    /*
     * Initializes the hardwaremap and gamepads to use the wrapped PsiKit ones, logs some metadata
     */
    fun psikitSetup() {
        this.hardwareMap = HardwareMapWrapper(hardwareMap)
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
