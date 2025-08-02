package org.psilynx.psikit.ftc

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.AnalogInputController
import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorImpl
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.DigitalChannelController
import com.qualcomm.robotcore.hardware.HardwareDevice.Manufacturer
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoImplEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.core.wpi.Struct
import org.psilynx.psikit.core.wpi.StructSerializable
import org.psilynx.psikit.ftc.structs.AnalogInputData
import org.psilynx.psikit.ftc.structs.CrServoData
import org.psilynx.psikit.ftc.structs.DigitalChannelData
import org.psilynx.psikit.ftc.structs.MotorData
import org.psilynx.psikit.ftc.structs.ServoData
import java.nio.ByteBuffer

class HardwareMapInput(
    val hardwareMap: HardwareMap
): LoggableInputs, HardwareMap(null, OpModeManagerImpl(null, null)){

    override fun toLog(table: LogTable) {
        hardwareMap.dcMotor.entrySet().forEach { (name, motor) ->
            table.put( "Motors/$name", MotorData(motor as DcMotorImplEx) )
        }
        hardwareMap.servo.entrySet().forEach { (name, motor) ->
            table.put( "Servos/$name", ServoData(motor as ServoImplEx) )
        }
        hardwareMap.crservo.entrySet().forEach { (name, motor) ->
            table.put( "CrServos/$name", CrServoData(motor as CRServoImplEx) )
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
                name, MotorData.MotorDataStruct(), MotorData.empty
            )
            dcMotor.put(name, value.device)

        }
        table.getSubtable("Servos").getAll(true).forEach { (name, _) ->
            val value = table.getSubtable("Servos").get(
                name, ServoData.ServoDataStruct(), ServoData.empty
            )
            servo.put(name, value.device)
        }

        table.getSubtable("CrServos").getAll(true).forEach { (name, _) ->
            val value = table.getSubtable("CrServos").get(
                name, CrServoData.CrServoDataStruct(), CrServoData.empty
            )
            crservo.put(name, value.device)
        }
        table.getSubtable("AnalogInputs").getAll(true).forEach { (name, _) ->
            val value = table.getSubtable("AnalogInputs").get(
                name,
                AnalogInputData.AnalogInputDataStruct(),
                AnalogInputData.empty
            )
            analogInput.put(name, value.device)
        }
        table.getSubtable("DigitalChannels").getAll(true).forEach { (name, _) ->
            val value = table.getSubtable("DigitalChannels").get(
                name,
                DigitalChannelData.DigitalChannelDataStruct(),
                DigitalChannelData.empty
            )

            digitalChannel.put(name, value.device)
        }



    }
}