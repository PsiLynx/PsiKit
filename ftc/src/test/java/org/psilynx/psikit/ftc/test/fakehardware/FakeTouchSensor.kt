package org.psilynx.psikit.ftc.test.fakehardware

import com.qualcomm.robotcore.hardware.TouchSensor


class FakeTouchSensor: FakeHardware, TouchSensor {
    private var _pressed = false

    override fun update(deltaTime: Double) { }

    override fun resetDeviceConfigurationForOpMode() { _pressed = false }

    override fun getValue() = if(_pressed) 1.0 else 0.0

    override fun isPressed() = _pressed

    fun setState(newState: Boolean){ _pressed = newState }
}