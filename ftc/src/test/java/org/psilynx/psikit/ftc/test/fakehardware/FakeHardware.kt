package org.psilynx.psikit.ftc.test.fakehardware

import com.qualcomm.robotcore.hardware.HardwareDevice

interface FakeHardware:HardwareDevice {
    override fun getManufacturer() = HardwareDevice.Manufacturer.Other
    override fun getDeviceName() = ""
    override fun getConnectionInfo() =  ""
    override fun getVersion() = 0
    override fun close() { }

    fun update(deltaTime: Double)
}