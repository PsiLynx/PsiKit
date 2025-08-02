package org.firstinspires.ftc.teamcode.fakehardware

import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction
import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD
import org.firstinspires.ftc.teamcode.hardware.HardwareMap.DeviceTimes
import org.firstinspires.ftc.teamcode.sim.FakeTimer


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
        FakeTimer.addTime(DeviceTimes.chubServo) //TODO: setup correctly
    }
    override fun getPower() = _power

    override fun getController() = throw NotImplementedError("please don't. why would you even try and do that?")
    override fun getPortNumber() = 0
}