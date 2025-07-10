package org.psilynx.psikit.ftc.fakehardware

import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction
import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD

class FakeCRServo: FakeHardware, CRServo {
    private var _power = 0.0
    private var _dir = FORWARD

    override fun update(deltaTime: Double) { }
    override fun resetDeviceConfigurationForOpMode() {
        _power = 0.0
        _dir = FORWARD
    }
    override fun setDirection(direction: Direction?) {
        _dir = direction!!
    }
    override fun getDirection() = _dir
    override fun setPower(power: Double) {
        _power = power.coerceIn(-1.0, 1.0)
    }
    override fun getPower() = _power
    override fun getController() = TODO("Not Yet Implemented")
    override fun getPortNumber() = 0
}

