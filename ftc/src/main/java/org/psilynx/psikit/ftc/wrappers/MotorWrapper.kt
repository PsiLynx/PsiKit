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
import org.psilynx.psikit.ftc.FtcLogTuning
import org.psilynx.psikit.core.LogTable

class MotorWrapper(
    private val device: DcMotorImplEx?
) : DcMotorImplEx(
    object : DcMotorControllerEx {
        override fun setMotorType(motor: Int, motorType: MotorConfigurationType?) {}
        override fun getMotorType(motor: Int): MotorConfigurationType {
            // Robolectric / SDK stubs can sometimes break the static unspecified motor lookup.
            // Prefer the underlying real device type when available; otherwise return a safe default.
            val fromDevice = try {
                device?.motorType
            } catch (_: Throwable) {
                null
            }
            return fromDevice ?: MotorConfigurationType()
        }
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

    companion object {
        const val LOG_PROFILE_FULL = 0
        const val LOG_PROFILE_FAST = 1
        const val LOG_PROFILE_BULK_ONLY = 2

        /**
         * Logging profile:
         * - FULL: existing behavior
         * - FAST: avoid expensive non-bulk readbacks where possible
         * - BULK_ONLY: only log bulk-cached motor fields (pos/vel/busy/overcurrent)
         */
        @JvmField var logProfile: Int = LOG_PROFILE_FULL

        /**
         * Motor velocity reads can be expensive on some hub/SDK combos.
         * Disable if you're seeing high `PsiKit/logTimes` for motors.
         */
        @JvmStatic var logVelocity: Boolean = true

        /**
         * If non-empty, velocity is only logged for motors whose HardwareMap name matches.
         * Example: set to {"fly_left", "fly_right"}.
         */
        @JvmStatic var velocityMotorNames: Set<String> = emptySet()

        /**
         * If non-empty (and [velocityMotorNames] is empty), velocity is only logged when the
         * motor name starts with one of these prefixes.
         */
        @JvmStatic var velocityMotorNamePrefixes: Set<String> = emptySet()

        /**
         * Encoder-related reads (position/targetPosition/isBusy) are usually served from Lynx bulk data,
         * but still incur overhead and can be wasted if a motor's encoder isn't connected/used.
         */
        @JvmStatic var logEncoderData: Boolean = true

        /**
         * If non-empty, encoder-related reads are skipped for motors whose HardwareMap name matches.
         * Example: set to {"left_front", "left_back", "right_front", "right_back"}.
         */
        @JvmStatic var skipEncoderMotorNames: Set<String> = emptySet()

        /**
         * If non-empty (and [skipEncoderMotorNames] is empty), encoder-related reads are skipped when
         * the motor name starts with one of these prefixes.
         */
        @JvmStatic var skipEncoderMotorNamePrefixes: Set<String> = emptySet()

        @JvmStatic fun setEncoderSkippedMotors(vararg names: String) {
            skipEncoderMotorNames = names.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }

        @JvmStatic fun setEncoderSkippedMotorPrefixes(vararg prefixes: String) {
            skipEncoderMotorNamePrefixes = prefixes.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }

        @JvmStatic fun setVelocityLoggedMotors(vararg names: String) {
            velocityMotorNames = names.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }

        @JvmStatic fun setVelocityLoggedMotorPrefixes(vararg prefixes: String) {
            velocityMotorNamePrefixes = prefixes.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }

        /**
         * Over-current status can be expensive; default off.
         */
        @JvmStatic var logOverCurrent: Boolean = false

        /**
         * Busy reads can be expensive depending on mode/controller; default on.
         */
        @JvmStatic var logBusy: Boolean = true

        /**
         * If true, only read/log `isBusy` when the motor mode is RUN_TO_POSITION.
         * In other modes this is usually meaningless and often always false.
         */
        @JvmStatic var logBusyOnlyInRunToPosition: Boolean = true

        /**
         * Optional throttle for velocity reads. Set to > 0 to only sample velocity at that period
         * and reuse the last value in between.
         */
        @JvmField var velocityRefreshPeriodSec: Double = 0.0

        /**
         * Metadata fields are effectively static; refresh them rarely.
         */
        @JvmStatic var metadataRefreshPeriodSec: Double = 2.0

        /**
         * Configuration-ish fields rarely change; refresh periodically.
         */
        @JvmStatic var configRefreshPeriodSec: Double = 0.25
    }

    internal var psikitName: String = ""

    private var _zeroPowerBehavior = DcMotor.ZeroPowerBehavior.UNKNOWN
    private var _powerFloat  = false
    private var _overCurrent = false
    private var _currentPos  = 0
    private var _currentVel  = 0.0
    private var _currentMilliamps = 0.0
    private var _targetVelTps = 0.0
    private var _targetVelAngular = 0.0
    private var _targetVelUnit: AngleUnit? = null
    private var _power       = 0.0
    private var _direction   = DcMotorSimple.Direction.FORWARD
    private var _mode        = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    private var _targetPos   = 0
    private var _busy        = false
    private var _deviceName  = "MockMotor"
    private var _version     = 1
    private var _connectionInfo = ""
    private var _manufacturer   = HardwareDevice.Manufacturer.Other

    private var lastMetadataUpdateNs: Long = Long.MIN_VALUE
    private var lastConfigUpdateNs: Long = Long.MIN_VALUE
    private var lastVelocityUpdateNs: Long = Long.MIN_VALUE
    private var lastMotorCurrentUpdateNs: Long = Long.MIN_VALUE
    private var lastNonBulkUpdateNs: Long = Long.MIN_VALUE
    private var syncedFromDeviceOnce: Boolean = false

    private fun shouldLogVelocity(): Boolean {
        if (!logVelocity) return false

        val name = psikitName
        if (velocityMotorNames.isNotEmpty()) {
            return velocityMotorNames.contains(name)
        }

        if (velocityMotorNamePrefixes.isNotEmpty()) {
            return velocityMotorNamePrefixes.any { prefix -> name.startsWith(prefix) }
        }

        return true
    }

    private fun shouldLogEncoderData(): Boolean {
        if (!logEncoderData) return false

        val name = psikitName
        if (skipEncoderMotorNames.isNotEmpty()) {
            return !skipEncoderMotorNames.contains(name)
        }

        if (skipEncoderMotorNamePrefixes.isNotEmpty()) {
            return skipEncoderMotorNamePrefixes.none { prefix -> name.startsWith(prefix) }
        }

        return true
    }

    private fun secondsSince(ns: Long): Double {
        if (ns == Long.MIN_VALUE) return Double.POSITIVE_INFINITY
        return (System.nanoTime() - ns) / 1_000_000_000.0
    }

    private fun shouldSampleVelocityNow(): Boolean {
        val period = velocityRefreshPeriodSec
        if (period <= 0.0) return true
        return secondsSince(lastVelocityUpdateNs) >= period
    }

    private fun shouldSampleMotorCurrentNow(): Boolean {
        val period = FtcLogTuning.motorCurrentReadPeriodSec
        if (period <= 0.0) return true
        return secondsSince(lastMotorCurrentUpdateNs) >= period
    }

    private fun shouldSampleNonBulkNow(): Boolean {
        val period = FtcLogTuning.nonBulkReadPeriodSec
        if (period <= 0.0) return true
        return secondsSince(lastNonBulkUpdateNs) >= period
    }

    private fun syncFromDeviceOnce(device: DcMotorImplEx) {
        if (syncedFromDeviceOnce) return
        syncedFromDeviceOnce = true
        // Best-effort: these are non-bulk readbacks on many controllers.
        try { _power = device.power } catch (_: Throwable) {}
        try { _direction = device.direction } catch (_: Throwable) {}
        try { _mode = device.mode } catch (_: Throwable) {}
        try { _targetPos = device.targetPosition } catch (_: Throwable) {}
        try { _zeroPowerBehavior = device.zeroPowerBehavior } catch (_: Throwable) {}
        try { _powerFloat = device.powerFloat } catch (_: Throwable) {}
    }

    override fun new(wrapped: DcMotorImplEx?) = MotorWrapper(wrapped)

    override fun toLog(table: LogTable) {
        device!!

        val profile = logProfile

        if (profile != LOG_PROFILE_FULL) {
            // In FAST/BULK_ONLY, avoid readback-based drift in the first log sample.
            syncFromDeviceOnce(device)
        }

        // Static-ish metadata: cache heavily.
        if (profile != LOG_PROFILE_BULK_ONLY) {
            if (secondsSince(lastMetadataUpdateNs) >= metadataRefreshPeriodSec) {
                lastMetadataUpdateNs = System.nanoTime()
                _deviceName        = device.deviceName
                _version           = device.version
                _connectionInfo    = device.connectionInfo
                _manufacturer      = device.manufacturer
            }
        }

        // Configuration-ish fields: refresh periodically (not every loop).
        if (profile == LOG_PROFILE_FULL) {
            // Only do config readbacks in FULL mode.
            if (secondsSince(lastConfigUpdateNs) >= configRefreshPeriodSec && shouldSampleNonBulkNow()) {
                lastConfigUpdateNs = System.nanoTime()
                lastNonBulkUpdateNs = lastConfigUpdateNs
                _zeroPowerBehavior = device.zeroPowerBehavior
                _powerFloat        = device.powerFloat
            }
        }

        _overCurrent       = if (logOverCurrent) device.isOverCurrent else false

        val logEnc = shouldLogEncoderData()
        _currentPos        = if (logEnc) device.currentPosition else 0

        val logVel = shouldLogVelocity()
        if (logVel && shouldSampleVelocityNow()) {
            lastVelocityUpdateNs = System.nanoTime()
            _currentVel = device.velocity
        } else if (!logVel) {
            _currentVel = 0.0
        }

        if (profile != LOG_PROFILE_BULK_ONLY && FtcLogTuning.logMotorCurrent && shouldSampleMotorCurrentNow()) {
            lastMotorCurrentUpdateNs = System.nanoTime()
            try {
                _currentMilliamps = device.getCurrent(CurrentUnit.MILLIAMPS)
            } catch (_: Throwable) {
            }
        }

        if (profile == LOG_PROFILE_FULL) {
            // These are often non-bulk readbacks. Rate limit if configured.
            if (shouldSampleNonBulkNow()) {
                lastNonBulkUpdateNs = System.nanoTime()
                _power             = device.power
                _direction         = device.direction
                _mode              = device.mode
                _targetPos         = if (logEnc) device.targetPosition else 0
            }
        } else {
            // FAST / BULK_ONLY: don't read non-bulk fields from hardware each loop.
            // Cached values are updated via the setters on this wrapper.
            if (!logEnc) {
                _targetPos = 0
            }
        }

        val shouldReadBusy = logBusy && logEnc && (!logBusyOnlyInRunToPosition || _mode == DcMotor.RunMode.RUN_TO_POSITION)
        _busy              = if (shouldReadBusy) device.isBusy else false

        // BULK_ONLY: only write bulk-backed fields (and nothing else).
        if (profile == LOG_PROFILE_BULK_ONLY) {
            table.put("overCurrent", _overCurrent)
            if (logEnc) {
                table.put("currentPos", _currentPos)
                table.put("busy", _busy)
            }
            if (logVel) {
                table.put("currentVel", _currentVel)
            }
            return
        }

        // FULL/FAST: write the full schema.
        table.put("zeroPowerBehavior", _zeroPowerBehavior)
        table.put("powerFloat", _powerFloat)
        table.put("overCurrent", _overCurrent)
        table.put("currentPos", _currentPos)
        table.put("currentVel", _currentVel)
        if (profile != LOG_PROFILE_BULK_ONLY && FtcLogTuning.logMotorCurrent) {
            table.put("currentMilliamps", _currentMilliamps)
        }
        table.put("targetVelTps", _targetVelTps)
        table.put("targetVelAngular", _targetVelAngular)
        table.put("targetVelUnit", _targetVelUnit?.name ?: "")
        table.put("power", _power)
        table.put("direction", _direction.name)
        table.put("mode", _mode.name)
        table.put("targetPos", _targetPos)
        table.put("busy", _busy)
        table.put("deviceName", _deviceName)
        table.put("version", _version)
        table.put("connectionInfo", _connectionInfo)
        table.put("manufacturer", _manufacturer)

    }

    override fun fromLog(table: LogTable) {
        _zeroPowerBehavior = table.get("zeroPowerBehavior", DcMotor.ZeroPowerBehavior.UNKNOWN)
        _powerFloat        = table.get("powerFloat", false)
        _overCurrent       = table.get("overCurrent", false)
        _currentPos        = table.get("currentPos", 0)
        _currentVel        = table.get("currentVel", 0.0)
        _currentMilliamps  = table.get("currentMilliamps", 0.0)
        _targetVelTps      = table.get("targetVelTps", 0.0)
        _targetVelAngular  = table.get("targetVelAngular", 0.0)
        _targetVelUnit     = table.get("targetVelUnit", "").let { if (it.isEmpty()) null else AngleUnit.valueOf(it) }
        _power             = table.get("power", 0.0)
        _direction         = DcMotorSimple.Direction.valueOf(table.get("direction", DcMotorSimple.Direction.FORWARD.name))
        _mode              = DcMotor.RunMode.valueOf(table.get("mode", DcMotor.RunMode.RUN_WITHOUT_ENCODER.name))
        _targetPos         = table.get("targetPos", 0)
        _busy              = table.get("busy", false)
        _deviceName        = table.get("deviceName", "MockMotor")
        _version           = table.get("version", 1)
        _connectionInfo    = table.get("connectionInfo", "")
        _manufacturer      = table.get("manufacturer", HardwareDevice.Manufacturer.Other)
    }

    override fun getZeroPowerBehavior() = device?.zeroPowerBehavior ?: _zeroPowerBehavior
    override fun setZeroPowerBehavior(zeroPowerBehavior: DcMotor.ZeroPowerBehavior?) {
        val resolved = zeroPowerBehavior ?: DcMotor.ZeroPowerBehavior.UNKNOWN
        _zeroPowerBehavior = resolved
        if (device != null) {
            device.zeroPowerBehavior = resolved
        } else {
            super.setZeroPowerBehavior(resolved)
        }
    }

    override fun getPowerFloat() = device?.powerFloat ?: _powerFloat
    override fun getCurrentPosition() = device?.currentPosition ?: _currentPos
    override fun getVelocity() = device?.velocity ?: _currentVel
    override fun getVelocity(unit: AngleUnit?) = device?.getVelocity(unit) ?: _currentVel
    override fun getPower() = device?.power ?: _power
    override fun isOverCurrent() = device?.isOverCurrent ?: _overCurrent

    override fun getCurrent(unit: CurrentUnit?): Double {
        if (device != null) {
            return try {
                device.getCurrent(unit)
            } catch (_: Throwable) {
                0.0
            }
        }

        return when (unit ?: CurrentUnit.MILLIAMPS) {
            CurrentUnit.AMPS -> _currentMilliamps / 1000.0
            else -> _currentMilliamps
        }
    }

    override fun setMode(mode: DcMotor.RunMode?) {
        val resolved = mode ?: DcMotor.RunMode.RUN_WITHOUT_ENCODER
        _mode = resolved
        if (device != null) {
            device.mode = resolved
        } else {
            super.setMode(resolved)
        }
    }

    override fun getMode(): DcMotor.RunMode {
        return device?.mode ?: _mode
    }

    override fun setTargetPosition(position: Int) {
        _targetPos = position
        if (device != null) {
            device.targetPosition = position
        } else {
            super.setTargetPosition(position)
        }
    }

    override fun getTargetPosition(): Int {
        return device?.targetPosition ?: _targetPos
    }

    override fun isBusy(): Boolean {
        return device?.isBusy ?: _busy
    }

    override fun setVelocity(ticksPerSecond: Double) {
        _targetVelTps = ticksPerSecond
        if (device != null) {
            device.velocity = ticksPerSecond
        } else {
            super.setVelocity(ticksPerSecond)
        }
    }

    override fun setVelocity(angularRate: Double, unit: AngleUnit?) {
        _targetVelAngular = angularRate
        _targetVelUnit = unit
        if (device != null) {
            device.setVelocity(angularRate, unit)
        } else {
            super.setVelocity(angularRate, unit)
        }
    }

    override fun setDirection(direction: DcMotorSimple.Direction?) {
        val resolved = direction ?: DcMotorSimple.Direction.FORWARD
        _direction = resolved
        if (device != null) {
            device.setDirection(resolved)
        } else {
            super.setDirection(resolved)
        }
    }

    override fun getDirection(): DcMotorSimple.Direction {
        return device?.direction ?: _direction
    }


    override fun getDeviceName() = _deviceName
    override fun getVersion() = _version
    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer() = _manufacturer

    override fun setPower(power: Double) {
        _power = power
        if (device != null) {
            device.power = power
        } else {
            super.setPower(power)
        }
    }

    override fun close() { device?.close() }

    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}