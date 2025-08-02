package org.psilynx.psikit.ftc.structs

import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.ServoImplEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import org.psilynx.psikit.core.wpi.Struct
import org.psilynx.psikit.core.wpi.StructSerializable
import java.nio.ByteBuffer
import java.security.MessageDigest

data class ServoData(
    val direction: Servo.Direction,
    val position: Double,
    val pwmLower: Double,
    val pwmUpper: Double,
    val pwmEnabled: Boolean,
) : StructSerializable {
    val struct = ServoDataStruct()

    val device = object : ServoImplEx(null, 0, ServoConfigurationType()) {
            override fun getDirection() = this@ServoData.direction
            override fun setDirection(direction: Servo.Direction) {}
            override fun getPosition() = this@ServoData.position
            override fun setPosition(position: Double) {}
            override fun scaleRange(min: Double, max: Double) {}
            override fun getPwmRange() = PwmControl.PwmRange(
                this@ServoData.pwmLower,
                this@ServoData.pwmUpper
            )
            override fun setPwmRange(range: PwmControl.PwmRange) {}
            override fun isPwmEnabled() = this@ServoData.pwmEnabled
            override fun setPwmEnable() {}
            override fun setPwmDisable() {}
        }
    constructor(servo: ServoImplEx) : this(
        direction = servo.direction,
        position = servo.position,
        pwmLower = servo.pwmRange.usPulseLower.toDouble(),
        pwmUpper = servo.pwmRange.usPulseUpper.toDouble(),
        pwmEnabled = servo.isPwmEnabled
    )

    class ServoDataStruct : Struct<ServoData> {
        override fun getTypeClass() = ServoData::class.java

        override fun getTypeName() = "servoData"

        override fun getSize() = (
            8 * 4 +
            Struct.kSizeInt8 +       // direction
            Struct.kSizeDouble * 3 + // position, scaledMin, scaledMax, pwmLower, pwmUpper
            Struct.kSizeBool         // pwmEnabled
        )

        override fun getSchema() = (
            "enum{forward=0,reverse=1}int8 direction; " +
            "double position; double pwmLower; double pwmUpper; bool pwmEnabled"
        )

        override fun unpack(bb: ByteBuffer): ServoData {
            return ServoData(
                direction = Servo.Direction.entries[bb.get().toInt()],
                position = bb.getDouble(),
                pwmLower = bb.getDouble(),
                pwmUpper = bb.getDouble(),
                pwmEnabled = bb.get() == 1.toByte()
            )
        }

        override fun pack(bb: ByteBuffer, value: ServoData) {
            with(value) {
                bb.put(direction.ordinal.toByte())
                bb.putDouble(position)
                bb.putDouble(pwmLower)
                bb.putDouble(pwmUpper)
                bb.put((if (pwmEnabled) 1 else 0).toByte())
            }
        }
    }
    companion object {
        val empty = ServoData(
            Servo.Direction.FORWARD,
            0.0,
            500.0,
            2500.0,
            true
        )
    }

}
