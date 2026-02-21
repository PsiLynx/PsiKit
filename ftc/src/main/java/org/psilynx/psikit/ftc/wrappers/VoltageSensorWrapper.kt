package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.VoltageSensor
import org.psilynx.psikit.ftc.FtcLogTuning
import org.psilynx.psikit.core.LogTable

class VoltageSensorWrapper(
    private val device: VoltageSensor?
) : VoltageSensor, HardwareInput<VoltageSensor> {

    private var _voltage = 0.0
    private var _sampledThisLoop = false
    private var _deviceName = "MockVoltageSensor"
    private var _version = 1
    private var _connectionInfo = ""
    private var _manufacturer = HardwareDevice.Manufacturer.Other

    override fun new(wrapped: VoltageSensor?) = VoltageSensorWrapper(wrapped)

    override fun toLog(table: LogTable) {
        val d = device

        if (d != null) {
            _deviceName = d.deviceName
            _version = d.version
            _connectionInfo = d.connectionInfo
            _manufacturer = d.manufacturer
        }

        if (FtcLogTuning.bulkOnlyLogging) {
            if (_sampledThisLoop) {
                table.put("voltage", _voltage)
            }
            table.put("voltage/sampled", _sampledThisLoop)
            _sampledThisLoop = false
            return
        }

        if (d != null) {
            _voltage = d.voltage
            _sampledThisLoop = true
        }

        if (_sampledThisLoop) {
            table.put("voltage", _voltage)
        }
        table.put("voltage/sampled", _sampledThisLoop)
        table.put("deviceName", deviceName)
        table.put("version", version)
        table.put("connectionInfo", connectionInfo)
        table.put("manufacturer", manufacturer)
        _sampledThisLoop = false


    }

    override fun fromLog(table: LogTable) {
        _voltage = table.get("voltage", 0.0)
        _deviceName = table.get("deviceName", "MockVoltageSensor")
        _version = table.get("version", 1)
        _connectionInfo = table.get("connectionInfo", "")
        _manufacturer = table.get("manufacturer", HardwareDevice.Manufacturer.Other)
        _sampledThisLoop = table.get("voltage/sampled", false)
    }

    override fun getVoltage(): Double {
        val d = device
        if (d != null) {
            _voltage = d.voltage
            _sampledThisLoop = true
        }
        return _voltage
    }

    override fun getDeviceName() = _deviceName
    override fun getVersion() = _version
    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer() = _manufacturer

    override fun close() { device?.close() }
    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}

