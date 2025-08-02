package org.firstinspires.ftc.teamcode.fakehardware

import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.AnalogInputController
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.util.SerialNumber

class FakeAnalogInputController(): AnalogInputController, FakeHardware {

    override fun getAnalogInputVoltage(channel: Int): Double {
        TODO("Not yet implemented")
    }
    override fun getMaxAnalogInputVoltage() = 3.2
    override fun getSerialNumber(): SerialNumber? { TODO("Not yet implemented") }
    override fun resetDeviceConfigurationForOpMode() { }

    override fun update(deltaTime: Double) { }

}
class FakeAnalogInput: FakeHardware, AnalogInput(
    FakeAnalogInputController(),
    0
) {
    var _voltage = 0.0

    fun setVoltage(newVoltage: Double) { _voltage = newVoltage }
    override fun getVoltage() = _voltage

    override fun update(deltaTime: Double) { }

    override fun getManufacturer(): HardwareDevice.Manufacturer {
        TODO("Not yet implemented")
    }

    override fun getDeviceName(): String {
        TODO("Not yet implemented")
    }

    override fun getConnectionInfo(): String {
        TODO("Not yet implemented")
    }

    override fun getVersion(): Int {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
