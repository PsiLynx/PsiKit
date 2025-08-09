package org.psilynx.psikit.ftc.structs

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoController
import com.qualcomm.robotcore.hardware.ServoControllerEx
import com.qualcomm.robotcore.hardware.ServoImplEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import org.psilynx.psikit.core.wpi.Struct
import java.nio.ByteBuffer

class ServoData(
    val direction: Servo.Direction,
    val position: Double,
    val pwmLower: Double,
    val pwmUpper: Double,
    val pwmEnabled: Boolean,
): HardwareData {

    override val device = Device(this)

    class Device(var thisRef: ServoData) : ServoImplEx(
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
            override fun getDeviceName() = "MockServo"
            override fun getConnectionInfo() = ""
            override fun getVersion() = 1
            override fun resetDeviceConfigurationForOpMode() {}
            override fun close() {}
        },
        0,
        ServoConfigurationType()
    ) {
        override fun getDirection() = thisRef.direction
        override fun setDirection(direction: Servo.Direction) {}
        override fun getPosition() = thisRef.position
        override fun setPosition(position: Double) {}
        override fun scaleRange(min: Double, max: Double) {}
        override fun getPwmRange() = PwmControl.PwmRange(thisRef.pwmLower, thisRef.pwmUpper)
        override fun setPwmRange(range: PwmControl.PwmRange) {}
        override fun isPwmEnabled() = thisRef.pwmEnabled
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
            Struct.kSizeInt8 +       // direction
            Struct.kSizeDouble * 3 + // position, scaledMin, scaledMax, pwmLower, pwmUpper
            Struct.kSizeInt8         // pwmEnabled
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
        @JvmField
        val struct = ServoDataStruct()
    }
}
