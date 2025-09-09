package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.MockI2cDeviceSyncSimple
import kotlin.math.PI
import kotlin.properties.Delegates

class PinpointWrapper(val device: GoBildaPinpointDriver?):
    HardwareInput<GoBildaPinpointDriver>,
    GoBildaPinpointDriver(
        device?.deviceClient ?: MockI2cDeviceSyncSimple(), true
    )
{
    var _deviceID = 0
    var _deviceVersion = 0
    var _yawScalar = 0f
    var _deviceStatus = DeviceStatus.CALIBRATING
    var _loopTime = 0
    var _xEncoderValue = 0
    var _yEncoderValue = 0
    var _xPosition = 0.0
    var _yPosition = 0.0
    var _hOrientation = 0.0
    var _xVelocity = 0.0
    var _yVelocity = 0.0
    var _hVelocity = 0.0
    var _xOffset = 0f
    var _yOffset = 0f

    private var cachedDeviceID by Delegates.notNull<Int>()
    private var cachedDeviceVersion by Delegates.notNull<Int>()
    private var cachedYawScalar by Delegates.notNull<Float>()
    private var cachedDeviceStatus by Delegates.notNull<DeviceStatus>()
    private var cachedXOffset by Delegates.notNull<Float>()
    private var cachedYOffset by Delegates.notNull<Float>()
    private var cacheFilled = false

    override fun new(wrapped: GoBildaPinpointDriver?) = PinpointWrapper(wrapped)

    override fun toLog(table: LogTable) {
       if(!cacheFilled) {
            cachedDeviceID = deviceID
            cachedDeviceVersion = deviceVersion
            cachedYawScalar = yawScalar
            cachedDeviceStatus = deviceStatus
            cachedXOffset = getXOffset(DistanceUnit.MM)
            cachedYOffset = getYOffset(DistanceUnit.MM)
        }
        cacheFilled = true

        device!!



        table.put("deviceId", cachedDeviceID)
        table.put("deviceVersion", cachedDeviceVersion)
        table.put("yawScalar", cachedYawScalar)
        table.put("xOffset", cachedXOffset)
        table.put("yOffset", cachedYOffset)
        table.put("xEncoderValue", encoderX)
        table.put("yEncoderValue", encoderY)
        table.put("loopTime", loopTime)
        table.put("deviceStatus", deviceStatus)
        table.put("xPosition", device.getPosX(DistanceUnit.MM))
        table.put("yPosition", device.getPosY(DistanceUnit.MM))
        table.put("hOrientation", device.getHeading(UnnormalizedAngleUnit.RADIANS))
        table.put("xVelocity", device.getVelX(DistanceUnit.MM))
        table.put("yVelocity", device.getVelY(DistanceUnit.MM))
        table.put("hVelocity", device.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS))
    }

    override fun fromLog(table: LogTable) {
        _deviceID      = table.get("deviceId", 0)
        _deviceVersion = table.get("deviceVersion", 0)
        _yawScalar     = table.get("yawScalar", 0f)
        _deviceStatus  = table.get(
            "deviceStatus",
            DeviceStatus.CALIBRATING
        )
        _loopTime      = table.get("loopTime", 0)
        _xEncoderValue = table.get("xEncoderValue", 0)
        _yEncoderValue = table.get("yEncoderValue", 0)
        _xPosition     = table.get("xPosition", 0.0)
        _yPosition     = table.get("yPosition", 0.0)
        _hOrientation  = table.get("hOrientation", 0.0)
        _xVelocity     = table.get("xVelocity", 0.0)
        _yVelocity     = table.get("yVelocity", 0.0)
        _hVelocity     = table.get("hVelocity", 0.0)
        _xOffset       = table.get("xOffset", 0f)
        _yOffset       = table.get("yOffset", 0f)
    }

    override fun update(){ if(!Logger.isReplay()) device!!.update() }
    override fun update(readData: ReadData){
        if(!Logger.isReplay()) device!!.update(readData)
    }

    override fun getDeviceID(): Int {
        return if (Logger.isReplay()) _deviceID
        else {
            val value = device!!.deviceID
            cachedDeviceID = value
            value
        }
    }
    override fun getDeviceVersion(): Int {
        return if (Logger.isReplay()) _deviceVersion
        else {
            val value = device!!.deviceVersion
            cachedDeviceVersion = value
            value
        }
    }
    override fun getYawScalar(): Float {
        return if (Logger.isReplay()) _yawScalar
        else {
            val value = device!!.yawScalar
            cachedYawScalar = value
            value
        }
    }
    override fun getDeviceStatus(): DeviceStatus {
        return if (Logger.isReplay()) _deviceStatus
        else {
            val value = device!!.deviceStatus
            cachedDeviceStatus = value
            value
        }
    }
    override fun getLoopTime(): Int {
        return if (Logger.isReplay())_loopTime
        else device!!.loopTime
    }
    override fun getFrequency(): Double {
        return if (_loopTime != 0) {
            1000000.0 / _loopTime;
        } else {
            0.0;
        }
    }
    override fun getEncoderX(): Int {
        return if (Logger.isReplay()) _xEncoderValue
        else device!!.encoderX
    }
    override fun getEncoderY(): Int {
        return if (Logger.isReplay()) _yEncoderValue
        else device!!.encoderY
    }
    override fun getPosX(distanceUnit: DistanceUnit): Double {
        return if (Logger.isReplay()) distanceUnit.fromMm(_xPosition)
        else device!!.getPosX(distanceUnit)
    }
    override fun getPosY(distanceUnit: DistanceUnit): Double {
        return if (Logger.isReplay()) distanceUnit.fromMm(_yPosition)
        else device!!.getPosY(distanceUnit)
    }
    override fun getHeading(angleUnit: AngleUnit): Double {
        return if (Logger.isReplay()) angleUnit.fromRadians(
            ( _hOrientation + PI) % ( 2 * PI) - PI
        )
        else device!!.getHeading(angleUnit)
    }
    override fun getHeading(unnormalizedAngleUnit: UnnormalizedAngleUnit): Double {
        return if (Logger.isReplay())
            unnormalizedAngleUnit.fromRadians(_hOrientation)
        else device!!.getHeading(unnormalizedAngleUnit)
    }
    override fun getVelX(distanceUnit: DistanceUnit): Double {
        return if (Logger.isReplay()) distanceUnit.fromMm(_xVelocity)
        else device!!.getVelX(distanceUnit)
    }
    override fun getVelY(distanceUnit: DistanceUnit): Double {
        return if (Logger.isReplay()) distanceUnit.fromMm(_yVelocity)
        else device!!.getVelY(distanceUnit)
    }
    override fun getHeadingVelocity(
        unnormalizedAngleUnit: UnnormalizedAngleUnit
    ): Double {
        return if (Logger.isReplay())
            unnormalizedAngleUnit.fromRadians(_hOrientation)
        else device!!.getHeadingVelocity(unnormalizedAngleUnit)
    }
    override fun getXOffset(distanceUnit: DistanceUnit): Float {
        return if (Logger.isReplay())
            distanceUnit.fromMm(_xOffset.toDouble()).toFloat()
        else {
            val value = device!!.getXOffset(distanceUnit)
            cachedXOffset = value
            value
        }
    }
    override fun getYOffset(distanceUnit: DistanceUnit): Float {
        return if (Logger.isReplay())
            distanceUnit.fromMm(_yOffset.toDouble()).toFloat()
        else {
            val value = device!!.getYOffset(distanceUnit)
            cachedYOffset = value
            value
        }
    }
    override fun getPosition() = Pose2D(
        DistanceUnit.MM, getPosX(DistanceUnit.MM), getPosY(DistanceUnit.MM),
        AngleUnit.RADIANS, getHeading(AngleUnit.RADIANS),
    )

}
