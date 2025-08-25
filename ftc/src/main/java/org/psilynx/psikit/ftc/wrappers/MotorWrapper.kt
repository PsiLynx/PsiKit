package org.psilynx.psikit.ftc.wrappers

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
import org.psilynx.psikit.core.LogTable

class MotorWrapper(
    private val device: DcMotorImplEx?
) : DcMotorImplEx(
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
        override fun isMotorOverCurrent(motor: Int) = false
    },
    0,
    DcMotorSimple.Direction.FORWARD,
    MotorConfigurationType()
), HardwareInput<DcMotorImplEx> {

    private var _zeroPowerBehavior = DcMotor.ZeroPowerBehavior.UNKNOWN
    private var _powerFloat  = false
    private var _overCurrent = false
    private var _currentPos  = 0
    private var _currentVel  = 0.0
    private var _power       = 0.0
    private var _deviceName  = "MockMotor"
    private var _version     = 1
    private var _connectionInfo = ""
    private var _manufacturer   = HardwareDevice.Manufacturer.Other

    override fun new(wrapped: DcMotorImplEx) = MotorWrapper(wrapped)

    override fun toLog(table: LogTable) {
        table.put("zeroPowerBehavior", zeroPowerBehavior)
        table.put("powerFloat", powerFloat)
        table.put("overCurrent", isOverCurrent)
        table.put("currentPos", currentPosition)
        table.put("currentVel", velocity)
        table.put("power", power)
        table.put("deviceName", deviceName)
        table.put("version", version)
        table.put("connectionInfo", connectionInfo)
        table.put("manufacturer", manufacturer)

        _zeroPowerBehavior = zeroPowerBehavior
        _powerFloat        = powerFloat
        _overCurrent       = isOverCurrent
        _currentPos        = currentPosition
        _currentVel        = velocity
        _power             = power
        _deviceName        = deviceName
        _version           = version
        _connectionInfo    = connectionInfo
        _manufacturer      = manufacturer
    }

    override fun fromLog(table: LogTable) {
        _zeroPowerBehavior = table.get("zeroPowerBehavior", DcMotor.ZeroPowerBehavior.UNKNOWN)
        _powerFloat        = table.get("powerFloat", false)
        _overCurrent       = table.get("overCurrent", false)
        _currentPos        = table.get("currentPos", 0)
        _currentVel        = table.get("currentVel", 0.0)
        _power             = table.get("power", 0.0)
        _deviceName        = table.get("deviceName", "MockMotor")
        _version           = table.get("version", 1)
        _connectionInfo    = table.get("connectionInfo", "")
        _manufacturer      = table.get("manufacturer", HardwareDevice.Manufacturer.Other)
    }

    override fun getZeroPowerBehavior() = _zeroPowerBehavior
    override fun setZeroPowerBehavior(zeroPowerBehavior: DcMotor.ZeroPowerBehavior?)
        = device?.setZeroPowerBehavior(zeroPowerBehavior) ?: Unit

    override fun getPowerFloat() = _powerFloat
    override fun getCurrentPosition() = _currentPos
    override fun getVelocity() = _currentVel
    override fun getPower() = _power
    override fun isOverCurrent() = _overCurrent


    override fun getDeviceName() = _deviceName
    override fun getVersion() = _version
    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer() = _manufacturer

    override fun setPower(power: Double) = device?.setPower(power) ?: Unit

    override fun close() { device?.close() }

    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}