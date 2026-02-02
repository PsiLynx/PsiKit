package org.psilynx.psikit.ftc.test.fakehardware

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit.MM
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import kotlin.Double.Companion.NaN
import kotlin.random.Random


class FakePinpoint: GoBildaPinpointDriver(FakeI2cDeviceSynchSimple(), false) {
    /*
    private val fl =
        HardwareMap.frontLeft(Component.Direction.FORWARD).hardwareDevice
        as FakeMotor
    private val fr =
        HardwareMap.frontRight(Component.Direction.FORWARD).hardwareDevice
        as FakeMotor
    private val bl =
        HardwareMap.backLeft(Component.Direction.FORWARD).hardwareDevice
        as FakeMotor
    private val br =
        HardwareMap.backRight(Component.Direction.FORWARD).hardwareDevice
        as FakeMotor
    */

    var chanceOfNaN = 0.0

    var _pos = Pose2D(
        MM,
        0.0,
        0.0,
        AngleUnit.RADIANS,
        0.0
    )
    private var lastPos = _pos

    override fun update() {
        /*
        val flSpeed =   fl.speed
        val blSpeed =   bl.speed
        val frSpeed = - fr.speed
        val brSpeed = - br.speed
        val drive  = ( flSpeed + frSpeed + blSpeed + brSpeed ) / 4
        val strafe = ( blSpeed + frSpeed - flSpeed - brSpeed ) / 4
        val turn   = ( brSpeed + frSpeed - flSpeed - blSpeed ) / 4
        lastPos = _pos
        val offset = Pose2D(
            drive  * CommandScheduler.deltaTime * maxDriveVelocity,
            strafe * CommandScheduler.deltaTime * maxStrafeVelocity,
            turn   * CommandScheduler.deltaTime * maxTurnVelocity,
        )
        _pos += (offset rotatedBy _pos.heading)
         */
        FakeTimer.addTime(DeviceTimes.pinpoint)
    }
    override fun resetPosAndIMU() {
        _pos = Pose2D(
            MM,
            0.0,
            0.0,
            AngleUnit.RADIANS,
            0.0
        )
    }
    override fun getPosition() =
        if(Random.nextDouble() < chanceOfNaN) Pose2D(
            MM,
            NaN,
            NaN,
            AngleUnit.RADIANS,
            NaN
        )
        else _pos

    override fun setPosition(pos: Pose2D) { _pos = pos }

    override fun getVelX(unit: DistanceUnit)
        = _pos.getX(unit) - lastPos.getX(unit)

    override fun getVelY(unit: DistanceUnit)
            = _pos.getY(unit) - lastPos.getY(unit)

    override fun getHeadingVelocity(unit: UnnormalizedAngleUnit)
            = _pos.getHeading(unit.normalized) - lastPos.getHeading(unit.normalized)

    override fun setOffsets(xOffset: Double, yOffset: Double, unit: DistanceUnit) { }
    override fun setEncoderDirections(
        xEncoder: EncoderDirection?,
        yEncoder: EncoderDirection?
    ) { }
    override fun setEncoderResolution(pods: GoBildaOdometryPods?) { }
}