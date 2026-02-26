package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.FtcLogTuning
import org.psilynx.psikit.ftc.MockI2cDeviceSyncSimple
import org.psilynx.psikit.ftc.StructPoseInputs

/**
 * Lightweight log adapter for the FTC SDK's goBILDA Pinpoint driver
 * (com.qualcomm.hardware.gobilda.GoBildaPinpointDriver).
 *
 * Design goal: follow the same model as other hardware wrappers:
 * - Pinpoint is only logged if user code accesses it via hardwareMap.get(...)
 * - PsiKit does not "own" update(); it only reads/logs state
 */
class PinpointWrapper(
    private val device: GoBildaPinpointDriver?,
) : GoBildaPinpointDriver(
    // In replay there is no real I2C device; supply a safe mock.
    // All public methods are overridden to use logged values when replaying.
    MockI2cDeviceSyncSimple(),
    true
), HardwareInput<GoBildaPinpointDriver> {

    /** Set by [org.psilynx.psikit.ftc.HardwareMapWrapper] so we can also emit /Odometry/<name>. */
    var psikitName: String? = null

    private val poses = StructPoseInputs("Pose2d", "Pose3d")

    private var cacheFilled = false
    private var cachedDeviceId: Int = 0
    private var cachedDeviceVersion: Int = 0
    private var cachedYawScalar: Float = 0f
    private var cachedXOffsetMm: Float = 0f
    private var cachedYOffsetMm: Float = 0f

    private var cachedDeviceStatus: DeviceStatus = DeviceStatus.CALIBRATING
    private var cachedLoopTime: Int = 0
    private var cachedXEncoderValue: Int = 0
    private var cachedYEncoderValue: Int = 0
    private var cachedXPositionMm: Double = 0.0
    private var cachedYPositionMm: Double = 0.0
    private var cachedHOrientationRad: Double = 0.0
    private var cachedXVelocityMm: Double = 0.0
    private var cachedYVelocityMm: Double = 0.0
    private var cachedHVelocityRad: Double = 0.0

    override fun new(wrapped: GoBildaPinpointDriver?): HardwareInput<GoBildaPinpointDriver> = PinpointWrapper(wrapped)

    override fun toLog(table: LogTable) {
        if (FtcLogTuning.bulkOnlyLogging) {
            return
        }

        val target = device ?: return

        // NOTE: PsiKit does not call update() here; consumer code owns updates.
        val pose: Pose2D? = try {
            target.position
        } catch (_: Throwable) {
            null
        }

        // These getters do their own I2C reads in the SDK driver; cache them once.
        if (!cacheFilled) {
            cachedDeviceId = target.deviceID
            cachedDeviceVersion = target.deviceVersion
            cachedYawScalar = target.yawScalar
            cachedXOffsetMm = target.getXOffset(DistanceUnit.MM)
            cachedYOffsetMm = target.getYOffset(DistanceUnit.MM)
            cacheFilled = true
        }

        // Dynamic fields.
        cachedXEncoderValue = target.encoderX
        cachedYEncoderValue = target.encoderY
        cachedLoopTime = target.loopTime
        cachedDeviceStatus = try {
            target.deviceStatus
        } catch (_: Throwable) {
            cachedDeviceStatus
        }

        cachedXPositionMm = target.getPosX(DistanceUnit.MM)
        cachedYPositionMm = target.getPosY(DistanceUnit.MM)
        cachedHOrientationRad = target.getHeading(UnnormalizedAngleUnit.RADIANS)

        cachedXVelocityMm = target.getVelX(DistanceUnit.MM)
        cachedYVelocityMm = target.getVelY(DistanceUnit.MM)
        cachedHVelocityRad = target.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS)

        // Common raw fields (match legacy naming).
        table.put("deviceId", cachedDeviceId)
        table.put("deviceVersion", cachedDeviceVersion)
        table.put("yawScalar", cachedYawScalar)
        table.put("xOffset", cachedXOffsetMm)
        table.put("yOffset", cachedYOffsetMm)

        table.put("xEncoderValue", cachedXEncoderValue)
        table.put("yEncoderValue", cachedYEncoderValue)
        table.put("loopTime", cachedLoopTime)
        table.put("deviceStatus", cachedDeviceStatus.name)

        table.put("xPosition", cachedXPositionMm)
        table.put("yPosition", cachedYPositionMm)
        table.put("hOrientation", cachedHOrientationRad)

        table.put("xVelocity", cachedXVelocityMm)
        table.put("yVelocity", cachedYVelocityMm)
        table.put("hVelocity", cachedHVelocityRad)

        if (pose == null) {
            // Still emit /Odometry only when we have a pose.
            return
        }

        val xMeters = pose.getX(DistanceUnit.METER)
        val yMeters = pose.getY(DistanceUnit.METER)
        val headingRad = pose.getHeading(AngleUnit.RADIANS)

        // Convenient pose view (these keys are new; raw keys above match legacy naming).
        table.put("xMeters", xMeters)
        table.put("yMeters", yMeters)
        table.put("headingRad", headingRad)

        // Provide a convenient Odometry schema for AdvantageScope field widgets.
        val name = psikitName
        if (FtcLogTuning.pinpointWrapperPublishesOdometry) {
            if (!name.isNullOrBlank()) {
                poses.set(xMeters, yMeters, headingRad)
                Logger.processInputs("/Odometry/$name", poses)
            }
        }
    }

    override fun fromLog(table: LogTable) {
        // Populate cached values for replay. These are then surfaced via overridden getters.
        cachedDeviceId = table.get("deviceId", cachedDeviceId)
        cachedDeviceVersion = table.get("deviceVersion", cachedDeviceVersion)
        cachedYawScalar = table.get("yawScalar", cachedYawScalar)
        cachedXOffsetMm = table.get("xOffset", cachedXOffsetMm)
        cachedYOffsetMm = table.get("yOffset", cachedYOffsetMm)

        cachedXEncoderValue = table.get("xEncoderValue", cachedXEncoderValue)
        cachedYEncoderValue = table.get("yEncoderValue", cachedYEncoderValue)
        cachedLoopTime = table.get("loopTime", cachedLoopTime)

        val statusName = table.get("deviceStatus", cachedDeviceStatus.name)
        cachedDeviceStatus = try {
            DeviceStatus.valueOf(statusName)
        } catch (_: Throwable) {
            cachedDeviceStatus
        }

        cachedXPositionMm = table.get("xPosition", cachedXPositionMm)
        cachedYPositionMm = table.get("yPosition", cachedYPositionMm)
        cachedHOrientationRad = table.get("hOrientation", cachedHOrientationRad)

        cachedXVelocityMm = table.get("xVelocity", cachedXVelocityMm)
        cachedYVelocityMm = table.get("yVelocity", cachedYVelocityMm)
        cachedHVelocityRad = table.get("hVelocity", cachedHVelocityRad)

        cacheFilled = true
    }

    // ---- Replay-safe overrides (delegate to real device when present) ----

    override fun update() {
        if (Logger.isReplay() || device == null) return
        device.update()
    }

    override fun update(data: ReadData?) {
        if (Logger.isReplay() || device == null) return
        if (data == null) device.update() else device.update(data)
    }

    override fun setOffsets(xOffset: Double, yOffset: Double, distanceUnit: DistanceUnit?) {
        if (Logger.isReplay() || device == null) return
        device.setOffsets(xOffset, yOffset, distanceUnit ?: DistanceUnit.MM)
    }

    override fun recalibrateIMU() {
        if (Logger.isReplay() || device == null) return
        device.recalibrateIMU()
    }

    override fun resetPosAndIMU() {
        if (Logger.isReplay() || device == null) return
        device.resetPosAndIMU()
    }

    override fun setEncoderDirections(xEncoder: EncoderDirection?, yEncoder: EncoderDirection?) {
        if (Logger.isReplay() || device == null) return
        device.setEncoderDirections(xEncoder, yEncoder)
    }

    override fun setEncoderResolution(pods: GoBildaOdometryPods?) {
        if (Logger.isReplay() || device == null) return
        device.setEncoderResolution(pods)
    }

    override fun setEncoderResolution(ticksPerUnit: Double, distanceUnit: DistanceUnit?) {
        if (Logger.isReplay() || device == null) return
        device.setEncoderResolution(ticksPerUnit, distanceUnit)
    }

    override fun setYawScalar(yawScalar: Double) {
        if (Logger.isReplay() || device == null) return
        device.setYawScalar(yawScalar)
    }

    override fun setPosition(pos: Pose2D?) {
        if (Logger.isReplay() || device == null) return
        device.setPosition(pos)
    }

    override fun setPosX(posX: Double, distanceUnit: DistanceUnit?) {
        if (Logger.isReplay() || device == null) return
        device.setPosX(posX, distanceUnit)
    }

    override fun setPosY(posY: Double, distanceUnit: DistanceUnit?) {
        if (Logger.isReplay() || device == null) return
        device.setPosY(posY, distanceUnit)
    }

    override fun setHeading(heading: Double, angleUnit: AngleUnit?) {
        if (Logger.isReplay() || device == null) return
        device.setHeading(heading, angleUnit)
    }

    override fun getDeviceID(): Int = if (Logger.isReplay() || device == null) cachedDeviceId else device.deviceID

    override fun getDeviceVersion(): Int = if (Logger.isReplay() || device == null) cachedDeviceVersion else device.deviceVersion

    override fun getYawScalar(): Float = if (Logger.isReplay() || device == null) cachedYawScalar else device.yawScalar

    override fun getDeviceStatus(): DeviceStatus = if (Logger.isReplay() || device == null) cachedDeviceStatus else device.deviceStatus

    override fun getLoopTime(): Int = if (Logger.isReplay() || device == null) cachedLoopTime else device.loopTime

    override fun getFrequency(): Double {
        if (!Logger.isReplay() && device != null) return device.frequency
        val lt = cachedLoopTime
        return if (lt > 0) 1000.0 / lt.toDouble() else 0.0
    }

    override fun getEncoderX(): Int = if (Logger.isReplay() || device == null) cachedXEncoderValue else device.encoderX

    override fun getEncoderY(): Int = if (Logger.isReplay() || device == null) cachedYEncoderValue else device.encoderY

    override fun getPosX(distanceUnit: DistanceUnit?): Double {
        if (!Logger.isReplay() && device != null) return device.getPosX(distanceUnit)
        val du = distanceUnit ?: DistanceUnit.MM
        return du.fromMm(cachedXPositionMm)
    }

    override fun getPosY(distanceUnit: DistanceUnit?): Double {
        if (!Logger.isReplay() && device != null) return device.getPosY(distanceUnit)
        val du = distanceUnit ?: DistanceUnit.MM
        return du.fromMm(cachedYPositionMm)
    }

    override fun getHeading(angleUnit: AngleUnit?): Double {
        if (!Logger.isReplay() && device != null) return device.getHeading(angleUnit)
        val au = angleUnit ?: AngleUnit.RADIANS
        return au.fromRadians(cachedHOrientationRad)
    }

    override fun getHeading(unnormalizedAngleUnit: UnnormalizedAngleUnit?): Double {
        if (!Logger.isReplay() && device != null) return device.getHeading(unnormalizedAngleUnit)
        val au = unnormalizedAngleUnit ?: UnnormalizedAngleUnit.RADIANS
        return au.fromRadians(cachedHOrientationRad)
    }

    override fun getVelX(distanceUnit: DistanceUnit?): Double {
        if (!Logger.isReplay() && device != null) return device.getVelX(distanceUnit)
        val du = distanceUnit ?: DistanceUnit.MM
        return du.fromMm(cachedXVelocityMm)
    }

    override fun getVelY(distanceUnit: DistanceUnit?): Double {
        if (!Logger.isReplay() && device != null) return device.getVelY(distanceUnit)
        val du = distanceUnit ?: DistanceUnit.MM
        return du.fromMm(cachedYVelocityMm)
    }

    override fun getHeadingVelocity(unnormalizedAngleUnit: UnnormalizedAngleUnit?): Double {
        if (!Logger.isReplay() && device != null) return device.getHeadingVelocity(unnormalizedAngleUnit)
        val au = unnormalizedAngleUnit ?: UnnormalizedAngleUnit.RADIANS
        return au.fromRadians(cachedHVelocityRad)
    }

    override fun getXOffset(distanceUnit: DistanceUnit?): Float {
        if (!Logger.isReplay() && device != null) {
            val du = distanceUnit ?: DistanceUnit.MM
            val v = device.getXOffset(du)
            if (du == DistanceUnit.MM) {
                cachedXOffsetMm = v
                cacheFilled = true
            }
            cacheFilled = true
            return v
        }
        val du = distanceUnit ?: DistanceUnit.MM
        return du.fromMm(cachedXOffsetMm.toDouble()).toFloat()
    }

    override fun getYOffset(distanceUnit: DistanceUnit?): Float {
        if (!Logger.isReplay() && device != null) {
            val du = distanceUnit ?: DistanceUnit.MM
            val v = device.getYOffset(du)
            if (du == DistanceUnit.MM) {
                cachedYOffsetMm = v
                cacheFilled = true
            }
            cacheFilled = true
            return v
        }
        val du = distanceUnit ?: DistanceUnit.MM
        return du.fromMm(cachedYOffsetMm.toDouble()).toFloat()
    }

    override fun getPosition(): Pose2D {
        if (!Logger.isReplay() && device != null) return device.position
        return Pose2D(
            DistanceUnit.MM,
            getPosX(DistanceUnit.MM),
            getPosY(DistanceUnit.MM),
            AngleUnit.RADIANS,
            getHeading(AngleUnit.RADIANS),
        )
    }
}
