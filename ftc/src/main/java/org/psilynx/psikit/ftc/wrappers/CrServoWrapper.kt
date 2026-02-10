package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.ServoController
import com.qualcomm.robotcore.hardware.ServoControllerEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import org.psilynx.psikit.ftc.FtcLogTuning
import org.psilynx.psikit.core.LogTable

class CrServoWrapper(private val device: CRServoImplEx?):
    CRServoImplEx(
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
    ),
    HardwareInput<CRServoImplEx>
{

    private var _direction   = DcMotorSimple.Direction.FORWARD
    private var _power       = 0.0
    private var _pwmLower    = 500.0
    private var _pwmUpper    = 2500.0
    private var _pwmEnabled  = true
    private var _deviceName  = "MockCrServo"
    private var _version     = 1
    private var _connectionInfo = ""
    private var _manufacturer   = HardwareDevice.Manufacturer.Other

    private var lastSampleNs: Long = Long.MIN_VALUE

    private fun secondsSince(ns: Long): Double {
        if (ns == Long.MIN_VALUE) return Double.POSITIVE_INFINITY
        return (System.nanoTime() - ns) / 1_000_000_000.0
    }

    private fun shouldSampleNow(): Boolean {
        val period = FtcLogTuning.nonBulkReadPeriodSec
        if (period <= 0.0) return true
        return secondsSince(lastSampleNs) >= period
    }

    override fun new(wrapped: CRServoImplEx?) = CrServoWrapper(wrapped)

    override fun toLog(table: LogTable) {
        device!!

        if (!shouldSampleNow()) {
            // Skip reads/writes this loop; LogTable retains the last values.
            return
        }
        lastSampleNs = System.nanoTime()

        _direction      = device.direction
        _power          = device.power
        _pwmLower       = device.pwmRange.usPulseLower.toDouble()
        _pwmUpper       = device.pwmRange.usPulseUpper.toDouble()
        _pwmEnabled     = device.isPwmEnabled
        _deviceName     = device.deviceName
        _version        = device.version
        _connectionInfo = device.connectionInfo
        _manufacturer   = device.manufacturer

        table.put("direction", direction)
        table.put("power", power)
        table.put("pwmLower", pwmRange.usPulseLower)
        table.put("pwmUpper", pwmRange.usPulseUpper)
        table.put("pwmEnabled", isPwmEnabled)
        table.put("deviceName", deviceName)
        table.put("version", version)
        table.put("connectionInfo", connectionInfo)
        table.put("manufacturer", manufacturer)

    }

    override fun fromLog(table: LogTable) {
        _direction      = table.get("direction", DcMotorSimple.Direction.FORWARD)
        _power          = table.get("power", 0.0)
        _pwmLower       = table.get("pwmLower", 500.0)
        _pwmUpper       = table.get("pwmUpper", 2500.0)
        _pwmEnabled     = table.get("pwmEnabled", true)
        _deviceName     = table.get("deviceName", "MockCrServo")
        _version        = table.get("version", 1)
        _connectionInfo = table.get("connectionInfo", "")
        _manufacturer   = table.get("manufacturer", HardwareDevice.Manufacturer.Other)
    }

    override fun getDirection() = _direction

    override fun setDirection(direction: DcMotorSimple.Direction) =
        device?.setDirection(direction) ?: Unit

    override fun setPower(power: Double) =
        device?.setPower(power) ?: Unit

    override fun setPwmRange(range: PwmControl.PwmRange) =
        device?.setPwmRange (range) ?: Unit

    override fun setPwmEnable() { device?.setPwmEnable() }

    override fun setPwmDisable() { device?.setPwmDisable() }

    override fun getPower() = _power
    override fun getPwmRange() = PwmControl.PwmRange(_pwmLower, _pwmUpper)
    override fun isPwmEnabled() = _pwmEnabled
    override fun getDeviceName() = _deviceName
    override fun getVersion() = _version
    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer() = _manufacturer

    override fun close() { device?.close() }
    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}