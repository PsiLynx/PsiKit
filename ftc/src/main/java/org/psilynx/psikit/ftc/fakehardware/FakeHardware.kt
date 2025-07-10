package org.psilynx.psikit.ftc.fakehardware

import com.qualcomm.robotcore.hardware.HardwareDevice

interface FakeHardware: HardwareDevice {
    override fun getManufacturer() = HardwareDevice.Manufacturer.Other
    override fun getConnectionInfo() =  ""
    override fun getDeviceName() = ""
    override fun getVersion() = 0
    override fun close() { }

    fun update(deltaTime: Double)
}