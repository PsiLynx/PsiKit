package org.psilynx.psikit.ftc.test.fakehardware

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.HardwareDeviceHealth
import com.qualcomm.robotcore.hardware.I2cAddr
import com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple
import com.qualcomm.robotcore.hardware.I2cWaitControl
import com.qualcomm.robotcore.hardware.TimestampedData

class FakeI2cDeviceSynchSimple: I2cDeviceSynchSimple {

    override fun getManufacturer(): HardwareDevice.Manufacturer {
        TODO("Not yet implemented")
    }

    override fun getDeviceName() = ""
    override fun getConnectionInfo() = ""
    override fun getVersion() = 0
    override fun resetDeviceConfigurationForOpMode() { }
    override fun close() { }
    override fun setHealthStatus(p0: HardwareDeviceHealth.HealthStatus?) { }

    override fun getHealthStatus(): HardwareDeviceHealth.HealthStatus {
        TODO("Not yet implemented")
    }

    override fun getI2cAddress(): I2cAddr {
        TODO("Not yet implemented")
    }

    override fun setI2cAddress(p0: I2cAddr?) { }
    override fun setUserConfiguredName(p0: String?) { }

    override fun getUserConfiguredName() = ""
    override fun read8(): Byte {
        TODO("Not yet implemented")
    }

    override fun read8(p0: Int): Byte {
        TODO("Not yet implemented")
    }

    override fun read(p0: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun read(p0: Int, p1: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun readTimeStamped(p0: Int): TimestampedData {
        TODO("Not yet implemented")
    }

    override fun readTimeStamped(p0: Int, p1: Int): TimestampedData {
        TODO("Not yet implemented")
    }

    override fun write8(p0: Int) {
    }

    override fun write8(p0: Int, p1: Int) {
    }

    override fun write8(p0: Int, p1: I2cWaitControl?) {

    }

    override fun write8(p0: Int, p1: Int, p2: I2cWaitControl?) {

    }

    override fun write(p0: ByteArray?) {

    }

    override fun write(p0: Int, p1: ByteArray?) {

    }

    override fun write(p0: ByteArray?, p1: I2cWaitControl?) {

    }

    override fun write(p0: Int, p1: ByteArray?, p2: I2cWaitControl?) {

    }

    override fun waitForWriteCompletions(p0: I2cWaitControl?) {

    }

    override fun enableWriteCoalescing(p0: Boolean) {

    }

    override fun isWriteCoalescingEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isArmed(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setI2cAddr(p0: I2cAddr?) {
        TODO("Not yet implemented")
    }

    override fun getI2cAddr(): I2cAddr {
        TODO("Not yet implemented")
    }

    override fun setLogging(p0: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getLogging(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setLoggingTag(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun getLoggingTag(): String {
        TODO("Not yet implemented")
    }
}