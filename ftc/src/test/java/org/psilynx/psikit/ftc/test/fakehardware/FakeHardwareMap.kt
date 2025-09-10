package org.psilynx.psikit.ftc.test.fakehardware

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.IMU
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.TouchSensor
import com.qualcomm.robotcore.hardware.VoltageSensor
import org.psilynx.psikit.ftc.GoBildaPinpointDriver

object FakeHardwareMap : JVMHardwareMap() {
    override var deviceTypes:
            MutableMap<Class<out HardwareDevice>, (String) -> HardwareDevice> =
        mutableMapOf(
            IMU::class.java to { FakeIMU() },
            Servo::class.java to { FakeServo() },
            DcMotor::class.java to { FakeMotor() },
            CRServo::class.java to { FakeCRServo() },
            AnalogInput::class.java to { FakeAnalogInput() },
            TouchSensor::class.java to { FakeTouchSensor() },
            LynxModule::class.java to { FakeLynxModule(true) },
            VoltageSensor::class.java to { FakeVoltageSensor() },
            GoBildaPinpointDriver::class.java to { FakePinpoint() },
        )

}
