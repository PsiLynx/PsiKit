package org.psilynx.psikit.ftc.fakehardware

import com.qualcomm.hardware.lynx.LynxDcMotorController
import com.qualcomm.hardware.lynx.LynxModule

class FakeDcMotorController(
    module: LynxModule
): LynxDcMotorController(null, module) {

    override fun getDeviceName(): String? {
        TODO("Not Yet Implemented")
    }
    override fun get
}