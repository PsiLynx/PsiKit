package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.HardwareDevice
import org.psilynx.psikit.core.LogTable

class AnalogInputWrapper(
    private val device: AnalogInput?
) : AnalogInput(null, 0), HardwareInput<AnalogInput> {

    private var _connectionInfo = ""
    private var _manufacturer = HardwareDevice.Manufacturer.Other
    private var _maxVoltage = 0.0
    private var _deviceName = ""
    private var _voltage = 0.0
    private var _version = 0

    override fun new(wrapped: AnalogInput) = AnalogInputWrapper(wrapped)

    override fun toLog(table: LogTable) {

        table.put("connectionInfo", connectionInfo)
        table.put("manufacturer", manufacturer)
        table.put("maxVoltage", maxVoltage)
        table.put("deviceName", deviceName)
        table.put("voltage", voltage)
        table.put("version", version)

        _connectionInfo = connectionInfo
        _manufacturer   = manufacturer
        _deviceName     = deviceName
        _maxVoltage     = maxVoltage
        _voltage        = voltage
        _version        = version
    }

    override fun fromLog(table: LogTable) {
        _connectionInfo = table.get("connectionInfo", "")
        _manufacturer   = table.get("manufacturer", HardwareDevice.Manufacturer.Other)
        _maxVoltage     = table.get("maxVoltage", 0.0)
        _deviceName     = table.get("deviceName", "")
        _voltage        = table.get("voltage", 0.0)
        _version        = table.get("version", 0)
    }

    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer()   = _manufacturer
    override fun getMaxVoltage()     = _maxVoltage
    override fun getDeviceName()     = _deviceName
    override fun getVoltage()        = _voltage
    override fun getVersion()        = _version

    override fun close() { device?.close() }

    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}