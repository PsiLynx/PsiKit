package org.psilynx.psikit.ftc.structs

import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import org.psilynx.psikit.core.wpi.Struct
import org.psilynx.psikit.core.wpi.StructSerializable
import java.nio.ByteBuffer

data class CrServoData(
    val direction: DcMotorSimple.Direction,
    val power: Double,
    val pwmLower: Double,
    val pwmUpper: Double,
    val pwmEnabled: Boolean,
) : StructSerializable {
    val struct = CrServoDataStruct()

    val device = object : CRServoImplEx(null, 0, ServoConfigurationType()) {
        override fun getDirection() = this@CrServoData.direction
        override fun setDirection(direction: DcMotorSimple.Direction) {}
        override fun getPower() = this@CrServoData.power
        override fun setPower(power: Double) {}
        override fun getPwmRange() = PwmControl.PwmRange(
            this@CrServoData.pwmLower,
            this@CrServoData.pwmUpper
        )
        override fun setPwmRange(range: PwmControl.PwmRange) {}
        override fun isPwmEnabled() = this@CrServoData.pwmEnabled
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
            Struct.kSizeDouble +      // power
            Struct.kSizeDouble * 2 +  // pwmLower, pwmUpper
            Struct.kSizeBool          // pwmEnabled
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
    }
}
