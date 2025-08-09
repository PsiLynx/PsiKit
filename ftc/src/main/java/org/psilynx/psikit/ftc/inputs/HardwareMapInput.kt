package org.psilynx.psikit.ftc.inputs

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.ServoImplEx
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.ftc.structs.AnalogInputData
import org.psilynx.psikit.ftc.structs.CrServoData
import org.psilynx.psikit.ftc.structs.DigitalChannelData
import org.psilynx.psikit.ftc.structs.MotorData
import org.psilynx.psikit.ftc.structs.ServoData

class HardwareMapInput(
    val hardwareMap: HardwareMap
): LoggableInputs, HardwareMap(
    hardwareMap.appContext,
    null
){
    /*
     * map of HardwareDevice classes to Inputs that wrap them. users should not
     * have to use this directly unless they are using an i2c device that
     * doesn't have support yet, in which case, they should look at the
     * PinpointInput as an example.
     */
    val i2cDeviceInputs = mapOf<Class<*>, Class<I2cInput<*>>>(
        GoBildaPinpointDriver::class.java
            to (PinpointInput::class.java as Class<I2cInput<*>>)
    )
    private val devicesToProcess = mutableListOf<LoggableInputs>()

    override fun <T : Any?> get(
        classOrInterface: Class<out T?>?,
        deviceName: String?
    ): T? {
        val device = hardwareMap.get<T>(classOrInterface, deviceName)

        val i2cDevice = (
            i2cDeviceInputs[classOrInterface as Class<out HardwareDevice>]
            ?.getConstructor(classOrInterface)
            ?.newInstance(device)
        )

        println("[PsiKit] hardwaremap call on $classOrInterface, got i2c " +
                "device $i2cDevice")
        if (i2cDevice != null) {
            devicesToProcess.add(i2cDevice)
            return i2cDevice as T
        }
        else return device
    }

    override fun toLog(table: LogTable) {
        hardwareMap.dcMotor.entrySet().forEach { (name, motor) ->
            table.put("Motors/$name", MotorData(motor as DcMotorImplEx))
        }
        hardwareMap.servo.entrySet().forEach { (name, motor) ->
            table.put("Servos/$name", ServoData(motor as ServoImplEx))
        }
        hardwareMap.crservo.entrySet().forEach { (name, motor) ->
            table.put("CrServos/$name", CrServoData(motor as CRServoImplEx))
        }
        hardwareMap.analogInput.entrySet().forEach { (name, motor) ->
            table.put(
                "AnalogInput/$name",
                AnalogInputData(motor as AnalogInput)
            )
        }
        hardwareMap.digitalChannel.entrySet().forEach { (name, motor) ->
            table.put(
                "DigitalChannel/$name",
                DigitalChannelData(motor as DigitalChannel)
            )
        }
    }

    override fun fromLog(table: LogTable) {
        table.getSubtable("Motors").getAll(true).forEach { (name, _) ->
            val value = table.getSubtable("Motors").get(
                name, MotorData.MotorDataStruct(), MotorData.Companion.empty
            )

            val existing = hardwareMap.dcMotor.entrySet().firstOrNull {
                it.key == name
            }
            if(existing == null) dcMotor.put(name, value.device)
            else (existing as MotorData.Device).thisRef = value
        }
        table.getSubtable("Servos").getAll(true).forEach { (name, _) ->
            val value = table.getSubtable("Servos").get(
                name, ServoData.ServoDataStruct(), ServoData.Companion.empty
            )

            val existing = hardwareMap.servo.entrySet().firstOrNull {
                it.key == name
            }
            if(existing == null) hardwareMap.servo.put(name, value.device)
            else (existing as ServoData.Device).thisRef = value
        }

        table.getSubtable("CrServos").getAll(true).forEach { (name, _) ->
            val value = table.getSubtable("CrServos").get(
                name, CrServoData.CrServoDataStruct(), CrServoData.Companion.empty
            )

            val existing = hardwareMap.crservo.entrySet().firstOrNull {
                it.key == name
            }
            if(existing == null) hardwareMap.crservo.put(name, value.device)
            else (existing as CrServoData.Device).thisRef = value
        }
        table.getSubtable("AnalogInputs").getAll(true).forEach { (name, _) ->
            val value = table.getSubtable("AnalogInputs").get(
                name,
                AnalogInputData.AnalogInputDataStruct(),
                AnalogInputData.Companion.empty
            )
            val existing = hardwareMap.analogInput.entrySet().firstOrNull {
                it.key == name
            }
            if(existing == null) hardwareMap.analogInput.put(name, value.device)
            else (existing as AnalogInputData.Device).thisRef = value
        }
        table.getSubtable("DigitalChannels").getAll(true).forEach { (name, _) ->
            val value = table.getSubtable("DigitalChannels").get(
                name,
                DigitalChannelData.DigitalChannelDataStruct(),
                DigitalChannelData.Companion.empty
            )

            val existing = hardwareMap.digitalChannel.entrySet().firstOrNull {
                it.key == name
            }
            if(existing == null) hardwareMap.digitalChannel.put(name, value.device)
            else (existing as DigitalChannelData.Device).thisRef = value
        }

    }
}