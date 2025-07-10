package org.psilynx.psikit.ftc.fakehardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotor.ZeroPowerBehavior.UNKNOWN
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType
import kotlin.math.abs

open class FakeMotor: FakeHardware, DcMotor {

    private var _power = 0.0
    private var _direction = FORWARD
    private var _zeroPowerBehavior = UNKNOWN

    override fun resetDeviceConfigurationForOpMode() {
         zeroPowerBehavior = UNKNOWN
         direction         = FORWARD //TODO: make sure this is correct
        _power             = 0.0
    }

    override fun getDirection() = _direction
    override fun setDirection(p0: DcMotorSimple.Direction?) { _direction = p0!!}

    override fun getPower() = _power
    override fun setPower(p0: Double) {
        _power = p0.coerceIn(-1.0, 1.0)
    }

    override fun getZeroPowerBehavior() = _zeroPowerBehavior
    override fun setZeroPowerBehavior(p0: DcMotor.ZeroPowerBehavior?) { _zeroPowerBehavior = p0!!}

    override fun getCurrentPosition() = _pos.toInt()

    open fun setCurrentPosition(newPos:Number){ _pos = newPos.toDouble() }

    // ==== dummy methods ====
    @Deprecated("Deprecated in Java")
    override fun setPowerFloat() { }
    override fun getPowerFloat() = false
    override fun setTargetPosition(p0: Int) { }
    override fun getTargetPosition() = 0
    override fun isBusy() = false
    override fun setMode(p0: DcMotor.RunMode?) { }
    override fun getMode() = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    override fun getMotorType() = MotorConfigurationType()
    override fun setMotorType(p0: MotorConfigurationType?) { }
    override fun getController() = TODO( "You're in too deep if you need the hardwareDevice's controller" )
    override fun getPortNumber() = 0

}
