package org.psilynx.psikit.ftc.fakehardware

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.ServoController
import com.qualcomm.robotcore.hardware.ServoControllerEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType

class FakeServoController: ServoController {
    override fun pwmEnable() { }
    override fun pwmDisable() { }
    override fun getPwmStatus(): ServoController.PwmStatus? {
        TODO("Not yet implemented")
    }
    override fun setServoPosition(servo: Int, position: Double) { }
    override fun getServoPosition(servo: Int): Double {
        TODO("Not yet implemented")
    }
    override fun getManufacturer(): HardwareDevice.Manufacturer? {
        TODO("Not yet implemented")
    }
    override fun getDeviceName(): String? { TODO("Not yet implemented") }
    override fun getConnectionInfo(): String? { TODO("Not yet implemented") }
    override fun getVersion(): Int { TODO("Not yet implemented") }
    override fun resetDeviceConfigurationForOpMode() { }
    override fun close() { }
}
