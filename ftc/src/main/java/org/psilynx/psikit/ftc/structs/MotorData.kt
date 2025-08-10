package org.psilynx.psikit.ftc.structs

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorControllerEx
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.PIDCoefficients
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.psilynx.psikit.core.wpi.Struct
import java.nio.ByteBuffer

class MotorData(
    val zeroPowerBehavior: DcMotor.ZeroPowerBehavior,
    val powerFloat: Boolean,
    val overCurrent: Boolean,
    val currentPos: Int,
    val currentVel: Double,
    val power: Double,
) : HardwareData {

    override val device = Device(this)

    class Device(var thisRef: MotorData) : DcMotorImplEx(
        object : DcMotorControllerEx {
            override fun setMotorType(motor: Int, motorType: MotorConfigurationType?) {}
            override fun getMotorType(motor: Int) = MotorConfigurationType.getUnspecifiedMotorType()
            override fun setMotorMode(motor: Int, mode: DcMotor.RunMode?) {}
            override fun getMotorMode(motor: Int) = DcMotor.RunMode.RUN_WITHOUT_ENCODER
            override fun setMotorPower(motor: Int, power: Double) {}
            override fun getMotorPower(motor: Int) = 0.0
            override fun isBusy(motor: Int) = false
            override fun setMotorZeroPowerBehavior(motor: Int, zeroPowerBehavior: DcMotor.ZeroPowerBehavior?) {}
            override fun getMotorZeroPowerBehavior(motor: Int) = DcMotor.ZeroPowerBehavior.UNKNOWN
            override fun getMotorPowerFloat(motor: Int) = false
            override fun setMotorTargetPosition(motor: Int, position: Int) {}
            override fun getMotorTargetPosition(motor: Int) = 0
            override fun getMotorCurrentPosition(motor: Int) = 0
            override fun resetDeviceConfigurationForOpMode(motor: Int) {}
            override fun getManufacturer() = HardwareDevice.Manufacturer.Other
            override fun getDeviceName() = "MockMotor"
            override fun getConnectionInfo() = ""
            override fun getVersion() = 1
            override fun resetDeviceConfigurationForOpMode() {}
            override fun close() {}
            override fun setMotorEnable(motor: Int) {}
            override fun setMotorDisable(motor: Int) {}
            override fun isMotorEnabled(motor: Int) = false
            override fun setMotorVelocity(motor: Int, ticksPerSecond: Double) {}
            override fun setMotorVelocity(motor: Int, angularRate: Double, unit: AngleUnit?) {}
            override fun getMotorVelocity(motor: Int) = 0.0
            override fun getMotorVelocity(motor: Int, unit: AngleUnit?) = 0.0
            override fun setPIDCoefficients(motor: Int, mode: DcMotor.RunMode?, pidCoefficients: PIDCoefficients?) {}
            override fun setPIDFCoefficients(motor: Int, mode: DcMotor.RunMode?, pidfCoefficients: PIDFCoefficients?) {}
            override fun getPIDCoefficients(motor: Int, mode: DcMotor.RunMode?) = PIDCoefficients()
            override fun getPIDFCoefficients(motor: Int, mode: DcMotor.RunMode?) = PIDFCoefficients()
            override fun setMotorTargetPosition(motor: Int, position: Int, tolerance: Int) {}
            override fun getMotorCurrent(motor: Int, unit: CurrentUnit?) = 0.0
            override fun getMotorCurrentAlert(motor: Int, unit: CurrentUnit?) = 0.0
            override fun setMotorCurrentAlert(motor: Int, current: Double, unit: CurrentUnit?) {}
            override fun isMotorOverCurrent(motor: Int) = thisRef.overCurrent
        },
        0,
        DcMotorSimple.Direction.FORWARD,
        MotorConfigurationType()
    ) {
        override fun getZeroPowerBehavior() = thisRef.zeroPowerBehavior
        override fun getCurrentPosition() = thisRef.currentPos
        override fun isOverCurrent() = thisRef.overCurrent
        override fun getPowerFloat() = thisRef.powerFloat
        override fun getVelocity() = thisRef.currentVel
        override fun getPower() = thisRef.power
    }

    constructor(motor: DcMotorImplEx) : this(
        zeroPowerBehavior = motor.zeroPowerBehavior,
        powerFloat = motor.zeroPowerBehavior == DcMotor.ZeroPowerBehavior.FLOAT,
        overCurrent = false, // This would normally require additional sensors
        currentPos = motor.currentPosition,
        currentVel = motor.velocity,
        power = motor.power
    )

    class MotorDataStruct : Struct<MotorData> {
        override fun getTypeClass() = MotorData::class.java

        override fun getTypeName() = "motorData"

        override fun getSize() = (
            + Struct.kSizeInt8
            + Struct.kSizeInt8
            + Struct.kSizeInt8
            + Struct.kSizeInt32
            + Struct.kSizeDouble
            + Struct.kSizeDouble
        )

        override fun getSchema() = (
            "enum{brake=0,float=1,unknown=2}int8 zeroPowerBehavior;" +
            "bool powerFloat;" +
            "bool overCurrent;int32 pos,double vel;double power"
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

        @JvmField
        val struct = MotorDataStruct()
    }
}
