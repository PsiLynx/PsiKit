package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoController
import com.qualcomm.robotcore.hardware.ServoControllerEx
import com.qualcomm.robotcore.hardware.ServoImplEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import org.psilynx.psikit.core.LogTable

class ServoWrapper(private val device: ServoImplEx?):
    ServoImplEx(
        object : ServoControllerEx {
            override fun setServoPwmRange(servo: Int, range: PwmControl.PwmRange) {}
            override fun getServoPwmRange(servo: Int) = PwmControl.PwmRange.defaultRange
            override fun setServoPwmEnable(servo: Int) {}
            override fun setServoPwmDisable(servo: Int) {}
            override fun isServoPwmEnabled(servo: Int) = true
            override fun setServoType(servo: Int, servoType: ServoConfigurationType?) {}
            override fun pwmEnable() {}
            override fun pwmDisable() {}
            override fun getPwmStatus() = ServoController.PwmStatus.ENABLED
            override fun setServoPosition(servo: Int, position: Double) {}
            override fun getServoPosition(servo: Int) = 0.0
            override fun getManufacturer() = HardwareDevice.Manufacturer.Other
            override fun getDeviceName() = "MockCrServo"
            override fun getConnectionInfo() = ""
            override fun getVersion() = 1
            override fun resetDeviceConfigurationForOpMode() {}
            override fun close() {}
        },
        0,
        ServoConfigurationType()
    ), HardwareInput<ServoImplEx> {
    private var _direction = Servo.Direction.FORWARD
    private var _position = 0.0
    private var _pwmRange = PwmControl.PwmRange(500.0, 2500.0)
    private var _pwmEnabled = false;

    override fun getDirection(): Servo.Direction = _direction
    override fun setDirection(direction: Servo.Direction) {
        device?.direction = direction
    }

    override fun getPosition(): Double = _position
    override fun setPosition(position: Double) {
        device?.position = position
    }

    override fun getPwmRange(): PwmControl.PwmRange = _pwmRange
    override fun setPwmRange(range: PwmControl.PwmRange) {
        device?.pwmRange = range
    }

    override fun isPwmEnabled(): Boolean = _pwmEnabled
    override fun setPwmEnable() {
        device?.setPwmEnable()
    }

    override fun setPwmDisable() {
        device?.setPwmDisable()
    }

    override fun toLog(table: LogTable) {
        device!!
        _direction = device.direction
        _position = device.position
        _pwmRange = device.pwmRange
        _pwmEnabled = device.isPwmEnabled

        table.put("Direction", direction.ordinal)
        table.put("Position", position)
        table.put("PwmLower", pwmRange.usPulseLower)
        table.put("PwmUpper", pwmRange.usPulseUpper)
        table.put("PwmEnabled", isPwmEnabled)
    }

    override fun fromLog(table: LogTable) {
        _direction = table.get("Direction", direction)
        _position = table.get("Position", position)
        val lower = table.get("PwmLower", pwmRange.usPulseLower.toDouble())
        val upper = table.get("PwmUpper", pwmRange.usPulseUpper.toDouble())
        _pwmRange = PwmControl.PwmRange(lower, upper)
        _pwmEnabled = table.get("PwmEnabled", isPwmEnabled)
    }

    override fun new(wrapped: ServoImplEx?) = ServoWrapper(wrapped)
}
