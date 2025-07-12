package org.psilynx.psikit.ftc.fakehardware

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import java.util.Optional
import kotlin.Double.Companion.NaN
import kotlin.random.Random


class FakePinpoint: GoBildaPinpointDriver(FakeI2cDeviceSynchSimple(), false) {
    private val fl =
        HardwareMap.frontLeft(DcMotorSimple.Direction.FORWARD).hardwareDevice
        as FakeDcMotor
    private val fr =
        HardwareMap.frontRight(DcMotorSimple.Direction.FORWARD).hardwareDevice
        as FakeDcMotor
    private val bl =
        HardwareMap.backLeft(DcMotorSimple.Direction.FORWARD).hardwareDevice
        as FakeDcMotor
    private val br =
        HardwareMap.backRight(DcMotorSimple.Direction.FORWARD).hardwareDevice
        as FakeDcMotor

    var chanceOfNaN = 0.0

    var _pos = zeroPose
    var _velocity = zeroPose
    var lastUpdateSeconds = Optional.empty<Double>()
    private var lastPos = _pos

    private val timer = FakeTimer()

    override fun update() {
        val flSpeed =   fl.speed
        val blSpeed =   bl.speed
        val frSpeed = - fr.speed
        val brSpeed = - br.speed
        val drive  = ( flSpeed + frSpeed + blSpeed + brSpeed ) / 4
        val strafe = ( blSpeed + frSpeed - flSpeed - brSpeed ) / 4
        val turn   = ( brSpeed + frSpeed - flSpeed - blSpeed ) / 4
        val offset = Pose2D(
            DistanceUnit.INCH,
            drive  * CommandScheduler.deltaTime * maxDriveVelocity,
            strafe * CommandScheduler.deltaTime * maxStrafeVelocity,
            AngleUnit.RADIANS,
            turn   * CommandScheduler.deltaTime * maxTurnVelocity,
        )
        _pos += (offset rotatedBy _pos.heading)

        if(lastUpdateSeconds.exists) {
            _velocity = (
                (_pos - lastPos)
                / (
                    Globals.currentTime
                    - ( lastUpdateSeconds or 0.0 )
                )
            )
        }
        lastUpdateSeconds = Optional(Globals.currentTime)
        lastPos = _pos
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

    override fun getVelocity() = _velocity

    override fun setOffsets(xOffset: Double, yOffset: Double) { }
    override fun setEncoderDirections(
        xEncoder: DcMotorSimple.Direction?,
        yEncoder: DcMotorSimple.Direction?
    ) { }
    override fun setEncoderResolution(pods: GoBildaOdometryPods?) { }

    private val zeroPose get() = Pose2D(
        DistanceUnit.INCH,
        0.0,
        0.0,
        AngleUnit.RADIANS,
        0.0
    )
}