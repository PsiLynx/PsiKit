package org.psilynx.psikit.ftc

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.eventloop.opmode.OpMode


abstract class PsikitOpmode: OpMode() {
    override fun init() {
        this.hardwareMap = HardwareMapInput(hardwareMap)
    }
}