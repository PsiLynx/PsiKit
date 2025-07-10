package org.psilynx.psikit.ftc.fakehardware

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.LogoFacingDirection
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.UsbFacingDirection
import com.qualcomm.robotcore.hardware.IMU
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.RADIANS
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference
import org.firstinspires.ftc.robotcore.external.navigation.Orientation
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles

class FakeIMU: FakeHardware, IMU{
    private var _angularVelocity = AngularVelocity(
        RADIANS,
        0.0F,
        0.0F,
        0.0F,
        0L
    )
    private var _orientation = YawPitchRollAngles(
        RADIANS,
        0.0,
        0.0,
        0.0,
        0L
    )

    private lateinit var _parameters:IMU.Parameters

    override fun resetDeviceConfigurationForOpMode() {
        _orientation = YawPitchRollAngles(
            RADIANS,
            0.0,
            0.0,
            0.0,
            0L
        )
    }

    override fun initialize(p0: IMU.Parameters?): Boolean {
        _parameters = p0?:IMU.Parameters(
            RevHubOrientationOnRobot(
                LogoFacingDirection.UP,
                UsbFacingDirection.FORWARD
            )
        )
        return true
    }

    override fun resetYaw() {
        _orientation = YawPitchRollAngles(
            RADIANS,
            0.0,
            _orientation.getPitch(RADIANS),
            _orientation.getRoll(RADIANS),
            _orientation.acquisitionTime
        )
    }

    override fun getRobotYawPitchRollAngles() = _orientation

    override fun getRobotOrientation(
        p0: AxesReference?,
        p1: AxesOrder?,
        p2: AngleUnit?
    ): Orientation {
        TODO("Not Yet Implemented")
    }

    override fun getRobotOrientationAsQuaternion() = TODO("Not Yet Implemented")

    override fun getRobotAngularVelocity(p0: AngleUnit?): AngularVelocity = (
            if (p0 != RADIANS) _angularVelocity
            else _angularVelocity.toAngleUnit(DEGREES)
            )

    override fun update(deltaTime: Double) { }


}