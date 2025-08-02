package org.firstinspires.ftc.teamcode.fakehardware

import org.firstinspires.ftc.teamcode.command.internal.CommandScheduler
import org.firstinspires.ftc.teamcode.component.Component
import org.firstinspires.ftc.teamcode.hardware.HardwareMap
import org.firstinspires.ftc.teamcode.hardware.HardwareMap.DeviceTimes
import org.firstinspires.ftc.teamcode.sim.FakeTimer
import org.firstinspires.ftc.teamcode.util.geometry.Pose2D
import org.firstinspires.ftc.teamcode.util.GoBildaPinpointDriver
import org.firstinspires.ftc.teamcode.sim.SimConstants.maxDriveVelocity
import org.firstinspires.ftc.teamcode.sim.SimConstants.maxStrafeVelocity
import org.firstinspires.ftc.teamcode.sim.SimConstants.maxTurnVelocity
import kotlin.Double.Companion.NaN
import kotlin.random.Random


class FakePinpoint: GoBildaPinpointDriver(FakeI2cDeviceSynchSimple(), false) {
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

    var chanceOfNaN = 0.0

    var _pos = Pose2D(0.0, 0.0, 0.0)
    private var lastPos = _pos

    override fun update() {
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
        FakeTimer.addTime(DeviceTimes.pinpoint)
    }
    override fun resetPosAndIMU() {
        _pos = Pose2D(0.0, 0.0, 0.0)
    }
    override fun getPosition() =
        if(Random.nextDouble() < chanceOfNaN) Pose2D(NaN, NaN, NaN)
        else _pos

    override fun setPosition(pos: Pose2D?): Pose2D {
        _pos = pos!!
        return _pos
    }

    override fun getVelocity() = _pos - lastPos

    override fun setOffsets(xOffset: Double, yOffset: Double) { }
    override fun setEncoderDirections(
        xEncoder: Component.Direction?,
        yEncoder: Component.Direction?
    ) { }
    override fun setEncoderResolution(pods: GoBildaOdometryPods?) { }
}