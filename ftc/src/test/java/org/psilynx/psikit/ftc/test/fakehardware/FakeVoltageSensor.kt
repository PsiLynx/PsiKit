package org.psilynx.psikit.ftc.test.fakehardware

import com.qualcomm.robotcore.hardware.VoltageSensor

class FakeVoltageSensor: FakeHardware, VoltageSensor {
    private var _voltage = 13.0

    override fun getVoltage() = _voltage
    fun setVoltage(v: Number){
        _voltage = v.toDouble()
    }

    override fun update(deltaTime: Double) { }
    override fun resetDeviceConfigurationForOpMode() { }
}