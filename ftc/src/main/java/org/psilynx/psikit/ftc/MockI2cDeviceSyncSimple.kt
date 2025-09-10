package org.psilynx.psikit.ftc

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.HardwareDeviceHealth
import com.qualcomm.robotcore.hardware.I2cAddr
import com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple
import com.qualcomm.robotcore.hardware.I2cWaitControl
import com.qualcomm.robotcore.hardware.TimestampedData

class MockI2cDeviceSyncSimple : I2cDeviceSynchSimple {
        override fun read8() = (0).toByte()
        override fun read8(ireg: Int) = (0).toByte()
        override fun read(creg: Int) = arrayOf((0).toByte()).toByteArray()
        override fun read(creg: Int, ireg: Int) = arrayOf((0).toByte()).toByteArray()
        override fun readTimeStamped(creg: Int) = TimestampedData()
        override fun readTimeStamped( ireg: Int, creg: Int ) = TimestampedData()
        override fun write8(bVal: Int) { }
        override fun write8(ireg: Int, bVal: Int) { }
        override fun write(data: ByteArray?) { }
        override fun write(ireg: Int, data: ByteArray?) { }
        override fun write8( bVal: Int, waitControl: I2cWaitControl? ) { }
        override fun write8( ireg: Int, bVal: Int, waitControl: I2cWaitControl? ) { }
        override fun write( data: ByteArray?, waitControl: I2cWaitControl? ) { }
        override fun write( ireg: Int, data: ByteArray?, waitControl: I2cWaitControl? ) { }
        override fun waitForWriteCompletions(waitControl: I2cWaitControl?) { }
        override fun enableWriteCoalescing(enable: Boolean) { }
        override fun isWriteCoalescingEnabled() = true
        override fun isArmed() = true
        override fun setI2cAddr(i2cAddr: I2cAddr?) { }
        override fun getI2cAddr() = I2cAddr(0)
        override fun setLogging(enabled: Boolean) { }
        override fun getLogging() = true
        override fun setLoggingTag(loggingTag: String?) { }
        override fun getLoggingTag() = ""
        override fun getManufacturer() = HardwareDevice.Manufacturer.Other
        override fun getDeviceName() = ""
        override fun getConnectionInfo() = ""
        override fun getVersion() = 0
        override fun resetDeviceConfigurationForOpMode() { }
        override fun close() { }
        override fun setHealthStatus(status: HardwareDeviceHealth.HealthStatus?) { }
        override fun getHealthStatus() = HardwareDeviceHealth.HealthStatus.UNKNOWN
        override fun setI2cAddress(newAddress: I2cAddr?) { }
        override fun getI2cAddress() = I2cAddr(0)
        override fun setUserConfiguredName(name: String?) { }
        override fun getUserConfiguredName() = ""
}