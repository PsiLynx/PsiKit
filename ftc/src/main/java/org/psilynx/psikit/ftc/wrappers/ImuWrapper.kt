package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.IMU
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference
import org.firstinspires.ftc.robotcore.external.navigation.Orientation
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles
import org.psilynx.psikit.ftc.FtcLogTuning
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.Logger

class ImuWrapper(
    private val device: IMU?
) : IMU, HardwareInput<IMU> {

    companion object {
        @Volatile
        private var logGyroRates = false

        @JvmStatic
        fun setLogGyroRates(enabled: Boolean) {
            logGyroRates = enabled
        }

        @JvmStatic
        fun getLogGyroRates(): Boolean = logGyroRates
    }

    private var _yawRad = 0.0
    private var _pitchRad = 0.0
    private var _rollRad = 0.0

    private var _yawRateRadPerSec = 0.0
    private var _pitchRateRadPerSec = 0.0
    private var _rollRateRadPerSec = 0.0

    private var _lastYawRad: Double? = null
    private var _lastPitchRad: Double? = null
    private var _lastRollRad: Double? = null
    private var _lastTimestampSec: Double? = null

    private var _deviceName = "MockIMU"
    private var _version = 1
    private var _connectionInfo = ""
    private var _manufacturer = HardwareDevice.Manufacturer.Other
    private var _orientationSampledThisLoop = false
    private var _ratesSampledThisLoop = false

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

    private fun updateAnglesAndDerivedRates(orientation: YawPitchRollAngles, nowSec: Double = Logger.getTimestamp()) {
        _yawRad = orientation.getYaw(AngleUnit.RADIANS)
        _pitchRad = orientation.getPitch(AngleUnit.RADIANS)
        _rollRad = orientation.getRoll(AngleUnit.RADIANS)

        val lastYaw = _lastYawRad
        val lastPitch = _lastPitchRad
        val lastRoll = _lastRollRad
        val lastTime = _lastTimestampSec
        if (!logGyroRates && lastYaw != null && lastPitch != null && lastRoll != null && lastTime != null) {
            val dt = nowSec - lastTime
            if (dt > 1e-6) {
                val dyaw = angleDiffRad(_yawRad, lastYaw)
                val dpitch = angleDiffRad(_pitchRad, lastPitch)
                val droll = angleDiffRad(_rollRad, lastRoll)
                _yawRateRadPerSec = dyaw / dt
                _pitchRateRadPerSec = dpitch / dt
                _rollRateRadPerSec = droll / dt
                _ratesSampledThisLoop = true
            }
        }

        _lastYawRad = _yawRad
        _lastPitchRad = _pitchRad
        _lastRollRad = _rollRad
        _lastTimestampSec = nowSec
        _orientationSampledThisLoop = true
    }

    private fun updateRatesFromAngularVelocity(angularVelocity: AngularVelocity, angleUnit: AngleUnit) {
        val toRad = if (angleUnit == AngleUnit.DEGREES) Math.PI / 180.0 else 1.0
        _pitchRateRadPerSec = angularVelocity.xRotationRate.toDouble() * toRad
        _rollRateRadPerSec = angularVelocity.yRotationRate.toDouble() * toRad
        _yawRateRadPerSec = angularVelocity.zRotationRate.toDouble() * toRad
        _ratesSampledThisLoop = true
    }

    private fun writeCachedIfSampled(table: LogTable) {
        if (_orientationSampledThisLoop) {
            table.put("yawRad", _yawRad)
            table.put("pitchRad", _pitchRad)
            table.put("rollRad", _rollRad)
        }
        if (_ratesSampledThisLoop) {
            table.put("yawRateRadPerSec", _yawRateRadPerSec)
            table.put("pitchRateRadPerSec", _pitchRateRadPerSec)
            table.put("rollRateRadPerSec", _rollRateRadPerSec)
        }
        table.put("sampledOrientation", _orientationSampledThisLoop)
        table.put("sampledRates", _ratesSampledThisLoop)
    }

    override fun new(wrapped: IMU?) = ImuWrapper(wrapped)

    override fun toLog(table: LogTable) {
        if (FtcLogTuning.bulkOnlyLogging) {
            writeCachedIfSampled(table)
            _orientationSampledThisLoop = false
            _ratesSampledThisLoop = false
            return
        }

        if (!FtcLogTuning.logImu) {
            return
        }

        val d = device ?: return

        if (!shouldSampleNow()) {
            writeCachedIfSampled(table)
            _orientationSampledThisLoop = false
            _ratesSampledThisLoop = false
            return
        }
        lastSampleNs = System.nanoTime()

        val orientation = d.getRobotYawPitchRollAngles()
        val nowSec = Logger.getTimestamp()
        updateAnglesAndDerivedRates(orientation, nowSec)

        if (logGyroRates) {
            // Real gyro rates (rad/s). Map robot yaw/pitch/roll rates to IMU x/y/z rotation rates.
            val angularVelocity = d.getRobotAngularVelocity(AngleUnit.RADIANS)
            updateRatesFromAngularVelocity(angularVelocity, AngleUnit.RADIANS)
        }

        _deviceName = d.deviceName
        _version = d.version
        _connectionInfo = d.connectionInfo
        _manufacturer = d.manufacturer

        writeCachedIfSampled(table)
        table.put("logGyroRates", logGyroRates)

        table.put("deviceName", _deviceName)
        table.put("version", _version)
        table.put("connectionInfo", _connectionInfo)
        table.put("manufacturer", _manufacturer)
        _orientationSampledThisLoop = false
        _ratesSampledThisLoop = false
    }

    override fun fromLog(table: LogTable) {
        _yawRad = table.get("yawRad", 0.0)
        _pitchRad = table.get("pitchRad", 0.0)
        _rollRad = table.get("rollRad", 0.0)

        _yawRateRadPerSec = table.get("yawRateRadPerSec", 0.0)
        _pitchRateRadPerSec = table.get("pitchRateRadPerSec", 0.0)
        _rollRateRadPerSec = table.get("rollRateRadPerSec", 0.0)
        _orientationSampledThisLoop = table.get("sampledOrientation", false)
        _ratesSampledThisLoop = table.get("sampledRates", false)

        _deviceName = table.get("deviceName", "MockIMU")
        _version = table.get("version", 1)
        _connectionInfo = table.get("connectionInfo", "")
        _manufacturer = table.get("manufacturer", HardwareDevice.Manufacturer.Other)
    }

    override fun initialize(parameters: IMU.Parameters?): Boolean {
        return device?.initialize(parameters) ?: true
    }

    override fun resetYaw() {
        device?.resetYaw()
    }

    override fun getRobotYawPitchRollAngles(): YawPitchRollAngles {
        if (Logger.isReplay() || device == null) {
            return YawPitchRollAngles(AngleUnit.RADIANS, _yawRad, _pitchRad, _rollRad, 0)
        }
        val orientation = device.getRobotYawPitchRollAngles()
        updateAnglesAndDerivedRates(orientation)
        return orientation
    }

    override fun getRobotOrientation(
        reference: AxesReference,
        order: AxesOrder,
        angleUnit: AngleUnit
    ): Orientation {
        if (!Logger.isReplay() && device != null) {
            return device.getRobotOrientation(reference, order, angleUnit)
        }

        val x = if (angleUnit == AngleUnit.RADIANS) _pitchRad.toFloat() else Math.toDegrees(_pitchRad).toFloat()
        val y = if (angleUnit == AngleUnit.RADIANS) _rollRad.toFloat() else Math.toDegrees(_rollRad).toFloat()
        val z = if (angleUnit == AngleUnit.RADIANS) _yawRad.toFloat() else Math.toDegrees(_yawRad).toFloat()

        val (first, second, third) = when (order) {
            AxesOrder.XYZ -> Triple(x, y, z)
            AxesOrder.XZY -> Triple(x, z, y)
            AxesOrder.YXZ -> Triple(y, x, z)
            AxesOrder.YZX -> Triple(y, z, x)
            AxesOrder.ZXY -> Triple(z, x, y)
            AxesOrder.ZYX -> Triple(z, y, x)

            AxesOrder.XZX -> Triple(x, z, x)
            AxesOrder.XYX -> Triple(x, y, x)
            AxesOrder.YXY -> Triple(y, x, y)
            AxesOrder.YZY -> Triple(y, z, y)
            AxesOrder.ZYZ -> Triple(z, y, z)
            AxesOrder.ZXZ -> Triple(z, x, z)
        }

        return Orientation(reference, order, angleUnit, first, second, third, 0)
    }

    override fun getRobotOrientationAsQuaternion(): Quaternion {
        if (!Logger.isReplay() && device != null) {
            return device.robotOrientationAsQuaternion
        }

        // Best-effort in replay: return identity quaternion.
        return Quaternion(1f, 0f, 0f, 0f, 0)
    }

    override fun getRobotAngularVelocity(angleUnit: AngleUnit): AngularVelocity {
        if (!Logger.isReplay() && device != null) {
            val angularVelocity = device.getRobotAngularVelocity(angleUnit)
            updateRatesFromAngularVelocity(angularVelocity, angleUnit)
            return angularVelocity
        }

        // In replay, return the logged rates. Map yaw/pitch/roll rates to IMU x/y/z rotation rates.
        val xRadPerSec = _pitchRateRadPerSec
        val yRadPerSec = _rollRateRadPerSec
        val zRadPerSec = _yawRateRadPerSec

        return if (angleUnit == AngleUnit.DEGREES) {
            AngularVelocity(
                AngleUnit.DEGREES,
                Math.toDegrees(xRadPerSec).toFloat(),
                Math.toDegrees(yRadPerSec).toFloat(),
                Math.toDegrees(zRadPerSec).toFloat(),
                0
            )
        } else {
            AngularVelocity(
                AngleUnit.RADIANS,
                xRadPerSec.toFloat(),
                yRadPerSec.toFloat(),
                zRadPerSec.toFloat(),
                0
            )
        }
    }

    private fun angleDiffRad(a: Double, b: Double): Double {
        val d = a - b
        return Math.atan2(Math.sin(d), Math.cos(d))
    }

    override fun getDeviceName() = _deviceName
    override fun getVersion() = _version
    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer() = _manufacturer

    override fun close() {
        device?.close()
    }

    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}
