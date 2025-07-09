package org.psilynx.psikit.ftc

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.eventloop.opmode.OpMode


abstract class PsikitOpmode: OpMode() {
    init {
        val modules =
            this.hardwareMap.getAll<LynxModule>(LynxModule::class.java)
        modules.forEach {
            //it.
        }
    }
}