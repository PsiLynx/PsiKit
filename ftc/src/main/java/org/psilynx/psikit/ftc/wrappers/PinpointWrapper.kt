package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.wrappers.HardwareInput
import org.psilynx.psikit.ftc.MockI2cDeviceSyncSimple
import kotlin.math.PI

class PinpointWrapper(val device: GoBildaPinpointDriver?):
    HardwareInput<GoBildaPinpointDriver>,
    GoBildaPinpointDriver(
        device?.deviceClient ?: MockI2cDeviceSyncSimple(), true
    )
{
    var _deviceID = 0
    var _deviceVersion = 0
    var _yawScalar = 0f
    var _deviceStatus = 0
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

    private var cachedDeviceID: Int? = null
    private var cachedDeviceVersion: Int? = null
    private var cachedYawScalar: Float? = null
    private var cachedDeviceStatus: DeviceStatus? = null
    private var cachedLoopTime: Int? = null
    private var cachedEncoderX: Int? = null
    private var cachedEncoderY: Int? = null

    override fun new(wrapped: GoBildaPinpointDriver) = PinpointWrapper(wrapped)

    override fun toLog(table: LogTable) {
        if(cachedDeviceID == null) cachedDeviceID = deviceID
        if(cachedDeviceVersion == null) cachedDeviceVersion = deviceVersion
        if(cachedYawScalar == null) cachedYawScalar = yawScalar
        if(cachedDeviceStatus == null) cachedDeviceStatus = deviceStatus
        if(cachedLoopTime == null) cachedLoopTime = loopTime
        if(cachedEncoderX == null) cachedEncoderX = encoderX
        if(cachedEncoderY == null) cachedEncoderY = encoderY

        device!!

        table.put("deviceId", deviceID)
        table.put("deviceVersion", deviceVersion)
        table.put("yawScalar", yawScalar)
        table.put("deviceStatus", deviceStatus.ordinal)
        table.put("loopTime", loopTime)
        table.put("xEncoderValue", encoderX)
        table.put("yEncoderValue", encoderY)
        table.put("xPosition", device.getPosX(DistanceUnit.MM))
        table.put("yPosition", device.getPosY(DistanceUnit.MM))
        table.put("hOrientation", device.getHeading(UnnormalizedAngleUnit.RADIANS))
        table.put("xVelocity", device.getVelX(DistanceUnit.MM))
        table.put("yVelocity", device.getVelY(DistanceUnit.MM))
        table.put("hVelocity", device.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS))
        table.put("xOffset", device.getXOffset(DistanceUnit.MM))
        table.put("yOffset", device.getYOffset(DistanceUnit.MM))
    }

    override fun fromLog(table: LogTable) {
        _deviceID      = table.get("deviceId", 0)
        _deviceVersion = table.get("deviceVersion", 0)
        _yawScalar     = table.get("yawScalar", 0f)
        _deviceStatus  = table.get(
            "deviceStatus",
            DeviceStatus.CALIBRATING.ordinal
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
        else super.getDeviceID()
    }
    override fun getDeviceVersion(): Int {
        return if(Logger.isReplay()) _deviceVersion
        else device!!.deviceVersion
    }
    override fun getYawScalar(): Float {
        return if(Logger.isReplay()) _yawScalar
        else device!!.yawScalar
    }
    override fun getDeviceStatus(): DeviceStatus {
        return if (Logger.isReplay()) DeviceStatus.entries[_deviceStatus]
        else device!!.deviceStatus
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
        else device!!.getXOffset(distanceUnit)
    }
    override fun getYOffset(distanceUnit: DistanceUnit): Float {
        return if (Logger.isReplay())
            distanceUnit.fromMm(_yOffset.toDouble()).toFloat()
        else device!!.getYOffset(distanceUnit)
    }
    override fun getPosition() = Pose2D(
        DistanceUnit.MM, getPosX(DistanceUnit.MM), getPosY(DistanceUnit.MM),
        AngleUnit.RADIANS, getHeading(AngleUnit.RADIANS),
    )

}