package org.psilynx.psikit.ftc

import com.qualcomm.hardware.rev.RevTouchSensor
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.CRServoImpl
import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorImpl
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoImpl
import com.qualcomm.robotcore.hardware.ServoImplEx
import com.qualcomm.robotcore.hardware.TouchSensor
import org.psilynx.psikit.core.LogReplaySource
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.structs.AnalogInputData
import org.psilynx.psikit.ftc.structs.CrServoData
import org.psilynx.psikit.ftc.structs.DigitalChannelData
import org.psilynx.psikit.ftc.structs.MotorData
import org.psilynx.psikit.ftc.structs.ServoData

class Replay(val opMode: PsiKitOpMode, val replaySource: LogReplaySource) {
    fun run(){
        opMode.hardwareMap = object : HardwareMap(null, null) {
            override fun <T : Any?> get(
                classOrInterface: Class<out T?>?,
                deviceName: String?
            ) = when(classOrInterface){
                DigitalChannel::class.java -> DigitalChannelData.empty
                RevTouchSensor::class.java -> DigitalChannelData.empty
                TouchSensor::class.java    -> DigitalChannelData.empty

                AnalogInput::class.java    -> AnalogInputData.empty

                CRServoImplEx::class.java  -> CrServoData.empty
                CRServoImpl::class.java    -> CrServoData.empty
                CRServo::class.java        -> CrServoData.empty

                ServoImplEx::class.java    -> ServoData.empty
                ServoImpl::class.java      -> ServoData.empty
                Servo::class.java          -> ServoData.empty

                DcMotorImplEx::class.java  -> MotorData.empty
                DcMotorSimple::class.java  -> MotorData.empty
                DcMotorImpl::class.java    -> MotorData.empty
                DcMotor::class.java        -> MotorData.empty

                else                       -> error(
                    "while replaying the OpMode " +
                    "\"${opMode::class.simpleName}\", it requested a " +
                    "${classOrInterface?.simpleName}, which is a " +
                    "device type that PsiKit doesn't auto log"
                )
            } as T
        }
        Logger.setReplay(true)
        Logger.setReplaySource(replaySource)
        opMode.runOpMode()

    }
}