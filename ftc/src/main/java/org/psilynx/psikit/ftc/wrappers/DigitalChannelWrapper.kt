package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.DigitalChannelController
import com.qualcomm.robotcore.hardware.HardwareDevice
import org.psilynx.psikit.ftc.FtcLogTuning
import org.psilynx.psikit.core.LogTable

class DigitalChannelWrapper(
    private val device: DigitalChannel?
) : DigitalChannel, HardwareInput<DigitalChannel> {

    private var _mode  = DigitalChannel.Mode.INPUT
    private var _state = false
    private var _deviceName = "MockDigitalChannel"
    private var _version = 1
    private var _connectionInfo = ""
    private var _manufacturer   = HardwareDevice.Manufacturer.Other

    override fun new(wrapped: DigitalChannel?) = DigitalChannelWrapper(wrapped)

    override fun toLog(table: LogTable) {
        device!!
        _state         = device.state

        if (FtcLogTuning.bulkOnlyLogging) {
            table.put("state", state)
            return
        }

        _mode          = device.mode
        _deviceName    = device.deviceName
        _version       = device.version
        _connectionInfo = device.connectionInfo
        _manufacturer   = device.manufacturer

        table.put("mode", mode)
        table.put("state", state)
        table.put("deviceName", deviceName)
        table.put("version", version)
        table.put("connectionInfo", connectionInfo)
        table.put("manufacturer", manufacturer)

    }

    override fun fromLog(table: LogTable) {
        _mode           = table.get("mode", DigitalChannel.Mode.INPUT)
        _state          = table.get("state", false)
        _deviceName     = table.get("deviceName", "MockDigitalChannel")
        _version        = table.get("version", 1)
        _connectionInfo = table.get("connectionInfo", "")
        _manufacturer   = table.get("manufacturer", HardwareDevice.Manufacturer.Other)
    }

    override fun getMode() = _mode
    override fun setMode(mode: DigitalChannel.Mode)
        = device?.setMode(mode) ?: Unit

    @Deprecated("Deprecated in Java")
    override fun setMode(mode: DigitalChannelController.Mode) =
        device?.setMode(mode) ?: Unit

    override fun getState() = _state
    override fun setState(state: Boolean) =
        device?.setState(state) ?: Unit

    override fun getDeviceName() = _deviceName
    override fun getVersion() = _version
    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer() = _manufacturer

    override fun close() { device?.close() }
    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}