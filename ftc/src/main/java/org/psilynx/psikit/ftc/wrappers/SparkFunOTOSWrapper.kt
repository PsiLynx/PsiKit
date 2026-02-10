package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.hardware.sparkfun.SparkFunOTOS
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.I2cDeviceSynchImplOnSimple
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.MockI2cDeviceSyncSimple

class SparkFunOTOSWrapper(
    private val device: SparkFunOTOS?
) : SparkFunOTOS(
    I2cDeviceSynchImplOnSimple(MockI2cDeviceSyncSimple(), true)
), HardwareInput<SparkFunOTOS> {

    private var _deviceName    = "MockSparkFunOTOS"
    private var _version       = 1
    private var _connectionInfo = ""
    private var _manufacturer  = HardwareDevice.Manufacturer.Other

    private var _isConnected   = false
    private var _imuCalProgress = 0
    private var _linearUnit    = DistanceUnit.MM
    private var _angularUnit   = AngleUnit.DEGREES
    private var _linearScalar  = 1.0
    private var _angularScalar = 1.0

    // Pose2D fields (primitive logging only)
    private var _pos: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
    private var _vel: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
    private var _acc: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
    private var _posStd: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
    private var _velStd: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
    private var _accStd: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)

    private var _offset: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
    private var _statusByte: Byte = 0
    private var _signalProcessConfigByte: Byte = 0

    override fun toLog(table: LogTable) {
        if (device != null) {
            _deviceName     = device.deviceName
            _version        = device.version
            _connectionInfo = device.connectionInfo
            _manufacturer   = device.manufacturer

            _isConnected    = device.isConnected
            _imuCalProgress = device.imuCalibrationProgress
            _linearUnit     = device.linearUnit
            _angularUnit    = device.angularUnit
            _linearScalar   = device.linearScalar
            _angularScalar  = device.angularScalar

            _pos    = poseToArray(device.position)
            _vel    = poseToArray(device.velocity)
            _acc    = poseToArray(device.acceleration)
            _posStd = poseToArray(device.positionStdDev)
            _velStd = poseToArray(device.velocityStdDev)
            _accStd = poseToArray(device.accelerationStdDev)

            _offset = poseToArray(device.offset)
            _statusByte = try { device.status.get() } catch (_: Throwable) { 0 }
            _signalProcessConfigByte = try { device.signalProcessConfig.get() } catch (_: Throwable) { 0 }
        }

        table.put("deviceName", _deviceName)
        table.put("version", _version)
        table.put("connectionInfo", _connectionInfo)
        table.put("manufacturer", _manufacturer)
        table.put("isConnected", _isConnected)
        table.put("imuCalibrationProgress", _imuCalProgress)
        table.put("linearUnit", _linearUnit.toString())
        table.put("angularUnit", _angularUnit.toString())
        table.put("linearScalar", _linearScalar)
        table.put("angularScalar", _angularScalar)
        table.put("pos", _pos)
        table.put("vel", _vel)
        table.put("acc", _acc)
        table.put("posStd", _posStd)
        table.put("velStd", _velStd)
        table.put("accStd", _accStd)
        table.put("offset", _offset)
        table.put("status", _statusByte.toInt())
        table.put("signalProcessConfig", _signalProcessConfigByte.toInt())
    }

    override fun fromLog(table: LogTable) {
        _deviceName     = table.get("deviceName", "MockSparkFunOTOS")
        _version        = table.get("version", 1)
        _connectionInfo = table.get("connectionInfo", "")
        _manufacturer   = table.get("manufacturer", HardwareDevice.Manufacturer.Other)
        _isConnected    = table.get("isConnected", false)
        _imuCalProgress = table.get("imuCalibrationProgress", 0)
        _linearUnit     = DistanceUnit.valueOf(table.get("linearUnit", DistanceUnit.MM.toString()))
        _angularUnit    = AngleUnit.valueOf(table.get("angularUnit", AngleUnit.DEGREES.toString()))
        _linearScalar   = table.get("linearScalar", 1.0)
        _angularScalar  = table.get("angularScalar", 1.0)

        _pos    = table.get("pos", doubleArrayOf(0.0, 0.0, 0.0))
        _vel    = table.get("vel", doubleArrayOf(0.0, 0.0, 0.0))
        _acc    = table.get("acc", doubleArrayOf(0.0, 0.0, 0.0))
        _posStd = table.get("posStd", doubleArrayOf(0.0, 0.0, 0.0))
        _velStd = table.get("velStd", doubleArrayOf(0.0, 0.0, 0.0))
        _accStd = table.get("accStd", doubleArrayOf(0.0, 0.0, 0.0))

        _offset = table.get("offset", doubleArrayOf(0.0, 0.0, 0.0))
        _statusByte = table.get("status", 0).toByte()
        _signalProcessConfigByte = table.get("signalProcessConfig", 0).toByte()
    }

    private fun poseToArray(pose: SparkFunOTOS.Pose2D?): DoubleArray {
        if (pose == null) return doubleArrayOf(0.0, 0.0, 0.0)
        return doubleArrayOf(pose.x, pose.y, pose.h)
    }

    override fun getManufacturer() = device?.manufacturer ?: _manufacturer
    override fun getDeviceName() = device?.deviceName ?: _deviceName

    override fun begin(): Boolean {
        if (Logger.isReplay()) return false
        return device?.begin() ?: false
    }

    override fun isConnected(): Boolean = device?.isConnected ?: _isConnected

    override fun selfTest(): Boolean {
        if (Logger.isReplay()) return false
        return device?.selfTest() ?: false
    }

    override fun calibrateImu(): Boolean {
        if (Logger.isReplay()) return false
        return device?.calibrateImu() ?: false
    }

    override fun calibrateImu(numSamples: Int, waitUntilDone: Boolean): Boolean {
        if (Logger.isReplay()) return false
        return device?.calibrateImu(numSamples, waitUntilDone) ?: false
    }

    override fun getImuCalibrationProgress(): Int = device?.imuCalibrationProgress ?: _imuCalProgress

    override fun getLinearUnit(): DistanceUnit = device?.linearUnit ?: _linearUnit
    override fun setLinearUnit(unit: DistanceUnit) {
        if (Logger.isReplay()) { _linearUnit = unit; return }
        device?.linearUnit = unit
    }

    override fun getAngularUnit(): AngleUnit = device?.angularUnit ?: _angularUnit
    override fun setAngularUnit(unit: AngleUnit) {
        if (Logger.isReplay()) { _angularUnit = unit; return }
        device?.angularUnit = unit
    }

    override fun getLinearScalar(): Double = device?.linearScalar ?: _linearScalar
    override fun setLinearScalar(scalar: Double): Boolean {
        if (Logger.isReplay()) { _linearScalar = scalar; return true }
        return device?.setLinearScalar(scalar) ?: false
    }

    override fun getAngularScalar(): Double = device?.angularScalar ?: _angularScalar
    override fun setAngularScalar(scalar: Double): Boolean {
        if (Logger.isReplay()) { _angularScalar = scalar; return true }
        return device?.setAngularScalar(scalar) ?: false
    }

    override fun resetTracking() {
        if (Logger.isReplay()) return
        device?.resetTracking()
    }

    override fun getSignalProcessConfig(): SparkFunOTOS.SignalProcessConfig {
        if (device != null && !Logger.isReplay()) return device.signalProcessConfig
        return SparkFunOTOS.SignalProcessConfig(_signalProcessConfigByte)
    }

    override fun setSignalProcessConfig(config: SparkFunOTOS.SignalProcessConfig) {
        if (Logger.isReplay()) {
            _signalProcessConfigByte = try { config.get() } catch (_: Throwable) { 0 }
            return
        }
        device?.signalProcessConfig = config
    }

    override fun getStatus(): SparkFunOTOS.Status {
        if (device != null && !Logger.isReplay()) return device.status
        return SparkFunOTOS.Status(_statusByte)
    }

    override fun getOffset(): SparkFunOTOS.Pose2D = SparkFunOTOS.Pose2D(
        _offset.getOrElse(0) { 0.0 },
        _offset.getOrElse(1) { 0.0 },
        _offset.getOrElse(2) { 0.0 },
    )

    override fun setOffset(offset: SparkFunOTOS.Pose2D) {
        if (Logger.isReplay()) {
            _offset = poseToArray(offset)
            return
        }
        device?.offset = offset
    }

    override fun getPosition(): SparkFunOTOS.Pose2D = SparkFunOTOS.Pose2D(
        _pos.getOrElse(0) { 0.0 },
        _pos.getOrElse(1) { 0.0 },
        _pos.getOrElse(2) { 0.0 },
    )

    override fun setPosition(position: SparkFunOTOS.Pose2D) {
        if (Logger.isReplay()) {
            _pos = poseToArray(position)
            return
        }
        device?.position = position
    }

    override fun getVelocity(): SparkFunOTOS.Pose2D = SparkFunOTOS.Pose2D(
        _vel.getOrElse(0) { 0.0 },
        _vel.getOrElse(1) { 0.0 },
        _vel.getOrElse(2) { 0.0 },
    )

    override fun getAcceleration(): SparkFunOTOS.Pose2D = SparkFunOTOS.Pose2D(
        _acc.getOrElse(0) { 0.0 },
        _acc.getOrElse(1) { 0.0 },
        _acc.getOrElse(2) { 0.0 },
    )

    override fun getPositionStdDev(): SparkFunOTOS.Pose2D = SparkFunOTOS.Pose2D(
        _posStd.getOrElse(0) { 0.0 },
        _posStd.getOrElse(1) { 0.0 },
        _posStd.getOrElse(2) { 0.0 },
    )

    override fun getVelocityStdDev(): SparkFunOTOS.Pose2D = SparkFunOTOS.Pose2D(
        _velStd.getOrElse(0) { 0.0 },
        _velStd.getOrElse(1) { 0.0 },
        _velStd.getOrElse(2) { 0.0 },
    )

    override fun getAccelerationStdDev(): SparkFunOTOS.Pose2D = SparkFunOTOS.Pose2D(
        _accStd.getOrElse(0) { 0.0 },
        _accStd.getOrElse(1) { 0.0 },
        _accStd.getOrElse(2) { 0.0 },
    )

    override fun close() { device?.close() }
    override fun resetDeviceConfigurationForOpMode() { device?.resetDeviceConfigurationForOpMode() }

    override fun new(wrapped: SparkFunOTOS?) = SparkFunOTOSWrapper(wrapped)
}
