package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.hardware.sparkfun.SparkFunOTOS
import com.qualcomm.robotcore.hardware.HardwareDevice
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.psilynx.psikit.core.LogTable

class SparkFunOTOSWrapper(
    private val device: SparkFunOTOS?
) : HardwareInput<SparkFunOTOS> {

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

    override fun toLog(table: LogTable) {
        table.put("deviceName", deviceName)
        table.put("version", version)
        table.put("connectionInfo", connectionInfo)
        table.put("manufacturer", manufacturer.toString())
        table.put("isConnected", isConnected)
        table.put("imuCalibrationProgress", imuCalibrationProgress)
        table.put("linearUnit", linearUnit.toString())
        table.put("angularUnit", angularUnit.toString())
        table.put("linearScalar", linearScalar)
        table.put("angularScalar", angularScalar)
        table.put("pos", _pos)
        table.put("vel", _vel)
        table.put("acc", _acc)
        table.put("posStd", _posStd)
        table.put("velStd", _velStd)
        table.put("accStd", _accStd)

        _deviceName     = deviceName
        _version        = version
        _connectionInfo = connectionInfo
        _manufacturer   = manufacturer
        _isConnected    = isConnected
        _imuCalProgress = imuCalibrationProgress
        _linearUnit     = linearUnit
        _angularUnit    = angularUnit
        _linearScalar   = linearScalar
        _angularScalar  = angularScalar

        _pos    = poseToArray(position)
        _vel    = poseToArray(velocity)
        _acc    = poseToArray(acceleration)
        _posStd = poseToArray(positionStdDev)
        _velStd = poseToArray(velocityStdDev)
        _accStd = poseToArray(accelerationStdDev)
    }

    override fun fromLog(table: LogTable) {
        _deviceName     = table.get("deviceName", "MockSparkFunOTOS")
        _version        = table.get("version", 1)
        _connectionInfo = table.get("connectionInfo", "")
        _manufacturer   = HardwareDevice.Manufacturer.Other
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
    }

    private fun poseToArray(pose: SparkFunOTOS.Pose2D?): DoubleArray {
        if (pose == null) return doubleArrayOf(0.0, 0.0, 0.0)
        return doubleArrayOf(pose.x, pose.y, pose.h)
    }

    val isConnected: Boolean
        get() = device?.isConnected ?: _isConnected

    val imuCalibrationProgress: Int
        get() = device?.imuCalibrationProgress ?: _imuCalProgress

    val linearUnit: DistanceUnit
        get() = device?.linearUnit ?: _linearUnit

    val angularUnit: AngleUnit
        get() = device?.angularUnit ?: _angularUnit

    val linearScalar: Double
        get() = device?.linearScalar ?: _linearScalar

    val angularScalar: Double
        get() = device?.angularScalar ?: _angularScalar

    val position: SparkFunOTOS.Pose2D?
        get() = device?.position

    val velocity: SparkFunOTOS.Pose2D?
        get() = device?.velocity

    val acceleration: SparkFunOTOS.Pose2D?
        get() = device?.acceleration

    val positionStdDev: SparkFunOTOS.Pose2D?
        get() = device?.positionStdDev

    val velocityStdDev: SparkFunOTOS.Pose2D?
        get() = device?.velocityStdDev

    val accelerationStdDev: SparkFunOTOS.Pose2D?
        get() = device?.accelerationStdDev

    val deviceName: String
        get() = device?.deviceName ?: _deviceName

    val version: Int
        get() = device?.version ?: _version

    val connectionInfo: String
        get() = device?.connectionInfo ?: _connectionInfo

    val manufacturer: HardwareDevice.Manufacturer
        get() = device?.manufacturer ?: _manufacturer

    fun close() { device?.close() }
    fun resetDeviceConfigurationForOpMode() { device?.resetDeviceConfigurationForOpMode() }

    override fun new(wrapped: SparkFunOTOS?) = SparkFunOTOSWrapper(wrapped)
}
