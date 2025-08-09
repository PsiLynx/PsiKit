package org.psilynx.psikit.ftc.structs

import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.ServoController
import com.qualcomm.robotcore.hardware.ServoControllerEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import org.psilynx.psikit.core.wpi.Struct
import java.nio.ByteBuffer

class CrServoData(
    val direction: DcMotorSimple.Direction,
    val power: Double,
    val pwmLower: Double,
    val pwmUpper: Double,
    val pwmEnabled: Boolean,
) : HardwareData {

    override val device = Device(this)

    class Device(var thisRef: CrServoData) : CRServoImplEx(
        object : ServoControllerEx {
            override fun setServoPwmRange(servo: Int, range: PwmControl.PwmRange) {}
            override fun getServoPwmRange(servo: Int) = PwmControl.PwmRange.defaultRange
            override fun setServoPwmEnable(servo: Int) {}
            override fun setServoPwmDisable(servo: Int) {}
            override fun isServoPwmEnabled(servo: Int) = true
            override fun setServoType(servo: Int, servoType: ServoConfigurationType?) {}
            override fun pwmEnable() {}
            override fun pwmDisable() {}
            override fun getPwmStatus() = ServoController.PwmStatus.ENABLED
            override fun setServoPosition(servo: Int, position: Double) {}
            override fun getServoPosition(servo: Int) = 0.0
            override fun getManufacturer() = HardwareDevice.Manufacturer.Other
            override fun getDeviceName() = "MockCrServo"
            override fun getConnectionInfo() = ""
            override fun getVersion() = 1
            override fun resetDeviceConfigurationForOpMode() {}
            override fun close() {}
        },
        0,
        ServoConfigurationType()
    ) {
        override fun getDirection() = thisRef.direction
        override fun setDirection(direction: DcMotorSimple.Direction) {}
        override fun getPower() = thisRef.power
        override fun setPower(power: Double) {}
        override fun getPwmRange() = PwmControl.PwmRange(thisRef.pwmLower, thisRef.pwmUpper)
        override fun setPwmRange(range: PwmControl.PwmRange) {}
        override fun isPwmEnabled() = thisRef.pwmEnabled
        override fun setPwmEnable() {}
        override fun setPwmDisable() {}
    }

    constructor(servo: CRServoImplEx) : this(
        direction = servo.direction,
        power = servo.power,
        pwmLower = servo.pwmRange.usPulseLower.toDouble(),
        pwmUpper = servo.pwmRange.usPulseUpper.toDouble(),
        pwmEnabled = servo.isPwmEnabled
    )

    class CrServoDataStruct : Struct<CrServoData> {
        override fun getTypeClass() = CrServoData::class.java

        override fun getTypeName() = "crServoData"

        override fun getSize() = (
            Struct.kSizeInt8 +        // direction
            Struct.kSizeDouble * 3 +  // power, pwmLower, pwmUpper
            Struct.kSizeInt8          // pwmEnabled
        )

        override fun getSchema() = (
            "enum{forward=0,reverse=1}int8 direction; " +
            "double power;double pwmLower;double pwmUpper;bool pwmEnabled"
        )

        override fun unpack(bb: ByteBuffer): CrServoData {
            return CrServoData(
                direction = DcMotorSimple.Direction.entries[bb.get().toInt()],
                power = bb.getDouble(),
                pwmLower = bb.getDouble(),
                pwmUpper = bb.getDouble(),
                pwmEnabled = bb.get() == 1.toByte()
            )
        }

        override fun pack(bb: ByteBuffer, value: CrServoData) {
            with(value) {
                bb.put(direction.ordinal.toByte())
                bb.putDouble(power)
                bb.putDouble(pwmLower)
                bb.putDouble(pwmUpper)
                bb.put((if (pwmEnabled) 1 else 0).toByte())
            }
        }
    }

    companion object {
        val empty = CrServoData(
            DcMotorSimple.Direction.FORWARD,
            0.0,
            500.0,
            2500.0,
            true
        )
        @JvmField
        val struct = CrServoDataStruct()
    }
}
