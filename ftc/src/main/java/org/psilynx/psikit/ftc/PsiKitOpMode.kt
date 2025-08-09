package org.psilynx.psikit.ftc

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.inputs.HardwareMapInput


abstract class PsiKitOpMode: LinearOpMode() {
    /*
     * updates the hardware map input. this must be called before accessing
     * any hardware every loop. It's safest to call it right after
     * Logger.periodicBeforeUser()
     */
    fun processHardwareMapInput() {
        Logger.processInputs(
            "HardwareMap",
            this.hardwareMap as HardwareMapInput
        )
        (this.hardwareMap as HardwareMapInput).devicesToProcess.forEach {
            Logger.processInputs("HardwareMap/I2c/${it.key}", it.value)
        }
    }

    override fun waitForStart() {
        if(!Logger.isReplay()) super.waitForStart()
    }

    /*
     * Initializes the hardwaremap to use the wrapped PsiKit one. If you want
     *  to override init(), you must call super.init() as the first line.
     */
    fun psikitSetup() {
        this.hardwareMap = HardwareMapInput(hardwareMap)
        val annotation = this::class.annotations.first {
            it is Autonomous || it is TeleOp
        }
        Logger.recordMetadata(
            "OpMode Name",
            when(annotation){
                is Autonomous -> annotation.name
                is TeleOp     -> annotation.name
                else          -> error("Impossible")
            }
        )
        Logger.recordMetadata(
            "Is Autonomous",
            (annotation is Autonomous).toString()
        )
    }
}