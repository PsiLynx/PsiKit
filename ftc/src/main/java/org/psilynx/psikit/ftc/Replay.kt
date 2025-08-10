package org.psilynx.psikit.ftc

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
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
import org.psilynx.psikit.ftc.inputs.PinpointInput
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
                GoBildaPinpointDriver::class.java -> PinpointInput(null)

                DigitalChannel::class.java -> DigitalChannelData.empty.device
                RevTouchSensor::class.java -> DigitalChannelData.empty.device
                TouchSensor::class.java    -> DigitalChannelData.empty.device

                AnalogInput::class.java    -> AnalogInputData.empty.device

                CRServoImplEx::class.java  -> CrServoData.empty.device
                CRServoImpl::class.java    -> CrServoData.empty.device
                CRServo::class.java        -> CrServoData.empty.device

                ServoImplEx::class.java    -> ServoData.empty.device
                ServoImpl::class.java      -> ServoData.empty.device
                Servo::class.java          -> ServoData.empty.device

                DcMotorImplEx::class.java  -> MotorData.empty.device
                DcMotorSimple::class.java  -> MotorData.empty.device
                DcMotorImpl::class.java    -> MotorData.empty.device
                DcMotor::class.java        -> MotorData.empty.device

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