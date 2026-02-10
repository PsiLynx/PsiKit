package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.DistanceSensor
import com.qualcomm.robotcore.hardware.ColorSensor
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.I2cAddr
import com.qualcomm.robotcore.hardware.NormalizedRGBA
import com.qualcomm.robotcore.hardware.NormalizedColorSensor
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.psilynx.psikit.ftc.FtcLogTuning
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.Logger

/**
 * Generic wrapper for devices that implement [NormalizedColorSensor] and/or [DistanceSensor].
 *
 * This is primarily intended for combined sensors like RevColorSensorV3, where user code may
 * request the device via an interface type.
 */
class ColorDistanceSensorWrapper(
    private val device: HardwareDevice?
) : HardwareInput<HardwareDevice>, NormalizedColorSensor, DistanceSensor, ColorSensor {

    private var _connectionInfo: String = ""
    private var _manufacturer: HardwareDevice.Manufacturer = HardwareDevice.Manufacturer.Other
    private var _deviceName: String = ""
    private var _version: Int = 0

    private var _normalized: NormalizedRGBA = NormalizedRGBA()
    private var _gain: Float = 0.0f
    private var _distanceMeters: Double = 0.0
    private var _colorSampledThisLoop: Boolean = false

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

    /**
     * Optional: if set, PsiKit will only read color when the distance is <= this threshold.
     * This can significantly reduce I2C time in loops where nothing is near the sensor.
     *
     * Note: distance is always sampled (when supported), because it's typically used for gating.
     */
    var colorReadDistanceMetersThreshold: Double? = null

    override fun new(wrapped: HardwareDevice?) = ColorDistanceSensorWrapper(wrapped)

    override fun toLog(table: LogTable) {
        if (!FtcLogTuning.processColorDistanceSensorsInBackground) {
            // When disabled, avoid background I2C reads entirely.
            // User code can still read on-demand via passthrough methods.
            return
        }

        if (!shouldSampleNow()) {
            // Skip reads/writes this loop; LogTable retains the last values.
            return
        }
        lastSampleNs = System.nanoTime()

        val d = device

        if (d != null) {
            _connectionInfo = d.connectionInfo
            _manufacturer = d.manufacturer
            _deviceName = d.deviceName
            _version = d.version
        }

        val distDevice = d as? DistanceSensor
        if (distDevice != null) {
            _distanceMeters = try {
                distDevice.getDistance(DistanceUnit.METER)
            } catch (_: Throwable) {
                0.0
            }
        }

        val threshold = colorReadDistanceMetersThreshold
        val shouldReadColor = threshold == null || _distanceMeters <= threshold

        val colorDevice = d as? NormalizedColorSensor
        if (colorDevice != null && shouldReadColor) {
            _colorSampledThisLoop = true
            _normalized = try {
                colorDevice.normalizedColors
            } catch (_: Throwable) {
                NormalizedRGBA()
            }
            _gain = try {
                colorDevice.gain
            } catch (_: Throwable) {
                0.0f
            }
        } else {
            _colorSampledThisLoop = false
            _normalized = NormalizedRGBA()
        }

        table.put("connectionInfo", _connectionInfo)
        table.put("manufacturer", _manufacturer)
        table.put("deviceName", _deviceName)
        table.put("version", _version)

        table.put("color/red", _normalized.red.toDouble())
        table.put("color/green", _normalized.green.toDouble())
        table.put("color/blue", _normalized.blue.toDouble())
        table.put("color/alpha", _normalized.alpha.toDouble())
        table.put("color/gain", _gain.toDouble())
        table.put("color/sampled", _colorSampledThisLoop)
        table.put("color/distanceThresholdMeters", threshold ?: -1.0)

        table.put("distance/meters", _distanceMeters)
    }

    override fun fromLog(table: LogTable) {
        _connectionInfo = table.get("connectionInfo", "")
        _manufacturer = table.get("manufacturer", HardwareDevice.Manufacturer.Other)
        _deviceName = table.get("deviceName", "")
        _version = table.get("version", 0)

        _normalized = NormalizedRGBA().apply {
            red = table.get("color/red", 0.0).toFloat()
            green = table.get("color/green", 0.0).toFloat()
            blue = table.get("color/blue", 0.0).toFloat()
            alpha = table.get("color/alpha", 0.0).toFloat()
        }
        _gain = table.get("color/gain", 0.0).toFloat()
        _colorSampledThisLoop = table.get("color/sampled", false)
        _distanceMeters = table.get("distance/meters", 0.0)
    }

    override fun getNormalizedColors(): NormalizedRGBA {
        if (!Logger.isReplay() && !FtcLogTuning.processColorDistanceSensorsInBackground) {
            val d = device as? NormalizedColorSensor
            if (d != null) {
                return try {
                    d.normalizedColors
                } catch (_: Throwable) {
                    NormalizedRGBA()
                }
            }
        }
        return _normalized
    }

    // ColorSensor compatibility: scale normalized [0..1] into [0..255] like typical SDK sensors.
    override fun red(): Int = (_normalized.red * 255.0f).toInt().coerceIn(0, 255)
    override fun green(): Int = (_normalized.green * 255.0f).toInt().coerceIn(0, 255)
    override fun blue(): Int = (_normalized.blue * 255.0f).toInt().coerceIn(0, 255)
    override fun alpha(): Int = (_normalized.alpha * 255.0f).toInt().coerceIn(0, 255)

    override fun argb(): Int {
        val a = alpha()
        val r = red()
        val g = green()
        val b = blue()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun enableLed(enable: Boolean) {
        val color = device as? ColorSensor
        color?.enableLed(enable)
    }

    override fun setI2cAddress(newAddress: I2cAddr?) {
        val color = device as? ColorSensor
        color?.setI2cAddress(newAddress)
    }

    override fun getI2cAddress(): I2cAddr {
        val color = device as? ColorSensor
        return color?.i2cAddress ?: I2cAddr(0)
    }

    override fun getGain(): Float = _gain

    override fun setGain(gain: Float) {
        _gain = gain
        try {
            (device as? NormalizedColorSensor)?.gain = gain
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun getDistance(unit: DistanceUnit): Double {
        if (!Logger.isReplay() && !FtcLogTuning.processColorDistanceSensorsInBackground) {
            val d = device as? DistanceSensor
            if (d != null) {
                return try {
                    d.getDistance(unit)
                } catch (_: Throwable) {
                    0.0
                }
            }
        }
        return try {
            unit.fromUnit(DistanceUnit.METER, _distanceMeters)
        } catch (_: Throwable) {
            0.0
        }
    }

    override fun getConnectionInfo(): String = _connectionInfo

    override fun getDeviceName(): String = _deviceName

    override fun getManufacturer(): HardwareDevice.Manufacturer = _manufacturer

    override fun getVersion(): Int = _version

    override fun resetDeviceConfigurationForOpMode() {
        try {
            device?.resetDeviceConfigurationForOpMode()
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun close() {
        try {
            device?.close()
        } catch (_: Throwable) {
            // ignore
        }
    }
}
