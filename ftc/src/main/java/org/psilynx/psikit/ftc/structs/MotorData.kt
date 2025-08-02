package org.psilynx.psikit.ftc.structs

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import com.qualcomm.robotcore.hardware.Servo
import org.psilynx.psikit.core.wpi.Struct
import org.psilynx.psikit.core.wpi.StructSerializable
import java.nio.ByteBuffer

data class MotorData(
    val zeroPowerBehavior: DcMotor.ZeroPowerBehavior,
    val powerFloat: Boolean,
    val overCurrent: Boolean,
    val currentPos: Int,
    val currentVel: Double,
    val power: Double,
): StructSerializable {
    val struct = MotorDataStruct()
    val device = object : DcMotorImplEx(null, 0) {
        override fun getZeroPowerBehavior() = this@MotorData.zeroPowerBehavior
        override fun getCurrentPosition() = this@MotorData.currentPos
        override fun isOverCurrent() = this@MotorData.overCurrent
        override fun getPowerFloat() = this@MotorData.powerFloat
        override fun getVelocity() = this@MotorData.currentVel
        override fun getPower() = this@MotorData.power
    }

    constructor(motor: DcMotorImplEx) : this(
        zeroPowerBehavior = motor.zeroPowerBehavior,
        powerFloat = motor.zeroPowerBehavior == DcMotor.ZeroPowerBehavior.FLOAT,
        overCurrent = false, // requires extra sensors to determine
        currentPos = motor.currentPosition,
        currentVel = motor.velocity,
        power = motor.power
    )
    class MotorDataStruct : Struct<MotorData> {
        override fun getTypeClass() = MotorData::class.java

        override fun getTypeName() = "motorData"

        override fun getSize() = (
            + Struct.kSizeInt8
            + Struct.kSizeBool
            + Struct.kSizeBool
            + Struct.kSizeInt16
            + Struct.kSizeDouble
            + Struct.kSizeDouble
        )

        override fun getSchema() = (
            "enum{brake=0,float=1,unknown=2}int8 zeroPowerBehavior;" +
            "bool powerFloat;" +
            "bool overCurrent;int16 pos,double vel;double power"
        )

        override fun unpack(bb: ByteBuffer) = MotorData(
            DcMotor.ZeroPowerBehavior.entries[bb.get().toInt()],
            powerFloat = bb.get() == (0x01).toByte(),
            overCurrent = bb.get() == (0x01).toByte(),
            currentPos = bb.getInt(),
            currentVel = bb.getDouble(),
            power = bb.getDouble(),
        )

        override fun pack(bb: ByteBuffer, value: MotorData) {
            with(value) {
                bb.put(zeroPowerBehavior.ordinal.toByte())
                bb.put((if (powerFloat) 1 else 0).toByte())
                bb.put((if (overCurrent) 1 else 0).toByte())
                bb.putInt(currentPos)
                bb.putDouble(currentVel)
                bb.putDouble(power)
            }
        }
    }
    companion object {
        val empty = MotorData(
            DcMotor.ZeroPowerBehavior.UNKNOWN,
            false,
            false,
            0,
            0.0,
            0.0
        )
    }
}
